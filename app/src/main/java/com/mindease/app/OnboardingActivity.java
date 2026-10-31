package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.mindease.app.databinding.ActivityOnboardingBinding;


// First-time onboarding carousel that introduces the three core app features:
// mood tracking, breathing exercises, and AI chat support. Uses a ViewPager2
// with dot indicators and skip/next navigation.
public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

    private final String[] icons   = {"🌿", "🧘", "💬"};
    private final String[] titles  = {"Track Your Emotions", "Breathe & Relax", "Chat & Get Support"};
    private final String[] descs   = {
        "Log your mood daily and discover patterns that help you understand your emotional wellbeing.",
        "Use guided breathing exercises to calm your mind and reduce stress anytime, anywhere.",
        "Talk to our AI companion whenever you need support. You're never alone on your journey."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OnboardingAdapter adapter = new OnboardingAdapter(icons, titles, descs);
        binding.viewPager.setAdapter(adapter);

        // Build dots
        buildDots(0);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                buildDots(position);
                if (position == titles.length - 1) {
                    binding.btnNext.setText("Get Started 🚀");
                } else {
                    binding.btnNext.setText("Next");
                }
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            int current = binding.viewPager.getCurrentItem();
            if (current < titles.length - 1) {
                binding.viewPager.setCurrentItem(current + 1);
            } else {
                SharedPrefManager.getInstance(this).setOnboardingDone();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });

        binding.btnSkip.setOnClickListener(v -> {
            SharedPrefManager.getInstance(this).setOnboardingDone();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void buildDots(int activeIndex) {
        binding.dotsLayout.removeAllViews();
        int dotCount = titles.length;
        for (int i = 0; i < dotCount; i++) {
            View dot = new View(this);
            int size = (i == activeIndex) ? dpToPx(24) : dpToPx(8);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, dpToPx(8));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == activeIndex
                    ? R.drawable.bg_tab_active
                    : R.drawable.circle_gray);
            binding.dotsLayout.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Adapter
    static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideVH> {
        private final String[] icons, titles, descs;

        OnboardingAdapter(String[] icons, String[] titles, String[] descs) {
            this.icons = icons;
            this.titles = titles;
            this.descs = descs;
        }

        @NonNull
        @Override
        public SlideVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideVH holder, int position) {
            holder.tvIcon.setText(icons[position]);
            holder.tvTitle.setText(titles[position]);
            holder.tvDesc.setText(descs[position]);
        }

        @Override
        public int getItemCount() { return titles.length; }

        static class SlideVH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvTitle, tvDesc;
            SlideVH(@NonNull View itemView) {
                super(itemView);
                tvIcon  = itemView.findViewById(R.id.tvSlideIcon);
                tvTitle = itemView.findViewById(R.id.tvSlideTitle);
                tvDesc  = itemView.findViewById(R.id.tvSlideDesc);
            }
        }
    }
}
