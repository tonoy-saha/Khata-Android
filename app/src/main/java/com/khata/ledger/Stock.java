package com.khata.ledger;

import java.util.HashMap;
import java.util.Map;

/** A stock buy/sell record (users/{uid}/data/app -> stockEntries). */
public class Stock {
    private double id = Util.newId();
    private String date = "";
    private String commodity = "";
    private double buyPrice = 0;
    private Double sellPrice = null;
    private double quantity = 0;
    private String note = "";

    public Stock() {
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

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
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
        m.put("commodity", commodity);
        m.put("buyPrice", buyPrice);
        m.put("sellPrice", sellPrice);
        m.put("quantity", quantity);
        m.put("note", note);
        return m;
    }

    public static Stock fromMap(Map<String, Object> m) {
        Stock e = new Stock();
        e.id = Util.num(m.get("id"), Util.newId());
        e.date = Util.date10(m.get("date"));
        e.commodity = Util.str(m.get("commodity"));
        e.buyPrice = Util.num(m.get("buyPrice"), 0);
        Object sp = m.get("sellPrice");
        e.sellPrice = (sp instanceof Number) ? ((Number) sp).doubleValue() : null;
        e.quantity = Util.num(m.get("quantity"), 0);
        e.note = Util.str(m.get("note"));
        return e;
    }
}
