package com.poolmonitor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final String TAG = "PoolMonitor";
    private static final String PREFS_NAME = "PoolMonitorPrefs";
    private static final String PREF_ADDRESS = "btc_address";
    private static final String[] ALLOWED_HOSTS = {
        "public-pool.io",
        "mempool.space",
        "api.coingecko.com"
    };

    private WebView webView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark status/nav bars
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#0d1117"));
        window.setNavigationBarColor(Color.parseColor("#0d1117"));

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        webView = new WebView(this);
        setContentView(webView);

        hardenWebView();

        webView.addJavascriptInterface(new AddressBridge(), "AndroidBridge");
        webView.setWebViewClient(new SecureWebViewClient());
        webView.setWebChromeClient(new SecureWebChromeClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void hardenWebView() {
        WebSettings settings = webView.getSettings();

        // JS required for app functionality
        settings.setJavaScriptEnabled(true);

        // Block all file/content access from JS
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        // No location, no passwords, no form data
        settings.setGeolocationEnabled(false);
        settings.setSavePassword(false);
        settings.setSaveFormData(false);

        // No cache - prevents sensitive data at rest
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // DOM storage needed for app state
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);

        // Block mixed HTTP/HTTPS content
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Viewport
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);

        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadsImagesAutomatically(true);

        // No remote debugging
        WebView.setWebContentsDebuggingEnabled(false);
    }

    private class SecureWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();
            String scheme = url.getScheme();
            // Allow local file loading only
            if ("file".equals(scheme)) {
                return false;
            }
            Log.w(TAG, "Blocked navigation to: " + url.toString());
            return true; // Block everything else
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();
            String scheme = url.getScheme();
            String host = url.getHost();

            // Allow local assets
            if ("file".equals(scheme)) {
                return null;
            }

            // Allow only HTTPS to whitelisted hosts
            if ("https".equals(scheme) && host != null) {
                for (String allowed : ALLOWED_HOSTS) {
                    if (host.equals(allowed) || host.endsWith("." + allowed)) {
                        return null;
                    }
                }
            }

            Log.w(TAG, "Blocked request to: " + url.toString());
            return new WebResourceResponse("text/plain", "utf-8", null);
        }
    }

    private class SecureWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                Log.e(TAG, "JS Error: " + consoleMessage.message()
                    + " [" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "]");
            }
            return true;
        }
    }

    public class AddressBridge {

        @JavascriptInterface
        public String loadAddress() {
            return prefs.getString(PREF_ADDRESS, "");
        }

        @JavascriptInterface
        public boolean saveAddress(String address) {
            if (address == null || address.length() > 100) {
                Log.w(TAG, "Rejected address: invalid length");
                return false;
            }
            // Alphanumeric only - covers legacy, P2SH, and bech32 addresses
            if (address.matches("^[a-zA-Z0-9]{25,90}$")) {
                prefs.edit().putString(PREF_ADDRESS, address).apply();
                return true;
            }
            Log.w(TAG, "Rejected address: invalid characters");
            return false;
        }

        @JavascriptInterface
        public void clearAddress() {
            prefs.edit().remove(PREF_ADDRESS).apply();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
