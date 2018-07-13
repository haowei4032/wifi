package hk.haowei.wifi.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.net.ssl.HttpsURLConnection;

import hk.haowei.wifi.AAplication;
import hk.haowei.wifi.BuildConfig;

public class HttpUtils {

    private static String pubKey = null;

    private static String getDeviceId() {

        /*String deviceId;
        if (AAplication.instance.get().pubKey != null) {
            Log.e("DEVICE.ID", AAplication.instance.get().deviceId + "");
            Log.e("DEVICE.PUBKEY", AAplication.instance.get().pubKey + "");
            pubKey = AAplication.instance.get().pubKey;
            return AAplication.instance.get().deviceId;
        }

        try {
            String path = AAplication.instance.get().getCacheDir().getAbsolutePath() + "/wifi.key";
            File file = new File(path);
            deviceId = AAplication.instance.get().deviceId;
            if (file.isFile()) {
                FileInputStream is = new FileInputStream(file);
                Long len = file.length();
                byte[] raw = new byte[len.intValue()];
                is.read(raw);
                is.close();
                JSONObject data = new JSONObject(new String(raw));
                if (data.opt("pubkey") != null) {
                    pubKey = data.getString("pubkey");
                }
            }
        }catch (Exception e) {
            pubKey = null;
            deviceId = null;
        }

        return deviceId;*/

        pubKey = AAplication.instance.get().pubKey;
        return AAplication.instance.get().deviceId;
    }

    public static String get(String urlpath) {
        String next;
        String body = "";
        String deviceId = getDeviceId();
        try {
            URL url = new URL(urlpath);
            HttpURLConnection http;
            if (urlpath.startsWith("https")) {
                http = (HttpsURLConnection) url.openConnection();
            }else {
                http = (HttpURLConnection) url.openConnection();
            }
            http.setUseCaches(false);
            http.setConnectTimeout(10 * 1000);
            http.setReadTimeout(15 * 1000);
            http.setRequestMethod("GET");
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("User-Agent", BuildConfig.USER_AGENT);
            http.setRequestProperty("Connection", "keep-alive");
            if (deviceId != null) http.setRequestProperty("X-Device-Id", deviceId);

            if (http.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(http.getInputStream());
                BufferedReader buffered = new BufferedReader(reader);
                while ((next = buffered.readLine()) != null) {
                    body = body.concat(next);
                }

                if (deviceId != null && pubKey != null) {
                    AesUtils.init(pubKey);
                    byte[] plaintext = AesUtils.decrypt(body);
                    return plaintext != null ? new String(plaintext).trim() : body;
                } else {
                    return body;
                }

            } else {
                throw new Exception("HTTP protocol response code is " + http.getResponseCode());
            }
        } catch (Exception e) {
            Log.e("HTTPUtils.EXCEPTION", e.getMessage());
        }

        return body;
    }

    public static Bitmap getBitmap(String urlpath) {
        try {
            URL url = new URL(urlpath);
            HttpURLConnection http;
            if (urlpath.startsWith("https")) {
                http = (HttpsURLConnection) url.openConnection();
            }else {
                http = (HttpURLConnection) url.openConnection();
            }
            http.setUseCaches(false);
            http.setConnectTimeout(15 * 1000);
            http.setReadTimeout(60 * 1000);
            http.setRequestMethod("GET");
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("User-Agent", BuildConfig.USER_AGENT);
            http.setRequestProperty("Connection", "close");
            http.setDoInput(true);

            Log.e("STATUS.CODE", "" + http.getResponseCode());

            if (http.getResponseCode() == 200) {
                return BitmapFactory.decodeStream(http.getInputStream());
            } else {
                throw new Exception("HTTP protocol response code is " + http.getResponseCode());
            }
        }catch (Exception e) {
           Log.e("HTTP.BITMAP", e.getMessage() + "");
        }

        return null;
    }

    public static boolean getFile(String urlpath, String filepath) {

        try {
            URL url = new URL(urlpath);
            HttpURLConnection http;
            if (urlpath.startsWith("https")) {
                http = (HttpsURLConnection) url.openConnection();
            }else {
                http = (HttpURLConnection) url.openConnection();
            }
            http.setDoInput(true);
            http.setUseCaches(false);
            http.setConnectTimeout(15 * 1000);
            http.setReadTimeout(60 * 1000);
            http.setRequestMethod("GET");
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("User-Agent", BuildConfig.USER_AGENT);
            http.setRequestProperty("Connection", "close");

            if (http.getResponseCode() == 200) {

                InputStream is = http.getInputStream();
                FileOutputStream os = new FileOutputStream(new File(filepath));
                int len;
                byte[] buf = new byte[8192];
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                is.close();
                os.close();

                if (new File(filepath).isFile()) return true;

            } else {
                throw new Exception("HTTP protocol response code is " + http.getResponseCode());
            }
        }catch (Exception e) {
            Log.e("HTTP.BITMAP", e.getMessage() + "");
        }

        return false;
    }

