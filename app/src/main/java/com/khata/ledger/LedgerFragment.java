package com.khata.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public class LedgerFragment extends Fragment {
    public static final String ARG_PARTY = "party";
    private static final String ALL = "__all__";

    private String partyFilter = ALL;
    private String commodityFilter = ALL;
    private String search = "";

    private final List<Ledger> shown = new ArrayList<>();
    private final Map<Double, Double> balances = new HashMap<>();
    private final List<String> partyItems = new ArrayList<>();
    private final List<String> commodityItems = new ArrayList<>();

    private EditText etSearch;
    private Spinner spParty, spCommodity;
    private LinearLayout stmtCard;
    private TextView tvStmtParty, tvStmtPhone, tvEmptyLedger, tvMonth;
    private View stmtSummary, monthSummary;
    private RecyclerView rvLedger;
    private Button btnAddEntry, btnClearAll;
    private LedgerAdapter ledgerAdapter;
    private boolean building = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ledger, container, false);
        etSearch = view.findViewById(R.id.etSearch);
        spParty = view.findViewById(R.id.spParty);
        spCommodity = view.findViewById(R.id.spCommodity);
        stmtCard = view.findViewById(R.id.stmtCard);
        tvStmtParty = view.findViewById(R.id.tvStmtParty);
        tvStmtPhone = view.findViewById(R.id.tvStmtPhone);
        stmtSummary = view.findViewById(R.id.stmtSummary);
        monthSummary = view.findViewById(R.id.monthSummary);
        tvMonth = view.findViewById(R.id.tvMonth);
        tvEmptyLedger = view.findViewById(R.id.tvEmptyLedger);
        rvLedger = view.findViewById(R.id.rvLedger);
        btnAddEntry = view.findViewById(R.id.btnAddEntry);
        btnClearAll = view.findViewById(R.id.btnClearAll);

        if (getArguments() != null && getArguments().getString(ARG_PARTY) != null) {
            partyFilter = getArguments().getString(ARG_PARTY);
        }

        ledgerAdapter = new LedgerAdapter(shown, balances, getContext(), new LedgerAdapter.Listener() {
            @Override
            public void onEdit(Ledger entry) {
                Intent intent = new Intent(getContext(), EntryActivity.class);
                intent.putExtra(EntryActivity.EXTRA_ID, entry.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Ledger entry) {
                deleteEntry(entry);
            }
        });
        rvLedger.setAdapter(ledgerAdapter);
        rvLedger.setLayoutManager(new LinearLayoutManager(getContext()));

        Util.watch(etSearch, new Runnable() {
            @Override
            public void run() {
                String s = etSearch.getText().toString();
                if (!building && !s.equals(search)) {
                    search = s;
                    refresh();
                }
            }
        });
        Util.onSelect(spParty, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                if (building || pos >= partyItems.size()) return;
                String v = pos == 0 ? ALL : partyItems.get(pos);
                if (!v.equals(partyFilter)) {
                    partyFilter = v;
                    refresh();
                }
            }
        });
        Util.onSelect(spCommodity, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                if (building || pos >= commodityItems.size()) return;
                String v = pos == 0 ? ALL : commodityItems.get(pos);
                if (!v.equals(commodityFilter)) {
                    commodityFilter = v;
                    refresh();
                }
            }
        });

        btnAddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EntryActivity.class));
            }
        });
        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.confirmClearLedger)
                        .setPositiveButton(R.string.yes, (d, w) -> {
                            DataStore.get().entries.clear();
                            DataStore.get().save();
                            partyFilter = ALL;
                            commodityFilter = ALL;
                            refreshAll();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
    }

    /** Rebuilds the two filter spinners (new parties may have been added) and the list. */
    private void refreshAll() {
        DataStore ds = DataStore.get();
        TreeSet<String> parties = new TreeSet<>();
        TreeSet<String> commodities = new TreeSet<>();
        for (Ledger e : ds.entries) {
            if (!e.getParty().isEmpty()) parties.add(e.getParty());
            if (!e.getCommodity().isEmpty()) commodities.add(e.getCommodity());
        }
        for (Stock s : ds.stock) {
            if (!s.getCommodity().isEmpty()) commodities.add(s.getCommodity());
        }

        building = true;
        partyItems.clear();
        partyItems.add(getString(R.string.allParties));
        partyItems.addAll(parties);
        commodityItems.clear();
        commodityItems.add(getString(R.string.allCommodities));
        commodityItems.addAll(commodities);
        Util.setItems(requireContext(), spParty, partyItems);
        Util.setItems(requireContext(), spCommodity, commodityItems);
        int pi = partyItems.indexOf(partyFilter);
        int ci = commodityItems.indexOf(commodityFilter);
        if (pi < 0) partyFilter = ALL;
        if (ci < 0) commodityFilter = ALL;
        spParty.setSelection(Math.max(pi, 0), false);
        spCommodity.setSelection(Math.max(ci, 0), false);
        etSearch.setText(search);
        building = false;

        refresh();
    }

    private void refresh() {
        DataStore ds = DataStore.get();
        String term = search.toLowerCase(Locale.ROOT);
        List<Ledger> base = new ArrayList<>();
        for (Ledger e : ds.entries) {
            if (!ALL.equals(partyFilter) && !e.getParty().equals(partyFilter)) continue;
            if (!ALL.equals(commodityFilter) && !e.getCommodity().equals(commodityFilter)) continue;
            if (!term.isEmpty() && !(e.getParty().toLowerCase(Locale.ROOT).contains(term)
                    || e.getNote().toLowerCase(Locale.ROOT).contains(term)
                    || e.getCommodity().toLowerCase(Locale.ROOT).contains(term))) continue;
            base.add(e);
        }

        // running balance is computed oldest -> newest, the list shows newest first
        List<Ledger> chrono = new ArrayList<>(base);
        Collections.sort(chrono, (x, y) -> {
            int c = x.getDate().compareTo(y.getDate());
            return c != 0 ? c : Double.compare(x.getId(), y.getId());
        });
        balances.clear();
        double run = 0;
        for (Ledger e : chrono) {
            run += "credit".equals(e.getType()) ? e.getAmount() : -e.getAmount();
            balances.put(e.getId(), run);
        }
        shown.clear();
        for (int i = chrono.size() - 1; i >= 0; i--) shown.add(chrono.get(i));
        ledgerAdapter.notifyDataSetChanged();
        rvLedger.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyLedger.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);

        // party statement
        if (!ALL.equals(partyFilter)) {
            double c = 0, d = 0;
            for (Ledger e : base) {
                if ("credit".equals(e.getType())) c += e.getAmount();
                else d += e.getAmount();
            }
            tvStmtParty.setText(partyFilter);
            String ph = ds.partyContacts.get(partyFilter);
            tvStmtPhone.setText(ph == null ? "" : ph);
            tvStmtPhone.setVisibility(ph == null || ph.isEmpty() ? View.GONE : View.VISIBLE);
            Util.bindSummary(stmtSummary, getString(R.string.totalCredit), getString(R.string.totalDebit),
                    getString(R.string.psBalance), c, d, c - d);
            stmtCard.setVisibility(View.VISIBLE);
        } else {
            stmtCard.setVisibility(View.GONE);
        }

        // this-month totals (all entries, like the website)
        String month = Util.today().substring(0, 7);
        double c = 0, d = 0;
        for (Ledger e : ds.entries) {
            if (!e.getDate().startsWith(month)) continue;
            if ("credit".equals(e.getType())) c += e.getAmount();
            else d += e.getAmount();
        }
        tvMonth.setText(Util.monthName(requireContext(), Integer.parseInt(month.substring(5))) + " " + month.substring(0, 4));
        Util.bindSummary(monthSummary, getString(R.string.totalCredit), getString(R.string.totalDebit),
                getString(R.string.netBalance), c, d, c - d);
    }

    private void deleteEntry(final Ledger entry) {
        final DataStore ds = DataStore.get();
        final int idx = ds.entries.indexOf(entry);
        if (idx < 0) return;
        ds.entries.remove(idx);
        ds.save();
        refreshAll();
        Snackbar.make(requireView(), R.string.deletedText, Snackbar.LENGTH_LONG)
                .setAction(R.string.undoBtn, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ds.entries.add(Math.min(idx, ds.entries.size()), entry);
                        ds.save();
                        refreshAll();
                    }
                }).show();
    }
}
