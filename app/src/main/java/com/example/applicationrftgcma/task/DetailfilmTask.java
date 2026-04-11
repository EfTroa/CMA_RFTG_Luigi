package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone qui désérialise le JSON d'un film en objet Film.
 * Ce fichier reçoit le JSON du film passé via l'Intent depuis ListefilmsActivity
 * et le transforme en objet Film utilisable par DetailfilmActivity.
 *
 * Pourquoi une AsyncTask pour juste parser du JSON ?
 *   Dans cette architecture, les traitements de données sont toujours faits hors du thread UI
 *   par convention (même si le parsing JSON seul est rapide). Cela permet aussi d'afficher
 *   une ProgressBar pendant le traitement et de garder la cohérence avec ListefilmsTask.
 *
 * Flux de données :
 *   ListefilmsActivity → [filmJson via Intent] → DetailfilmActivity
 *   → DetailfilmTask.doInBackground() → parse JSON → Film
 *   → DetailfilmActivity.mettreAJourActivityAvecFilm(film) → affichage
 */

import android.os.AsyncTask;

import com.example.applicationrftgcma.activity.DetailfilmActivity;
import com.example.applicationrftgcma.model.Film;
import android.util.Log;

import com.google.gson.Gson;

public class DetailfilmTask extends AsyncTask<String, Integer, Film> {

    // Référence à l'activité — volatile pour la visibilité entre threads
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
     * Reçoit le JSON du film et le désérialise en objet Film via Gson.
     *
     * @param params params[0] = le JSON du film passé depuis DetailfilmActivity
     * @return L'objet Film désérialisé, ou null en cas d'erreur de parsing
     */
    @Override
    protected Film doInBackground(String... params) {
        String filmJson = params[0];
        Film film = null;

        try {
            // Utiliser Gson pour convertir la chaîne JSON en objet Film
            // (même bibliothèque que pour la liste, donc même mapping @SerializedName)
            Gson gson = new Gson();
            film = gson.fromJson(filmJson, Film.class);

            Log.d("mydebug", ">>>DetailfilmTask - Film parsé : " + film.getTitre());
        } catch (Exception e) {
            Log.e("mydebug", ">>>DetailfilmTask - Erreur : " + e.getMessage());
            // film reste null — DetailfilmActivity gère ce cas dans mettreAJourActivityAvecFilm()
        }

        return film;
    }

    /**
     * Exécuté sur le thread UI APRÈS doInBackground().
     * Transmet l'objet Film (ou null) à DetailfilmActivity pour l'affichage.
     * Cache aussi la ProgressBar.
     *
     * @param film L'objet Film retourné par doInBackground() (peut être null)
     */
    @Override
    protected void onPostExecute(Film film) {
        // Notifier l'activité avec le film parsé (ou null en cas d'erreur)
        screen.mettreAJourActivityAvecFilm(film);

        // Cacher la ProgressBar maintenant que le traitement est terminé
        screen.showProgressBar(false);
    }
}
