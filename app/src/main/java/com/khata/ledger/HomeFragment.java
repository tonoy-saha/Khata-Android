package com.khata.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private View summary;
    private TextView tvNoOverdue;
    private LinearLayout overdueList;
    private Button btnNewEntry, btnPrediction;

    private static class Overdue {
        String party;
        double balance;
        long days;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        summary = view.findViewById(R.id.summary);
        tvNoOverdue = view.findViewById(R.id.tvNoOverdue);
        overdueList = view.findViewById(R.id.overdueList);
        btnNewEntry = view.findViewById(R.id.btnNewEntry);
        btnPrediction = view.findViewById(R.id.btnPrediction);

        btnNewEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EntryActivity.class));
            }
        });
        btnPrediction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PredictionActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        DataStore ds = DataStore.get();
        double credit = 0, debit = 0;
        for (Ledger e : ds.entries) {
            if ("credit".equals(e.getType())) credit += e.getAmount();
            else debit += e.getAmount();
        }
        Util.bindSummary(summary, getString(R.string.homeCreditLabel), getString(R.string.homeDebitLabel),
                getString(R.string.homeNetLabel), credit, debit, credit - debit);

        overdueList.removeAllViews();
        List<Overdue> list = overdue(ds.entries);
        tvNoOverdue.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        int red = ContextCompat.getColor(requireContext(), R.color.debitRed);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final Overdue o : list) {
            View row = Util.addKv(inflater, overdueList,
                    o.party + "  ·  " + o.days + " " + getString(R.string.daysOverdue), Util.fmt(o.balance), red);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) requireActivity()).showLedgerFor(o.party);
                }
            });
        }
    }

    /** Same rule as the website: positive (debit - credit) balance and last entry 30+ days ago. */
    private static List<Overdue> overdue(List<Ledger> entries) {
        Map<String, double[]> bal = new HashMap<>();
        Map<String, String> last = new HashMap<>();
        for (Ledger e : entries) {
            double[] b = bal.get(e.getParty());
            if (b == null) {
                b = new double[]{0};
                bal.put(e.getParty(), b);
            }
            b[0] += "debit".equals(e.getType()) ? e.getAmount() : -e.getAmount();
            String l = last.get(e.getParty());
            if (l == null || e.getDate().compareTo(l) > 0) last.put(e.getParty(), e.getDate());
        }
        List<Overdue> out = new ArrayList<>();
        for (Map.Entry<String, double[]> en : bal.entrySet()) {
            Overdue o = new Overdue();
            o.party = en.getKey();
            o.balance = en.getValue()[0];
            o.days = Util.daysSince(last.get(en.getKey()));
            if (o.balance > 0 && o.days >= 30) out.add(o);
        }
        Collections.sort(out, new Comparator<Overdue>() {
            @Override
            public int compare(Overdue x, Overdue y) {
                return Long.compare(y.days, x.days);
            }
        });
        return out;
    }
}
