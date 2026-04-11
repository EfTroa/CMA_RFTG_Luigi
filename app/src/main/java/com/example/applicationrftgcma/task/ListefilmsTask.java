package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour récupérer la liste de tous les films via l'API REST GET /films.
 * La réponse JSON (tableau de films) est transmise à ListefilmsActivity pour affichage.
 *
 * Flux d'exécution :
 *   ListefilmsActivity.onCreate()
 *   → new ListefilmsTask(this).execute(url)
 *   → onPreExecute()    : affiche la ProgressBar (thread UI)
 *   → doInBackground()  : appel HTTP GET sur le thread réseau, retourne le JSON brut
 *   → onPostExecute()   : transmet le JSON à ListefilmsActivity (thread UI)
 *
 * Gestion du 401 Unauthorized :
 *   Si le serveur répond 401 (token expiré ou invalide), la session est effacée
 *   et l'activité est fermée — l'utilisateur revient à MainActivity pour se reconnecter.
 *
 * Choix d'architecture (possibilité-3) :
 *   La tâche ne parse pas le JSON elle-même — elle délègue à ListefilmsActivity
 *   via mettreAJourActivityApresAppelRest(), qui s'occupe du parsing et de l'affichage.
 */

import android.os.AsyncTask;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.activity.ListefilmsActivity;
import com.example.applicationrftgcma.manager.TokenManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ListefilmsTask extends AsyncTask<URL, Integer, String> {

    // Référence à l'activité — volatile pour garantir la visibilité entre les threads
    // (doInBackground tourne sur un thread secondaire, onPreExecute/onPostExecute sur l'UI thread)
    private volatile ListefilmsActivity screen;

    /**
     * Constructeur — reçoit la référence à l'activité pour afficher/cacher la ProgressBar
     * et lui transmettre le résultat après l'appel réseau.
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
     * Délègue l'appel HTTP à appelerServiceRestHttp() et retourne le JSON brut.
     *
     * @param urls urls[0] = l'URL complète de l'API (ex: http://10.0.2.2:8180/films)
     * @return Le JSON de la liste des films sous forme de String, ou "" en cas d'erreur
     */
    @Override
    protected String doInBackground(URL... urls) {
        String resultat;
        URL urlAAppeler = urls[0];
        resultat = appelerServiceRestHttp(urlAAppeler);
        return resultat;
    }

    /**
     * Exécuté sur le thread UI APRÈS doInBackground().
     * Transmet le JSON reçu à ListefilmsActivity via le callback mettreAJourActivityApresAppelRest().
     *
     * Choix d'architecture (possibilité-3) :
     *   On appelle une méthode de l'activité plutôt que de modifier l'UI directement ici,
     *   ce qui garde la logique d'affichage centralisée dans l'activité.
     *
     * @param resultat Le JSON retourné par doInBackground() (liste des films)
     */
    @Override
    protected void onPostExecute(String resultat) {
        System.out.println(">>>onPostExecute / resultat=" + resultat);

        // IMPORTANT : plusieurs possibilités pour traiter les données reçues :
        // possibilité-1 : mettre à jour directement la liste dans l'activité principale
        // possibilité-2 : passer les données à l'activité principale et laisser cette dernière mettre à jour la liste
        // possibilité-3 : appeler une méthode de l'activité principale pour qu'elle mette à jour la liste ou les composants

        // Ici on choisit la possibilité-3 : appeler la méthode de mise à jour
        screen.mettreAJourActivityApresAppelRest(resultat);

        // Cacher la ProgressBar maintenant que le chargement est terminé
        screen.showProgressBar(false);
    }

    /**
     * Effectue l'appel HTTP GET vers l'URL de l'API et retourne la réponse en String.
     * Gère également le cas 401 (session expirée) en nettoyant le token et en fermant l'activité.
     *
     * @param urlAAppeler L'URL complète de l'endpoint REST à appeler
     * @return Le corps de la réponse HTTP sous forme de String, ou "" en cas d'erreur
     */
    private String appelerServiceRestHttp(URL urlAAppeler) {
        HttpURLConnection urlConnection = null;
        int responseCode = -1;
        String sResultatAppel = "";
        try {
            Log.d("mydebug", "Appel API films: " + urlAAppeler.toString());

            // Ouvrir la connexion et configurer les headers de la requête GET
            urlConnection = (HttpURLConnection) urlAAppeler.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", System.getProperty("http.agent"));
            // JWT token pour l'autorisation (lu depuis strings.xml)
            String jwt = screen.getResources().getString(R.string.api_jwt_token);
            urlConnection.setRequestProperty("Authorization", "Bearer " + jwt);

            Log.d("mydebug", "Connexion établie, lecture response code...");
            responseCode = urlConnection.getResponseCode();
            Log.d("mydebug", "Response Code: " + responseCode);

            // Gestion du 401 : session expirée ou token invalide
            // On doit revenir sur le thread UI pour modifier l'interface → runOnUiThread()
            if (responseCode == 401) {
                Log.e("mydebug", "Non autorisé. Redirection vers login.");
                screen.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Effacer la session et fermer l'activité → retour à MainActivity
                        TokenManager.getInstance(screen).clearToken();
                        screen.finish();
                    }
                });
                return "";
            }

            // Lire la réponse caractère par caractère depuis le flux d'entrée
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            int codeCaractere = -1;
            while ((codeCaractere = in.read()) != -1) {
                sResultatAppel = sResultatAppel + (char) codeCaractere;
            }
            in.close();

            Log.d("mydebug", "Films récupérés, taille: " + sResultatAppel.length());
        } catch (IOException ioe) {
            Log.e("mydebug", ">>>Pour appelerServiceRestHttp - IOException ioe =" + ioe.toString());
        } catch (Exception e) {
            Log.e("mydebug",">>>Pour appelerServiceRestHttp - Exception="+e.toString());
        } finally {
            // Toujours libérer la connexion HTTP dans le bloc finally
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return sResultatAppel;
    }

}
