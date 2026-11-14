package com.khata.ledger;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Same Firestore layout as the web app:
 *   users/{uid}/data/app   -> entries, stockEntries, expenseEntries, partyContacts, shopName, updatedAt
 *   priceHistory/{slug}    -> commodity, entries[{date, price, weather}]
 *   admins/{uid}           -> existence = admin
 */
public class DataStore {
    private static DataStore inst;

    public static DataStore get() {
        if (inst == null) inst = new DataStore();
        return inst;
    }

    public static void reset() { inst = null; }

    public boolean isLoaded() {
        return loaded;
    }

    public final List<Ledger> entries = new ArrayList<>();
    public final List<Stock> stock = new ArrayList<>();
    public final List<Expense> expenses = new ArrayList<>();
    public final Map<String, String> partyContacts = new HashMap<>();
    public String shopName = "";
    public boolean isAdmin = false;
    public Cb<String> onSyncError;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;
    private boolean loaded = false;
    private boolean dirty = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pushRun = new Runnable() {
        @Override public void run() { pushNow(); }
    };

    private DocumentReference docRef() {
        return db.collection("users").document(uid).collection("data").document("app");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                if (x instanceof Map) out.add((Map<String, Object>) x);
            }
        }
        return out;
    }

    public void load(String uid, final Cb<Boolean> cb) {
        this.uid = uid;
        loaded = false;
        docRef().get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { cb.on(false); return; }
            DocumentSnapshot s = task.getResult();
            entries.clear();
            stock.clear();
            expenses.clear();
            partyContacts.clear();
            shopName = "";
            if (s != null && s.exists()) {
                for (Map<String, Object> m : maps(s.get("entries"))) entries.add(Ledger.fromMap(m));
                for (Map<String, Object> m : maps(s.get("stockEntries"))) stock.add(Stock.fromMap(m));
                for (Map<String, Object> m : maps(s.get("expenseEntries"))) expenses.add(Expense.fromMap(m));
                Object pc = s.get("partyContacts");
                if (pc instanceof Map) {
                    for (Map.Entry<?, ?> en : ((Map<?, ?>) pc).entrySet()) {
                        partyContacts.put(Util.str(en.getKey()), Util.str(en.getValue()));
                    }
                }
                shopName = Util.str(s.get("shopName"));
            }
            loaded = true;
            cb.on(true);
        });
    }

    /** Debounced save of everything (same behaviour as the web app's pushToCloud). */
    public void save() {
        if (!loaded) return;
        dirty = true;
        handler.removeCallbacks(pushRun);
        handler.postDelayed(pushRun, 600);
    }

    public void flush() {
        if (dirty) {
            handler.removeCallbacks(pushRun);
            pushNow();
        }
    }

    private void pushNow() {
        dirty = false;
        if (!loaded || uid == null) return;
        Map<String, Object> m = new HashMap<>();
        List<Map<String, Object>> a = new ArrayList<>();
        for (Ledger e : entries) a.add(e.toMap());
        List<Map<String, Object>> b = new ArrayList<>();
        for (Stock e : stock) b.add(e.toMap());
        List<Map<String, Object>> c = new ArrayList<>();
        for (Expense e : expenses) c.add(e.toMap());
        m.put("entries", a);
        m.put("stockEntries", b);
        m.put("expenseEntries", c);
        m.put("partyContacts", new HashMap<>(partyContacts));
        m.put("shopName", shopName == null ? "" : shopName);
        m.put("updatedAt", FieldValue.serverTimestamp());
        docRef().set(m).addOnFailureListener(e -> {
            Log.e("Khata", "Cloud sync failed", e);
            if (onSyncError != null) onSyncError.on("Cloud sync failed: " + e.getMessage());
        });
    }

    public void checkAdmin(final Cb<Boolean> cb) {
        db.collection("admins").document(uid).get().addOnCompleteListener(t ->
                cb.on(t.isSuccessful() && t.getResult() != null && t.getResult().exists()));
    }

    // ---------------- priceHistory ----------------

    public static class PriceCommodity {
        public String slug, commodity;
        public int count;
    }

    public static String slug(String name) {
        return name.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private DocumentReference priceDoc(String commodity) {
        return db.collection("priceHistory").document(slug(commodity));
    }

    public void listPriceCommodities(final Cb<List<PriceCommodity>> cb) {
        db.collection("priceHistory").get().addOnCompleteListener(t -> {
            List<PriceCommodity> out = new ArrayList<>();
            if (t.isSuccessful() && t.getResult() != null) {
                for (DocumentSnapshot d : t.getResult().getDocuments()) {
                    PriceCommodity p = new PriceCommodity();
                    p.slug = d.getId();
                    String name = Util.str(d.get("commodity"));
                    p.commodity = name.isEmpty() ? d.getId() : name;
                    p.count = maps(d.get("entries")).size();
                    out.add(p);
                }
            }
            cb.on(out);
        });
    }

    /** Calls back with null if the read failed (so callers never overwrite data by mistake). */
    public void getPriceEntries(String commodity, final Cb<List<PricePoint>> cb) {
        priceDoc(commodity).get().addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { cb.on(null); return; }
            List<PricePoint> out = new ArrayList<>();
            DocumentSnapshot d = t.getResult();
            if (d != null && d.exists()) {
                for (Map<String, Object> m : maps(d.get("entries"))) out.add(PricePoint.fromMap(m));
            }
            cb.on(out);
        });
    }

    public void appendPrice(final String commodity, final List<PricePoint> add, final Cb<Boolean> cb) {
        getPriceEntries(commodity, existing -> {
            if (existing == null) { cb.on(false); return; }
            List<Map<String, Object>> all = new ArrayList<>();
            for (PricePoint p : existing) all.add(p.toMap());
            for (PricePoint p : add) all.add(p.toMap());
            Map<String, Object> m = new HashMap<>();
            m.put("commodity", commodity.trim());
            m.put("entries", all);
            priceDoc(commodity).set(m, SetOptions.merge()).addOnCompleteListener(t -> cb.on(t.isSuccessful()));
        });
    }

    public void replacePrice(String commodity, List<PricePoint> list, final Cb<Boolean> cb) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (PricePoint p : list) all.add(p.toMap());
        Map<String, Object> m = new HashMap<>();
        m.put("commodity", commodity.trim());
        m.put("entries", all);
        priceDoc(commodity).set(m).addOnCompleteListener(t -> cb.on(t.isSuccessful()));
    }
}
