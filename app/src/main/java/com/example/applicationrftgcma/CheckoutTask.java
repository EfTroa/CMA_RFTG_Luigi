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
 * TÂCHE ASYNCHRONE : CheckoutTask
 *
 * Valide le panier côté serveur en finalisant les locations.
 * Endpoint : POST /cart/checkout
 * Corps JSON : { "customerId": 42 }
 * Réponse JSON : { "itemsCount": 3 }
 *
 * Que fait l'API lors du checkout ?
 * Elle change le status_id de tous les rentals du client de 2 (dans le panier)
 * à 3 (location active). Le film est alors officiellement loué.
 *
 * Après succès :
 * - Le panier local SQLite est vidé (via PanierManager dans PanierActivity)
 * - Un message de confirmation avec le nombre de films est affiché
 *
 * PARAMÈTRES GÉNÉRIQUES AsyncTask<Params, Progress, Result> :
 *   - Params : Integer = le customerId passé à execute()
 *   - Progress : Void = pas de notification de progression
 *   - Result : String = le JSON de réponse brut (ou null en cas d'erreur)
 *
 * @SuppressWarnings("deprecation") : supprime l'avertissement de dépréciation d'AsyncTask
 */
@SuppressWarnings("deprecation")
public class CheckoutTask extends AsyncTask<Integer, Void, String> {

    // Tag pour le Logcat
    private static final String TAG = "CheckoutTask";

    // Référence à l'activité (pour accéder au contexte Android)
    private PanierActivity activity;

    // Contexte Android pour lire les ressources (token JWT)
    private android.content.Context context;

    /**
     * INTERFACE : CheckoutCallback
     *
     * Contrat pour recevoir le résultat de la validation du panier.
     * Implémentée par PanierActivity.
     */
    public interface CheckoutCallback {
        /**
         * Appelée si la validation a réussi.
         * @param itemsCount Le nombre de films dont la location a été validée
         */
        void onCheckoutSuccess(int itemsCount);

        /**
         * Appelée si la validation a échoué.
         * @param errorMessage Message d'erreur pour l'utilisateur
         */
        void onCheckoutError(String errorMessage);
    }

    // Le callback pour notifier PanierActivity du résultat
    private CheckoutCallback callback;

    /**
     * Constructeur : initialise la tâche avec l'activité et le callback.
     *
     * @param activity L'activité parente (PanierActivity)
     * @param callback Le gestionnaire de résultat
     */
    public CheckoutTask(PanierActivity activity, CheckoutCallback callback) {
        this.activity = activity;
        // PanierActivity hérite de AppCompatActivity qui hérite de Context
        // On peut donc l'utiliser directement comme contexte
        this.context = activity; // PanierActivity est un Context
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread en arrière-plan.
     * Effectue la requête HTTP POST vers /cart/checkout.
     *
     * @param params Le customerId passé via execute(customerId)
     * @return Le JSON de réponse brut, ou null en cas d'erreur
     */
    @Override
    protected String doInBackground(Integer... params) {
        // Récupérer le customerId passé en paramètre à execute()
        Integer customerId = params[0];

        try {
            // URL de l'API
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/checkout");
            Log.d(TAG, "URL appelée: " + url.toString());

            // Configurer la connexion HTTP POST
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // JWT token pour l'autorisation (lu depuis strings.xml)
            String jwt = context.getResources().getString(R.string.api_jwt_token);
            connection.setRequestProperty("Authorization", "Bearer " + jwt);
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent"));
            connection.setDoOutput(true); // Activer l'envoi du corps

            // Créer le JSON body avec seulement le customerId
            // L'API récupère côté serveur tous les rentals status_id=2 de ce client
            JSONObject requestBody = new JSONObject();
            requestBody.put("customerId", customerId);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

            // Envoyer le corps JSON
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            // Lire le code de réponse
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // HTTP 200 : checkout réussi, lire la réponse JSON
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Log.d(TAG, "Réponse reçue: " + response.toString());
                // Retourner le JSON brut (sera parsé dans onPostExecute)
                return response.toString();
            } else {
                // Erreur HTTP (ex: 400 Bad Request, 500 Internal Server Error)
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null;  // null signale l'erreur à onPostExecute
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du checkout", e);
            return null;
        }
    }

    /**
     * Exécuté sur le thread UI après la fin de doInBackground().
     * Parse le JSON de réponse et appelle le callback approprié.
     *
     * @param result Le JSON de réponse brut, ou null si erreur
     */
    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            if (result != null) {
                try {
                    // Parser la réponse JSON pour extraire le nombre de films validés
                    // Exemple de réponse : {"itemsCount": 3}
                    JSONObject jsonResponse = new JSONObject(result);
                    int itemsCount = jsonResponse.getInt("itemsCount");
                    // Notifier le succès avec le nombre de films loués
                    callback.onCheckoutSuccess(itemsCount);
                } catch (Exception e) {
                    // Erreur de parsing JSON (format de réponse inattendu)
                    Log.e(TAG, "Erreur parsing JSON", e);
                    callback.onCheckoutError("Erreur lors du traitement de la réponse");
                }
            } else {
                // result == null = erreur réseau ou HTTP dans doInBackground
                callback.onCheckoutError("Erreur de connexion au serveur");
            }
        }
    }
}
