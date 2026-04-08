package com.example.applicationrftgcma;

import android.content.Context;

import java.util.List;

/**
 * GESTIONNAIRE DU PANIER LOCAL : PanierManager
 *
 * Fournit un accès centralisé et simplifié à la base de données SQLite du panier.
 * Sert d'intermédiaire entre les activités/adaptateurs et le DatabaseHelper.
 *
 * PATRON DE CONCEPTION - Singleton :
 * Comme TokenManager, PanierManager utilise le patron Singleton pour garantir
 * qu'une seule connexion à la base de données est ouverte à la fois.
 * On y accède via PanierManager.getInstance(context).
 *
 * PATRON DE CONCEPTION - Façade :
 * PanierManager est aussi une "façade" : il cache la complexité de DatabaseHelper
 * derrière une interface simple. Les activités n'ont pas besoin de connaître
 * les détails de SQLite, elles utilisent juste ajouterFilm(), obtenirFilms(), etc.
 *
 * Architecture :
 *   PanierActivity / FilmAdapter / DetailfilmActivity
 *         ↓
 *   PanierManager (singleton - façade)
 *         ↓
 *   DatabaseHelper (logique SQLite)
 *         ↓
 *   SQLite (base de données "panier.db")
 */
public class PanierManager {

    // L'unique instance du gestionnaire (patron Singleton)
    private static PanierManager instance;

    // Le helper SQLite qui réalise effectivement les opérations de base de données
    private DatabaseHelper databaseHelper;

    /**
     * Constructeur PRIVÉ : empêche la création depuis l'extérieur.
     * Crée le DatabaseHelper avec le contexte de l'application (pas d'une Activity)
     * pour éviter les fuites mémoire.
     *
     * @param context Contexte Android
     */
    private PanierManager(Context context) {
        // getApplicationContext() retourne le contexte de l'app, pas de l'Activity
        // Cela évite qu'une Activity "vive" trop longtemps à cause du singleton
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    /**
     * Retourne l'unique instance de PanierManager (en la créant si nécessaire).
     *
     * synchronized : thread-safe (une seule création possible en cas d'appels simultanés)
     *
     * @param context Contexte Android
     * @return L'instance unique de PanierManager
     */
    // Singleton pour accéder au panier depuis n'importe où
    public static synchronized PanierManager getInstance(Context context) {
        if (instance == null) {
            instance = new PanierManager(context);
        }
        return instance;
    }

    /**
     * Ajoute un film au panier local (SQLite).
     * Si le film est déjà dans le panier, il ne sera pas ajouté en double.
     *
     * @param film Le film à ajouter
     * @return true si l'ajout a réussi, false si le film était déjà présent ou erreur
     */
    // Ajouter un film au panier
    public boolean ajouterFilm(Film film) {
        return databaseHelper.ajouterFilm(film);
    }

    /**
     * Récupère tous les films actuellement dans le panier.
     * Utilisé par PanierActivity pour afficher la liste.
     *
     * @return La liste des films dans le panier (vide si le panier est vide)
     */
    // Obtenir tous les films du panier
    public List<Film> obtenirFilms() {
        return databaseHelper.obtenirFilms();
    }

    /**
     * Supprime un film spécifique du panier par son identifiant.
     *
     * @param filmId L'identifiant unique du film à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    // Supprimer un film du panier
    public boolean supprimerFilm(int filmId) {
        return databaseHelper.supprimerFilm(filmId);
    }

    /**
     * Vide entièrement le panier (supprime tous les films de la table SQLite).
     * Appelée après une validation de panier réussie (CheckoutTask).
     */
    // Vider le panier
    public void viderPanier() {
        databaseHelper.viderPanier();
    }

    /**
     * Retourne le nombre de films actuellement dans le panier.
     * Peut être utilisé pour afficher un badge sur le bouton "Panier".
     *
     * @return Le nombre de films dans le panier (0 si vide)
     */
    // Obtenir le nombre de films dans le panier
    public int getNombreFilms() {
        return databaseHelper.getNombreFilms();
    }
}
