package com.mindease.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.mindease.app.databinding.ActivityChatBinding;
import java.util.ArrayList;
import java.util.List;

// AI-powered chat companion screen that provides mental wellness support.
// Uses the Gemini generative AI SDK for conversational responses, with a
// rule-based fallback system when the API is unavailable. Chat messages
// containing activity keywords (e.g. "Breathing", "Bubble Pop") are rendered
// as tappable links that navigate to the corresponding activity.
public class ChatActivity extends AppCompatActivity {

    // System prompt instructing the AI model on persona and response format.
    private static final String SYSTEM_PROMPT =
            "You are MindEase, a compassionate mental wellness AI companion. " +
            "Whenever appropriate, recommend activities using these exact phrases so they can be clicked: " +
            "\"Breathing\" (for relaxing exercises), \"Bubble Pop\" (for popping bubbles), " +
            "\"Star Catcher\" (for catching stars), or \"Color Sort\" (for sorting colors). " +
            "Keep responses warm, supportive, and under 3 sentences. User says: ";

    // Keywords that trigger clickable spans in AI message bubbles.
    private static final String[] CLICKABLE_KEYWORDS = {
            "Breathing Exercise", "Breathing Exercises", "Breathing",
            "Bubble Pop", "Star Catcher", "Color Sort", "Games"
    };

    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatFutures chatSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new ChatAdapter(messages);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        // Scroll to bottom when keyboard appears so the latest message stays visible
        binding.rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                binding.rvMessages.postDelayed(() -> {
                    if (messages.size() > 0) {
                        binding.rvMessages.scrollToPosition(messages.size() - 1);
                    }
                }, 100);
            }
        });

        addMessage("Hello! 👋 I'm MindEase AI. If you are feeling stressed, try a Breathing exercise or play a game of Bubble Pop to relax! How are you feeling today?", false);

        initGeminiSession();

        binding.btnSend.setOnClickListener(v -> sendMessage());

        // Quick reply chips for common user intents
        binding.chipBreathing.setOnClickListener(v -> sendQuick("Can you guide me through a breathing exercise?"));
        binding.chipBetter.setOnClickListener(v -> sendQuick("I'm feeling better now, thank you!"));
        binding.chipMore.setOnClickListener(v -> sendQuick("Tell me more about managing stress."));
    }

    // Initializes the Gemini generative AI chat session.
// Falls back gracefully to rule-based replies if the API key is missing.
    private void initGeminiSession() {
        try {
            String apiKey = BuildConfig.GEMINI_API_KEY;
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("null")) {
                android.util.Log.w("MindEaseChat", "Gemini API key not configured; using rule-based replies.");
                chatSession = null;
                return;
            }
            GenerativeModel model = new GenerativeModel("gemini-2.5-flash", apiKey);
            GenerativeModelFutures modelFutures = GenerativeModelFutures.from(model);
            chatSession = modelFutures.startChat();
        } catch (Exception e) {
            android.util.Log.e("MindEaseChat", "Failed to initialize Gemini session", e);
            chatSession = null;
        }
    }

    // Populates the input field and immediately sends the message.
    private void sendQuick(String text) {
        binding.etMessage.setText(text);
        sendMessage();
    }

    // Reads user input, adds it to the message list, and dispatches
// either an AI-powered or rule-based response.
    private void sendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        binding.etMessage.setText("");
        addMessage(text, true);

        if (chatSession != null) {
            sendToGemini(text);
        } else {
            addMessage(getRuleBasedReply(text), false);
        }
    }

    // Sends the user message to the Gemini API and handles the async response.
    private void sendToGemini(String userText) {
        try {
            Content content = new Content.Builder()
                    .addText(SYSTEM_PROMPT + userText)
                    .build();

            Futures.addCallback(chatSession.sendMessage(content),
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            String reply = result.getText();
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (reply != null && !reply.trim().isEmpty()) {
                                    addMessage(reply, false);
                                } else {
                                    addMessage(getRuleBasedReply(userText), false);
                                }
                            });
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            android.util.Log.e("MindEaseChat", "Gemini request failed", t);
                            new Handler(Looper.getMainLooper()).post(() ->
                                    addMessage(getRuleBasedReply(userText), false));
                        }
                    }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            android.util.Log.e("MindEaseChat", "Exception sending message", e);
            addMessage(getRuleBasedReply(userText), false);
        }
    }

    // Appends a message to the list and scrolls to the bottom.
    private void addMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);
    }

    // Provides keyword-matched wellness responses when the AI is unavailable.
