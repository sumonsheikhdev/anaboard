package helium314.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";

    private final SharedPreferences prefs;
    private final Context context;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public String getAuthHeader() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }

    public interface AuthCallback {
        void onSuccess(int id, String token);
        void onError(String message);
    }

    public void login(String email, String password, AuthCallback callback) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("email", email);
            jsonParam.put("password", password);

            new ApiClient(context).post("/api/auth/login", jsonParam, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    boolean flag = jsonResponse.optBoolean("flag", false);
                    String message = jsonResponse.optString("message", "Unknown error");

                    if (flag) {
                        JSONObject data = jsonResponse.optJSONObject("data");
                        int id = (data != null) ? data.optInt("id", -1) : -1;
                        String token = jsonResponse.optString("token", "");
                        saveToken(token);
                        callback.onSuccess(id, token);
                    } else {
                        callback.onError(message);
                    }
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}

