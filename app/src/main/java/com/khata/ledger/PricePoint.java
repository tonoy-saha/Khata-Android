package com.khata.ledger;

import java.util.HashMap;
import java.util.Map;

/** One price observation (priceHistory/{slug} -> entries). */
public class PricePoint {
    private String date = "";
    private double price = 0;
    private String weather = "Normal";

    public PricePoint() {
    }

    public PricePoint(String date, double price, String weather) {
        this.date = date;
        this.price = price;
        this.weather = weather;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    /** Field names match the website's Firestore documents exactly. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("date", date);
        m.put("price", price);
        m.put("weather", weather);
        return m;
    }

    public static PricePoint fromMap(Map<String, Object> m) {
        PricePoint e = new PricePoint();
        e.date = Util.date10(m.get("date"));
        e.price = Util.num(m.get("price"), 0);
        String w = Util.str(m.get("weather"));
        e.weather = w.isEmpty() ? "Normal" : w;
        return e;
    }
}
