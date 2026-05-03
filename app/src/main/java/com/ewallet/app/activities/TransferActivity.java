package com.ewallet.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ewallet.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TransferActivity extends AppCompatActivity {

    private DatabaseReference rootRef;
    private String currentUid;
    private double currentBalance = 0.0;

    // UI
    private TextView tvAvailableBalance;
    private EditText etReceiverUid, etAmount, etDescription;
    private Button btnTransfer;
    private ProgressBar progressBar;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        currentUid = user.getUid();

        rootRef = FirebaseDatabase
                .getInstance("https://ewallet-app-smd-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        initViews();
        setupToolbar();
        fetchBalance();

        btnTransfer.setOnClickListener(v -> {
            hideKeyboard();
            attemptTransfer();
        });
    }

    private void initViews() {
        tvAvailableBalance = findViewById(R.id.tv_available_balance);
        etReceiverUid = findViewById(R.id.et_receiver_uid);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        btnTransfer = findViewById(R.id.btn_transfer);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Send Money");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    // ── BALANCE ─────────────────────────────

    private void fetchBalance() {
        rootRef.child("users").child(currentUid)
                .child("balance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double bal = snapshot.getValue(Double.class);
                        currentBalance = bal != null ? bal : 0.0;
                        updateBalanceUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateBalanceUI() {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        fmt.setMinimumFractionDigits(2);
        tvAvailableBalance.setText("Available: PKR " + fmt.format(currentBalance));
    }

    // ── VALIDATION ─────────────────────────────

    private boolean validate(String uid, String amountStr) {

        clearError();

        if (TextUtils.isEmpty(uid)) {
            showError("Enter receiver UID");
            return false;
        }

        if (uid.equals(currentUid)) {
            showError("Cannot send money to yourself");
            return false;
        }

        if (TextUtils.isEmpty(amountStr)) {
            showError("Enter amount");
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            showError("Invalid amount");
            return false;
        }

        if (amount <= 0) {
            showError("Amount must be greater than 0");
            return false;
        }

        if (amount > currentBalance) {
            showError("Insufficient balance");
            return false;
        }

        return true;
    }

    private void attemptTransfer() {

        String receiverUid = etReceiverUid.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (!validate(receiverUid, amountStr)) return;

        double amount = Double.parseDouble(amountStr);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Transfer")
                .setMessage("Send PKR " + amount + " to:\n" + receiverUid)
                .setPositiveButton("Send", (d, w) ->
                        checkReceiverAndTransfer(receiverUid, amount, desc))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── CHECK RECEIVER ─────────────────────────────

    private void checkReceiverAndTransfer(String receiverUid, double amount, String desc) {

        rootRef.child("users").child(receiverUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            showError("User not found");
                            return;
                        }

                        processTransfer(receiverUid, amount, desc);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError(error.getMessage());
                    }
                });
    }

    // ── CORE TRANSFER ─────────────────────────────

    private void processTransfer(String receiverUid, double amount, String desc) {

        showLoading(true);

        DatabaseReference senderRef = rootRef.child("users").child(currentUid);
        DatabaseReference receiverRef = rootRef.child("users").child(receiverUid);

        senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnap) {

                Double senderBal = senderSnap.child("balance").getValue(Double.class);
                if (senderBal == null) senderBal = 0.0;

                if (senderBal < amount) {
                    showLoading(false);
                    showError("Insufficient balance");
                    return;
                }

                double newSenderBal = senderBal - amount;

                senderRef.child("balance").setValue(newSenderBal);

                receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot receiverSnap) {

                        Double receiverBal = receiverSnap.child("balance").getValue(Double.class);
                        if (receiverBal == null) receiverBal = 0.0;

                        double newReceiverBal = receiverBal + amount;

                        receiverRef.child("balance").setValue(newReceiverBal);

                        saveTransactions(receiverUid, amount, desc, newSenderBal, newReceiverBal);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        showError(error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                showError(error.getMessage());
            }
        });
    }

    // ── SAVE TRANSACTIONS ─────────────────────────────

    private void saveTransactions(String receiverUid, double amount, String desc,
                                  double senderBalAfter, double receiverBalAfter) {

        long now = System.currentTimeMillis();
        String txId = UUID.randomUUID().toString();

        Map<String, Object> base = new HashMap<>();
        base.put("amount", amount);
        base.put("date", now);
        base.put("description", desc);
        base.put("status", "success");

        Map<String, Object> senderTx = new HashMap<>(base);
        senderTx.put("title", "Sent");
        senderTx.put("flow", "debit");
        senderTx.put("balanceAfter", senderBalAfter);
        senderTx.put("to", receiverUid);

        Map<String, Object> receiverTx = new HashMap<>(base);
        receiverTx.put("title", "Received");
        receiverTx.put("flow", "credit");
        receiverTx.put("balanceAfter", receiverBalAfter);
        receiverTx.put("from", currentUid);

        rootRef.child("users").child(currentUid)
                .child("transactions").child(txId)
                .setValue(senderTx);

        rootRef.child("users").child(receiverUid)
                .child("transactions").child(txId)
                .setValue(receiverTx);

        showLoading(false);
        showSuccess();
    }

    // ── UI HELPERS ─────────────────────────────

    private void showSuccess() {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Transfer completed successfully")
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnTransfer.setEnabled(!show);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        tvError.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}