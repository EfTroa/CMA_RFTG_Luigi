package com.example.applicationrftg;

import android.content.Context;

import java.util.List;

public class PanierManager {

    private static PanierManager instance;
    private DatabaseHelper databaseHelper;

    private PanierManager(Context context) {
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    // Singleton pour accéder au panier depuis n'importe où
    public static synchronized PanierManager getInstance(Context context) {
        if (instance == null) {
            instance = new PanierManager(context);
        }
        return instance;
    }

    // Ajouter un film au panier
    public boolean ajouterFilm(Film film) {
        return databaseHelper.ajouterFilm(film);
    }

    // Obtenir tous les films du panier
    public List<Film> obtenirFilms() {
        return databaseHelper.obtenirFilms();
    }

    // Supprimer un film du panier
    public boolean supprimerFilm(int filmId) {
        return databaseHelper.supprimerFilm(filmId);
    }

    // Vider le panier
    public void viderPanier() {
        databaseHelper.viderPanier();
    }

    // Obtenir le nombre de films dans le panier
    public int getNombreFilms() {
        return databaseHelper.getNombreFilms();
    }
}