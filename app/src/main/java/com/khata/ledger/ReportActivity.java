package com.khata.ledger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Month-end report for the personal ledger: income vs expense and a breakdown by category. */
public class ReportActivity extends AppCompatActivity {
    private Spinner spMonth;
    private View reportSummary;
    private LinearLayout breakdownBox, detailBox;
    private Button btnToggle;
    private TextView btnBack;

    private String reportMonth = Util.today().substring(0, 7);
    private boolean detailOpen = false;
    private boolean building = false;
    private final List<String> months = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
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
        spMonth = findViewById(R.id.spMonth);
        reportSummary = findViewById(R.id.reportSummary);
        breakdownBox = findViewById(R.id.breakdownBox);
        detailBox = findViewById(R.id.detailBox);
        btnToggle = findViewById(R.id.btnToggle);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detailOpen = !detailOpen;
                refresh();
            }
        });

        TreeSet<String> set = new TreeSet<>(Collections.reverseOrder());
        set.add(Util.today().substring(0, 7));
        for (Expense e : DataStore.get().expenses) {
            if (e.getDate().length() >= 7) set.add(e.getDate().substring(0, 7));
        }
        months.addAll(set);
        building = true;
        Util.setItems(this, spMonth, months);
        spMonth.setSelection(Math.max(months.indexOf(reportMonth), 0), false);
        building = false;
        Util.onSelect(spMonth, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                if (building || pos >= months.size()) return;
                String m = months.get(pos);
                if (!m.equals(reportMonth)) {
                    reportMonth = m;
                    detailOpen = false;
                    refresh();
                }
            }
        });
        refresh();
    }

    private void refresh() {
        DataStore ds = DataStore.get();
        List<Expense> inMonth = new ArrayList<>();
        for (Expense e : ds.expenses) {
            if (e.getDate().startsWith(reportMonth)) inMonth.add(e);
        }

        double credit = 0, debit = 0;
        Map<String, double[]> byCat = new LinkedHashMap<>();
        Map<String, String> catType = new LinkedHashMap<>();
        for (Expense e : inMonth) {
            if ("credit".equals(e.getType())) credit += e.getAmount();
            else debit += e.getAmount();
            double[] t = byCat.get(e.getCategory());
            if (t == null) {
                t = new double[]{0};
                byCat.put(e.getCategory(), t);
                catType.put(e.getCategory(), e.getType());
            }
            t[0] += e.getAmount();
        }
        Util.bindSummary(reportSummary, getString(R.string.reportCredit), getString(R.string.reportDebit),
                getString(R.string.reportNet), credit, debit, credit - debit);

        int red = ContextCompat.getColor(this, R.color.debitRed);
        int blue = ContextCompat.getColor(this, R.color.creditBlue);
        int faded = ContextCompat.getColor(this, R.color.fadedInk);
        LayoutInflater inflater = getLayoutInflater();

        breakdownBox.removeAllViews();
        if (byCat.isEmpty()) {
            Util.addKv(inflater, breakdownBox, getString(R.string.noExpenseThisMonth), "", faded);
        } else {
            List<Map.Entry<String, double[]>> sorted = new ArrayList<>(byCat.entrySet());
            Collections.sort(sorted, (x, y) -> Double.compare(y.getValue()[0], x.getValue()[0]));
            for (Map.Entry<String, double[]> en : sorted) {
                boolean debitCat = "debit".equals(catType.get(en.getKey()));
                Util.addKv(inflater, breakdownBox, Util.catLabel(this, en.getKey()),
                        Util.fmt(en.getValue()[0]), debitCat ? red : blue);
            }
        }

        btnToggle.setVisibility(inMonth.isEmpty() ? View.GONE : View.VISIBLE);
        btnToggle.setText(detailOpen ? R.string.showLessEntries : R.string.showMoreEntries);
        detailBox.removeAllViews();
        if (detailOpen) {
            Collections.sort(inMonth, (x, y) -> x.getDate().compareTo(y.getDate()));
            for (Expense e : inMonth) {
                boolean debitEntry = "debit".equals(e.getType());
                String label = e.getDate() + "  ·  " + Util.catLabel(this, e.getCategory())
                        + (e.getNote().isEmpty() ? "" : "  ·  " + e.getNote());
                Util.addKv(inflater, detailBox, label, Util.fmt(e.getAmount()), debitEntry ? red : blue);
            }
        }
    }
}
