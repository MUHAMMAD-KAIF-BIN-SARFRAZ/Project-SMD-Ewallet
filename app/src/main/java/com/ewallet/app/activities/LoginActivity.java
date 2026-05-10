package com.ewallet.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ewallet.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView tabLogin, tabSignup;
    private LinearLayout llNameField;
    private TextView tvLabelName, tvForgotPassword;
    private EditText etName, etEmail, etPassword;
    private Button btnAction;
    private ProgressBar progressBar;
    private TextView tvError;
    private ImageView ivTogglePassword;

    private boolean isLoginMode = true;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase
                .getInstance("https://ewallet-app-smd-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        initViews();
        setupTabToggle();
        setupClickListeners();
    }

    private void initViews() {
        tabLogin         = findViewById(R.id.tab_login);
        tabSignup        = findViewById(R.id.tab_signup);
        llNameField      = findViewById(R.id.ll_name_field);
        tvLabelName      = findViewById(R.id.tv_label_name);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        etName           = findViewById(R.id.et_name);
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        btnAction        = findViewById(R.id.btn_action);
        progressBar      = findViewById(R.id.progress_bar);
        tvError          = findViewById(R.id.tv_error);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
    }

    private void setupTabToggle() {
        tabLogin.setOnClickListener(v -> switchToLogin());
        tabSignup.setOnClickListener(v -> switchToSignup());
    }

    private void switchToLogin() {
        isLoginMode = true;

        tabLogin.setBackground(getDrawable(R.drawable.bg_tab_active));
        tabLogin.setTextColor(getColor(android.R.color.white));
        tabSignup.setBackground(null);
        tabSignup.setTextColor(getColor(R.color.text_secondary));

        llNameField.setVisibility(View.GONE);
        tvLabelName.setVisibility(View.GONE);
        tvForgotPassword.setVisibility(View.VISIBLE);

        btnAction.setText("Login");
        clearError();
    }

    private void switchToSignup() {
        isLoginMode = false;

        tabSignup.setBackground(getDrawable(R.drawable.bg_tab_active));
        tabSignup.setTextColor(getColor(android.R.color.white));
        tabLogin.setBackground(null);
        tabLogin.setTextColor(getColor(R.color.text_secondary));

        llNameField.setVisibility(View.VISIBLE);
        tvLabelName.setVisibility(View.VISIBLE);
        tvForgotPassword.setVisibility(View.GONE);

        btnAction.setText("Create Account");
        clearError();
    }


    private void setupClickListeners() {

        btnAction.setOnClickListener(v -> {
            hideKeyboard();
            if (isLoginMode) handleLogin();
            else             handleSignup();
        });

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }


    private void handleLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateEmailPassword(email, password)) return;

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) goToMain();
                    else showError(getLoginErrorMessage(task.getException()));
                });
    }

    private void handleSignup() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            showError("Please enter your full name");
            etName.requestFocus();
            return;
        }
        if (!validateEmailPassword(email, password)) return;
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdate =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();

                            user.updateProfile(profileUpdate)
                                    .addOnCompleteListener(profileTask ->
                                            createUserInDatabase(user.getUid(), name, email));
                        } else {
                            showLoading(false);
                            goToMain();
                        }
                    } else {
                        showLoading(false);
                        showError(getSignupErrorMessage(task.getException()));
                    }
                });
    }

    private void createUserInDatabase(String uid, String name, String email) {

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid",          uid);
        userMap.put("name",         name);
        userMap.put("email",        email);
        userMap.put("balance",      0.00);
        userMap.put("currency",     "PKR");
        userMap.put("createdAt",    System.currentTimeMillis());
        userMap.put("transactions", new HashMap<>());

        mDatabase.child("users").child(uid)
                .setValue(userMap)
                .addOnCompleteListener(dbTask -> {
                    showLoading(false);
                    if (dbTask.isSuccessful()) {
                        goToMain();
                    } else {
                        Toast.makeText(this,
                                "Account created but profile save failed. " +
                                        "Please contact support if issues arise.",
                                Toast.LENGTH_LONG).show();
                        goToMain();
                    }
                });
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError("Enter your email address first");
            etEmail.requestFocus();
            return;
        }

        showLoading(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        clearError();
                        Toast.makeText(this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        showError("Could not send reset email. Check your address.");
                    }
                });
    }

    private boolean validateEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email");
            etEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private String getLoginErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException)
            return "No account found with this email";
        if (e instanceof FirebaseAuthInvalidCredentialsException)
            return "Incorrect password. Please try again";
        return "Login failed. Please try again";
    }

    private String getSignupErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException)
            return "An account with this email already exists";
        if (e instanceof FirebaseAuthWeakPasswordException)
            return "Password is too weak. Use at least 6 characters";
        if (e instanceof FirebaseAuthInvalidCredentialsException)
            return "Invalid email address";
        return "Sign up failed. Please try again";
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivTogglePassword.setAlpha(0.5f);
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivTogglePassword.setAlpha(1.0f);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAction.setEnabled(!show);
        btnAction.setAlpha(show ? 0.6f : 1.0f);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}