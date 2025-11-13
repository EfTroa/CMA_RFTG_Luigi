package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

public class DetailfilmTask extends AsyncTask<String, Integer, Film> {

    private volatile DetailfilmActivity screen;  // Référence à l'activité

    public DetailfilmTask(DetailfilmActivity s) {
        this.screen = s;
    }

    @Override
    protected void onPreExecute() {
        // Prétraitement (ex: afficher ProgressBar si nécessaire)
        screen.showProgressBar(true);
    }

    @Override
    protected Film doInBackground(String... params) {
        String filmJson = params[0];
        Film film = null;

        try {
            // Parser le JSON en objet Film
            Gson gson = new Gson();
            film = gson.fromJson(filmJson, Film.class);

            Log.d("mydebug", ">>>DetailfilmTask - Film parsé : " + film.getTitre());
        } catch (Exception e) {
            Log.e("mydebug", ">>>DetailfilmTask - Erreur : " + e.getMessage());
        }

        return film;
    }

    @Override
    protected void onPostExecute(Film film) {
        // Appeler la méthode de mise à jour de l'activité
        screen.mettreAJourActivityAvecFilm(film);

        // Cacher la ProgressBar
        screen.showProgressBar(false);
    }
}