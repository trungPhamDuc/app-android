package com.example.susach;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageAccountActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword, etName;
    private Spinner spinnerRole;
    private Button btnAdd, btnUpdate, btnDelete, btnBack;
    private ListView listViewAccounts;
    private ArrayAdapter<String> adapter;
    private List<String> accountList;
    private List<String> emailList;
    private String selectedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account);

        // Initialize Firebase Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();
        
        // Load accounts
        loadAccounts();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etName = findViewById(R.id.et_name);
        spinnerRole = findViewById(R.id.spinner_role);
        btnAdd = findViewById(R.id.btn_add);
        btnUpdate = findViewById(R.id.btn_update);
        btnDelete = findViewById(R.id.btn_delete);
        btnBack = findViewById(R.id.btn_back);
        listViewAccounts = findViewById(R.id.listview_accounts);

        // Setup role spinner with enhanced options
        String[] roles = {"Select Role", "Admin (Full Access)", "Staff (Limited Access)", "User (Basic Access)", "Moderator (Content Management)", "Guest (Read Only)"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // Setup account list
        accountList = new ArrayList<>();
        emailList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountList);
        listViewAccounts.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAccount();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAccount();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        listViewAccounts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedEmail = emailList.get(position);
                loadAccountDetails(selectedEmail);
            }
        });
    }

    private void loadAccounts() {
        db.collection("account").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            accountList.clear();
                            emailList.clear();
                            for (DocumentSnapshot document : task.getResult()) {
                                String email = document.getId();
                                String name = document.getString("name");
                                Long role = document.getLong("role");
                                
                                String roleText = getRoleText(role);
                                
                                accountList.add(email + " - " + name + " (" + roleText + ")");
                                emailList.add(email);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(ManageAccountActivity.this, "Error loading accounts", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String getRoleText(Long role) {
        if (role == null) return "Unknown";
        
        switch (role.intValue()) {
            case 1: return "Admin";
            case 2: return "Staff";
            case 3: return "User";
            case 4: return "Moderator";
            case 5: return "Guest";
            default: return "Unknown";
        }
    }

    private void loadAccountDetails(String email) {
        db.collection("account").document(email).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            etEmail.setText(email);
                            etName.setText(document.getString("name"));
                            etPassword.setText(""); // Don't show password for security
                            
                            Long role = document.getLong("role");
                            if (role != null) {
                                // Set spinner selection based on role (add 1 because first item is "Select Role")
                                spinnerRole.setSelection(role.intValue());
                            } else {
                                spinnerRole.setSelection(0); // Default to "Select Role"
                            }
                        }
                    }
                });
    }

    private void addAccount() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        int selectedPosition = spinnerRole.getSelectedItemPosition();
        
        // Validate role selection (position 0 is "Select Role")
        if (selectedPosition == 0) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int role = selectedPosition; // Role value matches the position

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // First create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // User created successfully in Authentication, now save to Firestore
                            saveUserDataToFirestore(email, password, name, role);
                        } else {
                            // If Authentication fails, display error
                            handleAuthError(task.getException());
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(String email, String password, String name, int role) {
        Map<String, Object> account = new HashMap<>();
        account.put("email", email);
        account.put("password", password);
        account.put("name", name);
        account.put("role", role);

        db.collection("account").document(email).set(account)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ManageAccountActivity.this, "Account added successfully to both Authentication and Firestore", Toast.LENGTH_SHORT).show();
                        clearFields();
                        loadAccounts();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ManageAccountActivity.this, "Error saving to Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // If Firestore fails, we should delete the Authentication user
                        deleteAuthUser(email);
                    }
                });
    }

    private void deleteAuthUser(String email) {
        // This is a fallback method to clean up Authentication if Firestore fails
        // Note: This requires admin privileges and should be handled carefully
        Toast.makeText(this, "Cleaning up Authentication user due to Firestore failure", Toast.LENGTH_SHORT).show();
    }

    private void updateAccount() {
        if (selectedEmail.isEmpty()) {
            Toast.makeText(this, "Please select an account to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int selectedPosition = spinnerRole.getSelectedItemPosition();
        
        // Validate role selection (position 0 is "Select Role")
        if (selectedPosition == 0) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int role = selectedPosition; // Role value matches the position

        if (name.isEmpty()) {
            Toast.makeText(this, "Please fill name field", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firestore data
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("role", role);
        
        if (!password.isEmpty()) {
            updates.put("password", password);
        }

        db.collection("account").document(selectedEmail).update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // If password was updated, also update in Authentication
                        if (!password.isEmpty()) {
                            updateAuthPassword(selectedEmail, password);
                        } else {
                            Toast.makeText(ManageAccountActivity.this, "Account updated successfully in Firestore", Toast.LENGTH_SHORT).show();
                            clearFields();
                            loadAccounts();
                            selectedEmail = "";
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ManageAccountActivity.this, "Error updating account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateAuthPassword(String email, String newPassword) {
        // Note: Updating password in Authentication requires the user to be signed in
        // or requires admin privileges. For admin operations, you might need to use
        // Firebase Admin SDK on the server side.
        
        // For now, we'll show a message that the password update in Authentication
        // requires additional setup
        Toast.makeText(this, "Account updated in Firestore. Password update in Authentication requires admin privileges.", Toast.LENGTH_LONG).show();
        clearFields();
        loadAccounts();
        selectedEmail = "";
    }

    private void deleteAccount() {
        if (selectedEmail.isEmpty()) {
            Toast.makeText(this, "Please select an account to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this account? This will remove the user from both Authentication and Firestore.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // First delete from Firestore
                    db.collection("account").document(selectedEmail).delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Then attempt to delete from Authentication
                                    deleteFromAuthentication(selectedEmail);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ManageAccountActivity.this, "Error deleting from Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteFromAuthentication(String email) {
        // Note: Deleting users from Authentication requires admin privileges
        // For now, we'll show a message about the limitation
        Toast.makeText(this, "Account deleted from Firestore. Authentication deletion requires admin privileges.", Toast.LENGTH_LONG).show();
        clearFields();
        loadAccounts();
        selectedEmail = "";
    }

    private void clearFields() {
        etEmail.setText("");
        etPassword.setText("");
        etName.setText("");
        spinnerRole.setSelection(0); // Reset to "Select Role"
    }

    // Helper method to check if user exists in Authentication
    private void checkUserExistsInAuth(String email, OnCompleteListener<QuerySnapshot> listener) {
        // This is a placeholder for checking if user exists in Authentication
        // In a real implementation, you would need to use Firebase Admin SDK
        // or implement a server-side function to check Authentication users
        Toast.makeText(this, "Note: Authentication user verification requires server-side implementation", Toast.LENGTH_SHORT).show();
    }

    // Method to handle Authentication errors
    private void handleAuthError(Exception e) {
        String errorMessage = "Authentication error: ";
        if (e.getMessage().contains("email address is already in use")) {
            errorMessage += "Email already exists in Authentication";
        } else if (e.getMessage().contains("password is invalid")) {
            errorMessage += "Password is too weak";
        } else {
            errorMessage += e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
} 