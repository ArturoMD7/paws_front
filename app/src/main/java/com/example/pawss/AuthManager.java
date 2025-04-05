package com.example.pawss;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class AuthManager {
    private final SharedPreferences sharedPreferences;

    public AuthManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "auth_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Error initializing AuthManager", e);
        }
    }

    public void saveAuthTokens(String accessToken, String refreshToken, int userId) {
        sharedPreferences.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putInt("user_id", userId)
                .apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString("access_token", null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString("refresh_token", null);
    }

    public int getUserId() {
        return sharedPreferences.getInt("user_id", -1);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearAuth() {
        sharedPreferences.edit().clear().apply();
    }
}