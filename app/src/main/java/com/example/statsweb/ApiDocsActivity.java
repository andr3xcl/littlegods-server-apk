package com.example.statsweb;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

public class ApiDocsActivity extends AppCompatActivity {

    private WaveView waveView;
    private LinearLayout apiContent;
    private boolean isNightMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive/edge-to-edge setup
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_api_docs);

        waveView = findViewById(R.id.waveView);
        apiContent = findViewById(R.id.apiContent);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Get theme from intent or system
        isNightMode = getIntent().getBooleanExtra("night_mode", true);
        
        populateEndpoints();
        updateTheme(isNightMode);
    }

    private void updateTheme(boolean isNight) {
        if (waveView != null) waveView.setNightMode(isNight);

        int bgColor = isNight ? 0xFF0F0F0F : 0xFFE0F2F7;
        int titleColor = isNight ? 0xFFFFFFFF : 0xFF1A1A1A;
        int subtitleColor = isNight ? 0x99FFFFFF : 0x99000000;
        int sectionColor = isNight ? 0x4DFFFFFF : 0x4D000000;

        findViewById(R.id.scrollView).setBackgroundColor(bgColor);
        
        ((TextView)findViewById(R.id.apiTitle)).setTextColor(titleColor);
        ((TextView)findViewById(R.id.apiSubtitle)).setTextColor(subtitleColor);
        
        // Update items if they exist
        for (int i = 0; i < apiContent.getChildCount(); i++) {
            View v = apiContent.getChildAt(i);
            if (v instanceof TextView) { // Section title
                ((TextView) v).setTextColor(sectionColor);
            } else if (v instanceof LinearLayout) { // Card
                updateCardStyle(v, isNight);
            }
        }
    }

    private void updateCardStyle(View card, boolean isNight) {
        int bgColor = isNight ? 0x1AFFFFFF : 0xFFFFFFFF;
        int strokeColor = isNight ? 0x20FFFFFF : 0x1A000000;
        float elevation = isNight ? 0f : 8f;

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dpToPx(24));
        gd.setStroke(dpToPx(1), strokeColor);
        card.setBackground(gd);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dpToPx(elevation));
        }
        
        // Update nested texts
        updateNestedTexts((ViewGroup) card, isNight);
    }

    private void updateNestedTexts(ViewGroup group, boolean isNight) {
        int titleColor = isNight ? 0xFFFFFFFF : 0xFF1A1A1A;
        int subtitleColor = isNight ? 0x99FFFFFF : 0x99000000;

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (tv.getId() == R.id.path) {
                    tv.setTextColor(titleColor);
                } else if (tv.getId() == R.id.description) {
                    tv.setTextColor(subtitleColor);
                }
            } else if (child instanceof ViewGroup) {
                updateNestedTexts((ViewGroup) child, isNight);
            }
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void populateEndpoints() {
        // Auth & Identity
        addSection("🔐 Auth & Identity");
        addEndpoint("GET", "/discord", "Initializes Discord OAuth2 login.");
        addEndpoint("GET", "/discord/callback", "Callback for Discord login.");
        addEndpoint("GET", "/logout", "Logs out the current user session.");
        addEndpoint("GET", "/me", "Returns current user profile (Discord data).");
        addEndpoint("GET", "/link-status", "Returns linking status and linked Plutonium accounts.");
        addEndpoint("POST", "/link-web", "Links a game account using playerName, guid, and password.");
        addEndpoint("POST", "/switch-account", "Switches the active Plutonium account for the user.");
        addEndpoint("POST", "/unlink", "Unlinks specific or all game accounts.");

        // Players & Stats
        addSection("👤 Players & Stats");
        addEndpoint("GET", "/search", "Admin: Search for players by name or GUID.");
        addEndpoint("GET", "/search-public", "Public search for players (min 3 characters).");
        addEndpoint("GET", "/:identifier/overall", "Gets comprehensive stats, map records, and matches.");

        // Economy & Rewards
        addSection("💰 Economy & Rewards");
        addEndpoint("GET", "/play/live-status", "Returns current in-game status of the player.");
        addEndpoint("POST", "/play/mystery-box", "Web-based mystery box spin (950 pts).");
        addEndpoint("POST", "/play/coin-flip", "Gambles a set amount of points on heads or tails.");
        addEndpoint("POST", "/play/mystery-box-online", "Sends weapon delivery request (2000 pts).");
        addEndpoint("POST", "/play/wunderfizz-online", "Sends perk delivery request (2000 pts).");
        addEndpoint("POST", "/play/pack-a-punch-online", "Upgrades current in-game weapon (8000 pts).");
        addEndpoint("POST", "/transfer", "Transfers points to another player.");
        addEndpoint("POST", "/claim-daily", "Claims the $5,000 daily reward.");
        addEndpoint("POST", "/welcome-reward", "Claims the one-time $30,000 welcome gift.");
        addEndpoint("GET", "/penalty-status", "Checks for lost funds due to inactivity.");
        addEndpoint("POST", "/penalty-claim", "Recovers previously lost penalty funds.");
        
        // Missions
        addSection("🎯 Missions");
        addEndpoint("GET", "/missions/global", "Lists active Daily, Weekly, and Monthly missions.");
        addEndpoint("POST", "/missions/:type/claim", "Claims reward for a completed mission.");
        addEndpoint("POST", "/missions/incentive/claim", "Claims the hourly Discord activity incentive.");

        // Maps
        addSection("🗺️ Maps");
        addEndpoint("GET", "/available", "Lists all supported maps and metadata.");
        addEndpoint("GET", "/current", "Gets currently selected map in user profile.");
        addEndpoint("POST", "/select", "Updates selected map for the user.");

        // GSC Integration
        addSection("🎮 GSC Integration (In-Game)");
        addEndpoint("POST", "/heartbeat", "Updates real-time status of a player GUID.");
        addEndpoint("GET", "/request/:guid", "In-game client polls for commands.");
        addEndpoint("GET", "/bank/balance/:guid", "Simple balance response (Text/Plain).");
        addEndpoint("POST", "/bank/transaction", "Handles in-game Bank transactions.");

        // Bot & Registry
        addSection("🤖 Bot & Registry");
        addEndpoint("GET", "/registry/data", "Returns current Discord bot settings.");
        addEndpoint("POST", "/registry/data", "Updates Discord bot settings.");
        addEndpoint("GET", "/bot/roles", "Lists available Discord roles.");
        addEndpoint("GET", "/bot/channels", "Lists available Discord channels.");
        addEndpoint("POST", "/bot/send-embed", "Sends custom Discord embed.");
        addEndpoint("POST", "/bot/edit-embed", "Edits existing custom embed.");
        addEndpoint("GET", "/bot/sent-embeds", "History of sent embeds.");

        // Admin Panel
        addSection("🛠️ Admin Panel");
        addEndpoint("POST", "/reset-daily", "Resets daily cooldown for a Discord ID.");
        addEndpoint("GET", "/missions/config", "View mission queue configuration.");
        addEndpoint("POST", "/missions/set", "Sets a new mission.");
        addEndpoint("POST", "/complete-mission", "Force-completes a mission.");
        addEndpoint("POST", "/api/economy/admin/adjust-bank", "Admin bank adjustment.");

        // GitHub Webhooks
        addSection("🔄 GitHub Webhooks");
        addEndpoint("POST", "/webhook", "Receives GitHub events and sends notifications.");
    }

    private void addSection(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        int sectionColor = isNightMode ? 0x4DFFFFFF : 0x4D000000;
        tv.setTextColor(sectionColor);
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(32), 0, dpToPx(16));
        tv.setLayoutParams(params);
        apiContent.addView(tv);
    }

    private void addEndpoint(String method, String path, String description) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_api_endpoint, apiContent, false);
        
        TextView methodTv = view.findViewById(R.id.method);
        TextView pathTv = view.findViewById(R.id.path);
        TextView descTv = view.findViewById(R.id.description);

        methodTv.setText(method);
        pathTv.setText(path);
        descTv.setText(description);

        // Styling based on method
        int methodColor = 0xFF00FFCC;
        if (method.equals("POST")) methodColor = 0xFFFFCC00;
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(adjustAlpha(methodColor, 0.2f));
        gd.setCornerRadius(dpToPx(4));
        methodTv.setBackground(gd);
        methodTv.setTextColor(methodColor);

        // updateCardStyle(view, isNightMode); // Handled in updateTheme if initially empty
        apiContent.addView(view);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        int red = android.graphics.Color.red(color);
        int green = android.graphics.Color.green(color);
        int blue = android.graphics.Color.blue(color);
        return android.graphics.Color.argb(alpha, red, green, blue);
    }
}
