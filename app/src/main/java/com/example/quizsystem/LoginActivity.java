package com.example.quizsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private RadioGroup roleRadioGroup;
    private Button loginButton, registerButton;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            emailEditText.setText(currentUser.getEmail());
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String selectedRole = getSelectedRole();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String savedRole = getUserRole(user.getEmail());
                            if (savedRole == null) {
                                // First time login, save the selected role
                                saveUserRole(user.getEmail(), selectedRole);
                                navigateToDashboard(selectedRole);
                            } else {
                                // Check if selected role matches saved role
                                if (selectedRole.equals(savedRole)) {
                                    navigateToDashboard(selectedRole);
                                } else {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this,
                                            "Please login as " + savedRole, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String selectedRole = getSelectedRole();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserRole(user.getEmail(), selectedRole);
                            navigateToDashboard(selectedRole);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getSelectedRole() {
        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectedId);
        return radioButton.getText().toString().toLowerCase();
    }

    private void saveUserRole(String email, String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(email, role);
        editor.apply();
    }

    private String getUserRole(String email) {
        return sharedPreferences.getString(email, null);
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if (role.equals("teacher")) {
            intent = new Intent(LoginActivity.this, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}