package hk.haowei.wifi.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.view.ImageViewEx;
import hk.haowei.wifi.view.ListViewEx;

public class MineActivity extends AppCompatActivity {

    protected ImageViewEx userAvatar;
    protected TextView userInfoName;
    protected TextView userInfoExtra;


    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);
        userAvatar = findViewById(R.id.userAvatar);
        userInfoName = findViewById(R.id.userInfoName);
        userInfoExtra = findViewById(R.id.userInfoExtra);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (AAplication.instance.get().userInfo != null) {
                    try {
                        JSONObject result = AAplication.instance.get().userInfo;
                        final String nickname = result.optString("Nickname");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                userInfoName.setText(nickname.isEmpty() ? getString(R.string.no_set_nickname) : nickname);
                                if (AAplication.instance.get().userAvatar != null)
                                    userAvatar.setImageBitmap(AAplication.instance.get().userAvatar);
                            }
                        });
                    }catch (Exception e) {
                        Log.e("AUTO.LOGIN", e.getMessage());
                    }
                }
            }
        }).start();

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

        CardView awardButton = findViewById(R.id.awardButton);
        awardButton.setClickable(true);

        final TextView userInfoName = findViewById(R.id.userInfoName);
        userInfoName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AAplication.instance.get().userInfo == null) {
                    startActivityForResult(new Intent(getContext(), LoginActivity.class), 1000);
                } else {
                    startActivityForResult(new Intent(getContext(), ProfileActivity.class), 2000);
                }
            }
        });

        userAvatar.setLongClickable(true);
        userAvatar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (AAplication.instance.get().userInfo != null ) startActivity(new Intent(getContext(), ImageViewerActivity.class));
                return false;
            }
        });

        userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                userInfoName.performClick();

            }
        });

        LinearLayout listContainer = findViewById(R.id.listContainer);

        try {
            final JSONArray list = new JSONArray();

            JSONObject next;
            JSONArray item = new JSONArray();

            next = new JSONObject();
            next.put("id", "message");
            next.put("icon", R.drawable.icon_message);
            next.put("text", "消息");
            item.put(next);

            next = new JSONObject();
            next.put("id", "task");
            next.put("icon", R.drawable.icon_task);
            next.put("text", "赚钱任务");
            item.put(next);

            next = new JSONObject();
            next.put("id", "invite");
            next.put("icon", R.drawable.icon_invite);
            next.put("text", "邀请好友");
            item.put(next);

            next = new JSONObject();
            next.put("id", "setting");
            next.put("icon", R.drawable.icon_setting);
            next.put("text", "设置");
            next.put("mark", AAplication.instance.get().hasMark);
            item.put(next);

            list.put(item);

            for (int i = 0; i < list.length(); i++) {
                final ListViewEx listView = new ListViewEx(getContext());
                listView.index = i;
                listView.setDividerHeight(1);
                listView.setBackgroundColor(Color.WHITE);
                listView.setAdapter(new DataAdapter(list.getJSONArray(i)));
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, dip2px(15), 0, 0);
                listView.setLayoutParams(params);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        try {
                            JSONObject itemData = list.getJSONArray(listView.index).getJSONObject(position);
                            Log.e("INDEX", "" + listView.index);
                            switch (itemData.optString("id")) {
                                case "message":
                                    startActivity(new Intent(getContext(), MessageActivity.class));
                                    break;
                                case "task":
                                    break;
                                case "invite":
                                    //startActivity(new Intent(getContext(), InviteActivity.class));
                                    break;
                                case "setting":
                                    startActivityForResult(new Intent(getContext(), SettingActivity.class), 1500);
                                    break;
                                default:
                                    //
                            }
                        }catch (Exception e) {
                            Log.e("MINE.CLICK", e.getMessage());
                        }
                    }
                });
                listContainer.addView(listView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == RESULT_OK) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String text = data.getStringExtra("result");
                            final JSONObject result = AAplication.instance.get().userInfo = new JSONObject(text);
                            AAplication.instance.get().setCached(result);
                            Log.e("USER.RESULT", AAplication.instance.get().userInfo.toString());

                            if (!result.getString("Avatar").isEmpty()) {
                                AAplication.instance.get().userAvatar = AAplication.instance.get().getAvatar(
                                        result.getString("Avatar")
                                );
                                Log.e("BITMAP", "" + AAplication.instance.get().userAvatar);
                            }

                            final String nickname = result.getString("Nickname");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), getString(R.string.login_successful), Toast.LENGTH_LONG).show();

                                    userInfoName.setText(nickname.isEmpty() ? getString(R.string.no_set_nickname) : nickname);
                                    if (AAplication.instance.get().userAvatar != null) userAvatar.setImageBitmap(AAplication.instance.get().userAvatar);
                                }
                            });

                        } catch (Exception e) {
                            Log.e("FOR.RESULT", e.getMessage());
                        }
                    }
                }).start();
            }
        } else if(requestCode == 1500) {
            if (resultCode == RESULT_OK) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userAvatar.setImageResource(R.drawable.avatar_2);
                        userInfoName.setText(R.string.user_login);
                        Toast.makeText(getContext(), getString(R.string.logout_successful), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else if(requestCode == 2000) {
            if (resultCode == RESULT_OK) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (AAplication.instance.get().userAvatar != null) {
                            userAvatar.setImageBitmap(AAplication.instance.get().userAvatar);
                        }
                        try {
                            String nickname = AAplication.instance.get().userInfo.getString("Nickname");
                            if (!nickname.isEmpty())userInfoName.setText(nickname);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

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
                Switch itemSwitch = convertView.findViewById(R.id.itemSwitch);
                View itemMark = convertView.findViewById(R.id.itemMark);

                if (item.opt("icon") != null) {
                    Drawable drawable = getDrawable(item.getInt("icon"));
                    drawable.setBounds(0, 0, dip2px(17), dip2px(17));
                    itemText.setCompoundDrawables(drawable, null, null, null);
                }

                itemText.setTextSize(15);
                itemText.setText(item.getString("text"));

                if (item.optBoolean("center", false)) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) itemText.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    itemText.setLayoutParams(params);
                    itemText.setTextColor(Color.RED);
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
