package com.ewallet.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ewallet.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private DatabaseReference txRef;

    private TextView tvShopping, tvDebt, tvEntertainment, tvTransfer, tvOthers;

    private TextView tvSalary, tvLoan, tvSavings, tvTopUp, tvIncomeOthers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        txRef = FirebaseDatabase
                .getInstance("https://ewallet-app-smd-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("transactions");

        initViews();
        loadStats();
        setupClickListeners();
    }


    private void initViews() {
        tvShopping      = findViewById(R.id.tv_spend_shopping);
        tvDebt          = findViewById(R.id.tv_spend_debt);
        tvEntertainment = findViewById(R.id.tv_spend_entertainment);
        tvTransfer      = findViewById(R.id.tv_spend_transfer);
        tvOthers        = findViewById(R.id.tv_spend_others);

        tvSalary        = findViewById(R.id.tv_income_salary);
        tvLoan          = findViewById(R.id.tv_income_loan);
        tvSavings       = findViewById(R.id.tv_income_savings);
        tvTopUp         = findViewById(R.id.tv_income_topup);
        tvIncomeOthers  = findViewById(R.id.tv_income_others);
    }

    private void loadStats() {
        txRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                double shopping = 0, debt = 0, entertainment = 0,
                        transfer = 0, expenseOthers = 0;
                double salary   = 0, loan = 0, savings = 0,
                        topUp    = 0, incomeOthers = 0;

                for (DataSnapshot tx : snapshot.getChildren()) {
                    String flow     = tx.child("flow").getValue(String.class);
                    Double amount   = tx.child("amount").getValue(Double.class);
                    String category = tx.child("category").getValue(String.class);

                    if (flow == null || amount == null) continue;
                    if (category == null) category = "others";
                    category = category.toLowerCase(Locale.US);

                    if ("debit".equals(flow)) {
                        switch (category) {
                            case "shopping":      shopping      += amount; break;
                            case "debt":          debt          += amount; break;
                            case "entertainment": entertainment += amount; break;
                            case "transfer":      transfer      += amount; break;
                            default:              expenseOthers += amount; break;
                        }
                    } else {
                        switch (category) {
                            case "salary":  salary       += amount; break;
                            case "loan":    loan         += amount; break;
                            case "savings": savings      += amount; break;
                            case "top_up":  topUp        += amount; break;
                            default:        incomeOthers += amount; break;
                        }
                    }
                }

                updateUI(shopping, debt, entertainment, transfer, expenseOthers,
                        salary, loan, savings, topUp, incomeOthers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI(double shopping, double debt, double entertainment,
                          double transfer, double expenseOthers,
                          double salary, double loan, double savings,
                          double topUp, double incomeOthers) {

        tvShopping.setText(fmt(shopping));
        tvDebt.setText(fmt(debt));
        tvEntertainment.setText(fmt(entertainment));
        tvTransfer.setText(fmt(transfer));
        tvOthers.setText(fmt(expenseOthers));


        tvSalary.setText(fmt(salary));
        tvLoan.setText(fmt(loan));
        tvSavings.setText(fmt(savings));
        tvTopUp.setText(fmt(topUp));
        tvIncomeOthers.setText(fmt(incomeOthers));
    }

    private String fmt(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "PKR " + nf.format(value);
    }

    private void setupClickListeners() {
        LinearLayout navHome = findViewById(R.id.nav_home_from_stats);
        if (navHome != null) navHome.setOnClickListener(v -> finish());

        LinearLayout navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, AccountActivity.class)));
        }

        TextView tvChartSpendings = findViewById(R.id.tv_view_chart_spendings);
        if (tvChartSpendings != null) {
            tvChartSpendings.setOnClickListener(v ->
                    android.widget.Toast.makeText(this,
                            "Spending chart coming soon", android.widget.Toast.LENGTH_SHORT).show());
        }

        TextView tvChartIncomes = findViewById(R.id.tv_view_chart_incomes);
        if (tvChartIncomes != null) {
            tvChartIncomes.setOnClickListener(v ->
                    android.widget.Toast.makeText(this,
                            "Income chart coming soon", android.widget.Toast.LENGTH_SHORT).show());
        }
    }
}