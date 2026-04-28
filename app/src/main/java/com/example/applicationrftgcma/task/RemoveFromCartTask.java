package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour supprimer un item du panier via l'API REST DELETE /cart/{rentalId}.
 * Côté serveur, cela supprime le rental — le film redevient disponible pour d'autres clients.
 *
 * Qui l'appelle : PanierActivity.afficherDialogueConfirmationSupprimer()
 * Ce qu'elle retourne : un Boolean (true si 200 OK, false si erreur réseau ou HTTP)
 *
 * Flux d'exécution :
 *   PanierActivity.afficherDialogueConfirmationSupprimer()
 *   → new RemoveFromCartTask(context, rentalId, callback).execute()
 *   → doInBackground() : construit l'URL, appel HTTP DELETE sur le thread réseau
 *   → onPostExecute()  : retour sur le thread UI, appel du callback
 *   → onRemoveSuccess() : suppression dans SQLite + mise à jour de l'affichage
 *
 * Différence avec les tâches POST : pas de body, le rentalId est intégré dans l'URL.
 * L'appel réseau est délégué à appelerReseauDelete(url) — sans body en paramètre.
 *
 * AsyncTask<Void, Void, Boolean> :
 *   - Void    → execute() ne reçoit rien (rentalId passe par le constructeur)
 *   - Void    → pas de progression intermédiaire publiée
 *   - Boolean → true si 200 OK, false si erreur réseau ou code HTTP inattendu
 */

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.UrlManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class RemoveFromCartTask extends AsyncTask<Void, Void, Boolean> {

    // Tag pour les logs Logcat — permet de filtrer les messages de cette tâche
    private static final String TAG = "RemoveFromCartTask";

    // Contexte Android — nécessaire pour lire le JWT dans strings.xml via getJwt()
    private Context context;

    // ID du rental à supprimer — intégré dans l'URL (pas dans le body)
    // Récupéré depuis SQLite au moment de l'appel (stocké lors de l'ajout au panier)
    private int rentalId;

    /**
     * Interface de callback permettant à PanierActivity de réagir au résultat.
     * Séparation des responsabilités : la tâche gère le réseau, l'activité gère l'UI.
     */
    public interface RemoveFromCartCallback {
        /** Appelé si la suppression côté serveur a réussi (200 OK). */
        void onRemoveSuccess();

        /**
         * Appelé en cas d'erreur réseau ou de réponse HTTP inattendue.
         * @param errorMessage Le message d'erreur à afficher à l'utilisateur
         */
        void onRemoveError(String errorMessage);
    }

    // Référence au callback fourni par l'appelant — appelé dans onPostExecute()
    private RemoveFromCartCallback callback;

    /**
     * Constructeur — reçoit toutes les données nécessaires à l'appel API.
     * Le rentalId est passé directement (stocké dans SQLite depuis l'ajout au panier).
     *
     * @param context  Contexte Android pour lire le JWT dans strings.xml
     * @param rentalId L'ID du rental à supprimer (intégré dans l'URL, pas dans le body)
     * @param callback Listener à notifier une fois la requête terminée
     */
    public RemoveFromCartTask(Context context, int rentalId, RemoveFromCartCallback callback) {
        this.context = context;
        this.rentalId = rentalId;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Construit l'URL avec le rentalId en segment de chemin et délègue à appelerReseauDelete().
     *
     * @param voids Aucun paramètre — toutes les données arrivent via le constructeur
     * @return true si le serveur répond 200 OK, false en cas d'erreur réseau ou HTTP
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/" + rentalId);
            Log.d(TAG, "URL appelée: " + url.toString());

            return appelerReseauDelete(url);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la suppression du rental", e);
            return false;
        }
    }

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Notifie le callback selon le résultat :
     *   true  → suppression réussie → onRemoveSuccess()
     *   false → erreur réseau ou HTTP → onRemoveError(message)
     *
     * @param success true si la suppression côté serveur a réussi, false sinon
     */
    @Override
    protected void onPostExecute(Boolean success) {
        if (callback == null) return;

        if (success) {
            // Serveur a confirmé la suppression → notifier l'activité pour mettre à jour SQLite et l'UI
            callback.onRemoveSuccess();
        } else {
            // Erreur réseau ou réponse HTTP inattendue → l'activité n'efface pas SQLite
            callback.onRemoveError("Erreur lors de la suppression du film");
        }
    }

    // ─── Utilitaires réseau ────────────────────────────────────────

    /**
     * Effectue l'appel HTTP DELETE vers l'URL donnée.
     * Configure les headers (Content-Type, Accept, Authorization) et retourne true si 200 OK.
     * Pas de body — la ressource est identifiée uniquement par l'URL.
     *
     * @param url L'URL de l'endpoint REST à appeler (avec le rentalId en segment de chemin)
     * @return true si le serveur répond 200 OK, false pour tout autre code ou erreur réseau
     * @throws Exception En cas d'erreur réseau (IOException, etc.)
     */
    private Boolean appelerReseauDelete(URL url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getJwt());

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            return responseCode == HttpURLConnection.HTTP_OK;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Lit le corps de la réponse HTTP ligne par ligne et le retourne en String.
     * Disponible si une lecture du corps de réponse DELETE est nécessaire dans le futur.
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
