package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;

public class CancelActivity extends AppCompatActivity {

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel);


        initView();

    }


    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.cancel_shared_title);

        TextView firstButton = findViewById(R.id.firstButton);
        firstButton.setClickable(true);
        firstButton.setText(R.string.first_button);
        firstButton.setTypeface(Typeface.createFromAsset(getAssets(), "apps.ttf"));

        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        final EditText wifiSsidName = findViewById(R.id.wifiSsidName);
        final EditText wifiPassword = findViewById(R.id.wifiPassword);
        final EditText wifiBssid = findViewById(R.id.wifiBssid);

        TextView submitButton = findViewById(R.id.submitButton);
        submitButton.setClickable(true);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                v.setEnabled(false);
                if (wifiSsidName.getText().toString().isEmpty() || wifiSsidName.getText().toString().trim().isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            v.setEnabled(true);
                            Toast.makeText(getContext(), "WiFi热点名称不能为空", Toast.LENGTH_LONG).show();
                        }
                    });
                    return ;
                }

                if (wifiPassword.getText().toString().isEmpty() || wifiPassword.getText().toString().trim().isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            v.setEnabled(true);
                            Toast.makeText(getContext(), "WiFi密码不能为空", Toast.LENGTH_LONG).show();
                        }
                    });
                    return ;
                }

                if (wifiBssid.getText().toString().isEmpty() || wifiBssid.getText().toString().trim().isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            v.setEnabled(true);
                            Toast.makeText(getContext(), "路由器Mac地址不能为空", Toast.LENGTH_LONG).show();
                        }
                    });
                    return ;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String body = String.format(
                                    "ssid=%s&bssid=%s&password=%s&latitude=%s&longitude=%s".toLowerCase(),
                                    wifiSsidName.getText().toString(),
                                    wifiBssid.getText().toString(),
                                    wifiPassword.getText().toString(),
                                    AAplication.instance.get().latitude,
                                    AAplication.instance.get().longitude
                            );
                            body = HttpUtils.post(BuildConfig.URL + "app/cancelHotspot", body);
                            Log.e("HTTP.CANCEL.HOTSPOT", body);
                            JSONObject data = new JSONObject(body);
                            if (data.optInt("code", -1) == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wifiSsidName.getText().clear();
                                        wifiBssid.getText().clear();
                                        wifiPassword.getText().clear();
                                        new AlertDialog.Builder(getContext()).setTitle("")
                                                .setMessage("您的请求我们已经收到，我们会马上处理！")
                                                .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                        onBackPressed();
                                                    }
                                                })
                                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        v.setEnabled(true);
                                                        dialog.dismiss();
                                                    }
                                                }).show();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        v.setEnabled(true);
                                        new AlertDialog.Builder(getContext()).setTitle("")
                                                .setMessage("服务异常，请重试")
                                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                }).show();
                                    }
                                });
                            }
                        }catch (Exception e) {
                            Log.e("CANCEL.HOTSPOT", e.getMessage());
                        }
                    }
                }).start();
            }
        });


    }

}
