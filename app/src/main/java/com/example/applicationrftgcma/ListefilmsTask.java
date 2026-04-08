package com.example.applicationrftgcma;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * TÂCHE ASYNCHRONE : ListefilmsTask
 *
 * Récupère la liste des films depuis l'API REST via une requête HTTP GET.
 * Endpoint : GET /films
 * Headers : Authorization: Bearer <token>, Content-Type: application/json
 * Réponse : tableau JSON de films [{"filmId": 1, "title": "..."}, ...]
 *
 * PARAMÈTRES GÉNÉRIQUES AsyncTask<Params, Progress, Result> :
 *   - Params : URL = l'URL à appeler est passée à execute()
 *   - Progress : Integer = pourrait servir pour indiquer le % de chargement
 *   - Result : String = le JSON brut reçu de l'API
 *
 * Relation avec ListefilmsActivity :
 * La tâche garde une référence "volatile" à l'activité.
 * volatile signifie que la variable peut être modifiée par différents threads
 * et qu'Android doit toujours lire sa valeur la plus récente (pas de cache CPU).
 */
public class ListefilmsTask extends AsyncTask<URL, Integer, String> {

    // Référence à l'activité (volatile pour la visibilité entre threads)
    // Permet d'appeler showProgressBar() et mettreAJourActivityApresAppelRest()
    private volatile ListefilmsActivity screen;

    /**
     * Constructeur : reçoit la référence à l'activité pour pouvoir rappeler ses méthodes.
     *
     * @param s La ListefilmsActivity qui a lancé cette tâche
     */
    public ListefilmsTask(ListefilmsActivity s) {
        this.screen = s;
    }

    /**
     * Exécuté sur le thread UI AVANT le démarrage de doInBackground().
     * Utilisé pour afficher l'indicateur de chargement.
     */
    @Override
    protected void onPreExecute() {
        // Prétraitement de l'appel (ex: afficher ProgressBar)
        screen.showProgressBar(true);
    }

    /**
     * Exécuté sur un thread en arrière-plan.
     * Effectue la requête HTTP GET et retourne le JSON brut.
     *
     * @param urls L'URL passée via execute() (urls[0])
     * @return Le JSON brut sous forme de String, ou chaîne vide en cas d'erreur
     */
    @Override
    protected String doInBackground(URL... urls) {
        String resultat = null;
        URL urlAAppeler = urls[0];  // On récupère la première (et seule) URL
        resultat = appelerServiceRestHttp(urlAAppeler);
        return resultat;
    }

    /**
     * Exécuté sur le thread UI APRÈS la fin de doInBackground().
     * Transmet le résultat à l'activité et masque l'indicateur de chargement.
     *
     * @param resultat Le JSON reçu de l'API (ou chaîne vide)
     */
    @Override
    protected void onPostExecute(String resultat) {
        System.out.println(">>>onPostExecute / resultat=" + resultat);

        // IMPORTANT : plusieurs possibilités pour traiter les données reçues :
        // possibilité-1 : mettre à jour directement la liste dans l'activité principale
        // possibilité-2 : passer les données à l'activité principale et laisser cette dernière mettre à jour la liste
        // possibilité-3 : appeler une méthode de l'activité principale pour qu'elle mette à jour la liste ou les composants

        // Ici on choisit la possibilité-3 : appeler la méthode de mise à jour
        // ListefilmsActivity.mettreAJourActivityApresAppelRest() parsera le JSON et affichera la liste
        screen.mettreAJourActivityApresAppelRest(resultat);

        // Cacher la ProgressBar une fois les données affichées
        screen.showProgressBar(false);
    }

    /**
     * Effectue la requête HTTP GET vers l'endpoint /films.
     * Lit la réponse caractère par caractère et la retourne sous forme de String.
     *
     * @param urlAAppeler L'URL complète de l'endpoint
     * @return Le contenu de la réponse HTTP (JSON), ou chaîne vide en cas d'erreur
     */
    private String appelerServiceRestHttp(URL urlAAppeler ) {
        HttpURLConnection urlConnection = null;
        int responseCode = -1;
        String sResultatAppel = "";  // Contiendra le JSON résultant
        try {
            Log.d("mydebug", "Appel API films: " + urlAAppeler.toString());

            // Ouvrir la connexion HTTP
            // Exemple pour un appel GET
            urlConnection = (HttpURLConnection) urlAAppeler.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            // User-Agent identifie l'application auprès du serveur
            urlConnection.setRequestProperty("User-Agent", System.getProperty("http.agent"));

            // JWT token pour l'autorisation (lu depuis res/values/strings.xml)
            // R.string.api_jwt_token référence la ressource "api_jwt_token" dans strings.xml
            String jwt = screen.getResources().getString(R.string.api_jwt_token);
            // En-tête Authorization avec le schéma "Bearer" (standard OAuth2/JWT)
            urlConnection.setRequestProperty("Authorization", "Bearer " + jwt);

            Log.d("mydebug", "Connexion établie, lecture response code...");
            // Envoyer la requête et lire le code de statut HTTP
            responseCode = urlConnection.getResponseCode();
            Log.d("mydebug", "Response Code: " + responseCode);

            // Vérifier si non autorisé (401 = token invalide ou expiré)
            if (responseCode == 401) {
                Log.e("mydebug", "Non autorisé. Redirection vers login.");
                // runOnUiThread() permet de modifier l'UI depuis un thread en arrière-plan
                screen.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Effacer le token invalide
                        TokenManager.getInstance(screen).clearToken();
                        // Fermer l'activité (retour à MainActivity implicitement)
                        screen.finish();
                    }
                });
                return "";
            }

            // Ouvrir le flux de lecture de la réponse
            // BufferedInputStream améliore les performances de lecture en bufferisant
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            // Lecture du résulat de l'appel et alimentation de la chaine de caractères à renvoyer vers l'appelant
            // Lecture octet par octet (simple mais efficace pour des réponses de taille moyenne)
            int codeCaractere = -1;
            while ((codeCaractere = in.read()) != -1) {
                // Concaténation de chaque caractère (à noter : StringBuilder serait plus efficace)
                sResultatAppel = sResultatAppel + (char) codeCaractere;
            }
            in.close();

            Log.d("mydebug", "Films récupérés, taille: " + sResultatAppel.length());
        } catch (IOException ioe) {
            // IOException : erreur de connexion réseau (serveur inaccessible, timeout, etc.)
            Log.e("mydebug", ">>>Pour appelerServiceRestHttp - IOException ioe =" + ioe.toString());
        } catch (Exception e) {
            // Exception générique : erreur inattendue
            Log.e("mydebug",">>>Pour appelerServiceRestHttp - Exception="+e.toString());
        } finally {
            // finally : exécuté TOUJOURS, même si une exception a été levée
            // Important pour fermer la connexion et libérer les ressources réseau
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return sResultatAppel;
    }

}
