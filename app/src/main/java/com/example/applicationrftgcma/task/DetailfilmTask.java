package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone qui désérialise le JSON d'un film en objet Film.
 * Ce fichier reçoit le JSON du film passé via l'Intent depuis ListefilmsActivity
 * et le transforme en objet Film utilisable par DetailfilmActivity.
 *
 * Qui l'appelle : DetailfilmActivity.onCreate()
 * Ce qu'elle retourne : un objet Film désérialisé (ou null en cas d'erreur de parsing)
 *
 * Pourquoi une AsyncTask pour juste parser du JSON ?
 *   Dans cette architecture, les traitements de données sont toujours faits hors du thread UI
 *   par convention (même si le parsing JSON seul est rapide). Cela permet aussi d'afficher
 *   une ProgressBar pendant le traitement et de garder la cohérence avec ListefilmsTask.
 *
 * Pas d'appel réseau dans cette tâche — le JSON est déjà disponible localement via l'Intent.
 * Aucune méthode appelerReseauPost / appelerReseauGet n'est donc nécessaire.
 *
 * Flux de données :
 *   ListefilmsActivity → [filmJson via Intent] → DetailfilmActivity
 *   → new DetailfilmTask(this).execute(filmJson)
 *   → doInBackground() : parse le JSON avec Gson → retourne l'objet Film
 *   → onPostExecute()  : appelle DetailfilmActivity.mettreAJourActivityAvecFilm(film)
 *
 * AsyncTask<String, Void, Film> :
 *   - String → execute() reçoit le JSON du film (passé depuis DetailfilmActivity)
 *   - Void   → pas de progression intermédiaire publiée
 *   - Film   → doInBackground retourne l'objet Film parsé, reçu par onPostExecute
 */

import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.activity.DetailfilmActivity;
import com.example.applicationrftgcma.model.Film;

import com.google.gson.Gson;

@SuppressWarnings("deprecation")
public class DetailfilmTask extends AsyncTask<String, Void, Film> {

    // Tag pour les logs Logcat — permet de filtrer les messages de cette tâche
    private static final String TAG = "DetailfilmTask";

    // Référence à l'activité — volatile pour la visibilité entre threads
    // (doInBackground tourne sur un thread secondaire, onPreExecute/onPostExecute sur l'UI thread)
    private volatile DetailfilmActivity screen;

    /**
     * Constructeur — reçoit la référence à l'activité pour pouvoir
     * appeler ses méthodes depuis onPreExecute et onPostExecute.
     *
     * @param s L'activité DetailfilmActivity qui a lancé cette tâche
     */
    public DetailfilmTask(DetailfilmActivity s) {
        this.screen = s;
    }

    /**
     * Exécuté sur le thread UI AVANT doInBackground().
     * Affiche la ProgressBar pour indiquer que le traitement est en cours.
     */
    @Override
    protected void onPreExecute() {
        screen.showProgressBar(true);
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Reçoit le JSON du film (params[0]) et le désérialise en objet Film via Gson.
     * Pas d'appel réseau ici — le JSON est déjà disponible depuis l'Intent.
     *
     * @param params params[0] = le JSON du film passé depuis DetailfilmActivity via execute()
     * @return L'objet Film désérialisé, ou null en cas d'erreur de parsing
     */
    @Override
    protected Film doInBackground(String... params) {
        // Récupérer le JSON du film passé en paramètre de execute()
        String filmJson = params[0];
        Film film = null;

        try {
            // Utiliser Gson pour convertir la chaîne JSON en objet Film
            // (même bibliothèque que pour la liste, donc même mapping @SerializedName)
            Gson gson = new Gson();
            film = gson.fromJson(filmJson, Film.class);

            Log.d(TAG, "Film parsé avec succès : " + film.getTitre());
        } catch (Exception e) {
            Log.e(TAG, "Erreur de parsing du JSON du film : " + e.getMessage());
            // film reste null — DetailfilmActivity gère ce cas dans mettreAJourActivityAvecFilm()
        }

        return film;
    }

    /**
     * Exécuté sur le thread UI APRÈS doInBackground().
     * Transmet l'objet Film (ou null en cas d'erreur) à DetailfilmActivity pour l'affichage.
     * Cache également la ProgressBar une fois le traitement terminé.
     *
     * @param film L'objet Film retourné par doInBackground() (peut être null si erreur de parsing)
     */
    @Override
    protected void onPostExecute(Film film) {
        // Notifier l'activité avec le film parsé (ou null en cas d'erreur)
        // DetailfilmActivity.mettreAJourActivityAvecFilm() gère le cas null
        screen.mettreAJourActivityAvecFilm(film);

        // Cacher la ProgressBar maintenant que le traitement est terminé
        screen.showProgressBar(false);
    }
}
