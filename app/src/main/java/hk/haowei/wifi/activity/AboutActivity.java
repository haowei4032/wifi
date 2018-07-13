package hk.haowei.wifi.activity;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.view.ListViewEx;

public class AboutActivity extends AppCompatActivity {

    protected Worker worker = new Worker(this);

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initView();

    }

    protected int dip2px(double dip) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (scale * dip + 0.5f);
    }

    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.about_title);

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

        LinearLayout listContainer = findViewById(R.id.listContainer);

        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(R.mipmap.ic_launcher);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dip2px(65), dip2px
                (65));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, dip2px(30), 0, 0);
        imageView.setLayoutParams(params);
        listContainer.addView(imageView);

        TextView textView = new TextView(getContext());
        textView.setText(R.string.app_name);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(18);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dip2px(15), 0, 0);
        textView.setLayoutParams(params);
        listContainer.addView(textView);

        textView = new TextView(getContext());
        textView.setText(String.format("版本号 v%s build %s-%d".toLowerCase(),
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_DATE,
                BuildConfig.CHANNEL_ID));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setTextColor(Color.parseColor("#888888"));
        textView.setTextSize(11);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dip2px(8), 0, 0);
        textView.setLayoutParams(params);
        listContainer.addView(textView);

        try {

            JSONObject next;
            final JSONArray list = new JSONArray();
            next = new JSONObject();
            next.put("id", "contact");
            next.put("text", "联系我们");
            next.put("subtext", "微信号：howay2015");
            list.put(next);

            /*next = new JSONObject();
            next.put("id", "question");
            next.put("text", "常见问题");
            list.put(next);*/

            next = new JSONObject();
            next.put("id", "privacy");
            next.put("text", "服务条款");
            list.put(next);

            next = new JSONObject();
            next.put("id", "upgrade");
            next.put("text", "检查版本");
            next.put("mark", AAplication.instance.get().hasMark);
            list.put(next);


            final ListViewEx listView = new ListViewEx(getContext());
            listView.setDividerHeight(1);
            listView.setBackgroundColor(Color.WHITE);
            listView.setAdapter(new DataAdapter(list));
            params = new LinearLayout.LayoutParams(RelativeLayout
                    .LayoutParams
                    .MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dip2px(30), 0, 0);
            listView.setLayoutParams(params);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        JSONObject itemData = list.getJSONObject(position);
                        switch (itemData.getString("id")) {
                            case "contact":
                                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                cm.setPrimaryClip(ClipData.newPlainText("微信号", "howay2015"));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "复制成功", Toast.LENGTH_LONG).show();
                                    }
                                });

                                break;
                            case "question":
                                Log.e("CLICK", "question");
                                break;
                            case "upgrade":
                                worker.sendEmptyMessage(1);
                                //newVersionUpgrade();
                                Log.e("CLICK", "upgrade");
                                break;
                            case "privacy":
                                Intent intent = new Intent(getContext(), BrowserActivity.class);
                                intent.putExtra("url", "http://app.haowei.me/app/agreement");
                                startActivity(intent);
                                break;
                        }
                    }catch (Exception e) {
                        Log.e("ABOUT.CLICK", e.getMessage());
                    }
                }
            });
            listContainer.addView(listView);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void checkingVersion() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ProgressDialog dialog = new ProgressDialog(getContext());
                    dialog.setTitle("");
                    dialog.setIndeterminate(true);
                    dialog.setMessage("检查更新中");
                    dialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int channelId = BuildConfig.CHANNEL_ID;
                                int versionCode = BuildConfig.VERSION_CODE;
                                String body = HttpUtils.get(BuildConfig.URL + "app/checkVersion?versionCode=" + versionCode + "&channelId="+ channelId +"&_random=" + System.currentTimeMillis());
                                Log.e("HTTP.VERSION", body);
                                JSONObject result = new JSONObject(body);
                                dialog.dismiss();
                                if (result.opt("result") != null) {
                                    newVersionUpgrade(result.getJSONObject("result"));
                                } else {
                                    AAplication.instance.get().hasMark = false;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "已经是最新版本", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }catch (Exception e) {
                    Log.e("UPGRADE.MODAL", e.getMessage());
                }
            }
        });

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

    protected static class Worker extends Handler {

        private WeakReference<AboutActivity> activity;

        public Worker(AboutActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                activity.get().checkingVersion();
            }
        }
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
                convertView = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, null);
            try {
                JSONObject item = data.getJSONObject(position);
                TextView itemText = convertView.findViewById(R.id.itemText);
                itemText.setText(item.optString("text", ""));
                Switch itemSwitch = convertView.findViewById(R.id.itemSwitch);
                TextView itemRightText = convertView.findViewById(R.id.itemRightText);
                if (!item.optString("subtext").isEmpty()) {
                    itemRightText.setText(item.getString("subtext"));
                    itemRightText.setVisibility(View.VISIBLE);
                } else {
                    itemRightText.setVisibility(View.GONE);
                }

                if (item.opt("switch") != null) {
                    itemSwitch.setChecked(true);
                    itemSwitch.setVisibility(View.VISIBLE);
                } else {
                    itemSwitch.setVisibility(View.GONE);
                }
                if (item.optBoolean("mark", false)) {
                    ((View) convertView.findViewById(R.id.itemMark)).setVisibility(View.VISIBLE);
                } else {
                    ((View) convertView.findViewById(R.id.itemMark)).setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }
    }

}
