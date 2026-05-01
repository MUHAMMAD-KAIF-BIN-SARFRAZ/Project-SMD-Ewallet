package com.ewallet.app.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.ewallet.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OnboardingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Check login state FIRST before inflating onboarding ──────────────
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Already logged in — go straight to home, skip onboarding & login
            goToMain();
            return;
        }

        // Not logged in — show onboarding
        setContentView(R.layout.activity_onboarding);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        btnGetStarted = findViewById(R.id.btn_get_started);

        new Handler(Looper.getMainLooper()).postDelayed(this::animateCards, 100);

        btnGetStarted.setOnClickListener(v -> goToLogin());
    }

    private void animateCards() {
        View cardBack  = findViewById(R.id.card_back);
        View cardFront = findViewById(R.id.card_front);

        if (cardBack == null || cardFront == null) return;

        cardBack.setAlpha(0f);
        cardBack.setTranslationY(-80f);
        cardFront.setAlpha(0f);
        cardFront.setTranslationY(80f);

        ObjectAnimator backFade   = ObjectAnimator.ofFloat(cardBack,  "alpha",        0f, 1f);
        ObjectAnimator backSlide  = ObjectAnimator.ofFloat(cardBack,  "translationY", -80f, 0f);
        ObjectAnimator frontFade  = ObjectAnimator.ofFloat(cardFront, "alpha",        0f, 1f);
        ObjectAnimator frontSlide = ObjectAnimator.ofFloat(cardFront, "translationY", 80f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(backFade, backSlide, frontFade, frontSlide);
        set.setDuration(700);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private void goToLogin() {
        Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void goToMain() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
