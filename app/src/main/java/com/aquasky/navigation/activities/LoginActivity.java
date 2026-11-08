package com.aquasky.navigation.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aquasky.navigation.R;
import com.aquasky.navigation.databinding.ActivityLoginBinding;
import com.aquasky.navigation.firebase.FirebaseAuthHelper;
import com.google.firebase.auth.FirebaseUser;

/**
 * AquaSky Authentication Activity (View Binding + Firebase Authentication)
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;   // generated from activity_login.xml
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());          // setContentView(R.layout...) er bodole

        setupListeners();
        updateTabState();
    }

    private void setupListeners() {
        binding.btnTabSignIn.setOnClickListener(v -> {
            isRegisterMode = false;
            updateTabState();
        });

        binding.btnTabRegister.setOnClickListener(v -> {
            isRegisterMode = true;
            updateTabState();
        });

        binding.btnSubmitAuth.setOnClickListener(v -> {
            if (isRegisterMode) performSignUp();
            else performSignIn();
        });
    }

    private void updateTabState() {
        int brand = ContextCompat.getColor(this, R.color.brand_ocean_primary);

        if (isRegisterMode) {
            binding.btnTabRegister.setBackgroundTintList(ColorStateList.valueOf(brand));
            binding.btnTabRegister.setTextColor(Color.WHITE);
            binding.btnTabSignIn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.btnTabSignIn.setTextColor(Color.GRAY);

            binding.tilFullName.setVisibility(View.VISIBLE);
            binding.layoutRoleSelectorContainer.setVisibility(View.VISIBLE);
            binding.btnSubmitAuth.setText("Create Navigator Account");
        } else {
            binding.btnTabSignIn.setBackgroundTintList(ColorStateList.valueOf(brand));
            binding.btnTabSignIn.setTextColor(Color.WHITE);
            binding.btnTabRegister.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.btnTabRegister.setTextColor(Color.GRAY);

            binding.tilFullName.setVisibility(View.GONE);
            binding.layoutRoleSelectorContainer.setVisibility(View.GONE);
            binding.btnSubmitAuth.setText("Sign In to AquaSky");
        }
    }

    private void performSignIn() {
        String email = textOf(binding.etEmail.getText());
        String pass = textOf(binding.etPassword.getText());

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuthHelper.signIn(email, pass, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String role) {
                Toast.makeText(LoginActivity.this, "Welcome, " + role, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performSignUp() {
        String name = textOf(binding.etFullName.getText());
        String email = textOf(binding.etEmail.getText());
        String pass = textOf(binding.etPassword.getText());

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email) || pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = "General Citizen / Public Traveler";   // default
        if (binding.rbSeaCaptain.isChecked()) role = "Master Sea Captain";
        else if (binding.rbAirlinePilot.isChecked()) role = "Commercial Airline Pilot";
        else if (binding.rbFleetDispatcher.isChecked()) role = "Highway Logistics Dispatcher";

        final String finalRole = role;
        FirebaseAuthHelper.signUp(email, pass, role, name, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String assignedRole) {
                Toast.makeText(LoginActivity.this, "Registered successfully as " + finalRole, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String textOf(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;     // memory leak ray
    }
}