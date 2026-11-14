package com.khata.ledger;

/** Tiny callback used by DataStore (same idea as Retrofit's Callback, but one method). */
public interface Cb<T> {
    void on(T value);
}