    public static String post(String urlpath, String data) {

        String next;
        String body = "";
        String deviceId = getDeviceId();
        if (deviceId != null && pubKey != null) {
            AesUtils.init(pubKey);
            Log.e("HTTP.POST.DATA", data);
            data = new String(Base64.encode(AesUtils.encrypt(data), Base64.DEFAULT));
        }

        try {
            URL url = new URL(urlpath);
            HttpURLConnection http;
            if (urlpath.startsWith("https")) {
                http = (HttpsURLConnection) url.openConnection();
            }else {
                http = (HttpURLConnection) url.openConnection();
            }
            http.setUseCaches(false);
            http.setConnectTimeout(15 * 1000);
            http.setReadTimeout(60 * 1000);
            http.setRequestMethod("POST");
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("User-Agent", BuildConfig.USER_AGENT);
            http.setRequestProperty("Connection", "close");
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            http.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            if (deviceId != null) http.setRequestProperty("X-Device-Id", deviceId);

            http.setDoOutput(true);
            http.setDoInput(true);

            OutputStream put = http.getOutputStream();
            put.write(data.getBytes());
            put.flush();
            put.close();

            if (http.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(http.getInputStream());
                BufferedReader buffered = new BufferedReader(reader);
                while ((next = buffered.readLine()) != null) {
                    body = body.concat(next);
                }

                if (deviceId != null && pubKey != null) {
                    AesUtils.init(pubKey);
                    byte[] plaintext = AesUtils.decrypt(body);
                    return plaintext != null ? new String(plaintext).trim() : body;
                } else {
                    return body;
                }

            } else {
                throw new Exception("HTTP protocol response code is " + http.getResponseCode());
            }
        } catch (Exception e) {
            Log.e("HTTPUtils.EXCEPTION", e.getMessage());
        }

        return body;
    }

    public static String postBitmap(String urlpath, String filepath) {

        String next;
        String body = "";
        String deviceId = getDeviceId();
        String boundary = Md5Utils.hex(String.valueOf(System.currentTimeMillis())).substring(0, 16);

        try {
            File file = new File(filepath);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filepath, options);
            String type = options.outMimeType;

            URL url = new URL(urlpath);
            HttpURLConnection http;
            if (urlpath.startsWith("https")) {
                http = (HttpsURLConnection) url.openConnection();
            } else {
                http = (HttpURLConnection) url.openConnection();
            }
            http.setUseCaches(false);
            http.setDoInput(true);
            http.setDoOutput(true);
            http.setConnectTimeout(15 * 1000);
            http.setReadTimeout(60 * 1000);
            http.setRequestMethod("POST");
            http.setChunkedStreamingMode(128 * 1024);
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("User-Agent", BuildConfig.USER_AGENT);
            http.setRequestProperty("Connection", "keep-alive");
            http.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            if (deviceId != null) http.setRequestProperty("X-Device-Id", deviceId);
            if (AAplication.instance.get().userInfo != null) {
                http.setRequestProperty("X-Token-Id", AAplication.instance.get().userInfo.getString("Token"));
            }

            OutputStream os = http.getOutputStream();
            os.write(String.format("--%s\r\n", boundary).getBytes());
            os.write(String.format("Content-Disposition: form-data; name=\"file\"; filename=\"%s\";\r\n", file.getName()).getBytes());
            os.write(String.format("Content-Type: %s\r\n", type).getBytes());
            os.write("\r\n".getBytes());

            FileInputStream fis = new FileInputStream(filepath);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = fis.read(buffer)) != -1)
            {
                os.write(buffer, 0, count);
            }
            fis.close();


            os.write("\r\n".getBytes());
            os.write(String.format("--%s--\r\n", boundary).getBytes());
            os.flush();
            os.close();

            if (http.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(http.getInputStream());
                BufferedReader buffered = new BufferedReader(reader);
                while ((next = buffered.readLine()) != null) {
                    body = body.concat(next);
                }

                return body;

            } else {
                throw new Exception("HTTP protocol response code is " + http.getResponseCode());
            }
        }catch (Exception e) {
            Log.e("POST.BITMAP.EXCEPTION", e.getMessage());
        }

        return body;
    }

    public static String postFile(String urlpath, String filepath) {

        String next;
        String body = "";
        String ext = MimeTypeMap.getFileExtensionFromUrl(filepath);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        String boundary = Md5Utils.hex(String.valueOf(System.currentTimeMillis())).substring(0, 16);
        File file = new File(filepath);

        try {
            URL url = new URL(urlpath);
            HttpURLConnection http;
            if (urlpath.startsWith("https")) {
                http = (HttpsURLConnection) url.openConnection();
            } else {
                http = (HttpURLConnection) url.openConnection();
            }

            http.setUseCaches(false);
            http.setDoInput(true);
            http.setDoOutput(true);
            http.setConnectTimeout(60 * 1000);
            http.setReadTimeout(60 * 1000);
            http.setRequestMethod("POST");
            http.setChunkedStreamingMode(128 * 1024);
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("User-Agent", BuildConfig.USER_AGENT);
            http.setRequestProperty("Connection", "keep-alive");
            http.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            OutputStream os = http.getOutputStream();

            os.write(String.format("--%s\r\n", boundary).getBytes());
            os.write(String.format("Content-Disposition: form-data; name=\"file\"; filename=\"%s\";\r\n", file.getName()).getBytes());
            os.write(String.format("Content-Type: %s\r\n", mime).getBytes());
            os.write("\r\n".getBytes());


            FileInputStream fis = new FileInputStream(filepath);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = fis.read(buffer)) != -1)
            {
                os.write(buffer, 0, count);
            }
            fis.close();


            os.write("\r\n".getBytes());
            os.write(String.format("--%s--\r\n", boundary).getBytes());
            os.flush();
            os.close();

            if (http.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(http.getInputStream());
                BufferedReader buffered = new BufferedReader(reader);
                while ((next = buffered.readLine()) != null) {
                    body = body.concat(next);
                }
                return body;

            } else {
                throw new Exception("HTTP protocol response code is " + http.getResponseCode());
            }

        }catch (Exception e) {
            Log.e("HTTP.EXCEPTION", e.getMessage());
        }

        return body;

    }

}