// Checks for mood-related keywords and suggests appropriate activities.
    private String getRuleBasedReply(String msg) {
        msg = msg.toLowerCase();
        if (msg.contains("sad") || msg.contains("unhappy") || msg.contains("cry"))
            return "I'm sorry you're feeling sad. Would you like to try a Breathing exercise? 🌬";
        if (msg.contains("anxious") || msg.contains("anxiety") || msg.contains("worried") || msg.contains("stress"))
            return "Anxiety can be tough. Try the 4-7-8 Breathing technique, or play Bubble Pop to distract your mind. 💙";
        if (msg.contains("game") || msg.contains("play") || msg.contains("bored"))
            return "Sure! Try playing Bubble Pop or Star Catcher to refresh your mood! 🎮";
        if (msg.contains("happy") || msg.contains("great") || msg.contains("good") || msg.contains("better"))
            return "That's wonderful! Keep that positive energy going! 😊";
        if (msg.contains("help"))
            return "I'm here for you. You can log your mood, try Breathing exercises, or talk to me anytime. 💙";
        if (msg.contains("breathing") || msg.contains("breathe"))
            return "Great choice! Head to the Breathing section for guided exercises. 🌿";
        return "Thank you for sharing. Remember, it's okay to feel what you feel. Try a Breathing exercise or play a game to relax. 🌿";
    }

        // Inner classes
        // Simple data holder for a single chat message.
    static class ChatMessage {
        final String text;
        final boolean isUser;

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    // RecyclerView adapter that renders user messages (right-aligned) and
// AI messages (left-aligned) with clickable activity suggestion links.
    static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_USER = 1;
        private static final int VIEW_TYPE_AI = 0;

        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).isUser ? VIEW_TYPE_USER : VIEW_TYPE_AI;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == VIEW_TYPE_USER
                    ? R.layout.item_chat_user : R.layout.item_chat_ai;
            View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            MessageViewHolder vh = (MessageViewHolder) holder;
            Context ctx = vh.itemView.getContext();

            if (getItemViewType(position) == VIEW_TYPE_AI) {
                vh.tvText.setText(buildClickableText(ctx, msg.text));
                vh.tvText.setMovementMethod(LinkMovementMethod.getInstance());
                bindSuggestionButtons(vh, msg, ctx);
            } else {
                vh.tvText.setText(msg.text);
            }
        }

        // Shows or hides quick-launch buttons based on keywords in the AI message.
        private void bindSuggestionButtons(MessageViewHolder vh, ChatMessage msg, Context ctx) {
            boolean showBreathing = msg.text.toLowerCase().contains("breathing") || msg.text.toLowerCase().contains("breathe");
            boolean showBubblePop = msg.text.toLowerCase().contains("bubble pop");
            boolean showStarCatcher = msg.text.toLowerCase().contains("star catcher");
            boolean showColorSort = msg.text.toLowerCase().contains("color sort");

            if (vh.layoutSuggestions != null) {
                if (showBreathing || showBubblePop || showStarCatcher || showColorSort) {
                    vh.layoutSuggestions.setVisibility(View.VISIBLE);
                    configureSuggestionButton(vh.btnSuggestBreathing, showBreathing, ctx, BreathingActivity.class);
                    configureSuggestionButton(vh.btnSuggestBubblePop, showBubblePop, ctx, BubblePopGameActivity.class);
                    configureSuggestionButton(vh.btnSuggestStarCatcher, showStarCatcher, ctx, StarCatcherActivity.class);
                    configureSuggestionButton(vh.btnSuggestColorSort, showColorSort, ctx, ColorSortGameActivity.class);
                } else {
                    vh.layoutSuggestions.setVisibility(View.GONE);
                }
            }
        }

        // Configures visibility and click target for a single suggestion button.
        private void configureSuggestionButton(View btn, boolean show, Context ctx, Class<?> targetActivity) {
            if (btn == null) return;
            if (show) {
                btn.setVisibility(View.VISIBLE);
                btn.setOnClickListener(v -> ctx.startActivity(new Intent(ctx, targetActivity)));
            } else {
                btn.setVisibility(View.GONE);
            }
        }

        // Scans AI response text for activity keywords and wraps each occurrence
// in a ClickableSpan that launches the corresponding activity.
        private SpannableString buildClickableText(Context context, String text) {
            SpannableString spannable = new SpannableString(text);
            String lowerText = text.toLowerCase();

            for (String keyword : CLICKABLE_KEYWORDS) {
                String lowerKeyword = keyword.toLowerCase();
                int searchStart = 0;

                while ((searchStart = lowerText.indexOf(lowerKeyword, searchStart)) != -1) {
                    int end = searchStart + keyword.length();
                    final String matchedKeyword = keyword;

                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            Intent intent = resolveActivityIntent(context, matchedKeyword);
                            if (intent != null) {
                                context.startActivity(intent);
                            }
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setColor(ContextCompat.getColor(context, R.color.primary));
                            ds.setUnderlineText(true);
                            ds.setFakeBoldText(true);
                        }
                    };
                    spannable.setSpan(clickableSpan, searchStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    searchStart = end;
                }
            }
            return spannable;
        }

        // Maps a keyword string to the corresponding activity intent.
        private Intent resolveActivityIntent(Context context, String keyword) {
            if (keyword.contains("Breathing")) {
                return new Intent(context, BreathingActivity.class);
            } else if (keyword.contains("Bubble Pop")) {
                return new Intent(context, BubblePopGameActivity.class);
            } else if (keyword.contains("Star Catcher")) {
                return new Intent(context, StarCatcherActivity.class);
            } else if (keyword.contains("Color Sort")) {
                return new Intent(context, ColorSortGameActivity.class);
            } else if (keyword.contains("Games")) {
                return new Intent(context, GamesActivity.class);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        // ViewHolder for both user and AI message item layouts.
        static class MessageViewHolder extends RecyclerView.ViewHolder {
            final TextView tvText;
            final View layoutSuggestions;
            final View btnSuggestBreathing, btnSuggestBubblePop;
            final View btnSuggestStarCatcher, btnSuggestColorSort;

            MessageViewHolder(@NonNull View v) {
                super(v);
                tvText = v.findViewById(R.id.tvChatText);
                layoutSuggestions = v.findViewById(R.id.layoutSuggestions);
                btnSuggestBreathing = v.findViewById(R.id.btnSuggestBreathing);
                btnSuggestBubblePop = v.findViewById(R.id.btnSuggestBubblePop);
                btnSuggestStarCatcher = v.findViewById(R.id.btnSuggestStarCatcher);
                btnSuggestColorSort = v.findViewById(R.id.btnSuggestColorSort);
            }
        }
    }
}
