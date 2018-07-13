package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;

public class BrowserActivity extends AppCompatActivity {

    protected WebView webView;
    protected TextView titleView;

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        initView();
    }

    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView firstButton = findViewById(R.id.firstButton);
        firstButton.setClickable(true);
        firstButton.setText(R.string.first_button);
        firstButton.setTypeface(Typeface.createFromAsset(getAssets(), "apps.ttf"));
        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        titleView = findViewById(R.id.titleView);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setUserAgentString(BuildConfig.USER_AGENT);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onReceivedTitle(WebView view, final String title) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleView.setText(title);
                    }
                });
            }
        });
        webView.setWebViewClient(new WebViewClient());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent();
                webView.loadUrl(intent.getStringExtra("url"));
            }
        });

    }

}
