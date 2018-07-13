package hk.haowei.wifi.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.view.ImageViewEx;
import hk.haowei.wifi.view.ListViewEx;

public class LoginActivity extends AppCompatActivity {

    protected ProgressDialog dialog;

    protected Button userSubmitButton;
    protected TextView userSendCode;
    protected EditText userMobile;
    protected EditText userSmsCode;
    protected int seconds = 60;

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();
    }


    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.login_title);

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

        userMobile = findViewById(R.id.userMobile);
        userSmsCode = findViewById(R.id.userSmsCode);
        userSendCode = findViewById(R.id.userSendCode);
        userSubmitButton = findViewById(R.id.userSubmitButton);
        //userSendCode.setClickable(true);

        userSendCode.setTextColor(Color.parseColor("#cccccc"));
        userSendCode.setFocusable(false);
        userSendCode.setClickable(false);

        userMobile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    userSubmitButton.setEnabled(false);
                }else {
                    userSendCode.setFocusable(true);
                    userSendCode.setClickable(true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            userSendCode.setTextColor(Color.parseColor("#333333"));
                        }
                    });

                    if (!userSmsCode.getText().toString().isEmpty()) {
                        userSubmitButton.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        userSmsCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    userSubmitButton.setEnabled(false);
                } else {
                    if (!userMobile.getText().toString().isEmpty()) {
                        userSubmitButton.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        userSendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userSendCode.setFocusable(false);
                    userSendCode.setClickable(false);
                    userSendCode.setTextColor(Color.parseColor("#cccccc"));
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String body = "mobile=" + userMobile.getText().toString();
                        body = HttpUtils.post(BuildConfig.URL + "app/smsVerifyCode", body);
                        Log.e("VERIFY.CODE", body);
                        JSONObject data = new JSONObject(body);
                        if (data.optInt("code", -1) == 2) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "发送验证码的间隔时间太短了", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }catch (Exception e) {
                        Log.e("HTTP.VERIFY.CODE", e.getMessage());
                    }
                }
            }).start();

               new Thread(new Runnable() {
                   @Override
                   public void run() {
                       try {
                           while(--seconds >= 0) {
                               runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       userSendCode.setText(String.format("%ds后重新发送".toLowerCase(), seconds));
                                   }
                               });
                               Thread.sleep(1000);
                           }
                           seconds = 60;
                           runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   userSendCode.setFocusable(true);
                                   userSendCode.setClickable(true);
                                   userSendCode.setTextColor(Color.parseColor("#333333"));
                                   userSendCode.setText(R.string.user_send_code);
                               }
                           });
                       }catch (Exception e) {
                           e.printStackTrace();
                       }
                   }
               }).start();
            }
        });

        userSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog = new ProgressDialog(getContext());
                        dialog.setTitle("");
                        dialog.setIndeterminate(true);
                        dialog.setCancelable(false);
                        dialog.setMessage("登录中");
                        dialog.show();
                    }
                });

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String body = String.format("mobile=%s&smscode=%s",
                                    userMobile.getText().toString(),
                                    userSmsCode.getText().toString());
                            body = HttpUtils.post(BuildConfig.URL + "app/checkUserIdentity", body);
                            Log.e("HTTP.USER", body);

                            final JSONObject data = new JSONObject(body);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                }
                            });

                            switch (data.optInt("code", -1)) {
                                case 0:
                                    Intent intent = getIntent();
                                    intent.putExtra("result", data.getJSONObject("result").toString());
                                    setResult(RESULT_OK, intent);
                                    finish();
                                    break;
                                case 1:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "验证码无效", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    break;
                                default:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "服务异常", Toast.LENGTH_LONG).show();
                                        }
                                    });

                            }

                        }catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }).start();

            }
        });


    }

}
