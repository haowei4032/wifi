package hk.haowei.wifi.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.R;

public class MainActivity extends AppCompatActivity implements AMapLocationListener {

    protected WifiManager wifiManager;
    protected ConnectivityManager connectivityManager;
    protected LocationManager locationManager;
    protected TelephonyManager telephonyManager;

    protected ListView listView;
    protected LinearLayout wifiPassword;
    protected LinearLayout wifiDisconnect;
    protected TextView currentLocation;
    protected TextView loadingText;
    protected TextView wifiNumber;
    protected Switch wifiSwitch;
    protected TextView autoConnected;
    protected TextView wifiListNumber;
    protected RelativeLayout notConnected;
    protected TextView wifiSsid;
    protected SwipeRefreshLayout noResult;
    protected RelativeLayout hasResult;
    protected RelativeLayout hasConnected;
    protected RelativeLayout noWifiService;
    protected RelativeLayout noLocationService;
    protected RelativeLayout loadingAnimation;
    protected SwipeRefreshLayout swipeRefreshView;

    protected AlertDialog alertDialog;

    private AMapLocationClient mLocationClient = null;
    public AMapLocationClientOption mLocationOption = null;

    protected boolean wifiShared = false;
    protected boolean doConnecting = false;
    protected String connectingSSID;
    protected String connectingPassword;
    protected String connectingBSSID;
    protected String connectingCipher;
    protected String autoConnectBssid = null;
    protected int connectingNetworkId = 0;
    protected int autoConnectingIndex = -1;

    protected JSONArray wifilist;
    protected String address;

    protected Worker worker = new Worker(this);

    protected static class Worker extends Handler {

        private WeakReference<MainActivity> activity;

