package helium314.keyboard;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private static final String BASE_URL = "https://ai.analysaai.com";
    private final AuthManager authManager;
    private final Context context;
    private UnauthorizedListener unauthorizedListener;

    public ApiClient(Context context) {
        this.context = context;
        this.authManager = new AuthManager(context);
    }

    public interface UnauthorizedListener {
        void onUnauthorized();
    }

    public void setUnauthorizedListener(UnauthorizedListener listener) {
        this.unauthorizedListener = listener;
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);

        void onError(String message);
    }

    public void post(String endpoint, JSONObject params, ApiCallback callback) {
        new Thread(() -> {
            try {
                boolean includeAuth = !endpoint.contains("/auth/login");
                HttpURLConnection conn = getHttpURLConnection(endpoint, includeAuth);

                if (params != null) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = params.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                        os.flush();
                    }
                }

                int responseCode = conn.getResponseCode();
                Log.e("responseCode: ", responseCode + "");
                // Handle 401 Unauthorized
                if (responseCode == 401) {
                    Log.w("ApiClient", "Received 401 Unauthorized, clearing token");
                    authManager.clearToken();
                    if (unauthorizedListener != null) {
                        unauthorizedListener.onUnauthorized();
                    }
                    callback.onError("Session expired. Please login again.");
                    return;
                }

                JSONObject jsonResponse = getJsonObject(conn, responseCode);
                Log.e("post: ", String.valueOf(jsonResponse));
                callback.onSuccess(jsonResponse);

            } catch (Exception e) {
                Log.e("ApiClient", "Request failed: " + endpoint, e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    @NonNull
    private static JSONObject getJsonObject(HttpURLConnection conn, int code) throws IOException, JSONException {
        BufferedReader br;
        if (code >= 200 && code < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            java.io.InputStream es = conn.getErrorStream();
            if (es != null) {
                br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8));
            } else {
                // Return a generic error if no stream is available
                return new JSONObject().put("flag", false).put("message", "HTTP error code: " + code);
            }
        }

        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine); // Don't trim, let's keep it as is
        }

        String responseStr = response.toString().trim();
        if (responseStr.isEmpty()) {
            return new JSONObject().put("flag", false).put("message", "Empty response from server");
        }
        return new JSONObject(responseStr);
    }

    @NonNull
    private HttpURLConnection getHttpURLConnection(String endpoint, boolean includeAuth) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        if (includeAuth) {
            String authHeader = authManager.getAuthHeader();
            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }
        }

        conn.setDoOutput(true);
        return conn;
    }

    // AI Specific methods
    public void polish(String text, String style, ApiCallback callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("text", text);
            params.put("style", style);
            post("/api/ai/polish", params, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void explain(String text, String lng, ApiCallback callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("text", text);
            params.put("lng", lng);
            post("/api/ai/explain", params, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void grammarFix(String text, ApiCallback callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("text", text);
            post("/api/ai/grammar-fix", params, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void translate(String text, String lng, ApiCallback callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("text", text);
            params.put("lng", lng);
            post("/api/ai/translate", params, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void reply(String text, String tone, ApiCallback callback) {
        try {
            JSONObject params = new JSONObject();
            params.put("text", text);
            params.put("tone", tone);
            post("/api/ai/reply", params, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}
