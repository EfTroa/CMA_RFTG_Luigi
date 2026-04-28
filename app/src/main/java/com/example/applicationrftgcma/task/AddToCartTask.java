package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour ajouter un film au panier via l'API REST POST /cart/add.
 * Crée un "rental" côté serveur avec status_id = 2 (statut "dans le panier").
 *
 * Qui l'appelle : FilmAdapter (bouton "Ajouter au panier" sur chaque item de liste)
 *                 DetailfilmActivity (bouton "Ajouter au panier" sur la page de détail)
 * Ce qu'elle retourne : "SUCCESS" (JSON brut avec rentalId), "UNAVAILABLE" (404) ou "ERROR"
 *
 * Flux d'exécution :
 *   Appelant (FilmAdapter ou DetailfilmActivity)
 *   → construit le JSONObject body { "customerId": N, "filmId": M }
 *   → new AddToCartTask(context, body, callback).execute()
 *   → doInBackground() : construit l'URL et délègue à appelerReseauPost()
 *   → onPostExecute()  : retour sur le thread UI, parsing JSON et appel du callback
 *
 * Codes de retour de doInBackground() :
 *   JSON brut → un exemplaire disponible a été réservé → onPostExecute extrait le rentalId
 *   "UNAVAILABLE" → HTTP 404 = aucun exemplaire disponible pour ce film
 *   null          → autre erreur HTTP (401, 500…) ou exception réseau
 *
 * Note : appelerReseauPost() retourne "UNAVAILABLE" sur 404 (au lieu de null)
 *   pour permettre à onPostExecute() de distinguer film indisponible d'une vraie erreur réseau.
 *
 * AsyncTask<Void, Void, String> :
 *   - Void   → execute() ne reçoit rien (toutes les données passent par le constructeur)
 *   - Void   → pas de progression intermédiaire publiée
 *   - String → doInBackground retourne le JSON brut ou un code d'erreur, reçu par onPostExecute
 */

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.UrlManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class AddToCartTask extends AsyncTask<Void, Void, String> {

    // Tag pour les logs Logcat — permet de filtrer les messages de cette tâche
    private static final String TAG = "AddToCartTask";

    // Contexte Android — nécessaire pour lire le JWT dans strings.xml via getJwt()
    private Context context;

    // Corps JSON formé dans l'appelant avant instanciation de cette tâche
    // Contient : { "customerId": N, "filmId": M }
    private JSONObject body;

    /**
     * Interface de callback permettant à l'appelant de réagir au résultat.
     * Pattern Observer : l'appelant implémente cette interface et fournit les
     * deux méthodes à appeler selon le résultat de l'opération.
     */
    public interface AddToCartCallback {
        /**
         * Appelé si le film a bien été ajouté au panier côté serveur (200 OK).
         * @param rentalId L'ID du rental créé — à stocker dans SQLite pour pouvoir le supprimer via API
         */
        void onAddToCartSuccess(int rentalId);

        /**
         * Appelé en cas d'erreur (film indisponible 404 ou problème réseau).
         * @param errorMessage Le message d'erreur à afficher à l'utilisateur
         */
        void onAddToCartError(String errorMessage);
    }

    // Référence au callback fourni par l'appelant — appelé dans onPostExecute()
    private AddToCartCallback callback;

    /**
     * Constructeur — reçoit toutes les données nécessaires à l'appel API.
     * Le body doit déjà être formé dans l'appelant (Activity ou Adapter).
     *
     * @param context  Contexte Android pour lire le JWT dans strings.xml
     * @param body     JSONObject { "customerId": N, "filmId": M } formé dans l'appelant
     * @param callback Listener à notifier une fois la requête terminée
     */
    public AddToCartTask(Context context, JSONObject body, AddToCartCallback callback) {
        this.context = context;
        this.body = body;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Construit l'URL et délègue l'appel réseau POST à appelerReseauPost().
     *
     * @param voids Aucun paramètre — toutes les données arrivent via le constructeur
     * @return Le JSON brut de la réponse (succès), "UNAVAILABLE" (404), ou null (autre erreur)
     */
    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/add");
            Log.d(TAG, "URL appelée: " + url.toString());

            return appelerReseauPost(url, body);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout au panier", e);
            return null;
        }
    }

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Interprète le résultat et notifie le callback :
     *   - JSON brut → parse le rentalId → onAddToCartSuccess(rentalId)
     *   - "UNAVAILABLE" → onAddToCartError("Aucun exemplaire disponible…")
     *   - null → onAddToCartError("Erreur de connexion…")
     *
     * @param result Le code retourné par doInBackground() (JSON brut, "UNAVAILABLE" ou null)
     */
    @Override
    protected void onPostExecute(String result) {
        if (callback == null) return;

        if ("UNAVAILABLE".equals(result)) {
            // Le serveur a répondu 404 : aucun exemplaire disponible
            callback.onAddToCartError("Aucun exemplaire disponible pour ce film");
        } else if (result == null) {
            // Erreur réseau ou HTTP non gérée
            callback.onAddToCartError("Erreur de connexion au serveur");
        } else {
            // Succès : extraire le rentalId depuis le JSON de réponse
            // Format attendu : { "message": "...", "rental": { "rentalId": 42, ... } }
            try {
                JSONObject json = new JSONObject(result);
                JSONObject rental = json.getJSONObject("rental");
                int rentalId = rental.getInt("rentalId");
                callback.onAddToCartSuccess(rentalId);
            } catch (Exception e) {
                Log.e(TAG, "Erreur parsing rentalId", e);
                callback.onAddToCartError("Erreur lors du traitement de la réponse");
            }
        }
    }

    // ─── Utilitaires réseau ────────────────────────────────────────

    /**
     * Effectue l'appel HTTP POST vers l'URL donnée avec le body JSON.
     * Configure les headers (Content-Type, Accept, Authorization), envoie le corps,
     * lit et retourne la réponse brute.
     * Retourne "UNAVAILABLE" si le serveur répond 404 (aucun exemplaire disponible),
     * null pour tout autre code non-200.
     *
     * @param url  L'URL de l'endpoint REST à appeler
     * @param body Le JSONObject à envoyer dans le corps de la requête
     * @return Le corps de la réponse en String, "UNAVAILABLE" (404), ou null en cas d'erreur
     * @throws Exception En cas d'erreur réseau (IOException, etc.)
     */
    private String appelerReseauPost(URL url, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getJwt());
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return lireReponse(connection);
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.e(TAG, "Film indisponible (404)");
                return "UNAVAILABLE";
            } else if (responseCode == 401) {
                Log.e(TAG, "Non autorisé (401)");
                return null;
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null;
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Lit le corps de la réponse HTTP ligne par ligne et le retourne en String.
     * Utilisé après une réponse HTTP_OK pour extraire le JSON retourné par l'API.
     *
     * @param connection La connexion HTTP ouverte dont on lit le flux d'entrée
     * @return Le contenu de la réponse sous forme de String (JSON brut)
     * @throws Exception En cas d'erreur de lecture du flux
     */
    private String lireReponse(HttpURLConnection connection) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

    /**
     * Récupère le token JWT depuis res/values/strings.xml (clé : api_jwt_token).
     * Utilisé dans le header Authorization de chaque requête HTTP.
     *
     * @return Le token JWT sous forme de String
     */
    private String getJwt() {
        return context.getResources().getString(R.string.api_jwt_token);
    }
}
