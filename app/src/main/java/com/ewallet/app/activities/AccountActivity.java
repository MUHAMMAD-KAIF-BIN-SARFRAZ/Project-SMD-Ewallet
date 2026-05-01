package com.ewallet.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ewallet.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountActivity extends AppCompatActivity {

    private TextView tvUid, tvEmail, btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadUserData();
        setupBottomNav();
        setupLogout();
    }

    private void initViews() {
        tvUid = findViewById(R.id.tv_uid);
        tvEmail = findViewById(R.id.tv_email);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            tvUid.setText("UID: " + user.getUid());
            tvEmail.setText("Email: " + user.getEmail());
        }
    }

    private void setupLogout() {
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            Intent intent = new Intent(AccountActivity.this, OnboardingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ✅ Bottom Navigation
    private void setupBottomNav() {

        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navStats = findViewById(R.id.nav_stats);
        LinearLayout navCards = findViewById(R.id.nav_cards);
        LinearLayout navFab = findViewById(R.id.nav_fab);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        if (navStats != null) {
            navStats.setOnClickListener(v -> {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
            });
        }

        if (navCards != null) {
            navCards.setOnClickListener(v ->
                    Toast.makeText(this, "Cards coming soon", Toast.LENGTH_SHORT).show());
        }

        if (navFab != null) {
            navFab.setOnClickListener(v ->
                    Toast.makeText(this, "Quick Action coming soon", Toast.LENGTH_SHORT).show());
        }

        // Already on profile
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                // do nothing
            });
        }
    }
}