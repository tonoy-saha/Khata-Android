package com.khata.ledger;

import java.util.HashMap;
import java.util.Map;

/** A personal-ledger entry (users/{uid}/data/app -> expenseEntries). */
public class Expense {
    private double id = Util.newId();
    private String date = "";
    private String category = "";
    private String type = "debit";
    private double amount = 0;
    private String note = "";

    public Expense() {
    }

    public double getId() {
        return id;
    }

    public void setId(double id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /** Field names match the website's Firestore documents exactly. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("date", date);
        m.put("category", category);
        m.put("type", type);
        m.put("amount", amount);
        m.put("note", note);
        return m;
    }

    public static Expense fromMap(Map<String, Object> m) {
        Expense e = new Expense();
        e.id = Util.num(m.get("id"), Util.newId());
        e.date = Util.date10(m.get("date"));
        e.category = Util.str(m.get("category"));
        e.type = "credit".equals(Util.str(m.get("type"))) ? "credit" : "debit";
        e.amount = Util.num(m.get("amount"), 0);
        e.note = Util.str(m.get("note"));
        return e;
    }
}
