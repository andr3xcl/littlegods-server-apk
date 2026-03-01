package com.example.statsweb;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.util.TypedValue;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import android.content.res.Configuration;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String STATS_URL = "https://web.littlegods.space/";
    private static final String ADMIN_URL = "https://admin.littlegods.space/";

    private String currentUrl = STATS_URL;

    private WebView webView;
    private NestedScrollView dashboard;
    private FrameLayout webContainer;
    private FrameLayout loadingOverlay;
    private LinearLayout errorOverlay;
    private TextView errorMessage;
    private View updatePill;
    private boolean hasError = false;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final String GITHUB_URL = "https://github.com/andr3xcl?tab=repositories";
    private final String PLUTONIUM_URL = "https://forum.plutonium.pw/user/littlegods";
    private final String DISCORD_URL = "https://discord.littlegods.space";
    private final String YOUTUBE_CHANNEL_URL = "https://www.youtube.com/@Littlegods_cl";
    private final String CHANNEL_LOGO_URL = "https://yt3.googleusercontent.com/YZ7OPX5q7qKb4pFESi9knX2_16YguxEgA-r_6rpw6gAYliNhLvKbODnA87nfCm18iniKvYNz=s160-c-k-c0x00ffffff-no-rj";
    private GitHubRelease latestRelease;
    
    private ImageView channelLogo;
    private TextView subscriberCount;
    private ImageView plutoniumIcon;
    private Handler plutoniumAnimHandler = new Handler(Looper.getMainLooper());
    private boolean showingPlutoniumLogo = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive/edge-to-edge setup
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Window window = getWindow();
        
        // Use a more modern way to achieve transparency and full screen
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // For newer APIs, ensure content flows behind system bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_main);

        // Find views
        dashboard = findViewById(R.id.dashboard);
        webContainer = findViewById(R.id.webContainer);
        webView = findViewById(R.id.webView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorOverlay = findViewById(R.id.errorOverlay);
        errorMessage = findViewById(R.id.errorMessage);
        
        View btnStats = findViewById(R.id.btnStats);
        View btnAdmin = findViewById(R.id.btnAdmin);
        View btnApi = findViewById(R.id.btnApi);
        ImageButton btnBackToHome = findViewById(R.id.btnBackToHome);
        Button retryButton = findViewById(R.id.retryButton);

        // Dashboard buttons
        btnStats.setOnClickListener(v -> openWebsite(STATS_URL));
        btnAdmin.setOnClickListener(v -> openWebsite(ADMIN_URL));
        btnApi.setOnClickListener(v -> {
            Intent intent = new Intent(this, ApiDocsActivity.class);
            // Current theme state
            DayNightToggle toggle = findViewById(R.id.dayNightToggle);
            intent.putExtra("night_mode", toggle != null ? toggle.isNight() : true);
            startActivity(intent);
        });

        // Home button in WebView
        btnBackToHome.setOnClickListener(v -> showDashboard());

        retryButton.setOnClickListener(v -> {
            errorOverlay.setVisibility(View.GONE);
            loadingOverlay.setVisibility(View.VISIBLE);
            hasError = false;
            webView.loadUrl(currentUrl);
        });

        // Configure WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setTextZoom(100);

        // User agent: identify as mobile browser
        String defaultUA = settings.getUserAgentString();
        settings.setUserAgentString(defaultUA);

        // Handle page navigation inside the WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hasError = false;
                loadingOverlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!hasError) {
                    loadingOverlay.setVisibility(View.GONE);
                    errorOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    hasError = true;
                    loadingOverlay.setVisibility(View.GONE);
                    errorOverlay.setVisibility(View.VISIBLE);
                    errorMessage.setText("No se pudo conectar al servidor\nError: " + error.getDescription());
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(0xFF000000);

        updatePill = findViewById(R.id.updatePill);
        updatePill.setOnClickListener(v -> showUpdateDialog());

        // Theme Toggle Setup
        DayNightToggle dayNightToggle = findViewById(R.id.dayNightToggle);
        
        // System Theme Detection
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemNight = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        
        if (dayNightToggle != null) {
            dayNightToggle.setNight(isSystemNight, false);
            dayNightToggle.setOnThemeChangeListener(this::updateTheme);
        }
        
        // Final initial theme apply
        updateTheme(isSystemNight);

        // Community Section
        channelLogo = findViewById(R.id.channelLogo);
        subscriberCount = findViewById(R.id.subscriberCount);
        
        findViewById(R.id.btnYoutubeChannel).setOnClickListener(v -> openInBrowser(YOUTUBE_CHANNEL_URL));
        findViewById(R.id.btnGithub).setOnClickListener(v -> openInBrowser(GITHUB_URL));
        findViewById(R.id.btnDiscord).setOnClickListener(v -> openDiscord());
        findViewById(R.id.btnPlutonium).setOnClickListener(v -> openInBrowser(PLUTONIUM_URL));

        plutoniumIcon = findViewById(R.id.plutoniumIcon);
        startPlutoniumAnimation();

        checkUpdates();
        setupProfile();

        // Handle Back Press with modern API
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webContainer.getVisibility() == View.VISIBLE) {
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        showDashboard();
                    }
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void openInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void openDiscord() {
        try {
            // Try to open via Discord app if installed
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DISCORD_URL));
            intent.setPackage("com.discord");
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to browser
            openInBrowser(DISCORD_URL);
        }
    }

    private void setupProfile() {
        // Use the logo URL we found
        Glide.with(this)
                .load(CHANNEL_LOGO_URL)
                .circleCrop()
                .into(channelLogo);
        
        // For now, we set the subscriber count manually as fetched, or could fetch it via a simple request if needed
        // Since it's 23, we'll display it elegantly.
        subscriberCount.setText("23 suscriptores");
    }

    private void openWebsite(String url) {
        currentUrl = url;
        dashboard.setVisibility(View.GONE);
        webContainer.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void showDashboard() {
        webView.stopLoading();
        webView.loadUrl("about:blank");
        webContainer.setVisibility(View.GONE);
        dashboard.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    private void checkUpdates() {
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/andr3xcl/littlegods-server-apk/releases/latest")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    latestRelease = new Gson().fromJson(json, GitHubRelease.class);
                    
                    if (latestRelease != null && latestRelease.tagName != null) {
                        String currentVersion = getAppVersion();
                        if (isNewerVersion(latestRelease.tagName, currentVersion)) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                updatePill.setVisibility(View.VISIBLE);
                            });
                        }
                    }
                }
            }
        });
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        // Simple version comparison (e.g. "1.2" vs "1.1")
        // Basic implementation, can be improved to handle "v1.2" etc.
        String l = latest.replace("v", "").trim();
        String c = current.replace("v", "").trim();
        return l.compareTo(c) > 0;
    }

    private void showUpdateDialog() {
        if (latestRelease == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update, null);
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                .setView(dialogView)
                .create();

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView version = dialogView.findViewById(R.id.versionName);
        TextView changelog = dialogView.findViewById(R.id.changelogText);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdateNow);
        View btnLater = dialogView.findViewById(R.id.btnLater);

        title.setText("Nueva Versión " + latestRelease.tagName);
        version.setText("Versión " + latestRelease.tagName + " disponible");
        changelog.setText(latestRelease.body);

        btnUpdate.setOnClickListener(v -> {
            if (latestRelease.assets != null && !latestRelease.assets.isEmpty()) {
                downloadAndInstall(latestRelease.assets.get(0).browserDownloadUrl);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "No se encontró el archivo de instalación", Toast.LENGTH_SHORT).show();
            }
        });

        btnLater.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void downloadAndInstall(String url) {
        Toast.makeText(this, "Descargando actualización...", Toast.LENGTH_LONG).show();
        
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(MainActivity.this, "Error al descargar", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                File apkFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(apkFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                }

                new Handler(Looper.getMainLooper()).post(() -> installApk(apkFile));
            }
        });
    }

    private void installApk(File file) {
        Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void updateTheme(boolean isNight) {
        WaveView waveView = findViewById(R.id.waveView);
        if (waveView != null) waveView.setNightMode(isNight);
        
        ImageView bannerImage = findViewById(R.id.bannerImage);
        if (bannerImage != null) {
            bannerImage.setImageResource(isNight ? R.drawable.banner_card_youtube : R.drawable.banner_card_youtube_light);
        }

        // Colors
        int bgColor = isNight ? 0xFF0F0F0F : 0xFFE0F2F7;
        int titleColor = isNight ? 0xFFFFFFFF : 0xFF1A1A1A;
        int subtitleColor = isNight ? 0x99FFFFFF : 0x99000000;
        int sectionColor = isNight ? 0x4DFFFFFF : 0x4D000000;

        // Animate Root Background
        ValueAnimator bgAnim = ValueAnimator.ofObject(new ArgbEvaluator(), 
            ((android.graphics.drawable.ColorDrawable)dashboard.getRootView().getBackground()).getColor(), bgColor);
        bgAnim.setDuration(500);
        bgAnim.addUpdateListener(animator -> dashboard.getRootView().setBackgroundColor((int) animator.getAnimatedValue()));
        // Note: WaveView covers the background, but this is good for consistency
        bgAnim.start();

        // Dashboard overlay
        dashboard.setBackgroundColor(isNight ? 0x00000000 : 0x11FFFFFF);

        // Update Text Colors
        animateText(findViewById(R.id.appTitle), titleColor);
        animateText(findViewById(R.id.appSubtitle), subtitleColor);
        animateText(findViewById(R.id.sectionServicios), sectionColor);
        animateText(findViewById(R.id.sectionComunidad), sectionColor);
        animateText(findViewById(R.id.channelName), 0xFFFFFFFF);
        animateText(findViewById(R.id.subscriberCount), 0x99FFFFFF);

        // Update Cards
        int cardBaseColor = isNight ? 0x1AFFFFFF : 0xFFFFFFFF;
        int cardStrokeColor = isNight ? 0x20FFFFFF : 0x1A000000;
        float cardElevation = isNight ? 0f : 8f;

        updateCardStyle(findViewById(R.id.btnStats), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        updateCardStyle(findViewById(R.id.btnAdmin), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        updateCardStyle(findViewById(R.id.profileCard), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        updateCardStyle(findViewById(R.id.btnGithub), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        updateCardStyle(findViewById(R.id.btnDiscord), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        updateCardStyle(findViewById(R.id.btnPlutonium), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        updateCardStyle(findViewById(R.id.btnApi), cardBaseColor, cardStrokeColor, cardElevation, isNight);
        
        // Internal text colors for cards
        updateNestedTexts((ViewGroup) findViewById(R.id.btnStats), titleColor, subtitleColor);
        updateNestedTexts((ViewGroup) findViewById(R.id.btnAdmin), titleColor, subtitleColor);
        updateNestedTexts((ViewGroup) findViewById(R.id.btnGithub), titleColor, subtitleColor);
        updateNestedTexts((ViewGroup) findViewById(R.id.btnDiscord), titleColor, subtitleColor);
        updateNestedTexts((ViewGroup) findViewById(R.id.btnPlutonium), titleColor, subtitleColor);
        updateNestedTexts((ViewGroup) findViewById(R.id.btnApi), titleColor, subtitleColor);
    }

    private void animateText(TextView view, int toColor) {
        if (view == null) return;
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), view.getCurrentTextColor(), toColor);
        anim.setDuration(500);
        anim.addUpdateListener(animation -> view.setTextColor((int) animation.getAnimatedValue()));
        anim.start();
    }

    private void updateCardStyle(View card, int bgColor, int strokeColor, float elevation, boolean isNight) {
        if (card == null) return;
        
        // Don't overwrite background for profileCard to keep the YouTube banner
        if (card.getId() == R.id.profileCard) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                card.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, elevation, getResources().getDisplayMetrics()));
            }
            return;
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        gd.setStroke((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()), strokeColor);
        card.setBackground(gd);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, elevation, getResources().getDisplayMetrics()));
        }
    }

    private void updateNestedTexts(ViewGroup group, int titleColor, int subtitleColor) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                // If it's a big emoji (size > 24sp approx), don't color it or just use titleColor
                if (tv.getTextSize() > TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics())) {
                    // Emoji, skip coloring
                } else if (tv.getTextSize() > TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics())) {
                    animateText(tv, titleColor);
                } else {
                    animateText(tv, subtitleColor);
                }
            } else if (child instanceof ViewGroup) {
                updateNestedTexts((ViewGroup) child, titleColor, subtitleColor);
            }
        }
    }

    private void startPlutoniumAnimation() {
        plutoniumAnimHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (plutoniumIcon == null) return;
                
                // Fade out
                plutoniumIcon.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            // Switch source
                            showingPlutoniumLogo = !showingPlutoniumLogo;
                            plutoniumIcon.setImageResource(showingPlutoniumLogo ? 
                                    R.drawable.icon_plutonium : R.drawable.icon_profile_plutonium);
                            // Fade in
                            plutoniumIcon.animate()
                                    .alpha(1f)
                                    .setDuration(500)
                                    .start();
                        })
                        .start();
                
                plutoniumAnimHandler.postDelayed(this, 3000);
            }
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        if (plutoniumAnimHandler != null) {
            plutoniumAnimHandler.removeCallbacksAndMessages(null);
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    // Gson data classes
    private static class GitHubRelease {
        @SerializedName("tag_name") String tagName;
        @SerializedName("body") String body;
        @SerializedName("assets") List<Asset> assets;
    }

    private static class Asset {
        @SerializedName("browser_download_url") String browserDownloadUrl;
    }
}
