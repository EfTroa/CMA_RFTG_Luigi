package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AsyncTask pour ajouter un film au panier via l'API POST /cart/add
 * Crée un rental avec status_id = 2 (dans le panier)
 */
@SuppressWarnings("deprecation")
public class AddToCartTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "AddToCartTask";
    private Integer customerId;
    private Integer filmId;
    private android.content.Context context;

    public interface AddToCartCallback {
        void onAddToCartSuccess();
        void onAddToCartError(String errorMessage);
    }

    private AddToCartCallback callback;

    public AddToCartTask(android.content.Context context, Integer customerId, Integer filmId, AddToCartCallback callback) {
        this.context = context;
        this.customerId = customerId;
        this.filmId = filmId;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            // URL de l'API
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/add");
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
            requestBody.put("filmId", filmId);

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
                return "SUCCESS";
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.e(TAG, "Aucun exemplaire disponible");
                return "UNAVAILABLE";
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return "ERROR";
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout au panier", e);
            return "ERROR";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            if ("SUCCESS".equals(result)) {
                callback.onAddToCartSuccess();
            } else if ("UNAVAILABLE".equals(result)) {
                callback.onAddToCartError("Aucun exemplaire disponible pour ce film");
            } else {
                callback.onAddToCartError("Erreur de connexion au serveur");
            }
        }
    }
}