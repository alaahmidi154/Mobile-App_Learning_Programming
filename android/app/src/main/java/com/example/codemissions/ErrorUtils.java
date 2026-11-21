package com.example.codemissions;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.json.JSONObject;

public class ErrorUtils {

    public static String message(VolleyError error) {
        NetworkResponse r = error.networkResponse;
        if (r == null) {
            return "Server unreachable. Check the connection and the server address.";
        }
        try {
            String body = new String(r.data, "UTF-8");
            JSONObject json = new JSONObject(body);
            return json.optString("message", "Error " + r.statusCode);
        } catch (Exception e) {
            return "Error " + r.statusCode;
        }
    }
}