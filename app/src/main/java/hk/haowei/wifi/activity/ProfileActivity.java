package hk.haowei.wifi.activity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.view.ImageViewEx;
import hk.haowei.wifi.view.ListViewEx;


public class ProfileActivity extends AppCompatActivity {

    protected ListViewEx listView;
    protected ProgressDialog dialog;
    protected String filepath = null;

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
    }

    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        TextView titleView = findViewById(R.id.titleView);
        titleView.setText(R.string.profile_title);

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

            JSONObject next;
            final JSONArray list = new JSONArray();
            next = new JSONObject();
            next.put("id", "avatar");
            next.put("text", "头像");
            next.put("type", "bitmap");
            next.put("defaultValue", AAplication.instance.get().userAvatar == null ?
                    ((BitmapDrawable) getDrawable(R.drawable.avatar_2)).getBitmap() :
                    AAplication.instance.get().userAvatar
            );
            list.put(next);

            next = new JSONObject();
            next.put("id", "nickname");
            next.put("text", "昵称");
            next.put("type", "text");
            next.put("defaultValue", AAplication.instance.get().userInfo.getString("Nickname").equals("") ?
            "未设置名称" :
            AAplication.instance.get().userInfo.getString("Nickname")
            );
            list.put(next);

            next = new JSONObject();
            next.put("id", "gender");
            next.put("text", "性别");
            next.put("type", "text");
            next.put("defaultValue", AAplication.instance.get().userInfo.getInt("Gender") == -1 ?
            "未知": ( AAplication.instance.get().userInfo.getInt("Gender") == 1 ? "男" : "女" ));
            list.put(next);

            next = new JSONObject();
            next.put("id", "birthday");
            next.put("text", "生日");
            next.put("type", "text");
            next.put("defaultValue", AAplication.instance.get().userInfo.getString("Birthday"));
            list.put(next);

            next = new JSONObject();
            next.put("id", "email");
            next.put("text", "邮箱");
            next.put("type", "text");
            next.put("defaultValue", AAplication.instance.get().userInfo.getString("EMail").isEmpty() ?
            "未设置邮箱" :
                    AAplication.instance.get().userInfo.getString("EMail"));
            list.put(next);

            next = new JSONObject();
            next.put("id", "mobile");
            next.put("text", "手机号");
            next.put("type", "text");
            next.put("defaultValue", AAplication.instance.get().userInfo.getString("Mobile"));
            list.put(next);

            listView = new ListViewEx(getContext());
            listView.setDividerHeight(1);
            listView.setBackgroundColor(Color.WHITE);
            listView.setAdapter(new DataAdapter(list));
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dip2px(15), 0, 0);
            listView.setLayoutParams(params);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {

                    Intent intent;

                    try {
                        switch (list.getJSONObject(position).getString("id")) {
                            case "avatar":
                                intent = new Intent(Intent.ACTION_PICK, android.provider
                                    .MediaStore.Images
                                        .Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(intent, 1000);
                                break;
                            case "nickname":
                                Log.e("NICKNAME.CLICK", "hit");
                                intent = new Intent(getContext(), ModifyActivity.class);
                                intent.putExtra("title", "更改名字");
                                intent.putExtra("field", "Nickname");
                                intent.putExtra("index", position);
                                intent.putExtra("defaultValue", AAplication.instance.get().userInfo.getString("Nickname"));
                                intent.putExtra("descript", "一个好名字，让朋友更容易记住你");
                                startActivityForResult(intent, 500);
                                break;
                            case "email":
                                Log.e("NICKNAME.CLICK", "hit");
                                intent = new Intent(getContext(), ModifyActivity.class);
                                intent.putExtra("title", "更改邮箱");
                                intent.putExtra("field", "EMail");
                                intent.putExtra("index", position);
                                //intent.putExtra("descript", "一个好名字，让朋友更容易记住你");
                                intent.putExtra("defaultValue", AAplication.instance.get().userInfo.getString("EMail"));
                                startActivityForResult(intent, 500);
                                break;
                            case "gender":
                                int itemGender = AAplication.instance.get().userInfo.getInt("Gender") != -1 ? (
                                        AAplication.instance.get().userInfo.getInt("Gender") == 1 ? 0 : 1
                                        ) : -1;
                                final AlertDialog dialog = new AlertDialog.Builder(getContext())
                                    .setTitle("性别")
                                    .setSingleChoiceItems(new String[]{"男", "女"}, itemGender, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String text = which == 0 ? "男" : "女";
                                        int gender = which == 0 ? 1 : 0;

                                        try {
                                            AAplication.instance.get().userInfo.put("Gender", gender);
                                            //AAplication.instance.get().cacheSelf();
                                        }catch (Exception e) {
                                            Log.e("GENDER.ERROR", e.getMessage());
                                        }
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                TextView itemRightText = view.findViewById(R.id.itemRightText);
                                                itemRightText.setText(text);
                                            }
                                        });
                                        dialog.dismiss();
                                    }
                                    }).create();
                                dialog.show();
                                break;
                            case "birthday":
                                Calendar calendar = Calendar.getInstance();
                                final DatePickerDialog dateDialog = new  DatePickerDialog(getContext(),
                                        null,
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH));
                                dateDialog.setCancelable(false);
                                dateDialog.setButton(DialogInterface.BUTTON_POSITIVE, "确认", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DatePicker picker = dateDialog.getDatePicker();
                                        Log.e("YEAR", picker.getYear() + "");
                                        Log.e("MONTH", picker.getMonth() + "");
                                        Log.e("DATE", picker.getDayOfMonth() + "");

                                        int year = picker.getYear();
                                        int month = picker.getMonth();
                                        int date = picker.getDayOfMonth();

                                        try {
                                            final String birthday = String.format("%d-%02d-%02d".toLowerCase(), year, month+1, date);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    TextView itemRightText = view.findViewById(R.id.itemRightText);
                                                    itemRightText.setText(birthday);
                                                }
                                            });
                                            AAplication.instance.get().userInfo.put("Birthday", birthday);
                                            Log.e("DATE", AAplication.instance.get().userInfo.getString("Birthday"));
                                            //AAplication.instance.get().cacheSelf();
                                        }catch (Exception e) {
                                            Log.e("BIRTHDAY.MODIFY", "error");
                                        }
                                    }
                                });
                                dateDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                dateDialog.show();
                                break;
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            listContainer.addView(listView);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AAplication.instance.get().cacheSelf();
            }
        }).start();
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000) {
            if (resultCode == RESULT_OK) {
                filepath = getExternalFilesDir("image").getAbsolutePath() + "/tmp.jpg";

                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(data.getData(), "image/*");
                intent.putExtra("scale", true);
                intent.putExtra("crop", true);
                intent.putExtra("return-data", false);
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                intent.putExtra("outputX", 1080);
                intent.putExtra("outputY", 1080);
                intent.putExtra("noFaceDetection", true);
                intent.putExtra("quality", 100);
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(filepath)));
                startActivityForResult(intent, 1500);
            }

        }else if(requestCode == 1500) {
            if (resultCode == RESULT_OK) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog = new ProgressDialog(getContext());
                                    dialog.setTitle("");
                                    dialog.setIndeterminate(true);
                                    dialog.setCancelable(false);
                                    dialog.setMessage("上传中");
                                    dialog.show();
                                }
                            });

                            String body = HttpUtils.postBitmap(BuildConfig.URL + "app/userAvatar", filepath);

                            //new File(filepath).delete();
                            filepath = null;

                            Log.e("HTTP.AVATAR", body);
                            JSONObject result = new JSONObject(body);
                            if (result.getInt("code") == 0 && result.opt("result") != null) {
                                final String url = result.getJSONObject("result").getString("url");
                                AAplication.instance.get().userInfo.put("Avatar", url);
                                AAplication.instance.get().setCached(AAplication.instance.get().userInfo);
                                AAplication.instance.get().userAvatar = AAplication.instance.get().getAvatar(
                                        url
                                );

                                //HttpUtils.getBitmap(url);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                        if (AAplication.instance.get().userAvatar != null) {
                                            View parent = listView.getChildAt(0);
                                            ImageViewEx imageView = parent.findViewById(R.id.itemRightAvatar);
                                            imageView.setImageBitmap(AAplication.instance.get().userAvatar);
                                        }
                                    }
                                });

                            }
                        }catch (Exception e) {
                            Log.e("HTTP.UPLOAD.EXCEPTION", e.getMessage());
                        }

                    }
                }).start();

            }
        }else if(requestCode == 500) {
            if (resultCode == RESULT_OK) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int index = data.getIntExtra("index", -1);
                        String value = data.getStringExtra("value");
                        View parent = listView.getChildAt(index);
                        if (parent != null) {
                            TextView textView = parent.findViewById(R.id.itemRightText);
                            textView.setText(value);
                        }
                    }
                });

            }
        }

    }

    protected int dip2px(double dip) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (scale * dip + 0.5f);
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
                convertView = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout
                        .list_item, null);
            try {
                JSONObject itemData = data.getJSONObject(position);
                TextView itemText = convertView.findViewById(R.id.itemText);
                TextView itemRightText = convertView.findViewById(R.id.itemRightText);
                ImageViewEx itemRightAvatar = convertView.findViewById(R.id.itemRightAvatar);
                itemRightAvatar.setVisibility(View.GONE);
                itemRightText.setVisibility(View.GONE);

                itemText.setText(itemData.getString("text"));
                itemText.setTextColor(Color.parseColor("#222222"));

                switch (itemData.getString("type")) {
                    case "bitmap":
                        itemRightAvatar.setImageBitmap((Bitmap) itemData.get("defaultValue"));
                        itemRightAvatar.setVisibility(View.VISIBLE);
                        break;
                    case "text":
                        itemRightText.setTextColor(Color.parseColor("#999999"));
                        itemRightText.setText(itemData.getString("defaultValue"));
                        itemRightText.setVisibility(View.VISIBLE);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }


    }

}

