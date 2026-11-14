package com.khata.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PersonalFragment extends Fragment {
    private String search = "";
    private boolean building = false;
    private final List<Expense> shown = new ArrayList<>();

    private TextView tvMonth, tvEmptyExpense;
    private View monthSummary;
    private Button btnAddExpense, btnReport, btnClearExpense;
    private EditText etSearch;
    private RecyclerView rvExpense;
    private ExpenseAdapter expenseAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);
        tvMonth = view.findViewById(R.id.tvMonth);
        monthSummary = view.findViewById(R.id.monthSummary);
        btnAddExpense = view.findViewById(R.id.btnAddExpense);
        btnReport = view.findViewById(R.id.btnReport);
        btnClearExpense = view.findViewById(R.id.btnClearExpense);
        etSearch = view.findViewById(R.id.etSearch);
        rvExpense = view.findViewById(R.id.rvExpense);
        tvEmptyExpense = view.findViewById(R.id.tvEmptyExpense);

        expenseAdapter = new ExpenseAdapter(shown, getContext(), new ExpenseAdapter.Listener() {
            @Override
            public void onEdit(Expense expense) {
                Intent intent = new Intent(getContext(), ExpenseEntryActivity.class);
                intent.putExtra(ExpenseEntryActivity.EXTRA_ID, expense.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Expense expense) {
                deleteExpense(expense);
            }
        });
        rvExpense.setAdapter(expenseAdapter);
        rvExpense.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ExpenseEntryActivity.class));
            }
        });
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ReportActivity.class));
            }
        });
        btnClearExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.confirmClearExpense)
                        .setPositiveButton(R.string.yes, (d, w) -> {
                            DataStore.get().expenses.clear();
                            DataStore.get().save();
                            refresh();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        building = true;
        etSearch.setText(search);
        building = false;
        refresh();
    }

    private void refresh() {
        DataStore ds = DataStore.get();
        String month = Util.today().substring(0, 7);
        String term = search.toLowerCase(Locale.ROOT);

        shown.clear();
        for (Expense e : ds.expenses) {
            if (!e.getDate().startsWith(month)) continue;
            if (!term.isEmpty() && !(e.getCategory().toLowerCase(Locale.ROOT).contains(term)
                    || Util.catLabel(requireContext(), e.getCategory()).toLowerCase(Locale.ROOT).contains(term)
                    || e.getNote().toLowerCase(Locale.ROOT).contains(term))) continue;
            shown.add(e);
        }
        Collections.sort(shown, (x, y) -> y.getDate().compareTo(x.getDate()));
        expenseAdapter.notifyDataSetChanged();
        tvEmptyExpense.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);

        // this-month totals
        double c = 0, d = 0;
        for (Expense e : ds.expenses) {
            if (!e.getDate().startsWith(month)) continue;
            if ("credit".equals(e.getType())) c += e.getAmount();
            else d += e.getAmount();
        }
        tvMonth.setText(Util.monthName(requireContext(), Integer.parseInt(month.substring(5))) + " " + month.substring(0, 4));
        Util.bindSummary(monthSummary, getString(R.string.expTotalCredit), getString(R.string.expTotalDebit),
                getString(R.string.expTotalNet), c, d, c - d);
    }

    private void deleteExpense(final Expense expense) {
        final DataStore ds = DataStore.get();
        final int idx = ds.expenses.indexOf(expense);
        if (idx < 0) return;
        ds.expenses.remove(idx);
        ds.save();
        refresh();
        Snackbar.make(requireView(), R.string.deletedText, Snackbar.LENGTH_LONG)
                .setAction(R.string.undoBtn, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ds.expenses.add(Math.min(idx, ds.expenses.size()), expense);
                        ds.save();
                        refresh();
                    }
                }).show();
    }
}
