package com.example.codemissions;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {
    /** Default = the Android emulator reaching the PC. On a real phone, set the server in the app. */
    public static final String DEFAULT_BASE_URL = "http://localhost:5007/api";

    private static ApiClient instance;
    private final RequestQueue queue;
    private final Context context;

    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.queue = Volley.newRequestQueue(this.context);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    // ---- server address (typed by the user on the login screen, e.g. "192.168.1.20") ----

    private static android.content.SharedPreferences serverPrefs(Context c) {
        return c.getSharedPreferences("server", Context.MODE_PRIVATE);
    }

    private String baseUrl() { return getServerUrl(context); }

    public static String getServerUrl(Context c) {
        return serverPrefs(c).getString("url", DEFAULT_BASE_URL);
    }

    /** What the user sees in the box: "192.168.1.20:5007". */
    public static String getServerShort(Context c) {
        String u = getServerUrl(c).replaceFirst("^https?://", "");
        return u.endsWith("/api") ? u.substring(0, u.length() - 4) : u;
    }

    /**
     * Accepts "192.168.1.20", "192.168.1.20:5007", "http://192.168.1.20:5007" or the full
     * ".../api" form, and stores "http://host:port/api". Empty = back to the emulator address.
     */
    public static void setServer(Context c, String input) {
        String v = input == null ? "" : input.trim();
        if (v.isEmpty()) {
            serverPrefs(c).edit().remove("url").apply();
            return;
        }
        v = v.replaceFirst("^https?://", "");
        int slash = v.indexOf('/');
        if (slash >= 0) v = v.substring(0, slash);
        if (!v.contains(":")) v = v + ":5007";
        serverPrefs(c).edit().putString("url", "http://" + v + "/api").apply();
    }

    private Map<String, String> authHeaders() {
        Map<String, String> headers = new HashMap<>();
        String token = Session.getToken(context);
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    public void postObject(String path, JSONObject body,
                           Response.Listener<JSONObject> ok,
                           Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, baseUrl() + path, body, ok, err) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders();
            }
        };
        // 20 s timeout, no automatic retry (avoids submitting the code twice)
        req.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1f));
        queue.add(req);
    }

    /** POST / PUT / DELETE with a JSON answer (the admin API always answers {"ok": true, ...}). */
    public void send(int method, String path, JSONObject body,
                     Response.Listener<JSONObject> ok,
                     Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(method, baseUrl() + path, body, ok, err) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders();
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1f));
        queue.add(req);
    }

    public void getObject(String path,
                          Response.Listener<JSONObject> ok,
                          Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, baseUrl() + path, null, ok, err) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders();
            }
        };
        queue.add(req);
    }

    public void getArray(String path,
                         Response.Listener<JSONArray> ok,
                         Response.ErrorListener err) {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, baseUrl() + path, null, ok, err) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders();
            }
        };
        queue.add(req);
    }
}