package com.example.codemissions;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.json.JSONObject;

public class ErrorUtils {

    /** Human-readable message: the API answers errors as {"message": "..."}. */
    public static String message(VolleyError error) {
        NetworkResponse r = error.networkResponse;
        if (r == null) {
            return "Serveur injoignable. Vérifiez la connexion et l'adresse du serveur.";
        }
        if (r.statusCode == 403) {
            return "Accès réservé aux administrateurs. Si tu viens d'être promu, déconnecte-toi puis reconnecte-toi.";
        }
        try {
            String body = new String(r.data, "UTF-8");
            JSONObject json = new JSONObject(body);
            return json.optString("message", "Erreur " + r.statusCode);
        } catch (Exception e) {
            return "Erreur " + r.statusCode;
        }
    }

    public static boolean isUnauthorized(VolleyError error) {
        return error.networkResponse != null && error.networkResponse.statusCode == 401;
    }

    /**
     * Token missing/expired (the API gives 12 h): go back to the login screen.
     * Returns true when it handled the error (the caller must then stop).
     * Never used on the login screen itself, where 401 means "wrong password".
     */
    public static boolean handleUnauthorized(Activity activity, VolleyError error) {
        if (!isUnauthorized(error) || !Session.isLoggedIn(activity)) return false;
        Session.clear(activity);
        Toast.makeText(activity, "Session expirée, reconnectez-vous.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }
}
