package com.khata.ledger;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Adds one buy/sell record to the stock list. */
public class StockEntryActivity extends AppCompatActivity {
    private EditText etDate, etBuy, etSell, etQty, etNote;
    private AutoCompleteTextView actCommodity;
    private Button btnSave;
    private TextView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stock_entry);
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
        etDate = findViewById(R.id.etDate);
        actCommodity = findViewById(R.id.actCommodity);
        etBuy = findViewById(R.id.etBuy);
        etSell = findViewById(R.id.etSell);
        etQty = findViewById(R.id.etQty);
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);

        Util.dateField(this, etDate, Util.today());
        TreeSet<String> commodities = new TreeSet<>();
        for (Stock s : ds.stock) {
            if (!s.getCommodity().isEmpty()) commodities.add(s.getCommodity());
        }
        Util.autoComplete(this, actCommodity, new ArrayList<>(commodities));

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DataStore.get().isLoaded()) DataStore.get().flush();
    }

    private void submit() {
        DataStore ds = DataStore.get();
        String commodity = actCommodity.getText().toString().trim();
        double buy = Util.parse(etBuy);
        double qty = Util.parse(etQty);
        if (commodity.isEmpty() || buy <= 0 || qty <= 0) {
            Util.toast(this, getString(R.string.fillRequired));
            return;
        }
        Stock s = new Stock();
        String date = etDate.getText().toString().trim();
        s.setDate(date.isEmpty() ? Util.today() : date);
        s.setCommodity(commodity);
        s.setBuyPrice(buy);
        s.setQuantity(qty);
        s.setSellPrice(etSell.getText().toString().trim().isEmpty() ? null : Util.parse(etSell));
        s.setNote(etNote.getText().toString().trim());
        ds.stock.add(s);
        ds.save();

        // Silently feed the buy price into priceHistory (same as the website); failures are ignored.
        List<PricePoint> one = new ArrayList<>();
        one.add(new PricePoint(s.getDate(), buy, "Normal"));
        ds.appendPrice(commodity, one, new Cb<Boolean>() {
            @Override
            public void on(Boolean ok) {
            }
        });

        Util.toast(this, getString(R.string.savedText));
        finish();
    }
}
