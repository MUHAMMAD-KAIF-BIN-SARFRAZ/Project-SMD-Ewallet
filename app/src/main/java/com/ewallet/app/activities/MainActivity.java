package com.ewallet.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ewallet.app.R;
import com.ewallet.app.adapters.TransactionAdapter;
import com.ewallet.app.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;

    // UI
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private TextView tvBalance;
    private TextView tvGreeting;
    private TextView tvEmptyState;

    // Live listener – detached in onStop, re-attached in onStart
    private ValueEventListener userListener;

    // Launcher for TransferActivity
    private ActivityResultLauncher<Intent> transferLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Register result launcher BEFORE any possible finish() call
        transferLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // The live Firebase listener already reflects balance/tx changes
                    // automatically – no extra work needed here.
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(this, "Transfer completed", Toast.LENGTH_SHORT).show();
                    }
                });

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            goToOnboarding();
            return;
        }

        mUserRef = FirebaseDatabase
                .getInstance("https://ewallet-app-smd-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(user.getUid());

        initViews();
        setupGreeting(user);
        setupRecyclerView();
        setupClickListeners();
        attachUserListener();
    }

    // ── Views ────────────────────────────────────────────────────────────────

    private void initViews() {
        tvGreeting     = findViewById(R.id.tv_greeting);
        tvBalance      = findViewById(R.id.tv_balance);
        rvTransactions = findViewById(R.id.rv_transactions);
        tvEmptyState   = findViewById(R.id.tv_empty_state); // optional – add to layout
    }

    // ── Greeting ─────────────────────────────────────────────────────────────

    private void setupGreeting(FirebaseUser user) {
        if (tvGreeting == null) return;
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            tvGreeting.setText("Hi " + displayName.split(" ")[0]);
        } else {
            tvGreeting.setText("Hi there");
        }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
        rvTransactions.setNestedScrollingEnabled(false);
    }

    // ── Firebase live listener ───────────────────────────────────────────────

    private void attachUserListener() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Balance
                Double balance = snapshot.child("balance").getValue(Double.class);
                updateBalanceUI(balance != null ? balance : 0.00);

                // Transactions
                List<Transaction> freshList = new ArrayList<>();
                for (DataSnapshot txEntry : snapshot.child("transactions").getChildren()) {
                    Transaction tx = parseTransaction(txEntry);
                    if (tx != null) freshList.add(tx);
                }

                // Newest first
                Collections.sort(freshList,
                        (a, b) -> Long.compare(b.getDate(), a.getDate()));

                transactionList.clear();
                transactionList.addAll(freshList);
                adapter.notifyDataSetChanged();

                boolean isEmpty = transactionList.isEmpty();
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                }
                rvTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Failed to load data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        mUserRef.addValueEventListener(userListener);
    }

    private Transaction parseTransaction(DataSnapshot snap) {
        try {
            return new Transaction(
                    snap.getKey(),
                    getStr(snap, "title"),
                    getStr(snap, "type"),
                    getStr(snap, "flow"),
                    getStr(snap, "category"),
                    getStr(snap, "description"),
                    getStr(snap, "status"),
                    getStr(snap, "referenceId"),
                    getDbl(snap, "amount"),
                    getDbl(snap, "balanceAfter"),
                    "credit".equalsIgnoreCase(getStr(snap, "flow")),
                    getLng(snap, "date")
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ── Balance UI ───────────────────────────────────────────────────────────

    private void updateBalanceUI(double balance) {
        if (tvBalance == null) return;
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        tvBalance.setText("PKR " + fmt.format(balance));
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onStop() {
        super.onStop();
        if (mUserRef != null && userListener != null) {
            mUserRef.removeEventListener(userListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mUserRef != null && userListener != null) {
            mUserRef.addValueEventListener(userListener);
        }
    }


    private void goToOnboarding() {
        Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── Click listeners ──────────────────────────────────────────────────────
    private void performTopUp(double amount) {

        if (mUserRef == null) return;

        mUserRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {

            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData mutableData) {

                Double balance = mutableData.child("balance").getValue(Double.class);
                if (balance == null) balance = 0.0;

                double newBalance = balance + amount;

                long now = System.currentTimeMillis();
                String refId = "TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String txKey = java.util.UUID.randomUUID().toString();

                // Create transaction
                java.util.Map<String, Object> txMap = new java.util.HashMap<>();
                txMap.put("title", "Top Up");
                txMap.put("type", "topup");
                txMap.put("flow", "credit");
                txMap.put("category", "income");
                txMap.put("description", "Wallet top-up");
                txMap.put("status", "success");
                txMap.put("referenceId", refId);
                txMap.put("amount", amount);
                txMap.put("balanceAfter", newBalance);
                txMap.put("date", now);

                // Update DB
                mutableData.child("balance").setValue(newBalance);
                mutableData.child("transactions").child(txKey).setValue(txMap);

                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {

                if (error != null) {
                    Toast.makeText(MainActivity.this,
                            "Top up failed: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (committed) {
                    Toast.makeText(MainActivity.this,
                            "PKR 100 added successfully",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void setupClickListeners() {

        LinearLayout btnTopUp    = findViewById(R.id.btn_top_up);
        LinearLayout btnTransfer = findViewById(R.id.btn_transfer);
        LinearLayout btnRequest  = findViewById(R.id.btn_request);
        LinearLayout btnMore     = findViewById(R.id.btn_more);

        if (btnTopUp != null) {
            btnTopUp.setOnClickListener(v -> performTopUp(100));
        }

        // Launch TransferActivity
        if (btnTransfer != null) {
            btnTransfer.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TransferActivity.class);
                transferLauncher.launch(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        if (btnRequest != null) {
            btnRequest.setOnClickListener(v ->
                    Toast.makeText(this, "Request coming soon", Toast.LENGTH_SHORT).show());
        }

        if (btnMore != null) {
            btnMore.setOnClickListener(v ->
                    Toast.makeText(this, "More coming soon", Toast.LENGTH_SHORT).show());
        }

        View tvSeeAll = findViewById(R.id.tv_see_all);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v ->
                    Toast.makeText(this, "See All Transactions", Toast.LENGTH_SHORT).show());
        }

        LinearLayout navStats = findViewById(R.id.nav_stats);
        if (navStats != null) {
            navStats.setOnClickListener(v -> {
                startActivity(new Intent(this, StatisticsActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        LinearLayout navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        LinearLayout navCards = findViewById(R.id.nav_cards);
        if (navCards != null) {
            navCards.setOnClickListener(v ->
                    Toast.makeText(this, "Cards coming soon", Toast.LENGTH_SHORT).show());
        }

        LinearLayout navFab = findViewById(R.id.nav_fab);
        if (navFab != null) {
            navFab.setOnClickListener(v ->
                    Toast.makeText(this, "Quick Action coming soon", Toast.LENGTH_SHORT).show());
        }
    }

    // ── DataSnapshot helpers ─────────────────────────────────────────────────

    private String getStr(DataSnapshot snap, String key) {
        Object val = snap.child(key).getValue();
        return val != null ? val.toString() : "";
    }

    private double getDbl(DataSnapshot snap, String key) {
        Double val = snap.child(key).getValue(Double.class);
        return val != null ? val : 0.0;
    }

    private long getLng(DataSnapshot snap, String key) {
        Long val = snap.child(key).getValue(Long.class);
        return val != null ? val : 0L;
    }
}