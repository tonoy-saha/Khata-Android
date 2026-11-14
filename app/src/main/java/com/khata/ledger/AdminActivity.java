package com.khata.ledger;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Admin-only (admins/{uid} must exist). Firestore rules still enforce this server-side. */
public class AdminActivity extends AppCompatActivity {
    private TextView btnBack, tvStatus;
    private Button btnImportCsv, btnAddPoint;
    private EditText etDate, etPrice;
    private AutoCompleteTextView actCommodity;
    private Spinner spWeather;
    private LinearLayout commodityList;

    private final ActivityResultLauncher<String> csvPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) importCsv(uri);
            });

    private boolean dead() {
        return isFinishing() || isDestroyed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DataStore ds = DataStore.get();
        if (!ds.isLoaded() || !ds.isAdmin) {
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        tvStatus = findViewById(R.id.tvStatus);
        btnImportCsv = findViewById(R.id.btnImportCsv);
        btnAddPoint = findViewById(R.id.btnAddPoint);
        etDate = findViewById(R.id.etDate);
        etPrice = findViewById(R.id.etPrice);
        actCommodity = findViewById(R.id.actCommodity);
        spWeather = findViewById(R.id.spWeather);
        commodityList = findViewById(R.id.commodityList);

        Util.dateField(this, etDate, Util.today());
        Util.setItems(this, spWeather, Arrays.asList(
                getString(R.string.weatherNormal), getString(R.string.weatherHeavyRain), getString(R.string.weatherFlood),
                getString(R.string.weatherDrought), getString(R.string.weatherCyclone), getString(R.string.weatherOther)));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnImportCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                csvPicker.launch("*/*");
            }
        });
        btnAddPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPoint();
            }
        });
        loadList();
    }

    private void addPoint() {
        String commodity = actCommodity.getText().toString().trim();
        double price = Util.parse(etPrice);
        if (commodity.isEmpty() || price <= 0) {
            Util.toast(this, getString(R.string.fillRequired));
            return;
        }
        List<PricePoint> one = new ArrayList<>();
        one.add(new PricePoint(etDate.getText().toString().trim(), price,
                Util.WEATHERS[Math.max(0, spWeather.getSelectedItemPosition())]));
        DataStore.get().appendPrice(commodity, one, new Cb<Boolean>() {
            @Override
            public void on(Boolean ok) {
                if (dead()) return;
                if (ok) {
                    Util.toast(AdminActivity.this, getString(R.string.savedText));
                    etPrice.setText("");
                    loadList();
                } else {
                    Util.toast(AdminActivity.this, getString(R.string.adminSaveFailed));
                }
            }
        });
    }

    private void loadList() {
        commodityList.removeAllViews();
        final int faded = ContextCompat.getColor(this, R.color.fadedInk);
        Util.addKv(getLayoutInflater(), commodityList, "…", "", faded);
        DataStore.get().listPriceCommodities(new Cb<List<DataStore.PriceCommodity>>() {
            @Override
            public void on(List<DataStore.PriceCommodity> res) {
                if (dead()) return;
                commodityList.removeAllViews();
                if (res.isEmpty()) {
                    Util.addKv(getLayoutInflater(), commodityList, getString(R.string.adminNone), "", faded);
                    return;
                }
                Collections.sort(res, (x, y) -> x.commodity.compareToIgnoreCase(y.commodity));
                for (DataStore.PriceCommodity c : res) {
                    Util.addKv(getLayoutInflater(), commodityList, c.commodity,
                            c.count + " " + getString(R.string.recordsWord), faded);
                }
            }
        });
    }

    // ---------- CSV import: Date, Commodity, Price, Weather ----------
    private static String[] splitCsv(String line) {
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
            parts[i] = s.trim();
        }
        return parts;
    }

    private void importCsv(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String[]> rows = new ArrayList<>();
                String err = null;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                    String line;
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        if (first) {
                            line = line.replace("﻿", "");
                            first = false;
                        }
                        if (line.trim().isEmpty()) continue;
                        rows.add(splitCsv(line));
                    }
                } catch (Exception e) {
                    err = e.getMessage();
                }
                final String error = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dead()) return;
                        if (error != null) {
                            Util.toast(AdminActivity.this, getString(R.string.adminReadFailed, error));
                            return;
                        }
                        afterParse(rows);
                    }
                });
            }
        }).start();
    }

    private void afterParse(List<String[]> rows) {
        if (rows.size() < 2) {
            Util.toast(this, getString(R.string.adminNoRows));
            return;
        }
        String[] header = rows.get(0);
        int iDate = -1, iCom = -1, iPrice = -1, iWeather = -1;
        for (int i = 0; i < header.length; i++) {
            String h = header[i].toLowerCase();
            if (h.equals("date")) iDate = i;
            else if (h.equals("commodity")) iCom = i;
            else if (h.equals("price")) iPrice = i;
            else if (h.equals("weather")) iWeather = i;
        }
        if (iCom < 0 || iPrice < 0) {
            Util.toast(this, getString(R.string.adminBadColumns));
            return;
        }
        final Map<String, List<PricePoint>> by = new TreeMap<>();
        int skipped = 0, valid = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String commodity = iCom < row.length ? row[iCom] : "";
            double price = 0;
            try {
                price = Double.parseDouble(iPrice < row.length ? row[iPrice] : "");
            } catch (Exception ignored) {
            }
            if (commodity.isEmpty() || price <= 0) {
                skipped++;
                continue;
            }
            String date = (iDate >= 0 && iDate < row.length && !row[iDate].isEmpty()) ? row[iDate] : Util.today();
            if (date.length() > 10) date = date.substring(0, 10);
            String weather = (iWeather >= 0 && iWeather < row.length && !row[iWeather].isEmpty()) ? row[iWeather] : "Normal";
            List<PricePoint> l = by.get(commodity);
            if (l == null) {
                l = new ArrayList<>();
                by.put(commodity, l);
            }
            l.add(new PricePoint(date, price, weather));
            valid++;
        }
        if (valid == 0) {
            Util.toast(this, getString(R.string.adminNoValid));
            return;
        }
        final int skippedF = skipped;
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.adminFound, valid, by.size()))
                .setPositiveButton(R.string.adminReplace, (d, w) -> runImport(by, true, skippedF))
                .setNeutralButton(R.string.adminAdd, (d, w) -> runImport(by, false, skippedF))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void runImport(Map<String, List<PricePoint>> by, boolean replace, final int skipped) {
        DataStore ds = DataStore.get();
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.adminUploading);
        final AtomicInteger left = new AtomicInteger(by.size());
        final AtomicInteger failed = new AtomicInteger(0);
        final int total = by.size();
        for (Map.Entry<String, List<PricePoint>> en : by.entrySet()) {
            Cb<Boolean> done = new Cb<Boolean>() {
                @Override
                public void on(Boolean ok) {
                    if (!ok) failed.incrementAndGet();
                    if (left.decrementAndGet() == 0 && !dead()) {
                        tvStatus.setText(getString(R.string.adminFinished, total - failed.get(), total, skipped, failed.get()));
                        loadList();
                    }
                }
            };
            if (replace) ds.replacePrice(en.getKey(), en.getValue(), done);
            else ds.appendPrice(en.getKey(), en.getValue(), done);
        }
    }
}
