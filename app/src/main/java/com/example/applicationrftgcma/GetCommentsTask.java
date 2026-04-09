package com.example.applicationrftgcma;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@SuppressWarnings("deprecation")
public class GetCommentsTask extends AsyncTask<Void, Void, List<FilmComment>> {

    private static final String TAG = "GetCommentsTask";

    private final Integer filmId;

    private final android.content.Context context;

    // Message d'erreur transmis au callback en cas d'échec
    private String errorMessage;

    public interface GetCommentsCallback {
        void onGetCommentsSuccess(List<FilmComment> comments);

        void onGetCommentsError(String errorMessage);
    }

    private final GetCommentsCallback callback;

    public GetCommentsTask(android.content.Context context, Integer filmId, GetCommentsCallback callback) {
        this.context = context;
        this.filmId = filmId;
        this.callback = callback;
    }

    @Override
    protected List<FilmComment> doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/films/commentaire");
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

            // Corps JSON avec l'identifiant du film
            JSONObject requestBody = new JSONObject();
            requestBody.put("filmId", filmId);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse JSON
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Log.d(TAG, "Réponse reçue: " + response.toString());

                // Désérialiser le JSON en liste de FilmComment
                Gson gson = new Gson();
                Type listType = new TypeToken<List<FilmComment>>() {}.getType();
                return gson.fromJson(response.toString(), listType);
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                errorMessage = "Erreur serveur (" + responseCode + ")";
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des commentaires", e);
            errorMessage = "Erreur de connexion au serveur";
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<FilmComment> comments) {
        if (callback != null) {
            if (comments != null) {
                callback.onGetCommentsSuccess(comments);
            } else {
                callback.onGetCommentsError(errorMessage != null ? errorMessage : "Erreur inconnue");
            }
        }
    }
}