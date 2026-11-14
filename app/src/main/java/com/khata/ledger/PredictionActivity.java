package com.khata.ledger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Statistical read of one commodity's price history (priceHistory/{slug}). */
public class PredictionActivity extends AppCompatActivity {
    private Spinner spCommodity;
    private TextView btnBack, tvPrompt, tvVerdict, tvConfidence, tvLiveWeather;
    private TextView[] tvStatLabel = new TextView[3];
    private TextView[] tvStat = new TextView[3];
    private LinearLayout statsRow, verdictBox, weatherCard, weatherRows;

    private String selected = "";
    private final List<DataStore.PriceCommodity> list = new ArrayList<>();

    private boolean dead() {
        return isFinishing() || isDestroyed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prediction);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!DataStore.get().isLoaded()) {
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        spCommodity = findViewById(R.id.spCommodity);
        tvPrompt = findViewById(R.id.tvPrompt);
        statsRow = findViewById(R.id.statsRow);
        verdictBox = findViewById(R.id.verdictBox);
        weatherCard = findViewById(R.id.weatherCard);
        weatherRows = findViewById(R.id.weatherRows);
        tvVerdict = findViewById(R.id.tvVerdict);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvLiveWeather = findViewById(R.id.tvLiveWeather);
        tvStatLabel[0] = findViewById(R.id.tvStat0Label);
        tvStatLabel[1] = findViewById(R.id.tvStat1Label);
        tvStatLabel[2] = findViewById(R.id.tvStat2Label);
        tvStat[0] = findViewById(R.id.tvStat0);
        tvStat[1] = findViewById(R.id.tvStat1);
        tvStat[2] = findViewById(R.id.tvStat2);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Util.setItems(this, spCommodity, Collections.singletonList(getString(R.string.selectPlaceholder)));
        Util.onSelect(spCommodity, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                if (pos <= 0 || pos > list.size()) {
                    selected = "";
                    showPrompt();
                    return;
                }
                String name = list.get(pos - 1).commodity;
                if (!name.equals(selected)) {
                    selected = name;
                    load(name);
                }
            }
        });

        DataStore.get().listPriceCommodities(new Cb<List<DataStore.PriceCommodity>>() {
            @Override
            public void on(List<DataStore.PriceCommodity> res) {
                if (dead()) return;
                list.clear();
                list.addAll(res);
                Collections.sort(list, (x, y) -> x.commodity.compareToIgnoreCase(y.commodity));
                List<String> items = new ArrayList<>();
                items.add(getString(R.string.selectPlaceholder));
                for (DataStore.PriceCommodity c : list) items.add(c.commodity + " (" + c.count + ")");
                Util.setItems(PredictionActivity.this, spCommodity, items);
            }
        });
        fetchWeather();
    }

    private void showPrompt() {
        tvPrompt.setText(R.string.predSelectPrompt);
        tvPrompt.setVisibility(View.VISIBLE);
        statsRow.setVisibility(View.GONE);
        verdictBox.setVisibility(View.GONE);
        weatherCard.setVisibility(View.GONE);
    }

    private void load(final String name) {
        tvPrompt.setText("…");
        tvPrompt.setVisibility(View.VISIBLE);
        statsRow.setVisibility(View.GONE);
        verdictBox.setVisibility(View.GONE);
        weatherCard.setVisibility(View.GONE);
        DataStore.get().getPriceEntries(name, new Cb<List<PricePoint>>() {
            @Override
            public void on(List<PricePoint> entries) {
                if (dead() || !name.equals(selected)) return;
                render(entries == null ? new ArrayList<PricePoint>() : entries);
            }
        });
    }

    private void render(List<PricePoint> entries) {
        Predictor.Result pr = Predictor.predict(entries);
        if (pr == null) {
            tvPrompt.setText(R.string.predNoData);
            return;
        }
        tvPrompt.setVisibility(View.GONE);

        int ink = ContextCompat.getColor(this, R.color.inkBlack);
        int red = ContextCompat.getColor(this, R.color.debitRed);
        int blue = ContextCompat.getColor(this, R.color.creditBlue);

        tvStatLabel[0].setText(getString(R.string.predCurrentAvg).toUpperCase(Locale.ROOT));
        tvStat[0].setText("৳ " + Util.fmt(pr.avgThisMonth));
        tvStat[0].setTextColor(ink);
        tvStatLabel[1].setText((Util.monthName(this, pr.nextMonth) + " " + getString(R.string.predSeasonalLabelSuffix)).toUpperCase(Locale.ROOT));
        tvStat[1].setText(pr.seasonalNextAvg != null ? "৳ " + Util.fmt(pr.seasonalNextAvg) : getString(R.string.predNoSeasonalData));
        tvStat[1].setTextColor(ink);
        tvStatLabel[2].setText(getString(R.string.predTrend).toUpperCase(Locale.ROOT));
        tvStat[2].setText((pr.trendPct >= 0 ? "+" : "") + String.format(Locale.US, "%.1f", pr.trendPct) + "%");
        tvStat[2].setTextColor(pr.trendPct >= 0 ? red : blue);
        statsRow.setVisibility(View.VISIBLE);

        int verdictText;
        int verdictColor;
        if ("rise".equals(pr.verdict)) {
            verdictText = R.string.predVerdictRise;
            verdictColor = red;
        } else if ("fall".equals(pr.verdict)) {
            verdictText = R.string.predVerdictFall;
            verdictColor = blue;
        } else {
            verdictText = R.string.predVerdictStable;
            verdictColor = ink;
        }
        tvVerdict.setText(verdictText);
        tvVerdict.setTextColor(verdictColor);
        int confidenceText = "high".equals(pr.confidence) ? R.string.predConfidenceHigh
                : ("medium".equals(pr.confidence) ? R.string.predConfidenceMedium : R.string.predConfidenceLow);
        tvConfidence.setText(getString(confidenceText) + " — " + getString(R.string.predBasedOn) + " "
                + pr.dataPoints + " " + getString(R.string.recordsWord));
        verdictBox.setVisibility(View.VISIBLE);

        weatherRows.removeAllViews();
        if (pr.weather.isEmpty()) {
            Util.addKv(getLayoutInflater(), weatherRows, getString(R.string.predNoWeatherData), "", ink);
        } else {
            LayoutInflater inflater = getLayoutInflater();
            int faded = ContextCompat.getColor(this, R.color.fadedInk);
            for (Map.Entry<String, double[]> en : pr.weather.entrySet()) {
                Util.addKv(inflater, weatherRows, Util.weatherLabel(this, en.getKey()),
                        "৳ " + Util.fmt(en.getValue()[0]) + " · " + (int) en.getValue()[1] + " " + getString(R.string.recordsWord), faded);
            }
        }
        weatherCard.setVisibility(View.VISIBLE);
    }

    // ---------- live weather (Open-Meteo, no key) — shown for context only ----------
    private void fetchWeather() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String text;
                try {
                    URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=23.8103&longitude=90.4125&current=temperature_2m,precipitation,weather_code&timezone=auto");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject cur = new JSONObject(sb.toString()).getJSONObject("current");
                    long temp = Math.round(cur.getDouble("temperature_2m"));
                    String desc = getString(codeText(cur.getInt("weather_code")));
                    text = desc + ", " + temp + "°C\n" + getString(R.string.predWeatherLocationLabel)
                            + "\n" + getString(R.string.predWeatherContextNote);
                } catch (Exception e) {
                    text = getString(R.string.predWeatherUnavailable);
                }
                final String out = text;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!dead()) tvLiveWeather.setText(out);
                    }
                });
            }
        }).start();
    }

    private static int codeText(int code) {
        switch (code) {
            case 0: return R.string.wCodeClear;
            case 1: return R.string.wCodeMainlyClear;
            case 2: return R.string.wCodePartlyCloudy;
            case 3: return R.string.wCodeOvercast;
            case 45:
            case 48: return R.string.wCodeFog;
            case 51:
            case 53:
            case 55: return R.string.wCodeDrizzle;
            case 61:
            case 63: return R.string.wCodeRain;
            case 65:
            case 82: return R.string.wCodeHeavyRain;
            case 80:
            case 81: return R.string.wCodeRainShowers;
            case 95:
            case 96:
            case 99: return R.string.wCodeThunderstorm;
            default: return R.string.wCodeOvercast;
        }
    }
}
