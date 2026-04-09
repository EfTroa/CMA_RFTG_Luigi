package com.example.applicationrftgcma;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class AddCommentTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "AddCommentTask";

    private final Integer filmId;

    private final Integer customerId;

    private final String commentText;

    private final android.content.Context context;


    public interface AddCommentCallback {
        void onAddCommentSuccess();
        void onAddCommentError(String errorMessage);
    }

    private final AddCommentCallback callback;

    public AddCommentTask(android.content.Context context, Integer filmId, Integer customerId,
                          String commentText, AddCommentCallback callback) {
        this.context = context;
        this.filmId = filmId;
        this.customerId = customerId;
        this.commentText = commentText;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/films/commentaire/add");
            Log.d(TAG, "URL appelée: " + url.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // JWT token pour l'autorisation
            String jwt = context.getResources().getString(R.string.api_jwt_token);
            connection.setRequestProperty("Authorization", "Bearer " + jwt);
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent"));

            connection.setDoOutput(true);

            // Corps JSON avec les données du commentaire
            JSONObject requestBody = new JSONObject();
            requestBody.put("filmId", filmId);
            requestBody.put("customerId", customerId);
            requestBody.put("commentText", commentText);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return "SUCCESS";
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return "ERROR";
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout du commentaire", e);
            return "ERROR";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            if ("SUCCESS".equals(result)) {
                callback.onAddCommentSuccess();
            } else {
                callback.onAddCommentError("Erreur de connexion au serveur");
            }
        }
    }
}