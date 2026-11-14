package com.khata.ledger;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Arrays;
import java.util.TreeSet;

/** New / edit a customer transaction. Pass EXTRA_ID to edit an existing entry. */
public class EntryActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "entry_id";

    private EditText etDate, etPhone, etQty, etRate, etNote;
    private AutoCompleteTextView actParty, actCommodity;
    private Spinner spUnit;
    private Button btnCredit, btnDebit, btnSave, btnCancel;
    private TextView btnBack, tvTotal, tvPageTitle, tvPageSub, tvReceiptEmpty;
    private LinearLayout receiptPanel, receiptRows;

    private Ledger editing;
    private Ledger lastReceipt;
    private String type = "credit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entry);
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
        tvPageSub = findViewById(R.id.tvPageSub);
        etDate = findViewById(R.id.etDate);
        actParty = findViewById(R.id.actParty);
        etPhone = findViewById(R.id.etPhone);
        actCommodity = findViewById(R.id.actCommodity);
        etQty = findViewById(R.id.etQty);
        spUnit = findViewById(R.id.spUnit);
        etRate = findViewById(R.id.etRate);
        btnCredit = findViewById(R.id.btnCredit);
        btnDebit = findViewById(R.id.btnDebit);
        etNote = findViewById(R.id.etNote);
        tvTotal = findViewById(R.id.tvTotal);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        receiptPanel = findViewById(R.id.receiptPanel);
        receiptRows = findViewById(R.id.receiptRows);
        tvReceiptEmpty = findViewById(R.id.tvReceiptEmpty);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // find the entry being edited (ids are doubles, same as the website)
        if (getIntent().hasExtra(EXTRA_ID)) {
            double id = getIntent().getDoubleExtra(EXTRA_ID, -1);
            for (Ledger e : ds.entries) {
                if (e.getId() == id) editing = e;
            }
        }

        Util.dateField(this, etDate, editing != null ? editing.getDate() : Util.today());

        TreeSet<String> parties = new TreeSet<>();
        TreeSet<String> commodities = new TreeSet<>();
        for (Ledger e : ds.entries) {
            if (!e.getParty().isEmpty()) parties.add(e.getParty());
            if (!e.getCommodity().isEmpty()) commodities.add(e.getCommodity());
        }
        for (Stock s : ds.stock) {
            if (!s.getCommodity().isEmpty()) commodities.add(s.getCommodity());
        }
        Util.autoComplete(this, actParty, new ArrayList<>(parties));
        Util.autoComplete(this, actCommodity, new ArrayList<>(commodities));
        actParty.setOnItemClickListener((parent, view, pos, id) -> {
            String contact = ds.partyContacts.get(actParty.getText().toString());
            if (contact != null && etPhone.getText().length() == 0) etPhone.setText(contact);
        });

        Util.setItems(this, spUnit, Arrays.asList(
                getString(R.string.unitKg), getString(R.string.unitPiece),
                getString(R.string.unitGram), getString(R.string.unitOthers)));

        btnCredit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = "credit";
                paintType();
            }
        });
        btnDebit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = "debit";
                paintType();
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
            type = editing.getType();
            tvPageTitle.setText(R.string.editEntryTitle);
            tvPageSub.setText(R.string.editEntrySub);
            btnSave.setText(R.string.saveChangesBtn);
            btnCancel.setVisibility(View.VISIBLE);
            receiptPanel.setVisibility(View.GONE);

            actParty.setText(editing.getParty());
            actParty.dismissDropDown();
            String phone = !editing.getPhone().isEmpty() ? editing.getPhone() : ds.partyContacts.get(editing.getParty());
            etPhone.setText(phone == null ? "" : phone);
            actCommodity.setText(editing.getCommodity());
            actCommodity.dismissDropDown();
            etQty.setText(Util.plain(editing.getQuantity()));
            etRate.setText(Util.plain(editing.getRate()));
            etNote.setText(editing.getNote());
            int ui = Arrays.asList(Util.UNITS).indexOf(editing.getUnit());
            spUnit.setSelection(ui < 0 ? 3 : ui);
        } else {
            tvPageSub.setText(R.string.newEntrySub);
            renderReceipt();
        }

        Util.watch(etQty, new Runnable() {
            @Override
            public void run() {
                updateTotal();
            }
        });
        Util.watch(etRate, new Runnable() {
            @Override
            public void run() {
                updateTotal();
            }
        });
        paintType();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DataStore.get().isLoaded()) DataStore.get().flush();
    }

    private void paintType() {
        style(btnCredit, "credit".equals(type), R.color.creditBlue);
        style(btnDebit, "debit".equals(type), R.color.debitRed);
        updateTotal();
    }

    private void style(Button b, boolean on, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        b.setBackgroundTintList(ColorStateList.valueOf(on ? color : ContextCompat.getColor(this, R.color.paperDark)));
        b.setTextColor(on ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.fadedInk));
    }

    private void updateTotal() {
        double t = Util.parse(etQty) * Util.parse(etRate);
        tvTotal.setText("৳ " + Util.fmt(t));
        tvTotal.setTextColor(ContextCompat.getColor(this, "debit".equals(type) ? R.color.debitRed : R.color.coverGreen));
    }

    private void submit() {
        DataStore ds = DataStore.get();
        String party = actParty.getText().toString().trim();
        double qty = Util.parse(etQty);
        double rate = Util.parse(etRate);
        if (party.isEmpty() || qty <= 0 || rate <= 0) {
            Util.toast(this, getString(R.string.fillRequired));
            return;
        }
        String phone = etPhone.getText().toString().trim();
        if (!phone.isEmpty()) ds.partyContacts.put(party, phone);

        Ledger e = editing != null ? editing : new Ledger();
        String date = etDate.getText().toString().trim();
        e.setDate(date.isEmpty() ? Util.today() : date);
        e.setParty(party);
        e.setPhone(phone);
        e.setCommodity(actCommodity.getText().toString().trim());
        e.setQuantity(qty);
        e.setUnit(Util.UNITS[Math.max(0, spUnit.getSelectedItemPosition())]);
        e.setRate(rate);
        e.setAmount(qty * rate);
        e.setType(type);
        e.setNote(etNote.getText().toString().trim());

        if (editing == null) ds.entries.add(e);
        ds.save();

        if (editing != null) {
            finish();
            return;
        }
        lastReceipt = e;
        Util.toast(this, getString(R.string.savedText));
        // clear the form for the next entry, keep date and type, show the bill
        actParty.setText("");
        etPhone.setText("");
        actCommodity.setText("");
        etQty.setText("");
        etRate.setText("");
        etNote.setText("");
        actParty.dismissDropDown();
        actCommodity.dismissDropDown();
        renderReceipt();
    }

    /** The "last entry" bill, so the shop can confirm the numbers before handing over cash. */
    private void renderReceipt() {
        receiptRows.removeAllViews();
        Ledger e = lastReceipt;
        if (e == null) {
            tvReceiptEmpty.setVisibility(View.VISIBLE);
            receiptRows.setVisibility(View.GONE);
            return;
        }
        tvReceiptEmpty.setVisibility(View.GONE);
        receiptRows.setVisibility(View.VISIBLE);

        android.view.LayoutInflater inflater = getLayoutInflater();
        boolean credit = "credit".equals(e.getType());
        int blue = ContextCompat.getColor(this, R.color.creditBlue);
        int red = ContextCompat.getColor(this, R.color.debitRed);
        int ink = ContextCompat.getColor(this, R.color.inkBlack);
        Util.addKv(inflater, receiptRows, e.getParty(), getString(credit ? R.string.creditBtn : R.string.debitBtn), credit ? blue : red);
        Util.addKv(inflater, receiptRows, getString(R.string.receiptDate), e.getDate(), ink);
        if (!e.getPhone().isEmpty()) Util.addKv(inflater, receiptRows, getString(R.string.receiptPhone), e.getPhone(), ink);
        if (!e.getCommodity().isEmpty()) Util.addKv(inflater, receiptRows, getString(R.string.receiptCommodity), e.getCommodity(), ink);
        Util.addKv(inflater, receiptRows, getString(R.string.receiptQty), Util.fmt(e.getQuantity()) + " " + Util.unitLabel(this, e.getUnit()), ink);
        Util.addKv(inflater, receiptRows, getString(R.string.receiptRate), "৳ " + Util.fmt(e.getRate()), ink);
        Util.addKv(inflater, receiptRows, getString(R.string.receiptTotal), "৳ " + Util.fmt(e.getAmount()), credit ? blue : red);
    }
}
