package com.example.susach;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views by ID
        View loginButton = findViewById(R.id.login_button);
        View signupRedirectText = findViewById(R.id.signupRedirectText);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = ((android.widget.EditText) findViewById(R.id.login_email)).getText().toString();
                String password = ((android.widget.EditText) findViewById(R.id.login_password)).getText().toString();

                if(email.equals("") || password.equals("")) {
                    Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                } else {
                    // Authenticate with Firebase
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Login successful, now check user role from 'account' collection using email
                                        checkUserRoleAndRedirect(email);
                                    } else {
                                        // If login fails, display a message to the user.
                                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkUserRoleAndRedirect(String email) {
        db.collection("account").document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Long role = document.getLong("role");
                                if (role != null) {
                                    redirectBasedOnRole(role.intValue());
                                } else {
                                    Toast.makeText(LoginActivity.this, "User role not found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Error getting user data: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void redirectBasedOnRole(int role) {
        Intent intent;
        String message;

        switch (role) {
            case 1: // Admin
                intent = new Intent(LoginActivity.this, AdminActivity.class);
                message = "Welcome Admin!";
                break;
            case 2: // Staff
                intent = new Intent(LoginActivity.this, StaffActivity.class);
                message = "Welcome Staff!";
                break;
            case 3: // User
                intent = new Intent(LoginActivity.this, UserActivity.class);
                message = "Welcome User!";
                break;
            default:
                Toast.makeText(LoginActivity.this, "Invalid user role", Toast.LENGTH_SHORT).show();
                return;
        }

        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
        startActivity(intent);
        finish(); // Close the login activity
    }
}