package hk.haowei.wifi;

import android.Manifest;
import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import hk.haowei.wifi.utils.HttpUtils;
import hk.haowei.wifi.utils.Md5Utils;


public class AAplication extends Application {

    public double latitude;
    public double longitude;
    public String deviceId = null;
    public String pubKey = null;
    public String downloadURL = null;
    public Bitmap userAvatar = null;
    public JSONObject userInfo = null;
    public JSONObject userInnode = null;
    public boolean locationShared = false;
    public boolean userLogout = false;
    public boolean hasMark = false;

    public WebSocketClient client;


    public LocationManager locationManager;
    public TelephonyManager telephonyManager;

    public static WeakReference<AAplication> instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = new WeakReference<>(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e("LAST.EXCEPTION", e.getMessage());
            }
        });

        // 创建设备公钥
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("TASK", "fetching pubkey");
                // 获取设备唯一识别码
                while (deviceId == null) {
                    try {
                        if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission
                                .READ_PHONE_STATE)) {
                            deviceId = telephonyManager.getDeviceId();
                            Log.e("DEVICE.ID", deviceId);
                        }
                    } catch (Exception e) {
                        Log.e("DEVICE.ID.ERROR", e.getMessage());
                    }
                }

                try {
                    String path = getCacheDir().getAbsolutePath() + "/wifi.key";
                    File file = new File(path);
                    if (!file.isFile()) {
                        String body = HttpUtils.get(BuildConfig.URL + "app/shakeHands");
                        Log.e("SHAKING.HAND", body);
                        if (!body.isEmpty()) {
                            JSONObject data = new JSONObject(body);
                            if (data.opt("pubkey") != null && !data.getString("pubkey").isEmpty()) {
                                pubKey = data.getString("pubkey");
                                FileOutputStream os = new FileOutputStream(path);
                                os.write(body.getBytes());
                                os.flush();
                                os.close();
                            }
                        }
                    }else{
                        FileInputStream is = new FileInputStream(file);
                        byte[] raw = new byte[is.available()];
                        is.read(raw);
                        is.close();
                        JSONObject data = new JSONObject(new String(raw));
                        if (data.opt("pubkey") != null && !data.getString("pubkey").isEmpty()) {
                            pubKey = data.getString("pubkey");
                        }
                    }
                    Log.e("LOAD.DEVICE.PUBKEY", pubKey);
                } catch (Exception e) {
                    Log.e("PUBKEY.ERROR", e.getMessage());
                }
            }

        }).start();


        // 分享设备位置
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (pubKey != null) {
                        if (locationShared) break;
                        if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission
                                .ACCESS_FINE_LOCATION)) {
                            try {
                                locationShared = true;
                                Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                                String body = String.format
                                        ("deviceId=%s&phone=%s&simSerial=%s&latitude=%s&longitude=%s&localtime=%s",
                                        deviceId,
                                        telephonyManager.getLine1Number(),
                                        telephonyManager.getSimSerialNumber(),
                                        latitude = location.getLatitude(),
                                        longitude = location.getLongitude(),
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                                        );
                                Log.e("LOCATION", body);
                                body = HttpUtils.post(BuildConfig.URL + "app/userShareLocation", body);
                                Log.e("HTTP.LOCATION", body);
                            }catch (Exception e) {
                                Log.e("GEOLOCATION.ERROR", e.getMessage());
                            }
                        }
                        break;
                    }
                }
            }
        }).start();


        //读取用户信息
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (pubKey != null) {
                        Log.e("TASK", "fetching user");
                        try {
                            String path = getCacheDir().getAbsolutePath() + "/user.db";
                            FileInputStream is = new FileInputStream(path);
                            byte[] raw = new byte[is.available()];
                            is.read(raw);
                            is.close();
                            Log.e("USER.INFO", new String(raw));

                            userInfo = new JSONObject(new String(raw));
                            if (!userInfo.optString("Avatar").isEmpty()) {

                                //载入用户头像
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            userAvatar = getAvatar(userInfo.getString("Avatar"));
                                        } catch (Exception e) {
                                            Log.e("LOAD.AVATAR.ERROR", e.getMessage());
                                        }
                                    }
                                }).start();
                            }
                        } catch (Exception e) {
                            Log.e("LOAD.USER.INFO", e.getMessage());
                        }
                        break;
                    }
                }
            }
        }).start();


        //检查版本
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String body = String.format("channelId=%s&versionCode=%s&_random=%s",
                            BuildConfig.CHANNEL_ID,
                            BuildConfig.VERSION_CODE,
                            System.currentTimeMillis());
                    body = HttpUtils.get(BuildConfig.URL + "app/checkVersion?" + body);
                    JSONObject result = new JSONObject(body);
                    if (result.opt("result") != null) {
                        hasMark = true;
                    }
                }catch (Exception e) {
                    Log.e("CHECK.VERSION.ERROR", e.getMessage());
                }
            }
        }).start();


        //用户退出事务
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (userLogout) {
                        Log.e("USER.LOGOUT", "hit");
                        userLogout = false;
                        deleteCached();
                    }
                }
            }
        }).start();


        //更新版本事务
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (downloadURL != null) {
                        downloadApk(downloadURL);
                    }
                }
            }
        }).start();


        //用户会话事务
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (userInfo != null) {
                        try {
                            /*if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission
                                    .ACCESS_FINE_LOCATION)) {
                                Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }*/
                            String body = String.format("tokenhash=%s&token=%s&latitude=%s&longitude=%s",
                                    Md5Utils.hex(userInfo.getString("Token")),
                                    userInfo.getString("Token"),
                                    latitude,
                                    longitude);
                            body = HttpUtils.post(BuildConfig.URL + "app/checkAlive", body);
                            Log.e("HTTP.ALIVE", body);
                            JSONObject result = new JSONObject(body);
                            if (result.optInt("code", -1) == 1) {
                                userLogout = true;
                                Log.e("USER.EXPIRED", "hit");
                            }
                            Thread.sleep(35 * 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();


        //消息服务器
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (pubKey != null) {
                        try {
                            String url = "ws://192.168.1.100:8080";
                            url = "ws://haowei.asia:7500";

                            client = new WebSocketClient(new URI
                                    (url)) {

                                @Override
                                public void onOpen( ServerHandshake handshakedata ) {
                                    client.send("{\"auth\":\"123456\"}".getBytes());
                                }

                                @Override
                                public void onMessage(final String message) {
                                    Log.e("RECEIVE.MESSAGE", message);
                                    Intent intent = new Intent(getPackageName() + ".IMESSAGE");
                                    intent.putExtra("data", message);
                                    sendBroadcast(intent);
                                }

                                @Override
                                public void onClose( int code, String reason, boolean remote ) {
                                    Log.e("SERVER.GONE", reason);
                                    client.reconnect();
                                }

                                public void onError( Exception e ) {
                                    Log.e("CONNECT.ERROR", e.getMessage());
                                    //client.reconnect();
                                }
                            };
                            client.connect();
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }).start();

    }

    public void installApk(String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(getApplicationContext(),
                String.format("%s.activity.FileProvider".toLowerCase(),
                getPackageName()),
                new File(path));
        } else {
            uri = Uri.parse("file://" + path);
        }
        Log.e("URI", uri.toString());
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void downloadApk(String url) {
        downloadURL = null;
        final String path = getExternalCacheDir().getAbsolutePath() + "/hwifi_latest.apk";
        Log.e("URL", url);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        request.setDestinationUri(Uri.parse("file://" + path));
        request.setVisibleInDownloadsUi(true);
        final DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final long taskId = downloadManager.enqueue(request);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(taskId);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    int state = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    switch (state) {
                        case DownloadManager.STATUS_FAILED:
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                        case DownloadManager.STATUS_RUNNING:
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            Log.e("DOWNLOAD.COMPLETED", "hit");
                            installApk(path);
                            break;
                    }

                }

            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public Bitmap getAvatar(String url) {
        try {
            Log.e("AVATAR.URL", url);
            String path = getCacheDir().getAbsolutePath() + "/_a" + Md5Utils.hex(url) + ".jpg";
            File file = new File(path);
            if (file.isFile()) {
                Log.e("AVATAR.CACHED", "hit");
                return BitmapFactory.decodeFile(path);
            } else {
                Log.e("AVATAR.REMOTED", "hit");
                Bitmap bitmap = HttpUtils.getBitmap(url);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(path));
                return bitmap;
            }
        }catch (Exception e) {
            Log.e("AVATAR.ERROR", e.getMessage());
        }
        return null;
    }

    public void setCached(final JSONObject result) {
        if (result == null) return ;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = getCacheDir().getAbsolutePath() + "/user.db";
                    FileOutputStream os = new FileOutputStream(path);
                    os.write(result.toString().getBytes());
                    os.flush();
                    os.close();
                }catch (Exception e) {
                    Log.e("WRITE.CACHE.ERROR", e.getMessage());
                }
            }
        }).start();
    }

    public void deleteCached() {
        if (userInfo == null) return ;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = getCacheDir().getAbsolutePath() + "/user.db";
                    File file = new File(path);
                    if (file.isFile()) file.delete();
                    String body = HttpUtils.post(BuildConfig.URL + "app/userLogout", userInfo.getString("Token"));
                    Log.e("HTTP.LOGOUT", body);
                }catch (Exception e) {
                    Log.e("USER.LOGOUT.ERROR", e.getMessage());
                }
                userInfo = null;
            }
        }).start();
    }

    public void cacheSelf() {
        if (userInfo == null) return ;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    StringBuilder query = new StringBuilder();
                    String[] fields = {
                        "UserID",
                        "Nickname",
                        "Birthday",
                        "EMail",
                        "Gender",
                        "AutoShareWifi",
                        "AutoAcceptPush"
                    };

                    for(int i = 0; i < fields.length; i++) {
                        query.append(String.format("%s=%s&", fields[i].equals("UserID") ? "userid" : fields[i],
                                URLEncoder.encode(userInfo
                                .get(fields[i]).toString(), "UTF-8")));
                    }
                    String body = HttpUtils.post(BuildConfig.URL + "app/userModify", query.toString());
                    Log.e("HTTP.USER.MODIFY", body);
                }catch (Exception e) {
                    Log.e("BUILD.CACHE.ERROR", e.getMessage());
                }
            }
        }).start();
    }

}