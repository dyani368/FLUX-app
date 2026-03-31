package com.example.flux.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flux.MainActivity;
import com.example.flux.R;
import com.example.flux.ui.onboarding.OnboardingActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // skip login if already signed in
        if (auth.getCurrentUser() != null) {
            goToMain(false);
            return;
        }

        setContentView(R.layout.activity_login);

        final TextInputEditText etEmail = findViewById(R.id.etEmail);
        final TextInputEditText etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(r -> goToMain(false)) // false = skip onboarding
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(r -> goToMain(true)) // true = show onboarding
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void goToMain(boolean isNewUser) {
        if (isNewUser) {
            startActivity(new Intent(this,
                com.example.flux.ui.onboarding.OnboardingActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
