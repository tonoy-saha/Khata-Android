package com.khata.ledger;

import java.util.HashMap;
import java.util.Map;

/** A customer / party transaction (users/{uid}/data/app -> entries). */
public class Ledger {
    private double id = Util.newId();
    private String date = "";
    private String party = "";
    private String phone = "";
    private String commodity = "";
    private double quantity = 1;
    private String unit = "kg";
    private double rate = 0;
    private double amount = 0;
    private String type = "credit";
    private String note = "";

    public Ledger() {
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

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        m.put("party", party);
        m.put("phone", phone);
        m.put("commodity", commodity);
        m.put("quantity", quantity);
        m.put("unit", unit);
        m.put("rate", rate);
        m.put("amount", amount);
        m.put("type", type);
        m.put("note", note);
        return m;
    }

    public static Ledger fromMap(Map<String, Object> m) {
        Ledger e = new Ledger();
        e.id = Util.num(m.get("id"), Util.newId());
        e.date = Util.date10(m.get("date"));
        e.party = Util.str(m.get("party"));
        e.phone = Util.str(m.get("phone"));
        e.commodity = Util.str(m.get("commodity"));
        e.amount = Util.num(m.get("amount"), 0);
        e.quantity = Util.num(m.get("quantity"), 1);
        if (e.quantity <= 0) e.quantity = 1;
        double r = Util.num(m.get("rate"), -1);
        e.rate = r >= 0 ? r : e.amount / e.quantity;
        String u = Util.str(m.get("unit"));
        e.unit = u.isEmpty() ? "kg" : u;
        e.type = "debit".equals(Util.str(m.get("type"))) ? "debit" : "credit";
        e.note = Util.str(m.get("note"));
        return e;
    }
}
