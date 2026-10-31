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
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityLoginBinding;

@SuppressWarnings("deprecation")
// Handles user authentication via email/password and Google Sign-In.
// On successful login, the user's profile data is loaded from Firestore
// into SharedPreferences before navigating to the home dashboard.
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
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
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener(v -> emailLogin());
        binding.btnGoogle.setOnClickListener(v -> googleLauncher.launch(googleClient.getSignInIntent()));
        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, QuestionnaireActivity.class)));
    }

    private void emailLogin() {
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> goHome())
                .addOnFailureListener(e -> {
                    binding.btnLogin.setEnabled(true);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Login Failed ❌")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(r -> goHome())
                .addOnFailureListener(e ->
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Google Login Failed ❌")
                                .setMessage(e.getMessage())
                                .setPositiveButton("OK", null)
                                .show());
    }

    private void goHome() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            SharedPrefManager.getInstance(this).setUserName(name != null ? name : "User");
                            SharedPrefManager.getInstance(this).setUserEmail(email != null ? email : user.getEmail());
                        } else {
                            SharedPrefManager.getInstance(this).setUserName(user.getDisplayName() != null ? user.getDisplayName() : "User");
                            SharedPrefManager.getInstance(this).setUserEmail(user.getEmail());
                        }
                        Toast.makeText(this, "Welcome back to MindEase! Successfully logged in. 👋", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finishAffinity();
                    })
                    .addOnFailureListener(e -> {
                        SharedPrefManager.getInstance(this).setUserName(user.getDisplayName() != null ? user.getDisplayName() : "User");
                        SharedPrefManager.getInstance(this).setUserEmail(user.getEmail());
                        Toast.makeText(this, "Logged in. Welcome back! 👋", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finishAffinity();
                    });
        } else {
            startActivity(new Intent(this, HomeActivity.class));
            finishAffinity();
        }
    }
}
