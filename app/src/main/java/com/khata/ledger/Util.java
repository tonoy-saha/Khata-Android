package com.khata.ledger;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Small helpers shared by every screen. */
public class Util {

    /** Personal-ledger categories: name (stored in Firestore) -> "credit" / "debit". Order = dropdown order. */
    public static final LinkedHashMap<String, String> CATS = new LinkedHashMap<>();

    static {
        CATS.put("Salary", "credit");
        CATS.put("Savings", "debit");
        CATS.put("Rent", "debit");
        CATS.put("Electricity / Utility", "debit");
        CATS.put("Transport", "debit");
        CATS.put("Fuel Cost", "debit");
        CATS.put("Meal Cost", "debit");
        CATS.put("Mobile Recharge", "debit");
        CATS.put("Festival Cost", "debit");
        CATS.put("Groceries", "debit");
        CATS.put("Education / Tuition", "debit");
        CATS.put("Medical / Health", "debit");
        CATS.put("Labor / Wages", "debit");
        CATS.put("Loading / Unloading", "debit");
        CATS.put("Shop Maintenance", "debit");
        CATS.put("Other Expense", "debit");
        CATS.put("Extra Income", "credit");
        CATS.put("Refund Received", "credit");
        CATS.put("Other Income", "credit");
    }

    public static final String[] UNITS = {"kg", "piece", "gram", "others"};
    public static final String[] WEATHERS = {"Normal", "Heavy Rain", "Flood", "Drought", "Cyclone", "Other"};

    public static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public static double num(Object o, double def) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    public static double newId() {
        return System.currentTimeMillis() + Math.random();
    }

    public static String date10(Object o) {
        String d = str(o);
        return d.length() > 10 ? d.substring(0, 10) : d;
    }

    public static String fmt(double n) {
        return String.format(Locale.US, "%,.2f", n);
    }

    /** Number without trailing zeros, for filling edit boxes. */
    public static String plain(double n) {
        if (n == Math.rint(n) && Math.abs(n) < 1e15) return String.valueOf((long) n);
        return new BigDecimal(n).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static int monthOf(String d) {
        try {
            return Integer.parseInt(d.substring(5, 7));
        } catch (Exception e) {
            return 0;
        }
    }

    public static long daysSince(String d) {
        try {
            Date x = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d.substring(0, 10));
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return Math.round((c.getTimeInMillis() - x.getTime()) / 86400000.0);
        } catch (Exception e) {
            return 0;
        }
    }

    public static double parse(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    /** Category name stored in Firestore -> label in the current language. */
    public static String catLabel(Context c, String name) {
        String[] labels = c.getResources().getStringArray(R.array.categories);
        int i = 0;
        for (String k : CATS.keySet()) {
            if (k.equals(name)) return labels[i];
            i++;
        }
        return name;
    }

    public static String monthName(Context c, int m) {
        if (m < 1 || m > 12) return "";
        return c.getResources().getStringArray(R.array.months)[m - 1];
    }

    public static String unitLabel(Context c, String unit) {
        switch (unit) {
            case "kg": return c.getString(R.string.unitKg);
            case "piece": return c.getString(R.string.unitPiece);
            case "gram": return c.getString(R.string.unitGram);
            default: return c.getString(R.string.unitOthers);
        }
    }

    public static String weatherLabel(Context c, String w) {
        switch (w) {
            case "Normal": return c.getString(R.string.weatherNormal);
            case "Heavy Rain": return c.getString(R.string.weatherHeavyRain);
            case "Flood": return c.getString(R.string.weatherFlood);
            case "Drought": return c.getString(R.string.weatherDrought);
            case "Cyclone": return c.getString(R.string.weatherCyclone);
            case "Other": return c.getString(R.string.weatherOther);
            default: return w;
        }
    }

    /** Makes an EditText open a date picker and hold a yyyy-MM-dd value. */
    public static void dateField(final Context c, final EditText e, String initial) {
        e.setFocusable(false);
        e.setText(initial);
        e.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                Calendar cal = Calendar.getInstance();
                try {
                    Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(e.getText().toString());
                    if (d != null) cal.setTime(d);
                } catch (Exception ignored) {
                }
                new DatePickerDialog(c, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int y, int m, int d) {
                        e.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    public static void watch(EditText e, final Runnable r) {
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                r.run();
            }
        });
    }

    /** Fills one view_summary block (three cells: credit / debit / net). */
    public static void bindSummary(View root, String l1, String l2, String l3, double credit, double debit, double net) {
        ((TextView) root.findViewById(R.id.tvSumCreditLabel)).setText(l1);
        ((TextView) root.findViewById(R.id.tvSumDebitLabel)).setText(l2);
        ((TextView) root.findViewById(R.id.tvSumNetLabel)).setText(l3);
        ((TextView) root.findViewById(R.id.tvSumCredit)).setText(fmt(credit));
        ((TextView) root.findViewById(R.id.tvSumDebit)).setText(fmt(debit));
        TextView tvNet = root.findViewById(R.id.tvSumNet);
        tvNet.setText(fmt(net));
        tvNet.setTextColor(androidx.core.content.ContextCompat.getColor(root.getContext(), net < 0 ? R.color.debitRed : R.color.inkBlack));
    }

    /** Spinner with plain text rows. */
    public static void setItems(Context c, Spinner spinner, java.util.List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public static void onSelect(Spinner spinner, final Cb<Integer> cb) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cb.on(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public static void autoComplete(Context c, android.widget.AutoCompleteTextView view, java.util.List<String> items) {
        view.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_dropdown_item_1line, items));
    }

    /** Adds a label/value row (item_kv) to a container and returns it. */
    public static View addKv(android.view.LayoutInflater inflater, android.view.ViewGroup parent, String label, String value, int color) {
        View row = inflater.inflate(R.layout.item_kv, parent, false);
        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        TextView v = row.findViewById(R.id.tvValue);
        v.setText(value);
        v.setTextColor(color);
        parent.addView(row);
        return row;
    }
}
