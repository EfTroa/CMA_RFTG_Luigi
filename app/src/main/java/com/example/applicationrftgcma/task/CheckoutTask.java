package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour valider le panier via l'API REST POST /cart/checkout.
 * Côté serveur, cette opération change le status_id de tous les rentals du client
 * de 2 (dans le panier) à 3 (location active / confirmée).
 *
 * Qui l'appelle : PanierActivity.validerPanier()
 * Ce qu'elle retourne : un String contenant le JSON brut {"itemsCount": N} ou null si erreur
 *
 * Flux d'exécution :
 *   PanierActivity.validerPanier()
 *   → construit le JSONObject body { "customerId": N }
 *   → new CheckoutTask(activity, callback, body).execute()
 *   → doInBackground() : construit l'URL, appelle appelerReseauPost(), retourne le JSON brut
 *   → onPostExecute()  : parse itemsCount depuis le JSON, appelle le callback sur le thread UI
 *
 * La réponse JSON de l'API contient : { "itemsCount": N }
 * où N est le nombre de films validés — affiché dans le message de confirmation.
 *
 * AsyncTask<Void, Void, String> :
 *   - Void   → execute() ne reçoit rien (le body passe par le constructeur)
 *   - Void   → pas de progression intermédiaire publiée
 *   - String → doInBackground retourne le JSON brut, reçu par onPostExecute
 */

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.activity.PanierActivity;
import com.example.applicationrftgcma.manager.UrlManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class CheckoutTask extends AsyncTask<Void, Void, String> {

    // Tag pour filtrer les logs de cette tâche dans Logcat
    private static final String TAG = "CheckoutTask";

    // Contexte Android — nécessaire pour lire le JWT dans strings.xml via getJwt()
    private Context context;

    // Corps JSON formé dans PanierActivity avant instanciation de cette tâche
    // Contient : { "customerId": N }
    private JSONObject body;

    /**
     * Interface de callback permettant à PanierActivity de réagir au résultat.
     * Séparation des responsabilités : la tâche gère le réseau, l'activité gère l'UI.
     */
    public interface CheckoutCallback {
        /**
         * Appelé si le checkout a réussi (200 OK).
         * @param itemsCount Le nombre de films validés retourné par l'API
         */
        void onCheckoutSuccess(int itemsCount);

        /**
         * Appelé en cas d'erreur réseau ou de réponse HTTP inattendue.
         * @param errorMessage Le message d'erreur à afficher à l'utilisateur
         */
        void onCheckoutError(String errorMessage);
    }

    // Callback fourni par PanierActivity — appelé dans onPostExecute() pour mettre à jour l'UI
    private CheckoutCallback callback;

    /**
     * Constructeur — reçoit le contexte, le callback et le body déjà formé.
     * Toutes les données nécessaires à l'appel API arrivent ici,
     * donc execute() n'a pas besoin de paramètres (Void).
     *
     * @param activity L'activité PanierActivity (utilisée comme Context pour getJwt())
     * @param callback Listener à notifier une fois la requête terminée
     * @param body     Le JSON formé dans PanierActivity : { "customerId": N }
     */
    public CheckoutTask(PanierActivity activity, JSONObject body, CheckoutCallback callback) {
        this.context = activity; // PanierActivity hérite de Context via AppCompatActivity
        this.body = body;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Construit l'URL et délègue l'appel réseau POST à appelerReseauPost().
     * Le body est déjà prêt (attribut de classe) — pas besoin de le reconstruire ici.
     *
     * @param voids Aucun paramètre — toutes les données arrivent via le constructeur
     * @return La réponse JSON brute (ex: {"itemsCount":3}), ou null en cas d'erreur
     */
    @Override
    protected String doInBackground(Void... voids) {
        try {
            // Construire l'URL de l'endpoint de validation du panier
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/checkout");
            Log.d(TAG, "URL appelée: " + url.toString());

            // Déléguer l'appel réseau POST à la méthode utilitaire — body déjà prêt
            return appelerReseauPost(url, body);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du checkout", e);
            return null;
        }
    }

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Si result != null → parse le JSON pour extraire itemsCount → notifie le callback.
     * Si result == null → erreur réseau ou HTTP → notifie le callback avec un message d'erreur.
     *
     * @param result La réponse JSON brute retournée par doInBackground(), ou null si échec
     */
    @Override
    protected void onPostExecute(String result) {
        if (callback == null) return;

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
            // doInBackground() a retourné null → erreur réseau ou code HTTP inattendu
            callback.onCheckoutError("Erreur de connexion au serveur");
        }
    }

    // ─── Utilitaires réseau ────────────────────────────────────────

    /**
     * Effectue l'appel HTTP POST vers l'URL donnée avec le body JSON.
     * Configure les headers (Content-Type, Accept, Authorization), envoie le corps,
     * lit et retourne la réponse brute.
     * Retourne null si le serveur répond 401 ou tout autre code non-200.
     *
     * @param url  L'URL de l'endpoint REST à appeler
     * @param body Le JSONObject à envoyer dans le corps de la requête
     * @return Le corps de la réponse en String, ou null en cas d'erreur HTTP
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

            // Écriture du corps JSON dans le flux de la requête en UTF-8
            OutputStream os = connection.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == 401) {
                // Token expiré ou invalide — l'Activity gèrera la redirection via onError
                Log.e(TAG, "Non autorisé (401)");
                return null;
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire et retourner le JSON brut de la réponse
                return lireReponse(connection);
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null;
            }
        } finally {
            // Toujours libérer la connexion, même en cas d'exception
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
