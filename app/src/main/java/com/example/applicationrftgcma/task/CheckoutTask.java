package com.example.applicationrftgcma.task;

import android.os.AsyncTask;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.activity.PanierActivity;
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
 * Tâche asynchrone pour valider le panier via l'API REST POST /cart/checkout.
 * Côté serveur, cette opération change le status_id de tous les rentals du client
 * de 2 (dans le panier) à 3 (location active / confirmée).
 *
 * Flux d'exécution :
 *   PanierActivity.validerPanier()
 *   → new CheckoutTask(activity, callback).execute(customerId)
 *   → doInBackground() : appel HTTP POST sur le thread réseau
 *   → onPostExecute() : retour sur le thread UI, appel du callback
 *
 * La réponse JSON de l'API contient : { "itemsCount": N }
 * où N est le nombre de films validés — affiché dans le message de confirmation.
 */
@SuppressWarnings("deprecation")
public class CheckoutTask extends AsyncTask<Integer, Void, String> {

    // Tag pour les logs Logcat
    private static final String TAG = "CheckoutTask";

    // Référence à PanierActivity — conservée pour accéder au contexte (resources, etc.)
    private PanierActivity activity;

    // Contexte Android pour lire les ressources (token JWT dans strings.xml)
    private android.content.Context context;

    /**
     * Interface de callback permettant à PanierActivity de réagir au résultat.
     * Séparation des responsabilités : la tâche gère le réseau, l'activité gère l'UI.
     */
    public interface CheckoutCallback {
        /**
         * Appelé si le checkout a réussi.
         * @param itemsCount Le nombre de films validés retourné par l'API
         */
        void onCheckoutSuccess(int itemsCount);
        /** Appelé en cas d'erreur réseau ou de parsing */
        void onCheckoutError(String errorMessage);
    }

    // Référence au callback fourni par PanierActivity — appelé dans onPostExecute()
    private CheckoutCallback callback;

    /**
     * Constructeur — reçoit l'activité (pour le contexte) et le callback de résultat.
     *
     * @param activity L'activité PanierActivity qui lance la validation
     * @param callback Listener à notifier une fois la requête terminée
     */
    public CheckoutTask(PanierActivity activity, CheckoutCallback callback) {
        this.activity = activity;
        this.context = activity; // PanierActivity hérite de Context via AppCompatActivity
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Effectue l'appel HTTP POST vers /cart/checkout avec le customerId en JSON.
     *
     * @param params params[0] = customerId du client dont on valide le panier
     * @return La réponse JSON brute (ex: {"itemsCount":3}), ou null en cas d'erreur
     */
    @Override
    protected String doInBackground(Integer... params) {
        Integer customerId = params[0]; // Récupéré depuis execute(customerId) dans PanierActivity

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
            connection.setDoOutput(true); // Indique qu'on envoie un corps (nécessaire pour POST)

            // Construire le corps JSON : { "customerId": X }
            JSONObject requestBody = new JSONObject();
            requestBody.put("customerId", customerId);

            Log.d(TAG, "JSON envoyé: " + requestBody.toString());

            // Envoyer le corps de la requête en UTF-8
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse JSON (contient le nombre d'items validés)
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Log.d(TAG, "Réponse reçue: " + response.toString());
                return response.toString(); // Retourné tel quel pour parsing dans onPostExecute
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null; // null signale une erreur à onPostExecute
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du checkout", e);
            return null;
        }
    }

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Parse la réponse JSON pour extraire itemsCount, puis notifie le callback.
     *
     * @param result La réponse JSON brute, ou null si doInBackground() a échoué
     */
    @Override
    protected void onPostExecute(String result) {
        if (callback != null) {
            if (result != null) {
                try {
                    // Parser la réponse JSON pour récupérer le nombre de films validés
                    JSONObject jsonResponse = new JSONObject(result);
                    int itemsCount = jsonResponse.getInt("itemsCount");
                    callback.onCheckoutSuccess(itemsCount);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur parsing JSON", e);
                    callback.onCheckoutError("Erreur lors du traitement de la réponse");
                }
            } else {
                // doInBackground() a retourné null → erreur réseau ou HTTP
                callback.onCheckoutError("Erreur de connexion au serveur");
            }
        }
    }
}