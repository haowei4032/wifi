package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URLEncoder;

import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;

public class LetterActivity extends AppCompatActivity {

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letter);

        initView();
    }


    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.letter_title);

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

        final EditText letterBody = findViewById(R.id.letterContent);
        final EditText letterEmail = findViewById(R.id.emailContent);
        TextView submitButton = findViewById(R.id.submitButton);
        submitButton.setClickable(true);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                v.setEnabled(false);

                if (letterBody.getText().toString().isEmpty() || letterBody.getText().toString().trim().isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            v.setEnabled(true);
                            Toast.makeText(getContext(), "建议内容不能为空", Toast.LENGTH_LONG).show();
                        }
                    });
                    return ;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String body = String.format("body=%s&email=%s",
                                    URLEncoder.encode(letterBody.getText().toString(), "UTF-8"),
                                    URLEncoder.encode(letterEmail.getText().toString(), "UTF-8"));
                            Log.e("HTTP.QUERY", body);
                            body = HttpUtils.post(BuildConfig.URL + "app/feedback", body);
                            JSONObject data = new JSONObject(body);
                            if (data.optInt("code", -1) == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        letterBody.getText().clear();
                                        letterEmail.getText().clear();
                                        new AlertDialog.Builder(getContext()).setTitle("")
                                                .setMessage("您的建议我们已经收到，谢谢！")
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
                            Log.e("LETTER.EXCEPTION", e.getMessage());
                        }
                    }
                }).start();
            }
        });


    }

}
