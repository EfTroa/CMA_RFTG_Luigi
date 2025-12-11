package com.example.applicationrftg;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static TokenManager instance;
    private static final String PREFS_NAME = "RFTGPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_CUSTOMER_ID = "customer_id";

    private String token;
    private Integer customerId;
    private SharedPreferences sharedPreferences;

    private TokenManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Charger depuis SharedPreferences
        token = sharedPreferences.getString(KEY_TOKEN, null);
        customerId = sharedPreferences.getInt(KEY_CUSTOMER_ID, -1);
        if (customerId == -1) {
            customerId = null;
        }
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        } else {
            // Recharger les données depuis SharedPreferences si elles ont changé
            instance.reloadFromPreferences();
        }
        return instance;
    }

    private void reloadFromPreferences() {
        if (token == null) {
            token = sharedPreferences.getString(KEY_TOKEN, null);
        }
        if (customerId == null) {
            int storedId = sharedPreferences.getInt(KEY_CUSTOMER_ID, -1);
            if (storedId != -1) {
                customerId = storedId;
            }
        }
    }

    public void saveToken(String token) {
        this.token = token;
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return token;
    }

    public void saveCustomerId(Integer customerId) {
        this.customerId = customerId;
        if (customerId != null) {
            sharedPreferences.edit().putInt(KEY_CUSTOMER_ID, customerId).apply();
        }
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void clearToken() {
        this.token = null;
        this.customerId = null;
        sharedPreferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_CUSTOMER_ID)
                .apply();
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
}
