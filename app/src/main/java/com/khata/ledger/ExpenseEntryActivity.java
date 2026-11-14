package com.khata.ledger;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

/** Add or edit one personal-ledger entry. The category decides debit (expense) or credit (income). */
public class ExpenseEntryActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "expense_id";

    private EditText etDate, etAmount, etNote;
    private Spinner spCategory;
    private TextView btnBack, tvCategoryType, tvPageTitle;
    private Button btnSave, btnCancel;
    private final List<String> catKeys = new ArrayList<>(Util.CATS.keySet());
    private Expense editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expense_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        final DataStore ds = DataStore.get();
        if (!ds.isLoaded()) {
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        etDate = findViewById(R.id.etDate);
        spCategory = findViewById(R.id.spCategory);
        tvCategoryType = findViewById(R.id.tvCategoryType);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        if (getIntent().hasExtra(EXTRA_ID)) {
            double id = getIntent().getDoubleExtra(EXTRA_ID, -1);
            for (Expense e : ds.expenses) {
                if (e.getId() == id) editing = e;
            }
        }

        Util.dateField(this, etDate, editing != null ? editing.getDate() : Util.today());

        List<String> items = new ArrayList<>();
        items.add(getString(R.string.selectPlaceholder));
        for (String k : catKeys) items.add(Util.catLabel(this, k));
        Util.setItems(this, spCategory, items);
        Util.onSelect(spCategory, new Cb<Integer>() {
            @Override
            public void on(Integer pos) {
                updateBadge();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (editing != null) {
            tvPageTitle.setText(R.string.editEntryTitle);
            btnSave.setText(R.string.saveChangesBtn);
            btnCancel.setVisibility(View.VISIBLE);
            spCategory.setSelection(catKeys.indexOf(editing.getCategory()) + 1);
            etAmount.setText(Util.plain(editing.getAmount()));
            etNote.setText(editing.getNote());
        }
        updateBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DataStore.get().isLoaded()) DataStore.get().flush();
    }

    private void updateBadge() {
        int pos = spCategory.getSelectedItemPosition();
        if (pos <= 0) {
            tvCategoryType.setText(R.string.selectCategory);
            tvCategoryType.setTextColor(ContextCompat.getColor(this, R.color.fadedInk));
            return;
        }
        boolean debit = "debit".equals(Util.CATS.get(catKeys.get(pos - 1)));
        tvCategoryType.setText(debit ? R.string.debitArrow : R.string.creditArrow);
        tvCategoryType.setTextColor(ContextCompat.getColor(this, debit ? R.color.debitRed : R.color.creditBlue));
    }

    private void submit() {
        DataStore ds = DataStore.get();
        int pos = spCategory.getSelectedItemPosition();
        double amount = Util.parse(etAmount);
        if (pos <= 0 || amount <= 0) {
            Util.toast(this, getString(R.string.fillRequired));
            return;
        }
        String category = catKeys.get(pos - 1);
        Expense e = editing != null ? editing : new Expense();
        String date = etDate.getText().toString().trim();
        e.setDate(date.isEmpty() ? Util.today() : date);
        e.setCategory(category);
        e.setType(Util.CATS.get(category));
        e.setAmount(amount);
        e.setNote(etNote.getText().toString().trim());
        if (editing == null) ds.expenses.add(e);
        ds.save();
        Util.toast(this, getString(R.string.savedText));
        finish();
    }
}
