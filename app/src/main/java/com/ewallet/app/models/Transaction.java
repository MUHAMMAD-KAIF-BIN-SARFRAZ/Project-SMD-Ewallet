package com.ewallet.app.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction {

    // DB fields
    private String id;           // Firebase push key
    private String title;        // e.g. "Sent to Ali"
    private String type;         // "top_up" | "transfer" | "payment" | "withdrawal"
    private String flow;         // "credit" | "debit"
    private String category;     // "food" | "shopping" | "utilities" | "other" | …
    private String description;  // optional user note
    private String status;       // "success" | "pending" | "failed"
    private String referenceId;  // receipt / ref number
    private double amount;
    private double balanceAfter; // wallet snapshot after this transaction
    private boolean isCredit;    // derived from flow
    private long date;           // epoch ms

    // ── Constructor (used by MainActivity when parsing from Firebase) ─────────
    public Transaction(String id, String title, String type, String flow,
                       String category, String description, String status,
                       String referenceId, double amount, double balanceAfter,
                       boolean isCredit, long date) {
        this.id           = id;
        this.title        = title;
        this.type         = type;
        this.flow         = flow;
        this.category     = category;
        this.description  = description;
        this.status       = status;
        this.referenceId  = referenceId;
        this.amount       = amount;
        this.balanceAfter = balanceAfter;
        this.isCredit     = isCredit;
        this.date         = date;
    }

    // ── No-arg constructor (required by Firebase deserializer if used with
    //    getValue(Transaction.class) in the future) ───────────────────────────
    public Transaction() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()           { return id; }
    public String getTitle()        { return title; }
    public String getType()         { return type; }
    public String getFlow()         { return flow; }
    public String getCategory()     { return category; }
    public String getDescription()  { return description; }
    public String getStatus()       { return status; }
    public String getReferenceId()  { return referenceId; }
    public double getAmount()       { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public boolean isCredit()       { return isCredit; }
    public long getDate()           { return date; }

    // ── Derived helpers (used by the adapter) ────────────────────────────────

    /** Returns amount formatted with sign: "+PKR 500.00" or "-PKR 500.00" */
    public String getFormattedAmount() {
        return String.format(Locale.US, "%sPKR %.2f",
                isCredit ? "+" : "-", amount);
    }

    /** Returns date as a readable string: "Dec 28, 2023" */
    public String getFormattedDate() {
        if (date == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(new Date(date));
    }

    /**
     * Returns a single emoji/label that can be shown as a category icon
     * in the adapter until you have custom drawables.
     */
    public String getCategoryEmoji() {
        if (category == null) return "💳";
        switch (category.toLowerCase(Locale.US)) {
            case "food":        return "🍔";
            case "shopping":    return "🛍️";
            case "utilities":   return "💡";
            case "transport":   return "🚗";
            case "health":      return "💊";
            case "salary":      return "💼";
            case "top_up":      return "💰";
            case "transfer":    return "🔄";
            case "withdrawal":  return "🏧";
            default:            return "💳";
        }
    }
}