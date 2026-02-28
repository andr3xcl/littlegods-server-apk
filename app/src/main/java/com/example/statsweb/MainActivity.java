package com.example.statsweb;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String STATS_URL = "http://68.211.73.62:3000/";
    private static final String ADMIN_URL = "http://68.211.73.62:1624/";

    private String currentUrl = STATS_URL;

    private WebView webView;
    private LinearLayout dashboard;
    private FrameLayout webContainer;
    private FrameLayout loadingOverlay;
    private LinearLayout errorOverlay;
    private TextView errorMessage;
    private boolean hasError = false;

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
        ImageButton btnBackToHome = findViewById(R.id.btnBackToHome);
        Button retryButton = findViewById(R.id.retryButton);

        // Dashboard buttons
        btnStats.setOnClickListener(v -> openWebsite(STATS_URL));
        btnAdmin.setOnClickListener(v -> openWebsite(ADMIN_URL));

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
    public void onBackPressed() {
        if (webContainer.getVisibility() == View.VISIBLE) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                showDashboard();
            }
        } else {
            super.onBackPressed();
        }
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

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
