package com.example.applicationrftgcma.helper;

/**
 * RÔLE DE CE FICHIER :
 * Couche d'accès aux données SQLite pour le panier.
 * Ce fichier gère directement la base de données locale de l'appareil :
 *   - Création de la table "panier" au premier lancement
 *   - Mise à jour du schéma si la version de la base change
 *   - Toutes les opérations CRUD (ajout, lecture, suppression) sur les films du panier
 *
 * Il est utilisé uniquement via PanierManager (pattern façade), jamais directement
 * depuis les activités.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.applicationrftgcma.model.Film;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Nom du fichier de base de données sur l'appareil
    private static final String DATABASE_NAME = "panier.db";

    // Version du schéma — à incrémenter si on modifie la structure de la table
    private static final int DATABASE_VERSION = 2;

    // Nom de la table stockant les films du panier
    private static final String TABLE_PANIER = "panier";

    // Noms des colonnes de la table
    private static final String COLUMN_ID             = "film_id";
    private static final String COLUMN_TITRE          = "titre";
    private static final String COLUMN_DESCRIPTION    = "description";
    private static final String COLUMN_ANNEE          = "annee";
    private static final String COLUMN_DUREE          = "duree";
    private static final String COLUMN_LANGUE         = "langue";
    private static final String COLUMN_CLASSIFICATION = "classification";
    private static final String COLUMN_REALISATEURS   = "realisateurs";
    private static final String COLUMN_ACTEURS        = "acteurs";
    private static final String COLUMN_CATEGORIES     = "categories";
    private static final String COLUMN_RENTAL_ID      = "rental_id";

    // Requête SQL de création de la table au premier lancement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PANIER + "("
            + COLUMN_ID             + " integer primary key, "
            + COLUMN_TITRE          + " text not null, "
            + COLUMN_DESCRIPTION    + " text, "
            + COLUMN_ANNEE          + " integer, "
            + COLUMN_DUREE          + " integer, "
            + COLUMN_LANGUE         + " text, "
            + COLUMN_CLASSIFICATION + " text, "
            + COLUMN_REALISATEURS   + " text, "
            + COLUMN_ACTEURS        + " text, "
            + COLUMN_CATEGORIES     + " text, "
            + COLUMN_RENTAL_ID      + " integer);";

    /**
     * Constructeur — initialise le helper avec le nom et la version de la base.
     * SQLiteOpenHelper s'occupe d'ouvrir ou créer le fichier .db sur l'appareil.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Appelé automatiquement par Android lors du tout premier accès à la base
     * (quand le fichier panier.db n'existe pas encore).
     * Crée la table "panier" en exécutant la requête SQL DATABASE_CREATE.
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        Log.d("mydebug", ">>>DatabaseHelper - Table panier créée");
    }

    /**
     * Appelé automatiquement quand DATABASE_VERSION est incrémenté.
     * Stratégie simple : on supprime l'ancienne table et on la recrée.
     * ATTENTION : cela efface toutes les données existantes du panier.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("mydebug", ">>>DatabaseHelper - Mise à jour de " + oldVersion + " vers " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANIER);
        onCreate(db);
    }

    /**
     * Ajoute un film dans la table "panier".
     * Vérifie d'abord si le film est déjà présent (grâce à filmExiste())
     * pour éviter les doublons — le film_id est la clé primaire.
     *
     * @param film L'objet Film à insérer
     * @return true si l'insertion a réussi, false si le film était déjà dans le panier
     */
    public boolean ajouterFilm(Film film) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Vérifier si le film existe déjà avant d'insérer
        if (filmExiste(film.getId())) {
            Log.d("mydebug", ">>>DatabaseHelper - Film déjà dans le panier");
            return false;
        }

        // Préparer les valeurs à insérer dans chaque colonne
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID,             film.getId());
        values.put(COLUMN_TITRE,          film.getTitre());
        values.put(COLUMN_DESCRIPTION,    film.getDescription());
        values.put(COLUMN_ANNEE,          film.getAnnee());
        values.put(COLUMN_DUREE,          film.getDuree());
        values.put(COLUMN_LANGUE,         film.getLangueOriginaleName());
        values.put(COLUMN_CLASSIFICATION, film.getClassification());
        // Les listes (réalisateurs, acteurs, catégories) sont converties en chaîne CSV
        values.put(COLUMN_REALISATEURS,   film.getRealisateursString());
        values.put(COLUMN_ACTEURS,        film.getActeursString());
        values.put(COLUMN_CATEGORIES,     film.getCategoriesString());
        // rental_id nécessaire pour appeler DELETE /cart/{rentalId} lors de la suppression
        values.put(COLUMN_RENTAL_ID,      film.getRentalId());

        // Insérer la ligne — retourne -1 en cas d'échec
        long result = db.insert(TABLE_PANIER, null, values);
        Log.d("mydebug", ">>>DatabaseHelper - Film ajouté : " + film.getTitre());

        return result != -1;
    }

    /**
     * Vérifie si un film est déjà présent dans le panier en cherchant son ID.
     * Méthode privée utilisée uniquement par ajouterFilm() pour éviter les doublons.
     *
     * @param filmId L'identifiant du film à chercher
     * @return true si le film existe déjà dans la table
     */
    private boolean filmExiste(int filmId) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Requête SELECT sur la colonne film_id avec le filtre WHERE film_id = ?
        Cursor cursor = db.query(
                TABLE_PANIER,
                new String[]{COLUMN_ID},       // colonnes à sélectionner
                COLUMN_ID + "=?",              // clause WHERE
                new String[]{String.valueOf(filmId)}, // valeur du paramètre
                null, null, null
        );

        boolean existe = cursor.getCount() > 0;
        cursor.close();
        return existe;
    }

    /**
     * Récupère tous les films stockés dans le panier.
     * Les listes (réalisateurs, acteurs, catégories) ne sont PAS restaurées
     * depuis la base car on a uniquement besoin d'afficher les infos de base dans le panier.
     *
     * @return Liste de tous les films présents dans le panier
     */
    public List<Film> obtenirFilms() {
        List<Film> films = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // SELECT * FROM panier (sans filtre, toutes les lignes)
        Cursor cursor = db.query(TABLE_PANIER, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                // Reconstruire un objet Film depuis chaque ligne de la base
                Film film = new Film();
                film.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                film.setTitre(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITRE)));
                film.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                film.setAnnee(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ANNEE)));
                film.setDuree(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DUREE)));
                film.setClassification(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSIFICATION)));
                // rental_id rechargé pour permettre la suppression via API
                film.setRentalId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RENTAL_ID)));

                // Note : réalisateurs, acteurs et catégories sont stockés en string CSV
                // mais ne sont pas rechargés ici car PanierActivity n'en a pas besoin

                films.add(film);
            } while (cursor.moveToNext());
        }

        cursor.close();
        Log.d("mydebug", ">>>DatabaseHelper - " + films.size() + " films récupérés");
        return films;
    }

    /**
     * Supprime un film du panier en ciblant son ID.
     *
     * @param filmId L'identifiant du film à supprimer
     * @return true si une ligne a bien été supprimée, false si le film n'existait pas
     */
    public boolean supprimerFilm(int filmId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // DELETE FROM panier WHERE film_id = filmId
        int result = db.delete(TABLE_PANIER, COLUMN_ID + "=?", new String[]{String.valueOf(filmId)});
        Log.d("mydebug", ">>>DatabaseHelper - Film supprimé : " + filmId);
        return result > 0;
    }

    /**
     * Supprime toutes les lignes de la table panier (sans supprimer la table elle-même).
     * Utilisé par le bouton "Vider le panier" et lors de la validation du panier.
     */
    public void viderPanier() {
        SQLiteDatabase db = this.getWritableDatabase();
        // DELETE FROM panier (sans clause WHERE = supprime tout)
        db.delete(TABLE_PANIER, null, null);
        Log.d("mydebug", ">>>DatabaseHelper - Panier vidé");
    }

    /**
     * Retourne le nombre de films actuellement dans le panier.
     * Utilisé pour afficher le compteur dans l'interface.
     *
     * @return Nombre de lignes dans la table panier
     */
    public int getNombreFilms() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PANIER, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }
}
