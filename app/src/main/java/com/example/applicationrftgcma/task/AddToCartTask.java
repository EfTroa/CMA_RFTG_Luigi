package com.example.applicationrftgcma.task;

import android.os.AsyncTask;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.UrlManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour ajouter un film au panier via l'API REST POST /cart/add.
 * Crée un "rental" côté serveur avec status_id = 2 (statut "dans le panier").
 *
 * Flux d'exécution :
 *   Appelant (FilmAdapter ou DetailfilmActivity)
 *   → new AddToCartTask(...).execute()
 *   → doInBackground() : appel HTTP POST sur le thread réseau
 *   → onPostExecute() : retour sur le thread UI, appel du callback
 *
 * Codes de retour de doInBackground() :
 *   "SUCCESS"     → un exemplaire disponible a été réservé
 *   "UNAVAILABLE" → HTTP 404 = aucun exemplaire disponible pour ce film
 *   "ERROR"       → autre erreur HTTP ou exception réseau
 */
@SuppressWarnings("deprecation")
public class AddToCartTask extends AsyncTask<Void, Void, String> {

    // Tag pour les logs Logcat — permet de filtrer les messages de cette tâche
    private static final String TAG = "AddToCartTask";

    // Identifiant du client connecté — envoyé dans le corps JSON de la requête
    private Integer customerId;

    // Identifiant du film à ajouter — envoyé dans le corps JSON de la requête
    private Integer filmId;

    // Contexte Android nécessaire pour lire les ressources (token JWT dans strings.xml)
    private android.content.Context context;

    /**
     * Interface de callback permettant à l'appelant de réagir au résultat.
     * Pattern Observer : l'appelant implémente cette interface et fournit les
     * deux méthodes à appeler selon le résultat de l'opération.
     */
    public interface AddToCartCallback {
        /** Appelé si le film a bien été ajouté au panier côté serveur */
        void onAddToCartSuccess();
        /** Appelé en cas d'erreur (film indisponible ou problème réseau) */
        void onAddToCartError(String errorMessage);
    }

    // Référence au callback fourni par l'appelant — appelé dans onPostExecute()
    private AddToCartCallback callback;

    /**
     * Constructeur — reçoit toutes les données nécessaires à l'appel API.
     *
     * @param context    Contexte Android pour lire les ressources
     * @param customerId Identifiant du client connecté
     * @param filmId     Identifiant du film à ajouter au panier
     * @param callback   Listener à notifier une fois la requête terminée
     */
    public AddToCartTask(android.content.Context context, Integer customerId, Integer filmId, AddToCartCallback callback) {
        this.context = context;
        this.customerId = customerId;
        this.filmId = filmId;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Effectue l'appel HTTP POST vers /cart/add avec le customerId et le filmId en JSON.
     *
     * @return "SUCCESS", "UNAVAILABLE" ou "ERROR" selon la réponse du serveur
     */
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
            connection.setDoOutput(true); // Indique qu'on envoie un corps (nécessaire pour POST)

            // Construire le corps JSON : { "customerId": X, "filmId": Y }
            JSONObject requestBody = new JSONObject();
            requestBody.put("customerId", customerId);
            requestBody.put("filmId", filmId);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

            // Envoyer le corps de la requête en UTF-8
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse (non utilisée mais on vide le flux pour libérer la connexion)
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
                // 404 = aucun exemplaire physique disponible pour ce film en stock
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

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Traduit le code de retour en appel de callback pour notifier l'appelant.
     *
     * @param result Le code retourné par doInBackground() ("SUCCESS", "UNAVAILABLE" ou "ERROR")
     */
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