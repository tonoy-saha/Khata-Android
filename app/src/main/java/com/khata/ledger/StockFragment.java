package com.khata.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public class StockFragment extends Fragment {
    private static final String ALL = "__all__";

    private String filter = ALL;
    private String analysisCommodity = "";
    private boolean building = false;

    private final List<Stock> shown = new ArrayList<>();
    private final List<String> filterItems = new ArrayList<>();
    private final List<String> analysisItems = new ArrayList<>();

    private Button btnAddStock, btnPrediction, btnClearStock, btnCheckSignal;
    private Spinner spStockFilter, spSeasonal;
    private RecyclerView rvStock;
    private TextView tvEmptyStock, tvSeasonalHint, tvSignal;
    private LinearLayout seasonalBox;
    private EditText etSignalPrice;
    private StockAdapter stockAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stock, container, false);
        btnAddStock = view.findViewById(R.id.btnAddStock);
        btnPrediction = view.findViewById(R.id.btnPrediction);
        btnClearStock = view.findViewById(R.id.btnClearStock);
        btnCheckSignal = view.findViewById(R.id.btnCheckSignal);
        spStockFilter = view.findViewById(R.id.spStockFilter);
        spSeasonal = view.findViewById(R.id.spSeasonal);
        rvStock = view.findViewById(R.id.rvStock);
        tvEmptyStock = view.findViewById(R.id.tvEmptyStock);
        tvSeasonalHint = view.findViewById(R.id.tvSeasonalHint);
        tvSignal = view.findViewById(R.id.tvSignal);
        seasonalBox = view.findViewById(R.id.seasonalBox);
        etSignalPrice = view.findViewById(R.id.etSignalPrice);

        stockAdapter = new StockAdapter(shown, getContext(), new StockAdapter.Listener() {
            @Override
            public void onDelete(Stock stock) {
                deleteStock(stock);
            }
        });
        rvStock.setAdapter(stockAdapter);
        rvStock.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAddStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), StockEntryActivity.class));
            }
        });
        btnPrediction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PredictionActivity.class));
            }
        });
        btnClearStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.confirmClearStock)
                        .setPositiveButton(R.string.yes, (d, w) -> {
                            DataStore.get().stock.clear();
                            DataStore.get().save();
                            filter = ALL;
                            analysisCommodity = "";
                            refreshAll();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        btnCheckSignal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSignal(Util.parse(etSignalPrice));
            }
        });

        Util.onSelect(spStockFilter, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                if (building || pos >= filterItems.size()) return;
                String v = pos == 0 ? ALL : filterItems.get(pos);
                if (!v.equals(filter)) {
                    filter = v;
                    refreshList();
                }
            }
        });
        Util.onSelect(spSeasonal, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                if (building || pos >= analysisItems.size()) return;
                String v = pos == 0 ? "" : analysisItems.get(pos);
                if (!v.equals(analysisCommodity)) {
                    analysisCommodity = v;
                    renderSeasonal();
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
    }

    private void refreshAll() {
        TreeSet<String> set = new TreeSet<>();
        for (Stock s : DataStore.get().stock) {
            if (!s.getCommodity().isEmpty()) set.add(s.getCommodity());
        }
        building = true;
        filterItems.clear();
        filterItems.add(getString(R.string.allCommodities));
        filterItems.addAll(set);
        analysisItems.clear();
        analysisItems.add(getString(R.string.selectPlaceholder));
        analysisItems.addAll(set);
        Util.setItems(requireContext(), spStockFilter, filterItems);
        Util.setItems(requireContext(), spSeasonal, analysisItems);
        int fi = filterItems.indexOf(filter);
        int ai = analysisItems.indexOf(analysisCommodity);
        if (fi < 0) filter = ALL;
        if (ai < 0) analysisCommodity = "";
        spStockFilter.setSelection(Math.max(fi, 0), false);
        spSeasonal.setSelection(Math.max(ai, 0), false);
        building = false;

        refreshList();
        renderSeasonal();
    }

    private void refreshList() {
        shown.clear();
        for (Stock s : DataStore.get().stock) {
            if (ALL.equals(filter) || s.getCommodity().equals(filter)) shown.add(s);
        }
        Collections.sort(shown, (x, y) -> y.getDate().compareTo(x.getDate()));
        stockAdapter.notifyDataSetChanged();
        tvEmptyStock.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void deleteStock(final Stock stock) {
        final DataStore ds = DataStore.get();
        final int idx = ds.stock.indexOf(stock);
        if (idx < 0) return;
        ds.stock.remove(idx);
        ds.save();
        refreshAll();
        Snackbar.make(requireView(), R.string.deletedText, Snackbar.LENGTH_LONG)
                .setAction(R.string.undoBtn, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ds.stock.add(Math.min(idx, ds.stock.size()), stock);
                        ds.save();
                        refreshAll();
                    }
                }).show();
    }

    /** month -> {avg of buy price, count} for one commodity, from the user's own stock entries. */
    private Map<Integer, double[]> seasonalFor(String commodity) {
        List<PricePoint> pts = new ArrayList<>();
        for (Stock s : DataStore.get().stock) {
            if (s.getCommodity().equals(commodity)) {
                pts.add(new PricePoint(s.getDate(), s.getBuyPrice(), "Normal"));
            }
        }
        return Predictor.seasonal(pts);
    }

    private void renderSeasonal() {
        seasonalBox.removeAllViews();
        if (analysisCommodity.isEmpty()) {
            tvSeasonalHint.setText(R.string.selectCommodityAbove);
            tvSeasonalHint.setVisibility(View.VISIBLE);
            return;
        }
        Map<Integer, double[]> seas = seasonalFor(analysisCommodity);
        if (seas.isEmpty()) {
            tvSeasonalHint.setText(R.string.noDataCommodity);
            tvSeasonalHint.setVisibility(View.VISIBLE);
            return;
        }
        tvSeasonalHint.setVisibility(View.GONE);
        double max = 0;
        for (double[] v : seas.values()) max = Math.max(max, v[0]);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int m = 1; m <= 12; m++) {
            double[] v = seas.get(m);
            if (v == null) continue;
            View row = inflater.inflate(R.layout.item_bar, seasonalBox, false);
            ((TextView) row.findViewById(R.id.tvBarMonth)).setText(Util.monthName(requireContext(), m));
            ((TextView) row.findViewById(R.id.tvBarAvg)).setText(Util.fmt(v[0]));
            ProgressBar bar = row.findViewById(R.id.barValue);
            bar.setProgress(max > 0 ? (int) Math.max(2, Math.round(v[0] / max * 100)) : 2);
            seasonalBox.addView(row);
        }
    }

    private void checkSignal(double price) {
        if (analysisCommodity.isEmpty()) {
            Util.toast(requireContext(), getString(R.string.selectCommodityFirst));
            return;
        }
        if (price <= 0) {
            Util.toast(requireContext(), getString(R.string.enterValidPrice));
            return;
        }
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        String monthLabel = Util.monthName(requireContext(), month);
        double[] data = seasonalFor(analysisCommodity).get(month);
        tvSignal.setVisibility(View.VISIBLE);
        if (data == null) {
            tvSignal.setText(getString(R.string.noDataForCommodityMonth)
                    .replace("{c}", analysisCommodity).replace("{m}", monthLabel));
            tvSignal.setTextColor(ContextCompat.getColor(requireContext(), R.color.inkBlack));
            return;
        }
        double diff = (price - data[0]) / data[0] * 100;
        String verdict;
        int color;
        if (diff <= -10) {
            verdict = getString(R.string.verdictBuy);
            color = R.color.creditBlue;
        } else if (diff >= 10) {
            verdict = getString(R.string.verdictSell);
            color = R.color.debitRed;
        } else {
            verdict = getString(R.string.verdictNeutral);
            color = R.color.inkBlack;
        }
        tvSignal.setText(analysisCommodity + " — " + monthLabel + ": " + getString(R.string.historicalAvg) + " = " + Util.fmt(data[0])
                + " (" + (int) data[1] + " " + getString(R.string.recordsWord) + "), " + getString(R.string.todaysPrice) + " = " + Util.fmt(price)
                + " (" + (diff >= 0 ? "+" : "") + String.format(Locale.US, "%.1f", diff) + "%)\n" + verdict);
        tvSignal.setTextColor(ContextCompat.getColor(requireContext(), color));
    }
}
