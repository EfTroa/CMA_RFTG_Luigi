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
 * TÂCHE ASYNCHRONE : AddToCartTask
 *
 * Ajoute un film au panier côté serveur via une requête HTTP POST.
 * Endpoint : POST /cart/add
 * Corps JSON : { "customerId": 42, "filmId": 1 }
 * Réponse :
 *   - HTTP 200 : film ajouté avec succès (crée un rental avec status_id = 2 = "dans le panier")
 *   - HTTP 404 : aucun exemplaire disponible pour ce film
 *   - Autre : erreur serveur
 *
 * Double stockage :
 * Après un ajout réussi côté serveur, le film est aussi stocké localement
 * dans SQLite (via PanierManager) pour l'affichage dans PanierActivity sans
 * avoir besoin de refaire un appel réseau.
 *
 * PARAMÈTRES GÉNÉRIQUES AsyncTask<Params, Progress, Result> :
 *   - Params : Void = pas de paramètre à execute()
 *   - Progress : Void = pas de notification de progression
 *   - Result : String = "SUCCESS", "UNAVAILABLE", ou "ERROR"
 *
 * @SuppressWarnings("deprecation") : supprime l'avertissement de dépréciation d'AsyncTask
 */
@SuppressWarnings("deprecation")
public class AddToCartTask extends AsyncTask<Void, Void, String> {

    // Tag pour le Logcat
    private static final String TAG = "AddToCartTask";

    // L'identifiant du client connecté (nécessaire pour l'API)
    private Integer customerId;

    // L'identifiant du film à ajouter
    private Integer filmId;

    // Contexte Android pour accéder aux ressources (token JWT dans strings.xml)
    private android.content.Context context;

    /**
     * INTERFACE : AddToCartCallback
     *
     * Contrat pour recevoir le résultat de l'ajout au panier.
     * Implémentée par FilmAdapter et DetailfilmActivity.
     */
    public interface AddToCartCallback {
        /** Appelée si l'ajout a réussi (HTTP 200) */
        void onAddToCartSuccess();

        /**
         * Appelée si l'ajout a échoué.
         * @param errorMessage Message d'erreur explicatif pour l'utilisateur
         */
        void onAddToCartError(String errorMessage);
    }

    // Le callback pour notifier l'appelant du résultat
    private AddToCartCallback callback;

    /**
     * Constructeur : initialise la tâche avec les données nécessaires.
     *
     * @param context    Contexte Android (pour lire les ressources)
     * @param customerId L'identifiant du client connecté
     * @param filmId     L'identifiant du film à louer
     * @param callback   Le gestionnaire de résultat
     */
    public AddToCartTask(android.content.Context context, Integer customerId, Integer filmId, AddToCartCallback callback) {
        this.context = context;
        this.customerId = customerId;
        this.filmId = filmId;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread en arrière-plan.
     * Effectue la requête HTTP POST vers /cart/add.
     *
     * @param voids Aucun paramètre attendu
     * @return "SUCCESS", "UNAVAILABLE" ou "ERROR"
     */
    @Override
    protected String doInBackground(Void... voids) {
        try {
            // URL de l'API
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/add");
            Log.d(TAG, "URL appelée: " + url.toString());

            // Configurer la connexion HTTP
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // JWT token pour l'autorisation (lu depuis strings.xml)
            String jwt = context.getResources().getString(R.string.api_jwt_token);
            connection.setRequestProperty("Authorization", "Bearer " + jwt);
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent"));

            // setDoOutput(true) = on va envoyer des données dans le corps de la requête
            connection.setDoOutput(true);

            // Créer le JSON body avec l'identifiant client et l'identifiant du film
            JSONObject requestBody = new JSONObject();
            requestBody.put("customerId", customerId);
            requestBody.put("filmId", filmId);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

            // Envoyer le corps JSON
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            // Lire le code de réponse du serveur
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // HTTP 200 : ajout réussi
                // On lit la réponse (même si on ne l'utilise pas ici)
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
                // HTTP 404 : aucun exemplaire disponible pour ce film
                Log.e(TAG, "Aucun exemplaire disponible");
                return "UNAVAILABLE";
            } else {
                // Autre code : erreur serveur non prévue
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return "ERROR";
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout au panier", e);
            return "ERROR";
        }
    }

    /**
     * Exécuté sur le thread UI après la fin de doInBackground().
     * Interprète le résultat String et appelle le callback approprié.
     *
     * @param result "SUCCESS", "UNAVAILABLE" ou "ERROR"
     */
    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            if ("SUCCESS".equals(result)) {
                // L'ajout a réussi côté serveur
                callback.onAddToCartSuccess();
            } else if ("UNAVAILABLE".equals(result)) {
                // Aucun exemplaire disponible : message explicatif pour l'utilisateur
                callback.onAddToCartError("Aucun exemplaire disponible pour ce film");
            } else {
                // Erreur de connexion ou erreur serveur générique
                callback.onAddToCartError("Erreur de connexion au serveur");
            }
        }
    }
}
