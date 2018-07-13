package hk.haowei.wifi.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;
import hk.haowei.wifi.R;
import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.utils.Md5Utils;
import hk.haowei.wifi.view.RecordButton;

public class ChatActivity extends AppCompatActivity {

    protected TextView titleView;
    protected ListView messageListView;
    protected EditText messageText;
    protected RecordButton voicePressed;
    protected ImageView messageTextBtn;
    protected ImageView messageVoiceBtn;
    protected ArrayList<HashMap> MessageList = new ArrayList<>();
    protected InputMethodManager ime;

    protected Handler handler = new Handler();
    protected MediaPlayer player;
    protected MediaRecorder record;

    protected long startMilliseconds = 0L;

    protected Context getContext() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initView();
    }

    protected void initView() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        titleView = findViewById(R.id.titleView);

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

        Intent intent = getIntent();
        titleView.setText(intent.getStringExtra("title"));

        ime = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        voicePressed = findViewById(R.id.voicePressed);
        messageText = findViewById(R.id.messageText);
        voicePressed = findViewById(R.id.voicePressed);
        messageTextBtn = findViewById(R.id.messageTextBtn);
        messageVoiceBtn = findViewById(R.id.messageVoiceBtn);
        messageVoiceBtn.setClickable(true);
        messageTextBtn.setClickable(true);
        voicePressed.setClickable(true);

        messageTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        messageText.requestFocus();
                        ime.showSoftInput(messageText, InputMethodManager.SHOW_FORCED);
                    }
                });
                messageVoiceBtn.setVisibility(View.VISIBLE);
                messageTextBtn.setVisibility(View.GONE);
                messageText.setVisibility(View.VISIBLE);
                voicePressed.setVisibility(View.GONE);
            }
        });

        messageVoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ime.hideSoftInputFromWindow(v.getWindowToken(), 0);
                messageVoiceBtn.setVisibility(View.GONE);
                messageTextBtn.setVisibility(View.VISIBLE);
                messageText.setVisibility(View.GONE);
                voicePressed.setVisibility(View.VISIBLE);
            }
        });

        voicePressed.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                RecordButton view = (RecordButton) v;
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startMilliseconds = System.currentTimeMillis();
                        view.setText(R.string.unpress_to_send);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("ACTION.DOWN", "hit");
                                startVoice();
                            }
                        }).start();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (x < 0 || x > v.getWidth() || y < -view.getHeight() * 3 || y >
                                view.getHeight()) {
                            view.setText(R.string.move_to_cancel);
                        } else {
                            view.setText(R.string.unpress_to_send);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        view.performClick();
                        view.setText(R.string.pressed_to_speaking);
                        if (System.currentTimeMillis() - 1000 > startMilliseconds) {
                            endVoice();
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                return false;
            }
        });
        ImageView messageMore = findViewById(R.id.messageMore);
        messageMore.setClickable(true);
        messageMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.e("MESSAGE.SEND", "hit");
                            JSONObject data = new JSONObject();
                            data.put("FromUserID", 1);
                            data.put("ToUserID", 2);
                            data.put("Type", "text");
                            data.put("Text", messageText.getText().toString());
                            data.put("LocalTime", System.currentTimeMillis());
                            Log.e("SOURCE.DATA", data.toString());

                            AAplication.instance.get().client.send(data.toString());

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageText.getText().clear();
                                }
                            });

                        } catch (Exception e) {
                            Log.e("MESSAGE.ERROR", e.getMessage());
                        }

                    }
                }).start();
            }
        });

        messageListView = findViewById(R.id.messageListView);


        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                Log.e("DATA", intent.getStringExtra("data"));
                try {
                    JSONObject data = new JSONObject(intent.getStringExtra("data"));
                    final HashMap<String, Object> item = new HashMap<>();
                    Iterator<String> iterator = data.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        item.put(key, data.get(key));
                    }

                    item.put("categories", 0);
                    if ((int) item.get("FromUserID") == 1) {
                        item.put("categories", 2);
                    }

                    MessageList.add(item);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("HANDLER.POST", "hit");
                            DataAdapter adapter = new DataAdapter();
                            adapter.setDataSource(MessageList);
                            messageListView.setAdapter(adapter);
                            messageListView.setSelection(adapter.getCount() - 1);
                        }
                    });

                } catch (Exception e) {
                    Log.e("META.ERROR", e.getMessage());
                }
            }
        }, new IntentFilter(getPackageName() + ".IMESSAGE"));

    }

    protected int dip2px(double dip) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (scale * dip + 0.5f);
    }


    protected void startVoice() {
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "录音开始", Toast.LENGTH_SHORT).show();
                }
            });

            try {
                String tempFile = getExternalCacheDir().getAbsolutePath() + "/tmp.amr";
                record = new MediaRecorder();
                record.setAudioSource(MediaRecorder.AudioSource.MIC);
                record.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                record.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                record.setOutputFile(tempFile);
                record.setAudioChannels(1);
                record.setAudioSamplingRate(44100);
                record.prepare();
                record.start();
            }catch (Exception e) {
                Log.e("RECORD.ERROR", e.getMessage());
            }
        }
    }

    protected void endVoice() {
        if (record == null) return ;
        record.stop();
        record.release();

        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "录音结束", Toast.LENGTH_SHORT).show();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {

                String tempFile = getExternalCacheDir().getAbsolutePath() + "/tmp.amr";

                int dur = 0;
                try {
                    player = new MediaPlayer();
                    player.setDataSource(tempFile);
                    player.prepare();
                    dur = player.getDuration();
                    player.release();
                    player = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final int vLen = dur;


                String body = HttpUtils.postFile(BuildConfig.URL +
                        "app/uploadedFile", tempFile);
                Log.e("HTTP.UPLOADED", body + "");

                try {
                    JSONObject result = new JSONObject(body);
                    if (result.getInt("code") == 0) {
                        JSONObject data = new JSONObject();
                        data.put("FromUserID", 1);
                        data.put("ToUserID", 2);
                        data.put("Type", "voice");
                        data.put("Voice", result.getJSONObject("result").getString
                                ("url"));
                        data.put("VoiceLength", String.format("%s".toLowerCase()
                                , vLen / 1000));
                        data.put("LocalTime", System.currentTimeMillis());
                        AAplication.instance.get().client.send(data.toString());
                    }
                } catch (Exception e) {
                    Log.e("JSON.ERROR", e.getMessage());
                }
            }
        }).start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            startVoice();
        }

    }

    public void playVoiceMsg(String urlpath) {

        String path = getExternalCacheDir().getAbsolutePath() + "/" + Md5Utils.hex(urlpath) + "" +
                ".amr";
        File file = new File(path);

        Uri uri = null;
        if (urlpath.startsWith("http")) {
            if (file.isFile()) {
                uri = Uri.fromFile(file);
            } else {
                if (HttpUtils.getFile(urlpath, path)) uri = Uri.fromFile(file);
            }
        } else {
            uri = Uri.fromFile(file);
        }

        try {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
            player = new MediaPlayer();
            player.setDataSource(getContext(), uri);
            player.prepare();
            player.start();
        } catch (Exception e) {
            Log.e("PLAYER.VOICE", e.getMessage());
        }

    }

    protected class DataAdapter extends BaseAdapter {

        private ArrayList<HashMap> data;

        private void setDataSource(ArrayList<HashMap> data) {
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
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

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout
                        .chat_list_item, null);
            }

            HashMap itemData = data.get(position);
            RelativeLayout yoursView = convertView.findViewById(R.id.yoursView);
            RelativeLayout myView = convertView.findViewById(R.id.myView);
            TextView yoursMsgText = convertView.findViewById(R.id.yoursMsgText);
            TextView myMsgText = convertView.findViewById(R.id.myMsgText);

            View unreadVoiceMsg = convertView.findViewById(R.id.unreadVoiceMsg);
            final ImageView yoursImage = convertView.findViewById(R.id.yoursImage);
            final ImageView myImage = convertView.findViewById(R.id.myImage);

            TextView yoursVoiceMsg = convertView.findViewById(R.id.yoursVoiceMsg);
            TextView myVoiceMsg = convertView.findViewById(R.id.myVoiceMsg);

            unreadVoiceMsg.setVisibility(View.GONE);
            yoursVoiceMsg.setVisibility(View.GONE);
            yoursVoiceMsg.setClickable(true);
            myVoiceMsg.setVisibility(View.GONE);
            myVoiceMsg.setClickable(true);

            yoursImage.setVisibility(View.GONE);
            myImage.setVisibility(View.GONE);

            yoursView.setVisibility(View.GONE);
            myView.setVisibility(View.GONE);

            yoursMsgText.setVisibility(View.GONE);
            myMsgText.setVisibility(View.GONE);

            if ((int) itemData.get("categories") == 0) {
                yoursView.setVisibility(View.VISIBLE);

                if (itemData.get("Type").equals("text")) {
                    String text = itemData.get("Text").toString();
                    text = text.replace("\n", "<br/>");
                    Log.e("FORMAT.TEXT", text);
                    Spanned result;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(text);
                    }
                    yoursMsgText.setVisibility(View.VISIBLE);
                    yoursMsgText.setText(result);
                } else if (itemData.get("Type").equals("image")) {
                    String url = itemData.get("Image").toString();
                    Log.e("IMAGE.TYPE", "hit");
                    Log.e("IMAGE.URL", url);
                    yoursView.setVisibility(View.VISIBLE);
                    yoursImage.setVisibility(View.VISIBLE);

                    new HttpAsyncTask(yoursImage).execute(url);

                } else if (itemData.get("Type").equals("voice")) {
                    unreadVoiceMsg.setVisibility(View.VISIBLE);
                    Drawable drawable = getDrawable(R.drawable.icon_u_voice);
                    drawable.setBounds(0, 0, dip2px(16), dip2px(16));
                    yoursVoiceMsg.setCompoundDrawables(drawable, null, null, null);
                    yoursVoiceMsg.setCompoundDrawablePadding(dip2px(Integer.valueOf((String)itemData
                            .get("VoiceLength")) * 3 + 15));
                    yoursVoiceMsg.setTag(itemData.get("Voice").toString());
                    yoursVoiceMsg.setVisibility(View.VISIBLE);
                    yoursVoiceMsg.setText(String.format("%s\"".toLowerCase(), itemData.get
                            ("VoiceLength").toString()));
                    yoursVoiceMsg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        playVoiceMsg(v.getTag().toString());
                                    } catch (Exception e) {
                                        Log.e("PLAY.VOICE", e.getMessage());
                                    }
                                }
                            }).start();
                        }
                    });

                }

                if (position == (data.size() - 1)) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) yoursView
                            .getLayoutParams();
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
                            dip2px(15));
                    yoursView.setLayoutParams(params);
                }
            } else if ((int) itemData.get("categories") == 2) {
                myView.setVisibility(View.VISIBLE);

                if (itemData.get("Type").equals("text")) {
                    String text = itemData.get("Text").toString();
                    text = text.replace("\n", "<br/>");
                    Log.e("FORMAT.TEXT", text);

                    Spanned result;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(text);
                    }
                    myMsgText.setVisibility(View.VISIBLE);
                    myMsgText.setText(result);
                } else if (itemData.get("Type").equals("image")) {
                    myImage.setVisibility(View.VISIBLE);
                    Bitmap bitmap = HttpUtils.getBitmap(itemData.get("Image").toString());
                    myImage.setImageBitmap(bitmap);
                } else if (itemData.get("Type").equals("voice")) {

                    Drawable drawable = getDrawable(R.drawable.icon_i_voice);
                    drawable.setBounds(0, 0, dip2px(16), dip2px(16));
                    myVoiceMsg.setCompoundDrawables(drawable, null, null, null);
                    myVoiceMsg.setCompoundDrawablePadding(dip2px(Integer.valueOf((String)itemData
                            .get("VoiceLength")) * 3 + 15));
                    myVoiceMsg.setText(String.format("%s\"".toLowerCase(), itemData.get
                            ("VoiceLength").toString()));

                    myVoiceMsg.setTag(itemData.get("Voice").toString());
                    myVoiceMsg.setVisibility(View.VISIBLE);

                    myVoiceMsg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        playVoiceMsg(v.getTag().toString());
                                    } catch (Exception e) {
                                        Log.e("PLAY.VOICE", e.getMessage());
                                    }
                                }
                            }).start();
                        }
                    });

                }

                if (position == (data.size() - 1)) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) myView
                            .getLayoutParams();
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
                            dip2px(15));
                    myView.setLayoutParams(params);
                }
            }
            return convertView;
        }


    }

    protected class HttpAsyncTask extends AsyncTask<String, Void, Bitmap> {

        ImageView view;

        HttpAsyncTask(ImageView v) {
            view = v;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return HttpUtils.getBitmap(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            view.setImageBitmap(bitmap);
        }
    }

}
