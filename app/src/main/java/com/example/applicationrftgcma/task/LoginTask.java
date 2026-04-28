package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour authentifier un utilisateur via l'API REST POST /customers/verify.
 * L'API attend un email et un mot de passe hashé en MD5, et retourne le customerId si valide.
 *
 * Qui l'appelle : MainActivity.ouvrirPageListefilms()
 * Ce qu'elle retourne : un Integer (customerId > 0 si succès, -1 si identifiants incorrects)
 *
 * Flux d'exécution :
 *   MainActivity.ouvrirPageListefilms()
 *   → construit le JSONObject body { "email": "...", "password": "hash_md5" }
 *   → new LoginTask(context, body, callback).execute()
 *   → doInBackground() : appel HTTP POST via appelerReseauPost(), retourne le JSON brut
 *   → onPostExecute()  : parse le customerId, appelle le callback sur le thread UI
 *
 * AsyncTask<Void, Void, Integer> :
 *   - Void    → execute() ne reçoit rien (toutes les données passent par le constructeur)
 *   - Void    → pas de progression intermédiaire publiée
 *   - Integer → doInBackground retourne le customerId (ou -1), reçu par onPostExecute
 *
 * Règle métier : si l'API retourne customerId > 0, la connexion est réussie.
 * Un customerId = -1 signifie identifiants incorrects.
 *
 * Note : le corps JSON (body) est construit dans MainActivity avant instanciation de cette tâche.
 * Le hachage MD5 du mot de passe est également effectué dans MainActivity.
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
public class LoginTask extends AsyncTask<Void, Void, Integer> {

    // Tag pour les logs Logcat — permet de filtrer les messages de cette tâche
    private static final String TAG = "LoginTask";

    // Contexte Android — nécessaire pour lire le JWT dans strings.xml via getJwt()
    private Context context;

    // Corps JSON déjà formé dans MainActivity : { "email": "...", "password": "hash_md5..." }
    // Le hachage MD5 est réalisé dans MainActivity avant la création de cette tâche
    private JSONObject body;

    // Callback pour notifier MainActivity du résultat (succès ou échec)
    private LoginCallback callback;

    /**
     * Interface de callback permettant à MainActivity de réagir au résultat de la connexion.
     * Séparation des responsabilités : la tâche gère le réseau, l'activité gère l'UI.
     */
    public interface LoginCallback {
        /**
         * Appelé si la connexion a réussi (customerId > 0).
         * @param customerId L'identifiant du client retourné par l'API
         */
        void onLoginSuccess(Integer customerId);

        /**
         * Appelé si la connexion a échoué (identifiants incorrects ou erreur réseau).
         * @param errorMessage Le message d'erreur à afficher à l'utilisateur
         */
        void onLoginError(String errorMessage);
    }

    /**
     * Constructeur — reçoit le contexte, le corps JSON déjà formé et le callback de résultat.
     * Le body doit déjà contenir le mot de passe hashé en MD5 (effectué dans MainActivity).
     *
     * @param context  Contexte Android pour accéder aux ressources (JWT)
     * @param body     JSONObject { "email": "...", "password": "hash_md5..." } formé dans MainActivity
     * @param callback Listener à notifier une fois la requête terminée
     */
    public LoginTask(Context context, JSONObject body, LoginCallback callback) {
        this.context = context;
        this.body = body;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Construit l'URL et délègue l'appel HTTP POST à appelerReseauPost().
     * Le body est déjà prêt (attribut de classe) — pas besoin de le reconstruire ici.
     *
     * @param voids Aucun paramètre — toutes les données arrivent via le constructeur
     * @return Le customerId si connexion réussie (> 0), ou -1 en cas d'échec / null reçu
     */
    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            // Construire l'URL de l'endpoint de vérification
            URL url = new URL(UrlManager.getURLConnexion() + "/customers/verify");
            Log.d(TAG, "URL appelée: " + url.toString());

            // Déléguer l'appel réseau à appelerReseauPost() — retourne le JSON brut ou null
            String responseJson = appelerReseauPost(url, body);

            if (responseJson == null) {
                // Erreur HTTP (401, autre code non-200) ou problème réseau
                Log.e(TAG, "Login failed: réponse nulle de appelerReseauPost");
                return -1;
            }

            // Parser la réponse JSON pour extraire le customerId
            JSONObject jsonResponse = new JSONObject(responseJson);
            int customerId = jsonResponse.getInt("customerId");
            Log.d(TAG, "Response customerId: " + customerId);

            // Vérifier si le customerId est valide (> 0 = succès, -1 = identifiants incorrects)
            if (customerId > 0) {
                Log.d(TAG, "Login successful. CustomerId: " + customerId);
                return customerId;
            } else {
                Log.e(TAG, "Login failed: identifiants incorrects (customerId = " + customerId + ")");
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du login", e);
            return -1;
        }
    }

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Notifie le callback avec le résultat de la connexion.
     * Si customerId > 0 → succès, sinon → erreur avec message explicite.
     *
     * @param customerId Le customerId retourné par doInBackground() (> 0 si succès, -1 si échec)
     */
    @Override
    protected void onPostExecute(Integer customerId) {
        if (callback != null) {
            if (customerId != null && customerId > 0) {
                // Connexion réussie → transmettre le customerId à MainActivity
                callback.onLoginSuccess(customerId);
            } else {
                // Connexion échouée → informer l'utilisateur
                callback.onLoginError("Échec de connexion. Vérifiez vos identifiants.");
            }
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
                // Token expiré ou invalide
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
