package com.ewallet.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ewallet.app.R;
import com.ewallet.app.models.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final List<Transaction> transactions;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context      = context;
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = transactions.get(position);

        holder.tvName.setText(t.getTitle());

        holder.tvDate.setText(t.getFormattedDate());

        holder.tvAmount.setText(t.getFormattedAmount());

        holder.tvAmount.setTextColor(
                t.isCredit()
                        ? ContextCompat.getColor(context, R.color.green_income)
                        : ContextCompat.getColor(context, R.color.red_expense));

        holder.tvIcon.setText(t.getCategoryEmoji());

        int tint = t.isCredit()
                ? Color.parseColor("#E8F5E9")   // soft green for credit
                : Color.parseColor("#FBE9E7");  // soft red  for debit
        holder.tvIcon.setBackgroundColor(tint);

        if (holder.tvStatus != null) {
            String status = t.getStatus();
            if ("success".equalsIgnoreCase(status)) {
                holder.tvStatus.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText(
                        "pending".equalsIgnoreCase(status) ? "Pending" : "Failed");
                holder.tvStatus.setTextColor(
                        "pending".equalsIgnoreCase(status)
                                ? Color.parseColor("#F59E0B")   // amber
                                : Color.parseColor("#EF4444")); // red
            }
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvIcon;

        TextView  tvName;
        TextView  tvDate;
        TextView  tvAmount;
        TextView  tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon   = itemView.findViewById(R.id.tv_transaction_icon);
            tvName   = itemView.findViewById(R.id.tv_transaction_name);
            tvDate   = itemView.findViewById(R.id.tv_transaction_date);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvStatus = itemView.findViewById(R.id.tv_transaction_status);
        }
    }
}