package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivitySignupBinding;
import java.util.HashMap;
import java.util.Map;

// Registration screen supporting both email/password and Google Sign-In.
// Collects user demographics (name, age, gender) from the questionnaire
// intent extras, creates the Firebase Auth account, and stores the user
// profile document in Firestore.
public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    int statusCode = e.getStatusCode();
                    String errorMsg = "Google sign-in failed: " + e.getMessage();
                    if (statusCode == 10 || statusCode == 12500) {
                        errorMsg = "Google Sign-In configuration error. Please ensure:\n" +
                                "1. Your Firebase Console contains your debug SHA-1 key.\n" +
                                "2. strings.xml's default_web_client_id is set to your correct Web Client ID.";
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        binding.btnSignup.setOnClickListener(v -> doSignup());
        binding.btnGoogleSignup.setOnClickListener(v -> {
            if (!binding.cbTerms.isChecked()) {
                Toast.makeText(this, "Please accept the terms", Toast.LENGTH_SHORT).show();
                return;
            }
            googleLauncher.launch(googleClient.getSignInIntent());
        });
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void doSignup() {
        String fullName  = binding.etName.getText().toString().trim();
        String ageStr    = binding.etAge.getText().toString().trim();
        String email     = binding.etEmail.getText().toString().trim();
        String password  = binding.etPassword.getText().toString().trim();
        String confirm   = binding.etConfirmPassword.getText().toString().trim();

        String occupation = getIntent().getStringExtra("occupation");
        String ageCategory = getIntent().getStringExtra("ageCategory");
        String issues = getIntent().getStringExtra("issues");
        String discoverySource = getIntent().getStringExtra("discoverySource");

        if (fullName.isEmpty() || ageStr.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int ageVal;
        try {
            ageVal = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid age number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ageVal < 18) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Age Restriction 🔞")
                    .setMessage("MindEase is intended for users aged 18 and older. Registration is not permitted for minors.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the terms", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSignup.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("name", fullName);
                    user.put("email", email);
                    user.put("age", ageVal);
                    user.put("occupation", occupation != null ? occupation : "");
                    user.put("ageCategory", ageCategory != null ? ageCategory : "");
                    user.put("issues", issues != null ? issues : "");
                    user.put("discoverySource", discoverySource != null ? discoverySource : "");
                    user.put("createdAt", FieldValue.serverTimestamp());
                    user.put("streak", 0);
                    user.put("totalDays", 0);

                    SharedPrefManager.getInstance(this).setUserName(fullName);
                    SharedPrefManager.getInstance(this).setUserEmail(email);

                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(v -> {
                                auth.signOut(); // Sign out from auto-login to let user login manually
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Account Created 🎉")
                                        .setMessage("Your MindEase account has been successfully created! Please log in with your credentials.")
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnSignup.setEnabled(true);
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Sync Failed ❌")
                                        .setMessage("Profile sync failed: " + e.getMessage())
                                        .setPositiveButton("OK", null)
                                        .show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.btnSignup.setEnabled(true);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Registration Failed ❌")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        binding.btnGoogleSignup.setEnabled(false);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        String uid = user.getUid();
                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        SharedPrefManager.getInstance(this).setUserName(doc.getString("name"));
                                        SharedPrefManager.getInstance(this).setUserEmail(doc.getString("email"));
                                        Toast.makeText(this, "Logged in. Welcome back! 👋", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(this, HomeActivity.class));
                                        finishAffinity();
                                    } else {
                                        String fullName = user.getDisplayName() != null ? user.getDisplayName() : "User";
                                        String email = user.getEmail();
                                        
                                        String occupation = getIntent().getStringExtra("occupation");
                                        String ageCategory = getIntent().getStringExtra("ageCategory");
                                        String issues = getIntent().getStringExtra("issues");
                                        String discoverySource = getIntent().getStringExtra("discoverySource");

                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("name", fullName);
                                        userMap.put("email", email != null ? email : "");
                                        userMap.put("age", 18);
                                        userMap.put("occupation", occupation != null ? occupation : "");
                                        userMap.put("ageCategory", ageCategory != null ? ageCategory : "");
                                        userMap.put("issues", issues != null ? issues : "");
                                        userMap.put("discoverySource", discoverySource != null ? discoverySource : "");
                                        userMap.put("createdAt", FieldValue.serverTimestamp());
                                        userMap.put("streak", 0);
                                        userMap.put("totalDays", 0);

                                        SharedPrefManager.getInstance(this).setUserName(fullName);
                                        SharedPrefManager.getInstance(this).setUserEmail(email);

                                        FirebaseFirestore.getInstance()
                                                .collection("users").document(uid)
                                                .set(userMap)
                                                .addOnSuccessListener(v -> {
                                                    Toast.makeText(this, "Account Created Successfully! 🎉", Toast.LENGTH_LONG).show();
                                                    startActivity(new Intent(this, HomeActivity.class));
                                                    finishAffinity();
                                                })
                                                .addOnFailureListener(e -> {
                                                    binding.btnGoogleSignup.setEnabled(true);
                                                    Toast.makeText(this, "Profile sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    binding.btnGoogleSignup.setEnabled(true);
                                    Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        binding.btnGoogleSignup.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnGoogleSignup.setEnabled(true);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Google Login Failed ❌")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
    }
}
