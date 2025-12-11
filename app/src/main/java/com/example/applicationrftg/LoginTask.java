package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "LoginTask";
    private static final String API_URL = "http://10.0.2.2:8180/customers/verify";

    private String email;
    private String password;
    private LoginCallback callback;

    public interface LoginCallback {
        void onLoginSuccess(Integer customerId);
        void onLoginError(String errorMessage);
    }

    public LoginTask(String email, String password, LoginCallback callback) {
        this.email = email;
        this.password = password;
        this.callback = callback;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Créer le JSON body
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);

            // Envoyer la requête
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parser la réponse JSON
                JSONObject jsonResponse = new JSONObject(response.toString());
                int customerId = jsonResponse.getInt("customerId");

                Log.d(TAG, "Response customerId: " + customerId);

                // Vérifier si le customerId est valide
                if (customerId > 0) {
                    Log.d(TAG, "Login successful. CustomerId: " + customerId);
                    return customerId;
                } else {
                    Log.e(TAG, "Login failed: Invalid credentials (customerId = -1)");
                    return -1;
                }
            } else {
                Log.e(TAG, "Login failed with code: " + responseCode);
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during login", e);
            return -1;
        }
    }

    @Override
    protected void onPostExecute(Integer customerId) {
        if (callback != null) {
            if (customerId != null && customerId > 0) {
                callback.onLoginSuccess(customerId);
            } else {
                callback.onLoginError("Échec de connexion. Vérifiez vos identifiants.");
            }
        }
    }
}
