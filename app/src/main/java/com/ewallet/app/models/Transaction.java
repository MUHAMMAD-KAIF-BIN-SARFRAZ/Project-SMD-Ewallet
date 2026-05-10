package com.ewallet.app.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction {


    private String id;
    private String title;
    private String type;
    private String flow;
    private String category;
    private String description;
    private String status;
    private String referenceId;
    private double amount;
    private double balanceAfter;
    private boolean isCredit;
    private long date;

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
    public Transaction() {}


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

    public String getFormattedAmount() {
        return String.format(Locale.US, "%sPKR %.2f",
                isCredit ? "+" : "-", amount);
    }

    public String getFormattedDate() {
        if (date == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(new Date(date));
    }


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