        Worker(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    activity.get().queryHasKey();
                    break;
            }
        }
    }

    protected Context getContext() {
        return this;
    }

    protected void newVersionUpgrade(final JSONObject result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final AlertDialog alertDialog = new AlertDialog.Builder(getContext()).setView(R.layout.widget_upgrade).create();
                    alertDialog.show();
                    LinearLayout versionDescript = alertDialog.getWindow().findViewById(R.id.versionDescript);
                    TextView textView;
                    TextView confirmButton = alertDialog.getWindow().findViewById(R.id.confirmButton);
                    confirmButton.setClickable(true);
                    confirmButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                alertDialog.dismiss();
                                AAplication.instance.get().downloadURL = result.getString("url");
                            }catch (Exception e) {

                            }
                        }
                    });

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0,0, dip2px(5));

                    for (int i = 0; i < result.getJSONArray("text").length(); i++) {
                        textView = new TextView(getContext());
                        textView.setText(result.getJSONArray("text").getString(i));
                        textView.setTextColor(Color.parseColor("#444444"));
                        textView.setLayoutParams(params);
                        versionDescript.addView(textView);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected int dip2px(double dip) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (scale * dip + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);


        loadingAnimation = findViewById(R.id.loadingAnimation);
        loadingText = findViewById(R.id.loadingText);
        wifiSwitch = findViewById(R.id.wifiSwitch);
        autoConnected = findViewById(R.id.autoConnected);
        wifiNumber = findViewById(R.id.wifiNumber);
        wifiListNumber = findViewById(R.id.wifiListNumber);
        hasResult = findViewById(R.id.hasResult);
        noResult = findViewById(R.id.noResult);
        hasConnected = findViewById(R.id.hasConnected);
        notConnected = findViewById(R.id.notConnected);
        wifiSsid = findViewById(R.id.wifiSsid);
        noWifiService = findViewById(R.id.noWifiService);
        noLocationService = findViewById(R.id.noLocationService);
        currentLocation = findViewById(R.id.currentLocation);
        wifiPassword = findViewById(R.id.wifiPassword);
        wifiDisconnect = findViewById(R.id.wifiDisconnect);
        swipeRefreshView = findViewById(R.id.swipeRefresh);
        listView = findViewById(R.id.listView);

        noResult.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        noResult.setRefreshing(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wifiScan();
                            }
                        });
                    }
                }, 1800);
            }

        });



        initView();

        initReceiver();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int channelId = BuildConfig.CHANNEL_ID;
                    int versionCode = BuildConfig.VERSION_CODE;
                    String body = HttpUtils.get(BuildConfig.URL + "app/checkVersion?versionCode=" + versionCode + "&channelId="+ channelId +"&_random=" + System.currentTimeMillis());
                    Log.e("HTTP.VERSION", body);
                    JSONObject result = new JSONObject(body);
                    if (result.opt("result") != null) {
                        if (result.getJSONObject("result").optInt("force", 0) == 1) {
                            Log.e("UPGRADE.BROADCAST", "111");

                            try {
                                JSONObject data = new JSONObject();
                                data.put("url", result.getJSONObject("result").getString("url"));
                                data.put("text", result.getJSONObject("result").getJSONArray("text"));
                                newVersionUpgrade(data);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    protected void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        //filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);

        registerReceiver(new BroadcastReceiver() {

            protected boolean disconnected = false;
            protected boolean connected = false;
            protected boolean wrong_password = false;

            @Override
            public void onReceive(Context context, final Intent intent) {

                switch (intent.getAction()) {

                    //WiFi身份验证
                    case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                        int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1000);
                        if (!wrong_password && error == WifiManager.ERROR_AUTHENTICATING) {
                            Log.e("WIFI", "WRONG_PASSWORD");

                            wifiManager.removeNetwork(connectingNetworkId);
                            wifiManager.saveConfiguration();

                            if (doConnecting) {
                                doConnecting = false;
                                if (connectingSSID != null) {

                                    if (listView != null && autoConnectingIndex > -1) {
                                        final View childView = listView.getChildAt(autoConnectingIndex);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                childView.setEnabled(true);
                                                childView.findViewById(R.id.itemRightImage).setVisibility(View.VISIBLE);
                                                childView.findViewById(R.id.itemRightLoading).setVisibility(View.GONE);
                                            }
                                        });

                                        autoConnectingIndex = -1;
                                    }

                                    prepareConnect(connectingSSID);
                                }
                            } else {
                                if (autoConnectBssid != null && autoConnectBssid.length() > 0) {

                                    if (listView != null && autoConnectingIndex > -1) {
                                        final View childView = listView.getChildAt(autoConnectingIndex);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                childView.setEnabled(true);
                                                childView.findViewById(R.id.itemRightImage).setVisibility(View.VISIBLE);
                                                childView.findViewById(R.id.itemRightLoading).setVisibility(View.GONE);
                                            }
                                        });

                                        autoConnectingIndex = -1;
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            autoConnected.setText(R.string.auto_connected);
                                            autoConnected.setEnabled(true);

                                            Toast.makeText(getContext(), String.format(getString(R.string.one_connect_wrong_password), connectingSSID), Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String body = String.
                                                    format("ssid=%s&bssid=%s&cipher=%s" +
                                                                    "&password=%s&deviceId=%s&" +
                                                                    "latitude=%s&longitude=%s",
                                                            connectingSSID,
                                                            connectingBSSID,
                                                            connectingCipher,
                                                            connectingPassword,
                                                            AAplication.instance.get().deviceId,
                                                            AAplication.instance.get().latitude,
                                                            AAplication.instance.get().longitude);
                                            body = HttpUtils.post(BuildConfig.URL + "app/hotspotReport", body);
                                            Log.e("HTTP.POT.REPORT", body);

                                            //AAplication.instance.get().sendEmptyMessage(1);

                                        }
                                    }).start();
                                }
                            }
                        }
                        break;

                    //位置服务变化
                    case LocationManager.PROVIDERS_CHANGED_ACTION:

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!wifiManager.isWifiEnabled()) return;

                                noResult.setVisibility(View.GONE);
                                hasResult.setVisibility(View.GONE);
                                noWifiService.setVisibility(View.GONE);
                                noLocationService.setVisibility(View.GONE);

                                loadingAnimation.setVisibility(View.VISIBLE);
                                loadingText.setText(R.string.wifi_scanning);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingAnimation.setVisibility(View.GONE);
                                        wifiScan();
                                    }
                                }, 1800);

                            }
                        });

                        if (checkLocationStatus()) onPosition();

                        break;

                    //WiFi状态变化
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1000);
                        switch (state) {
                            case WifiManager.WIFI_STATE_ENABLED:
                                Log.e("WIFI_STATE", "ENABLED");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiSwitch.setChecked(true);
                                        noWifiService.setVisibility(View.GONE);
                                        notConnected.setVisibility(View.VISIBLE);
                                        hasConnected.setVisibility(View.GONE);
                                        noResult.setVisibility(View.GONE);
                                        hasResult.setVisibility(View.GONE);

                                        loadingAnimation.setVisibility(View.VISIBLE);
                                        loadingText.setText(R.string.wifi_scanning);
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                loadingAnimation.setVisibility(View.GONE);
                                                if (wifiManager.isWifiEnabled()) wifiScan();
                                            }
                                        }, 1800);

                                    }
                                });
                                break;
                            case WifiManager.WIFI_STATE_ENABLING:
                                Log.e("WIFI_STATE", "ENABLING");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        noResult.setVisibility(View.GONE);
                                        notConnected.setVisibility(View.VISIBLE);
                                        noWifiService.setVisibility(View.GONE);
                                        loadingAnimation.setVisibility(View.VISIBLE);
                                        loadingText.setText(R.string.openning_wifi_service);
                                    }
                                });
                                break;
                            case WifiManager.WIFI_STATE_DISABLED:
                                Log.e("WIFI_STATE", "DISABLED");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        wifiSwitch.setChecked(false);
                                        noLocationService.setVisibility(View.GONE);
                                        noWifiService.setVisibility(View.GONE);
                                        hasConnected.setVisibility(View.GONE);
                                        notConnected.setVisibility(View.GONE);
                                        hasResult.setVisibility(View.GONE);
                                        noResult.setVisibility(View.GONE);
                                        loadingAnimation.setVisibility(View.GONE);
                                        noWifiService.setVisibility(View.VISIBLE);
                                    }
                                });
                                break;
                            case WifiManager.WIFI_STATE_DISABLING:
                                Log.e("WIFI_STATE", "DISABLING");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                       hasResult.setVisibility(View.GONE);
                                        noResult.setVisibility(View.GONE);
                                    }
                                });
                                break;
                            default:

                        }
                        break;

                    // WiFi网络变化
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                            if (!connected) {
                                connected = true;
                                Log.e("WIFI_NETWORK_STATE", "CONNECTED");

                                if (doConnecting) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (connectingPassword == null
                                                    || connectingCipher == null
                                                    || connectingPassword.equals("")
                                                    || connectingCipher.equals("")) return ;

                                            if (!wifiShared) return ;

                                            try {
                                                Thread.sleep(800);
                                            }catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String latitude = "";
                                            String longitude = "";
                                            String deviceId = "";



                                            if (PackageManager.PERMISSION_GRANTED ==
                                                    checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                                                    &&
                                                    PackageManager.PERMISSION_GRANTED ==
                                                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                try {
                                                    Criteria criteria = new Criteria();
                                                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                                                    criteria.setAltitudeRequired(false);
                                                    criteria.setBearingRequired(false);
                                                    criteria.setCostAllowed(true);
                                                    criteria.setPowerRequirement(Criteria.POWER_LOW);

                                                    Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));
                                                    if (location != null) {
                                                        longitude += location.getLongitude();
                                                        latitude += location.getLatitude();
                                                    }
                                                    deviceId = telephonyManager.getDeviceId();

                                                } catch (Exception e) {
                                                    Log.e("GEOLOCATION", e.getMessage());
                                                }
                                            }

                                            try {
                                                String body = "ssid=" + connectingSSID;
                                                body += "&";
                                                body += "bssid=" + connectingBSSID;
                                                body += "&";
                                                body += "cipher=" + connectingCipher;
                                                body += "&";
                                                body += "password=" + connectingPassword;
                                                body += "&";
                                                body += "latitude=" + latitude;
                                                body += "&";
                                                body += "longitude=" + longitude;
                                                body += "&";
                                                body += "deviceId=" + deviceId;
                                                body = HttpUtils.post(BuildConfig.URL +
                                                        "app/userShareWifi", body);
                                                Log.e("HTTP.WIFI", body);
                                            } catch (Exception e) {
                                                Log.e("WIFI.SHARE", e.getMessage());
                                            }
                                        }
                                    }).start();
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            autoConnected.setText(R.string.auto_connected);
                                            autoConnected.setEnabled(true);

                                            notConnected.setVisibility(View.GONE);
                                            hasConnected.setVisibility(View.VISIBLE);
                                            WifiInfo info = wifiManager.getConnectionInfo();
                                            wifiSsid.setText(info.getSSID().replace("\"", ""));
                                            wifiScan();
                                        }catch (Exception e) {
                                            Log.e("DOGE", e.getMessage());
                                        }
                                    }
                                });

                                onPosition();
                            }
                        } else if (info.getState().equals(NetworkInfo.State.CONNECTING)) {
                            Log.e("WIFI_NETWORK_STATE", "CONNECTING");

                        } else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                            if (connected) {
                                autoConnectBssid = "";
                                connected = false;
                                Log.e("WIFI_NETWORK_STATE", "DISCONNECTED");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiScan();
                                        hasConnected.setVisibility(View.GONE);
                                        notConnected.setVisibility(View.VISIBLE);
                                    }
                                });
                            }

                        } else if (info.getState().equals(NetworkInfo.State.DISCONNECTING)) {
                            //Log.e("WIFI_NETWORK_STATE", "DISCONNECTING");
                            autoConnectBssid = "";
                            connected = false;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    wifiScan();
                                    hasConnected.setVisibility(View.GONE);
                                    notConnected.setVisibility(View.VISIBLE);
                                }
                            });

                        } else {
                            Log.e("WIFI_NETWORK_STATE", "UNKNOW");
                        }
                        break;
                    case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                       ///
                        break;
                }
            }
        }, filter);
    }

    protected void initView() {

        TextView titleView = findViewById(R.id.titleView);
        autoConnected.setClickable(true);
        autoConnected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ONE.CONNECT", "click");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("WIFI.LIST.LENGTH", "" + wifilist.length());
                        if (wifilist.length() > 0) {
                            try {
                                for (int i = 0; i < wifilist.length(); i++) {
                                    if (wifilist.getJSONObject(i).opt("password") != null) {
                                        if (autoConnectBssid != null &&
                                                autoConnectBssid.equals(
                                                        wifilist.getJSONObject(i).
                                                                getString("bssid")))
                                            continue;

                                        autoConnectingIndex = i;

                                        connectingCipher = wifilist.getJSONObject(i).getString("cipher");

                                        autoConnectBssid = connectingBSSID = wifilist.getJSONObject(i).getString("bssid");
                                        String ssid = connectingSSID = wifilist.getJSONObject(i).getString("ssid");
                                        String password = connectingPassword = wifilist.getJSONObject(i).getString("password");

                                        final View childView = listView.getChildAt(autoConnectingIndex);

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                childView.setEnabled(false);
                                                childView.findViewById(R.id.itemRightImage).setVisibility(View.GONE);
                                                childView.findViewById(R.id.itemRightLoading).setVisibility(View.VISIBLE);

                                                autoConnected.setText(String.format(getString(R.string.one_connecting), connectingSSID));
                                                autoConnected.setEnabled(false);
                                            }
                                        });


                                        Log.e("ONE.SSID", connectingSSID);
                                        Log.e("ONE.BSSID", connectingBSSID);
                                        wifiConnect(ssid, password, connectingCipher);
                                        break;

                                    }
                                }
                            }catch (Exception e) {
                                Log.e("ONE.CONNECT.ERROR", e.getMessage());
                            }
                        }
                    }
                }).start();
            }
        });
        titleView.setClickable(true);
        titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(getContext(), MapActivity.class));
                startActivity(new Intent(getContext(), MineActivity.class));
            }
        });

        if (wifiManager.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
        } else {
            wifiSwitch.setChecked(false);
        }

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               wifiSwitch.setChecked(isChecked);
               if (isChecked) {
                   if (!wifiManager.setWifiEnabled(true)) {
                       wifiSwitch.setChecked(false);
                   }
                }else{
                    if (wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(false);
                }
            }
        });

        swipeRefreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshView.setRefreshing(false);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wifiScan();
                            }
                        });

                    }
                }, 1800);



            }

        });

        TextView wifiItemPassword = findViewById(R.id.wifiItemPassword);
        TextView wifiItemDisconnect = findViewById(R.id.wifiItemDisconnect);

        Drawable drawable = getDrawable(R.drawable.view_pwd);
        drawable.setBounds(0, 0, dip2px(20), dip2px(20));
        wifiItemPassword.setCompoundDrawables(drawable, null, null, null);


        wifiPassword.setClickable(true);
        wifiPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext()).setTitle("查看密码").setMessage("密码被隐藏不可见").create().show();
            }
        });

        drawable = getDrawable(R.drawable.wifi_disconnect);
        drawable.setBounds(0, 0, dip2px(20), dip2px(20));
        wifiItemDisconnect.setCompoundDrawables(drawable, null, null, null);

        wifiDisconnect.setClickable(true);
        wifiDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.disconnect();
            }
        });

    }

    protected void queryHasKey() {
        Log.e("QUERY.HAS.KEY", "1");
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    JSONArray data = new JSONArray();
                    JSONObject next;
                    Log.e("WIFI.LIST", wifilist.toString());
                    for(int i = 0; i < wifilist.length(); i++) {
                        if (wifilist.getJSONObject(i).getString("cipher").isEmpty()) continue;
                        next = new JSONObject();
                        next.put("ssid", wifilist.getJSONObject(i).getString("ssid"));
                        next.put("bssid", wifilist.getJSONObject(i).getString("bssid"));
                        data.put(next);
                    }
                    Log.e("WIFI.DATA.LIST", data.toString());
                    final String body = HttpUtils.post(BuildConfig.URL + "app/batchHotspot", data.toString
                            ());
                    Log.e("HTTP.HOTSPOT", body);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray wifikey = new JSONArray(body);
                                for (int i = 0; i < wifikey.length(); i++) {
                                    for (int n = 0; n < wifilist.length(); n++) {
                                        if (wifikey.getJSONObject(i).getString("ssid")
                                                .equals(wifilist.getJSONObject(n).getString("ssid"))) {
                                            if (wifilist.getJSONObject(n).opt("connected") != null) break;
                                            wifilist.getJSONObject(n).put("password",
                                                    wifikey.getJSONObject(i).getString("password"));

                                            View parent = listView.getChildAt(n);
                                            if (parent != null) {
                                                ImageView rightImage = parent.findViewById(R.id.itemRightImage);
                                                rightImage.setImageResource(wifilist.getJSONObject(n).getInt("share"));
                                            }
                                            break;
                                        }
                                    }
                                }
                            }catch (Exception e) {
                                Log.e("JSON.ERROR", e.getMessage());
                            }
                        }
                    });

                }catch (Exception e) {
                    Log.e("REFERSH.LIST", e.getMessage());
                }

            }
        }).start();
    }

    protected void onPosition() {
        mLocationClient = new AMapLocationClient(getContext());
        mLocationClient.setLocationListener(this);
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setNeedAddress(true);
        mLocationOption.setOnceLocation(true);
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.startLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onLocationChanged(final AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                address = aMapLocation.getCountry();
                address += aMapLocation.getProvince();
                address += aMapLocation.getCity();
                address += aMapLocation.getDistrict();
                address += aMapLocation.getStreet();
                address += aMapLocation.getStreetNum();
                address += aMapLocation.getAoiName();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentLocation.setSingleLine(true);
                        currentLocation.setFocusable(true);
                        currentLocation.setFocusableInTouchMode(true);
                        currentLocation.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                        currentLocation.setSelected(true);
                        currentLocation.setText(aMapLocation.getAddress());
                    }
                });

                Log.e("ADDRESS", address);
                Log.e("ADDR", aMapLocation.getAddress());
            }
        }
    }

    protected void wifiScan() {
        if (!checkLocationStatus() && !wifiManager.isWifiEnabled()) return;
        if (!checkLocationStatus()) {
            if (wifiManager.isWifiEnabled()) {
                noWifiService.setVisibility(View.GONE);
                noLocationService.setVisibility(View.VISIBLE);
            } else {
                noWifiService.setVisibility(View.VISIBLE);
                noLocationService.setVisibility(View.GONE);
            }
            return;
        }

        if (loadingAnimation.getVisibility() == View.VISIBLE) return ;

        wifilist = new JSONArray();
        try {

            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult next : results) {
                JSONObject item = new JSONObject();
                if (next.SSID.equals("")) continue;
                NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiInfo info = wifiManager.getConnectionInfo();
                    if (info != null && (info.getSSID().equals(next.SSID) || info.getSSID().equals("\"" + next.SSID + "\"")) && info.getBSSID().equals(next.BSSID)) {
                        item.put("connected", R.drawable.connected_pass);
                    }
                }

                int none = R.drawable.wifi_4;
                int share = R.drawable.wifi_key_4;
                int icon = R.drawable.wifi_lock_4;
                if (next.level >= -70 && next.level < -55) {
                    none = R.drawable.wifi_3;
                    icon = R.drawable.wifi_lock_3;
                    share = R.drawable.wifi_key_3;
                } else if (next.level >= -85 && next.level < -70) {
                    none = R.drawable.wifi_2;
                    icon = R.drawable.wifi_lock_2;
                    share = R.drawable.wifi_key_2;
                } else if (next.level >= -100 && next.level < -85) {
                    none = R.drawable.wifi_1;
                    icon = R.drawable.wifi_lock_1;
                    share = R.drawable.wifi_key_1;
                } else if (next.level < -100) {
                    none = R.drawable.wifi_0;
                    icon = R.drawable.wifi_lock_0;
                    share = R.drawable.wifi_key_0;
                }

                String cipher = "";
                if (next.capabilities.contains("WPA2")) {
                    cipher = "WPA2";
                } else if (next.capabilities.contains("WPA")) {
                    cipher = "WPA";
                } else if (next.capabilities.contains("WEP")) {
                    cipher = "WEP";
                }

                item.put("bssid", next.BSSID);
                item.put("ssid", next.SSID);
                item.put("rssi", next.level);
                item.put("cipher", cipher);
                item.put("icon", icon);
                item.put("share", share);
                item.put("none", none);

                wifilist.put(item);
            }

            for (int i = 0; i < wifilist.length(); i++) {
                for (int n = i + 1; n < wifilist.length(); n++) {
                    if (wifilist.getJSONObject(i).getInt("rssi") < wifilist.getJSONObject(n).getInt("rssi")) {
                        JSONObject swap = wifilist.getJSONObject(i);
                        wifilist.put(i, wifilist.getJSONObject(n));
                        wifilist.put(n, swap);
                    }
                }
            }

            if (wifilist.length() > 0) {
                wifiListNumber.setVisibility(View.VISIBLE);
                wifiNumber.setText(String.format(getString(R.string.has_connected_number), wifilist.length()));
                wifiListNumber.setText(String.format(getString(R.string.wifi_list_number), wifilist.length()));

                noResult.setVisibility(View.GONE);
                hasResult.setVisibility(View.VISIBLE);

                worker.sendEmptyMessage(1);

            } else {
                wifiListNumber.setVisibility(View.GONE);
                wifiNumber.setText(getString(R.string.no_connected_number));
                hasResult.setVisibility(View.GONE);
                noResult.setVisibility(View.VISIBLE);
            }

            listView.setAdapter(new DataAdapter(wifilist));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {

                        connectingCipher = wifilist.getJSONObject(position).getString("cipher");

                        if (wifilist.getJSONObject(position).opt("connected") == null) {

                            if (wifilist.getJSONObject(position).getString("cipher").equals("")) {
                                //connectingCipher = "";
                                connectingBSSID = wifilist.getJSONObject(position).getString("bssid");
                                connectingSSID = wifilist.getJSONObject(position).getString("ssid");
                                wifiConnect(wifilist.getJSONObject(position).getString("ssid"),
                                        null, null);
                            } else if(wifilist.getJSONObject(position).opt("password") != null) {
                                autoConnectingIndex = position;
                                doConnecting = true;
                                wifiShared = false;
                                connectingBSSID = wifilist.getJSONObject(position).getString("bssid");
                                connectingSSID = wifilist.getJSONObject(position).getString("ssid");
                                final View childView = listView.getChildAt(position);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        childView.setEnabled(false);
                                        childView.findViewById(R.id.itemRightImage).setVisibility(View.GONE);
                                        childView.findViewById(R.id.itemRightLoading).setVisibility(View.VISIBLE);
                                    }
                                });

                                wifiConnect(wifilist.getJSONObject(position).getString("ssid"),
                                        wifilist.getJSONObject(position).getString("password"), connectingCipher);

                            } else {
                                connectingBSSID = wifilist.getJSONObject(position).getString("bssid");
                                connectingSSID = wifilist.getJSONObject(position).getString("ssid");
                                prepareConnect(connectingSSID);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("LIST.CLICK", e.getMessage());
                    }
                }
            });



        } catch (Exception e) {
            Log.e("WIFI.LIST", e.getMessage() + e.getStackTrace()[1].getLineNumber());
        }
    }

    protected void prepareConnect(final String ssid) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                alertDialog = new AlertDialog.Builder(getContext()).setView(R.layout.widget_wifi_connect).setCancelable(false).create();
                alertDialog.show();

                if (alertDialog.getWindow() != null) {

                    final TextView connectWifiSsid = alertDialog.getWindow().findViewById(R.id.connectWifiSsid);
                    final TextView connectWifiOk = alertDialog.getWindow().findViewById(R.id.connectWifiOk);
                    final TextView connectWifiCancel = alertDialog.getWindow().findViewById(R.id.connectWifiCancel);
                    final EditText connectWifiPassword = alertDialog.getWindow().findViewById(R.id.connectWifiPassword);
                    final CheckBox connectWifiShared = alertDialog.getWindow().findViewById(R.id.connectWifiShared);
                    final CheckBox connectWifiSwitchPwd = alertDialog.getWindow().findViewById(R.id.connectWifiSwitchPwd);

                    connectWifiSwitchPwd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                connectWifiPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                            } else {
                                connectWifiPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            }

                            connectWifiPassword.setSelection(connectWifiPassword.length());
                        }
                    });

                    connectWifiPassword.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (s.length() >= 8) {
                                connectWifiOk.setEnabled(true);
                            } else {
                                connectWifiOk.setEnabled(false);
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                connectWifiPassword.requestFocus();
                                InputMethodManager ime = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                ime.showSoftInput(connectWifiPassword, 0);
                            } catch (Exception e) {
                                Log.e("IME.ERROR", e.getMessage());
                            }
                        }
                    }, 100);

                    connectWifiOk.setClickable(true);
                    connectWifiOk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            wifiShared = connectWifiShared.isChecked();
                            alertDialog.dismiss();
                            wifiConnect(ssid, connectWifiPassword.getText().toString(), null);
                            doConnecting = true;
                            connectingPassword = connectWifiPassword.getText().toString();
                        }
                    });
                    connectWifiCancel.setClickable(true);
                    connectWifiCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialog.dismiss();
                        }
                    });

                    connectWifiSsid.setText(String.format(getString(R.string.wifi_connect_to), " " + ssid));
                }

            }
        });
    }

    protected void wifiConnect(String ssid, @Nullable String password, @Nullable String cipher) {

        if (cipher == null) cipher = "";
        if (password == null) password = "";

        Log.e("WIFI.SSID", ssid);
        Log.e("WIFI.PASSWORD", password);
        Log.e("WIFI.CIPHER", cipher);

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = String.format("\"%s\"", ssid);
        if (cipher.contains("WPA")) {
            config.preSharedKey = String.format("\"%s\"", password);
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else if (cipher.contains("WEP")) {
            config.hiddenSSID = true;
            config.wepKeys[0] = String.format("\"%s\"", password);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        connectingNetworkId = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(connectingNetworkId, true);

    }

    protected boolean checkLocationStatus() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    protected class DataAdapter extends BaseAdapter {

        private JSONArray data;

        DataAdapter(JSONArray mData) {
            data = mData;
        }

        @Override
        public int getCount() {
            return data.length();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, null);

            try {
                JSONObject itemData = data.getJSONObject(position);
                String cipher = itemData.getString("cipher");
                TextView itemText = convertView.findViewById(R.id.itemText);
                ImageView itemRightImage = convertView.findViewById(R.id.itemRightImage);

                itemText.setText(itemData.optString("ssid"));
                itemText.setTextSize(14);

                if (itemData.opt("password") != null) {
                    itemRightImage.setImageResource(itemData.getInt("share"));
                } else {
                    if (itemData.opt("connected") != null) {
                        itemRightImage.setImageResource(itemData.getInt("connected"));
                    } else {
                        if (cipher.equals("")) {
                            itemRightImage.setImageResource(itemData.getInt("none"));
                        }else {
                            itemRightImage.setImageResource(itemData.getInt("icon"));
                        }
                    }
                }
                itemRightImage.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }


    }
}
