package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import com.mindease.app.databinding.ActivityQuestionnaireBinding;

// Pre-registration questionnaire that collects user demographics and
// mental wellness goals before proceeding to the sign-up screen.
// Captures role, age category, and primary challenges via dropdown spinners.
public class QuestionnaireActivity extends AppCompatActivity {

    private ActivityQuestionnaireBinding binding;

    private final String[] roles = {"Student", "Employee", "Entrepreneur", "Retired", "Homemaker", "Other"};
    private final String[] ageCategories = {"Under 18", "18-24", "25-34", "35-44", "45+"};
    private final String[] challenges = {"Anxiety", "Stress", "Sleep Problems", "Low Mood", "Relationship Issues", "Other"};
    private final String[] discoverySources = {"Social Media", "Friend/Family", "App Store", "Web Search", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionnaireBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRole.setAdapter(roleAdapter);

        // Setup age spinner
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ageCategories);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAgeCategory.setAdapter(ageAdapter);

        // Setup challenges spinner
        ArrayAdapter<String> challengesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, challenges);
        challengesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerIssues.setAdapter(challengesAdapter);

        // Setup hear-about spinner
        ArrayAdapter<String> hearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, discoverySources);
        hearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerHearAbout.setAdapter(hearAdapter);

        binding.btnContinue.setOnClickListener(v -> navigateToSignup());
    }

    private void navigateToSignup() {
        String roleStr = roles[binding.spinnerRole.getSelectedItemPosition()];
        String ageStr = ageCategories[binding.spinnerAgeCategory.getSelectedItemPosition()];
        String issueStr = challenges[binding.spinnerIssues.getSelectedItemPosition()];
        String referrerStr = discoverySources[binding.spinnerHearAbout.getSelectedItemPosition()];

        if ("Under 18".equals(ageStr)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Age Restriction 🔞")
                    .setMessage("MindEase is intended for users aged 18 and older. Registration is not permitted for minors.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Intent intent = new Intent(this, SignupActivity.class);
        intent.putExtra("occupation", roleStr);
        intent.putExtra("ageCategory", ageStr);
        intent.putExtra("issues", issueStr);
        intent.putExtra("discoverySource", referrerStr);
        startActivity(intent);
    }
}
