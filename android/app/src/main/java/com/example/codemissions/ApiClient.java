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
    public static final boolean USE_MOCK_DATA = false; // true = demo mode without server (fake data)
    // Emulator: 10.0.2.2 ; real phone: http://<PC IP>:5007/api (same Wi-Fi, firewall port 5007 open)
    public static final String BASE_URL = "http://10.0.2.2:5007/api";

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
                Request.Method.POST, BASE_URL + path, body, ok, err) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders();
            }
        };
        // 20 s timeout, no automatic retry (avoids submitting the code twice)
        req.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1f));
        queue.add(req);
    }

    public void getObject(String path,
                          Response.Listener<JSONObject> ok,
                          Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, BASE_URL + path, null, ok, err) {
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
                Request.Method.GET, BASE_URL + path, null, ok, err) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return authHeaders();
            }
        };
        queue.add(req);
    }
}