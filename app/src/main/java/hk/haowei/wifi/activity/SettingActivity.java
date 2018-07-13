package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.view.ListViewEx;

public class SettingActivity extends AppCompatActivity {

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initView();

    }

    public void onBackPressed() {
        AAplication.instance.get().cacheSelf();
        super.onBackPressed();
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
        titleView.setText(R.string.setting_title);

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

        LinearLayout listContainer = findViewById(R.id.listContainer);
        RelativeLayout.LayoutParams params;

        try {

            JSONArray item;
            JSONObject next;

            final JSONArray list = new JSONArray();
            item = new JSONArray();
            next = new JSONObject();
            next.put("id", "autoShareWifi");
            next.put("text", "自动分享热点");
            next.put("switch", AAplication.instance.get().userInfo == null || AAplication.instance.get().userInfo.getInt("AutoShareWifi") == 1);
            item.put(next);

            next = new JSONObject();
            next.put("id", "autoAcceptPush");
            next.put("text", "接受推送消息");
            next.put("switch", AAplication.instance.get().userInfo == null || AAplication.instance.get().userInfo.getInt("AutoAcceptPush") == 1);
            item.put(next);

            next = new JSONObject();
            next.put("id", "cancelShareWifi");
            next.put("text", "取消分享热点");
            item.put(next);
            list.put(item);

            item = new JSONArray();
            next = new JSONObject();
            next.put("id", "advert");
            next.put("text", "广告投放");
            item.put(next);
            list.put(item);

            item = new JSONArray();
            next = new JSONObject();
            next.put("id", "discuss");
            next.put("text", "给我好评");
            item.put(next);

            next = new JSONObject();
            next.put("id", "letter");
            next.put("text", "意见反馈");
            item.put(next);

            next = new JSONObject();
            next.put("id", "about");
            next.put("text", "关于我们");
            next.put("mark", AAplication.instance.get().hasMark);
            item.put(next);

            list.put(item);

            if (AAplication.instance.get().userInfo != null) {
                item = new JSONArray();
                next = new JSONObject();
                next.put("id", "logout");
                next.put("text", "退出登录");
                next.put("center", true);
                item.put(next);
                list.put(item);
            }

            for (int i = 0; i < list.length(); i++) {
                final ListViewEx listView = new ListViewEx(getContext());
                listView.index = i;
                listView.setDividerHeight(1);
                listView.setBackgroundColor(Color.WHITE);
                listView.setAdapter(new DataAdapter(list.getJSONArray(i)));
                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, dip2px(15), 0, 0);
                listView.setLayoutParams(params);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        try {
                            Intent intent;
                            String ids = list.getJSONArray(listView.index).
                                    getJSONObject(position).
                                    getString("id");
                            switch (ids) {
                                case "autoShareWifi":
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.e("SWITCH", "xxx");
                                            Switch itemSwitch = listView.getChildAt(position).findViewById(R.id.itemSwitch);
                                            itemSwitch.toggle();
                                            try {
                                                Log.e("WIFI.SHARE.CHECKED", itemSwitch.isChecked() + "");
                                                AAplication.instance.get().userInfo.put("AutoShareWifi", itemSwitch.isChecked() ? 1 : 0);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                    break;
                                case "autoAcceptPush":
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.e("SWITCH", "xxx");
                                            Switch itemSwitch = listView.getChildAt(position).findViewById(R.id.itemSwitch);
                                            itemSwitch.toggle();
                                            try {
                                                Log.e("ACCEPT.PUSH.CHECKED", itemSwitch.isChecked() + "");
                                                AAplication.instance.get().userInfo.put("AutoAcceptPush", itemSwitch.isChecked() ? 1 : 0);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                    break;
                                case "discuss":
                                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
                                    intent.setPackage(BuildConfig.CHANNEL_PACKAGE);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    break;
                                case "advert":
                                    intent = new Intent(getContext(), BrowserActivity.class);
                                    intent.putExtra("url", "file:///android_asset/html/advert.html");
                                    startActivity(intent);
                                    break;
                                case "letter":
                                    startActivity(new Intent(getContext(), LetterActivity.class));
                                    break;
                                case "cancelShareWifi":
                                    startActivity(new Intent(getContext(), CancelActivity.class));
                                    break;
                                case "logout":
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                final AlertDialog alertDialog = new AlertDialog.
                                                        Builder(getContext()).
                                                        setCancelable(true).
                                                        setView(R.layout.widget_user_logout).
                                                        create();
                                                alertDialog.show();

                                                Window parent = alertDialog.getWindow();
                                                TextView okButton = parent.findViewById(R.id.ok_button);
                                                TextView cancelButton = parent.findViewById(R.id.cancel_button);
                                                okButton.setClickable(true);
                                                cancelButton.setClickable(true);

                                                cancelButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        alertDialog.dismiss();
                                                    }
                                                });

                                                okButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        AAplication.instance.get().userLogout = true;
                                                        alertDialog.dismiss();
                                                        setResult(RESULT_OK);
                                                        SettingActivity.this.finish();
                                                    }
                                                });
                                            } catch (Exception e) {
                                                Log.e("RENDER.ERROR", e.getMessage());
                                            }
                                        }
                                    });
                                    break;
                                case "about":
                                    startActivity(new Intent(getContext(), AboutActivity.class));
                                    break;
                            }

                        } catch (Exception e) {
                            Log.e("LIST.CLICK", e.getMessage());
                        }
                    }
                });

                listContainer.addView(listView);
            }

        } catch (Exception e) {
            e.printStackTrace();
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
                Switch itemSwitch = convertView.findViewById(R.id.itemSwitch);
                View itemMark = convertView.findViewById(R.id.itemMark);

                itemText.setText(item.optString("text", ""));
                if (item.optBoolean("center", false)) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) itemText.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    itemText.setLayoutParams(params);
                    itemText.setTextColor(Color.RED);
                    itemText.setTextSize(18);
                }

                if (item.opt("switch") != null) {
                    itemSwitch.setChecked(item.optBoolean("switch"));
                    itemSwitch.setVisibility(View.VISIBLE);
                } else {
                    itemSwitch.setVisibility(View.GONE);
                }
                if (item.optBoolean("mark", false)) {
                    itemMark.setVisibility(View.VISIBLE);
                } else {
                    itemMark.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }


    }

}
