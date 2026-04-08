package com.example.applicationrftgcma;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

/**
 * TÂCHE ASYNCHRONE : DetailfilmTask
 *
 * Désérialise le JSON d'un film (reçu depuis l'Intent) en objet Film Java.
 * Contrairement aux autres AsyncTasks, celle-ci ne fait PAS d'appel réseau.
 * Le film a déjà été reçu de l'API dans ListefilmsTask, puis passé via l'Intent.
 *
 * Pourquoi utiliser AsyncTask si ce n'est pas du réseau ?
 * - La désérialisation JSON d'un grand objet peut prendre quelques millisecondes
 * - On garde une cohérence architecturale avec les autres activités
 * - La ProgressBar peut être affichée pendant le traitement pour une meilleure UX
 *
 * PARAMÈTRES GÉNÉRIQUES AsyncTask<Params, Progress, Result> :
 *   - Params : String = le JSON du film passé à execute()
 *   - Progress : Integer = non utilisé (pas de progression intermédiaire)
 *   - Result : Film = l'objet Film désérialisé
 */
public class DetailfilmTask extends AsyncTask<String, Integer, Film> {

    // Référence à l'activité (volatile pour la visibilité entre threads)
    // volatile garantit que la valeur lue est toujours la plus récente
    private volatile DetailfilmActivity screen;

    /**
     * Constructeur : reçoit la référence à DetailfilmActivity.
     *
     * @param s L'activité qui a lancé cette tâche (sera rappelée à la fin)
     */
    public DetailfilmTask(DetailfilmActivity s) {
        this.screen = s;
    }

    /**
     * Exécuté sur le thread UI AVANT le démarrage de doInBackground().
     * Affiche l'indicateur de chargement.
     */
    @Override
    protected void onPreExecute() {
        // Prétraitement (ex: afficher ProgressBar si nécessaire)
        screen.showProgressBar(true);
    }

    /**
     * Exécuté sur un thread en arrière-plan.
     * Utilise Gson pour convertir le JSON String en objet Film Java.
     *
     * @param params Le JSON du film (params[0]), passé via execute(filmJson)
     * @return L'objet Film désérialisé, ou null en cas d'erreur de parsing
     */
    @Override
    protected Film doInBackground(String... params) {
        // Récupérer la chaîne JSON passée en paramètre
        String filmJson = params[0];
        Film film = null;

        try {
            // Parser le JSON en objet Film
            // Gson.fromJson(json, Classe.class) convertit le JSON en objet Java
            // C'est l'opération inverse de Gson.toJson() utilisée dans ListefilmsActivity
            Gson gson = new Gson();
            film = gson.fromJson(filmJson, Film.class);

            Log.d("mydebug", ">>>DetailfilmTask - Film parsé : " + film.getTitre());
        } catch (Exception e) {
            Log.e("mydebug", ">>>DetailfilmTask - Erreur : " + e.getMessage());
        }

        return film;  // null si le parsing a échoué
    }

    /**
     * Exécuté sur le thread UI APRÈS la fin de doInBackground().
     * Transmet l'objet Film à DetailfilmActivity pour l'affichage.
     *
     * @param film L'objet Film désérialisé (peut être null si erreur)
     */
    @Override
    protected void onPostExecute(Film film) {
        // Appeler la méthode de mise à jour de l'activité avec le film prêt à afficher
        // DetailfilmActivity.mettreAJourActivityAvecFilm() remplira tous les TextViews
        screen.mettreAJourActivityAvecFilm(film);

        // Cacher la ProgressBar une fois le traitement terminé
        screen.showProgressBar(false);
    }
}
