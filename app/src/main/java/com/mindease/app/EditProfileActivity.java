package com.mindease.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityEditProfileBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Profile editing screen where users can update their name, age, gender,
// and profile picture. Updates are persisted to both Firestore and local
// SharedPreferences. Profile images are saved to internal storage for
// offline access.
public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot select profile picture.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        binding.ivProfilePicture.setVisibility(View.VISIBLE);
                        binding.tvAvatarFallback.setVisibility(View.GONE);
                        binding.ivProfilePicture.setImageURI(imageUri);
                        saveProfilePicture(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Load profile picture cache
        String picPath = SharedPrefManager.getInstance(this).getProfilePicturePath();
        if (!picPath.isEmpty()) {
            binding.ivProfilePicture.setVisibility(View.VISIBLE);
            binding.tvAvatarFallback.setVisibility(View.GONE);
            binding.ivProfilePicture.setImageURI(Uri.parse(picPath));
        } else {
            binding.ivProfilePicture.setVisibility(View.GONE);
            binding.tvAvatarFallback.setVisibility(View.VISIBLE);
        }

        binding.profilePictureContainer.setOnClickListener(v -> checkPermissionAndPickImage());

        // 1. Pre-fill cached / FirebaseUser data immediately so fields are not blank
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String cachedName = SharedPrefManager.getInstance(this).getUserName();
        String cachedEmail = SharedPrefManager.getInstance(this).getUserEmail();

        if (cachedName != null && !cachedName.equals("User")) {
            binding.etName.setText(cachedName);
        } else if (currentUser != null && currentUser.getDisplayName() != null) {
            binding.etName.setText(currentUser.getDisplayName());
        } else {
            binding.etName.setText("User");
        }

        if (cachedEmail != null && !cachedEmail.isEmpty()) {
            binding.etEmail.setText(cachedEmail);
        } else if (currentUser != null && currentUser.getEmail() != null) {
            binding.etEmail.setText(currentUser.getEmail());
        }

        // 2. Fetch latest data from Firestore
        String uid = currentUser != null ? currentUser.getUid() : null;
        if (uid != null) {
            binding.progressBar.setVisibility(android.view.View.VISIBLE);
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        binding.progressBar.setVisibility(android.view.View.GONE);
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            if (name != null && !name.isEmpty()) {
                                binding.etName.setText(name);
                                SharedPrefManager.getInstance(this).setUserName(name);
                            }
                            if (email != null && !email.isEmpty()) {
                                binding.etEmail.setText(email);
                                SharedPrefManager.getInstance(this).setUserEmail(email);
                            }
                            Object age = doc.get("age");
                            if (age != null) {
                                binding.etAge.setText(age.toString());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(android.view.View.GONE);
                    });
        }

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name  = binding.etName.getText().toString().trim();
        String ageStr   = binding.etAge.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ageStr.isEmpty()) {
            Toast.makeText(this, "Age cannot be empty", Toast.LENGTH_SHORT).show();
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
                    .setMessage("MindEase is intended for users aged 18 and older. Minors are not permitted.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("age", ageVal);

        binding.btnSave.setEnabled(false);
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    SharedPrefManager.getInstance(this).setUserName(name);
                    Toast.makeText(this, "Profile updated! ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkPermissionAndPickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void saveProfilePicture(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return;

            File file = new File(getFilesDir(), "profile_picture.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            String localPath = file.getAbsolutePath();
            SharedPrefManager.getInstance(this).setProfilePicturePath(localPath);
            Toast.makeText(this, "Profile picture saved! 📸", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show();
        }
    }
}
