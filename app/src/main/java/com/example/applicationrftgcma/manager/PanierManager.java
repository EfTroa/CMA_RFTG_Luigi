package com.example.applicationrftgcma.manager;

/**
 * RÔLE DE CE FICHIER :
 * Façade singleton pour toutes les opérations sur le panier.
 * Ce fichier sert d'intermédiaire entre les activités (FilmAdapter, DetailfilmActivity,
 * PanierActivity) et la couche base de données (DatabaseHelper).
 *
 * Avantages de ce pattern :
 *   - Les activités n'ont pas besoin de connaître SQLite (séparation des responsabilités)
 *   - Une seule instance de DatabaseHelper pour toute l'app (pas de fuite de ressources)
 *   - Si on change le stockage (ex: passage à Room ou Firebase), seul ce fichier change
 *
 * Usage : PanierManager.getInstance(context).ajouterFilm(film)
 */

import android.content.Context;

import com.example.applicationrftgcma.helper.DatabaseHelper;
import com.example.applicationrftgcma.model.Film;
import java.util.List;

public class PanierManager {

    // Instance unique du Singleton
    private static PanierManager instance;

    // Référence au helper SQLite qui effectue réellement les opérations
    private DatabaseHelper databaseHelper;

    /**
     * Constructeur privé (Singleton).
     * Crée le DatabaseHelper en passant le contexte applicatif (pas d'activité)
     * pour éviter les fuites mémoire si une activité est détruite.
     */
    private PanierManager(Context context) {
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    /**
     * Point d'accès unique au Singleton.
     * Crée l'instance au premier appel, la retourne ensuite directement.
     * Synchronized pour éviter la création en double en cas d'accès concurrent.
     *
     * @param context Contexte Android (activité ou application)
     * @return L'instance unique de PanierManager
     */
    public static synchronized PanierManager getInstance(Context context) {
        if (instance == null) {
            instance = new PanierManager(context);
        }
        return instance;
    }

    /**
     * Ajoute un film au panier (délègue à DatabaseHelper).
     * Si le film est déjà présent, l'ajout est refusé (pas de doublon).
     *
     * @param film Le film à ajouter
     * @return true si l'ajout a réussi, false si déjà dans le panier
     */
    public boolean ajouterFilm(Film film) {
        return databaseHelper.ajouterFilm(film);
    }

    /**
     * Retourne la liste de tous les films actuellement dans le panier.
     * Appelé par PanierActivity pour afficher le contenu.
     *
     * @return Liste des films du panier (vide si le panier est vide)
     */
    public List<Film> obtenirFilms() {
        return databaseHelper.obtenirFilms();
    }

    /**
     * Supprime un film spécifique du panier par son identifiant.
     * Appelé lors d'un clic sur le bouton "Supprimer" dans PanierActivity.
     *
     * @param filmId L'identifiant du film à supprimer
     * @return true si la suppression a réussi
     */
    public boolean supprimerFilm(int filmId) {
        return databaseHelper.supprimerFilm(filmId);
    }

    /**
     * Vide entièrement le panier (supprime tous les films).
     * Appelé par le bouton "Vider le panier" et lors de la validation du panier.
     */
    public void viderPanier() {
        databaseHelper.viderPanier();
    }

    /**
     * Retourne le nombre de films dans le panier.
     * Peut être utilisé pour afficher un badge ou un compteur dans l'interface.
     *
     * @return Nombre de films dans le panier
     */
    public int getNombreFilms() {
        return databaseHelper.getNombreFilms();
    }
}
