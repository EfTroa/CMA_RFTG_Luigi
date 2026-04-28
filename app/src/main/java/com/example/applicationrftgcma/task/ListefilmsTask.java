package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour récupérer la liste de tous les films via l'API REST GET /films.
 * La réponse JSON (tableau de films) est transmise à ListefilmsActivity pour affichage.
 *
 * Qui l'appelle : ListefilmsActivity.onCreate()
 * Ce qu'elle retourne : un String contenant le JSON brut des films (ou null en cas d'erreur)
 *
 * Flux d'exécution :
 *   ListefilmsActivity.onCreate()
 *   → new ListefilmsTask(this).execute()
 *   → onPreExecute()   : affiche la ProgressBar (thread UI)
 *   → doInBackground() : construit l'URL, appelle appelerReseauGet(), retourne le JSON brut
 *   → onPostExecute()  : transmet le JSON à ListefilmsActivity via le callback (thread UI)
 *
 * Gestion du 401 Unauthorized :
 *   Si le serveur répond 401 (token expiré ou invalide), appelerReseauGet() retourne null
 *   et ListefilmsActivity gère la redirection vers la connexion dans mettreAJourActivityApresAppelRest().
 *
 * Choix d'architecture (possibilité-3) :
 *   La tâche ne parse pas le JSON elle-même — elle délègue à ListefilmsActivity
 *   via mettreAJourActivityApresAppelRest(), qui s'occupe du parsing et de l'affichage.
 *
 * AsyncTask<Void, Void, String> :
 *   - Void   → execute() ne reçoit rien (l'URL est construite directement dans doInBackground)
 *   - Void   → pas de progression intermédiaire publiée
 *   - String → doInBackground retourne le JSON brut, reçu par onPostExecute
 */

import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.activity.ListefilmsActivity;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.manager.UrlManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class ListefilmsTask extends AsyncTask<Void, Void, String> {

    // Tag pour les logs Logcat — permet de filtrer les messages de cette tâche
    private static final String TAG = "ListefilmsTask";

    // Référence à l'activité — volatile pour garantir la visibilité entre les threads
    // (doInBackground tourne sur un thread secondaire, onPreExecute/onPostExecute sur l'UI thread)
    private volatile ListefilmsActivity screen;

    /**
     * Constructeur — reçoit la référence à l'activité pour afficher/cacher la ProgressBar,
     * accéder au contexte (JWT, TokenManager) et lui transmettre le résultat.
     *
     * @param s L'activité ListefilmsActivity qui a lancé cette tâche
     */
    public ListefilmsTask(ListefilmsActivity s) {
        this.screen = s;
    }

    /**
     * Exécuté sur le thread UI AVANT doInBackground().
     * Affiche l'overlay de chargement pour indiquer que l'appel réseau est en cours.
     */
    @Override
    protected void onPreExecute() {
        screen.showProgressBar(true);
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Construit l'URL de l'endpoint GET /films et délègue l'appel à appelerReseauGet().
     *
     * @param voids Aucun paramètre — l'URL est construite ici depuis UrlManager
     * @return Le JSON de la liste des films sous forme de String, ou null en cas d'erreur
     */
    @Override
    protected String doInBackground(Void... voids) {
        try {
            // Construire l'URL de l'endpoint REST de la liste des films
            URL url = new URL(UrlManager.getURLConnexion() + "/films");
            Log.d(TAG, "URL appelée: " + url.toString());

            // Déléguer l'appel réseau GET à la méthode utilitaire
            return appelerReseauGet(url);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des films", e);
            return null;
        }
    }

    /**
     * Exécuté sur le thread UI APRÈS doInBackground().
     * Transmet le JSON reçu à ListefilmsActivity via le callback mettreAJourActivityApresAppelRest().
     *
     * Choix d'architecture (possibilité-3) :
     *   On appelle une méthode de l'activité plutôt que de modifier l'UI directement ici,
     *   ce qui garde la logique d'affichage centralisée dans l'activité.
     *
     * @param resultat Le JSON retourné par doInBackground() (liste des films, ou null si erreur)
     */
    @Override
    protected void onPostExecute(String resultat) {
        Log.d(TAG, "onPostExecute / taille résultat: " + (resultat != null ? resultat.length() : "null"));

        // Notifier l'activité avec le JSON brut — elle se charge du parsing et de l'affichage
        screen.mettreAJourActivityApresAppelRest(resultat);

        // Cacher la ProgressBar maintenant que le chargement est terminé
        screen.showProgressBar(false);
    }

    // ─── Utilitaires réseau ────────────────────────────────────────

    /**
     * Effectue l'appel HTTP GET vers l'URL donnée.
     * Configure les headers (Content-Type, Accept, Authorization), lit et retourne la réponse.
     * Gère le cas 401 (token expiré) : efface la session et ferme l'activité.
     * Retourne null si le serveur répond 401 ou tout autre code non-200.
     *
     * @param url L'URL de l'endpoint REST à appeler
     * @return Le corps de la réponse en String (JSON brut), ou null en cas d'erreur HTTP
     * @throws Exception En cas d'erreur réseau (IOException, etc.)
     */
    private String appelerReseauGet(URL url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getJwt());

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == 401) {
                // Token expiré ou invalide — effacer la session et fermer l'activité
                Log.e(TAG, "Non autorisé (401)");
                screen.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Effacer le token et fermer l'activité → retour à MainActivity
                        TokenManager.getInstance(screen).clearToken();
                        screen.finish();
                    }
                });
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
     * Utilise screen comme contexte car ListefilmsTask ne reçoit pas de Context séparé.
     *
     * @return Le token JWT sous forme de String
     */
    private String getJwt() {
        return screen.getResources().getString(R.string.api_jwt_token);
    }
}
