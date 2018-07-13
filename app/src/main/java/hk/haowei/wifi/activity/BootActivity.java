package hk.haowei.wifi.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.utils.AesUtils;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.utils.Md5Utils;
import hk.haowei.wifi.R;

public class BootActivity extends AppCompatActivity {

    protected boolean pending = true;
    protected int seconds = 3;

    protected RelativeLayout bottomView;
    protected TextView skipButton;
    protected ImageView wallPaper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boot);
        initView();
    }

    protected Context getContext() {
        return this;
    }

    protected void initView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        bottomView = findViewById(R.id.bottomView);
        wallPaper = findViewById(R.id.wallPaper);
        skipButton = findViewById(R.id.skipButton);
        skipButton.setText(String.format(getResources().getString(R.string.app_skip_button), ""));
        skipButton.setClickable(true);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seconds = -1;
                if (!pending) countDown(0);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (!pending) {
                        while (seconds >= 0) {
                            try {
                                countDown(seconds--);
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

                if (connectivityManager == null ||
                        connectivityManager.getActiveNetworkInfo() == null ||
                        !connectivityManager.getActiveNetworkInfo().isAvailable()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bottomView.setVisibility(View.VISIBLE);
                        }
                    });

                    return ;
                }

                try {

                    Double latitude = null;
                    Double longitude = null;

                    if (PackageManager.PERMISSION_GRANTED ==
                            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Criteria criteria = new Criteria();
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        criteria.setAltitudeRequired(false);
                        criteria.setBearingRequired(false);
                        criteria.setCostAllowed(true);
                        criteria.setPowerRequirement(Criteria.POWER_LOW);

                        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                    }

                    String query = "latitude=" + latitude;
                    query += "&";
                    query += "longitude=" + longitude;
                    String result = HttpUtils.get(BuildConfig.URL + "app/checkActivity?" + query);

                    Log.e("HTTP.RESULT", result);
                    final JSONObject data = new JSONObject(result);
                    if (data.opt("url") != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (data.optBoolean("fullScreen")) {
                                    bottomView.setVisibility(View.GONE);
                                } else {
                                    bottomView.setVisibility(View.VISIBLE);
                                }
                            }
                        });

                        Uri uri = Uri.parse(data.getString("url"));
                        Log.e("PATH", uri.getPath());
                        String filename = Md5Utils.hex(uri.getPath()) + ".jpg";
                        String path = getExternalCacheDir().getAbsolutePath() + "/" + filename;
                        File file = new File(path);
                        if (file.exists()) {
                            loadImage(BitmapFactory.decodeStream(new FileInputStream(file)));
                        } else {
                            Bitmap bitmap = HttpUtils.getBitmap(data.getString("url"));
                            if (bitmap != null) {
                                FileOutputStream fos = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                fos.close();
                                loadImage(bitmap);
                            }
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                bottomView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }catch (Exception e) {
                    Log.e("JSON.ERROR", e.getMessage());
                }
            }
        }).start();


        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);

    }

    protected void loadImage(final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wallPaper.setImageBitmap(bitmap);
            }
        });
    }

    protected void permissionDeny(final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (name) {
                    case Manifest.permission.ACCESS_FINE_LOCATION:
                        new AlertDialog.Builder(getContext()).setMessage("缺少定位权限").setCancelable
                                (false)
                                .setPositiveButton("打开设置", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.fromParts("package", getPackageName()
                                                , null));
                                        startActivity(intent);
                                    }
                                }).create().show();
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        new AlertDialog.Builder(getContext()).setMessage("缺少存储卡写入权限")
                                .setCancelable
                                (false)
                                .setPositiveButton("打开设置", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.fromParts("package", getPackageName()
                                                , null));
                                        startActivity(intent);
                                    }
                                }).create().show();
                        break;
                    case Manifest.permission.READ_PHONE_STATE:
                        new AlertDialog.Builder(getContext()).setMessage("缺少读取手机识别码权限")
                                .setCancelable
                                (false)
                                .setPositiveButton("打开设置", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.fromParts("package", getPackageName()
                                                , null));
                                        startActivity(intent);
                                    }
                                }).create().show();
                        break;
                }
            }
        });
    }

    protected void countDown(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                skipButton.setText(String.format(getResources().getString(R.string.app_skip_button), i > 0 ? " " + i : ""));
                if (i == 0) {
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                    //startActivity(new Intent(getContext(), MainActivity.class));
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        int grants = 0;
        for(int i  = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(permissions[i])) {
                    requestPermissions(new String[]{permissions[i]}, requestCode);
                } else {
                    permissionDeny(permissions[i]);
                }
                break;
            } else {
                grants++;
            }
        }

        if (grants == grantResults.length) pending = false;

    }
}
