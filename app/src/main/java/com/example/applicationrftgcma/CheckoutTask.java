package com.example.applicationrftgcma;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AsyncTask pour valider le panier via l'API POST /cart/checkout
 * Change le status_id de 2 (dans le panier) à 3 (location active)
 */
@SuppressWarnings("deprecation")
public class CheckoutTask extends AsyncTask<Integer, Void, String> {
    private static final String TAG = "CheckoutTask";
    private PanierActivity activity;
    private android.content.Context context;

    public interface CheckoutCallback {
        void onCheckoutSuccess(int itemsCount);
        void onCheckoutError(String errorMessage);
    }

    private CheckoutCallback callback;

    public CheckoutTask(PanierActivity activity, CheckoutCallback callback) {
        this.activity = activity;
        this.context = activity; // PanierActivity est un Context
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Integer... params) {
        Integer customerId = params[0];

        try {
            // URL de l'API
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/checkout");
            Log.d(TAG, "URL appelée: " + url.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // JWT token pour l'autorisation (lu depuis strings.xml)
            String jwt = context.getResources().getString(R.string.api_jwt_token);
            connection.setRequestProperty("Authorization", "Bearer " + jwt);
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent"));
            connection.setDoOutput(true);

            // Créer le JSON body
            JSONObject requestBody = new JSONObject();
            requestBody.put("customerId", customerId);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

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

                Log.d(TAG, "Réponse reçue: " + response.toString());
                return response.toString();
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du checkout", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            if (result != null) {
                try {
                    // Parser la réponse JSON
                    JSONObject jsonResponse = new JSONObject(result);
                    int itemsCount = jsonResponse.getInt("itemsCount");
                    callback.onCheckoutSuccess(itemsCount);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur parsing JSON", e);
                    callback.onCheckoutError("Erreur lors du traitement de la réponse");
                }
            } else {
                callback.onCheckoutError("Erreur de connexion au serveur");
            }
        }
    }
}