package com.khata.ledger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of the web app's prediction formula: score = seasonalDelta% * 0.6 + trend% * 0.4. */
public class Predictor {

    public static class Result {
        public String verdict = "stable";     // rise | fall | stable
        public String confidence = "low";     // high | medium | low
        public int dataPoints, nextMonth;
        public double avgThisMonth, seasonalDeltaPct, trendPct;
        public Double seasonalNextAvg;
        public LinkedHashMap<String, double[]> weather = new LinkedHashMap<>(); // tag -> {avg, count}
    }

    /** month -> {avg, count} */
    public static Map<Integer, double[]> seasonal(List<PricePoint> list) {
        Map<Integer, double[]> sum = new HashMap<>();
        for (PricePoint p : list) {
            int m = Util.monthOf(p.getDate());
            if (m == 0) continue;
            double[] v = sum.get(m);
            if (v == null) { v = new double[]{0, 0}; sum.put(m, v); }
            v[0] += p.getPrice();
            v[1] += 1;
        }
        Map<Integer, double[]> out = new HashMap<>();
        for (Map.Entry<Integer, double[]> e : sum.entrySet()) {
            out.put(e.getKey(), new double[]{e.getValue()[0] / e.getValue()[1], e.getValue()[1]});
        }
        return out;
    }

    private static double trend(List<PricePoint> sorted) {
        int n = sorted.size();
        int from = Math.max(0, n - 5);
        List<PricePoint> recent = sorted.subList(from, n);
        if (recent.size() < 2) return 0;
        double first = recent.get(0).getPrice(), last = recent.get(recent.size() - 1).getPrice();
        if (first == 0) return 0;
        return (last - first) / first * 100;
    }

    public static Result predict(List<PricePoint> entries) {
        if (entries == null || entries.isEmpty()) return null;
        List<PricePoint> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, new Comparator<PricePoint>() {
            @Override public int compare(PricePoint a, PricePoint b) { return a.getDate().compareTo(b.getDate()); }
        });
        int thisMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int nextMonth = thisMonth == 12 ? 1 : thisMonth + 1;

        Map<Integer, double[]> seas = seasonal(entries);
        double s = 0;
        int c = 0;
        for (PricePoint p : sorted) {
            if (Util.monthOf(p.getDate()) == thisMonth) { s += p.getPrice(); c++; }
        }
        double avgThis = c > 0 ? s / c : sorted.get(sorted.size() - 1).getPrice();

        double[] next = seas.get(nextMonth);
        double seasonalDelta = (next != null && avgThis != 0) ? (next[0] - avgThis) / avgThis * 100 : 0;
        double trendPct = trend(sorted);
        double score = seasonalDelta * 0.6 + trendPct * 0.4;

        Result r = new Result();
        if (next != null && score >= 8) r.verdict = "rise";
        else if (next != null && score <= -8) r.verdict = "fall";
        r.dataPoints = entries.size();
        r.confidence = r.dataPoints >= 24 ? "high" : (r.dataPoints >= 8 ? "medium" : "low");
        r.avgThisMonth = avgThis;
        r.seasonalNextAvg = next != null ? next[0] : null;
        r.seasonalDeltaPct = seasonalDelta;
        r.trendPct = trendPct;
        r.nextMonth = nextMonth;

        Map<String, double[]> w = new HashMap<>();
        for (PricePoint p : entries) {
            double[] v = w.get(p.getWeather());
            if (v == null) { v = new double[]{0, 0}; w.put(p.getWeather(), v); }
            v[0] += p.getPrice();
            v[1] += 1;
        }
        List<Map.Entry<String, double[]>> list = new ArrayList<>(w.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, double[]>>() {
            @Override public int compare(Map.Entry<String, double[]> a, Map.Entry<String, double[]> b) {
                return Double.compare(b.getValue()[0] / b.getValue()[1], a.getValue()[0] / a.getValue()[1]);
            }
        });
        for (Map.Entry<String, double[]> e : list) {
            r.weather.put(e.getKey(), new double[]{e.getValue()[0] / e.getValue()[1], e.getValue()[1]});
        }
        return r;
    }
}
