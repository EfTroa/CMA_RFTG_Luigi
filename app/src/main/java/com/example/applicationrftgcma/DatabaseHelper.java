package com.example.applicationrftgcma;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * HELPER BASE DE DONNÉES : DatabaseHelper
 *
 * Gère la base de données SQLite locale qui stocke le panier de l'utilisateur.
 * Hérite de SQLiteOpenHelper, classe Android qui simplifie la gestion du cycle de vie
 * de la base de données (création, mise à jour, accès).
 *
 * STRUCTURE DE LA BASE :
 * Fichier : panier.db
 * Table : "panier"
 * Colonnes :
 *   - film_id (INTEGER, clé primaire) : identifiant unique du film
 *   - titre (TEXT, obligatoire)
 *   - description (TEXT)
 *   - annee (INTEGER)
 *   - duree (INTEGER, en minutes)
 *   - langue (TEXT)
 *   - classification (TEXT, ex: "PG")
 *   - realisateurs (TEXT, chaîne formatée ex: "John Ford, Orson Welles")
 *   - acteurs (TEXT, chaîne formatée)
 *   - categories (TEXT, chaîne formatée)
 *
 * Note : Les listes (réalisateurs, acteurs, catégories) sont stockées en String
 * car SQLite ne supporte pas les types complexes. On les "sérialise" manuellement.
 *
 * Cycle de vie SQLiteOpenHelper :
 * - onCreate() est appelé une seule fois à la création de la base
 * - onUpgrade() est appelé si DATABASE_VERSION augmente (migration de schéma)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Nom du fichier de base de données SQLite stocké dans le stockage interne de l'app
    private static final String DATABASE_NAME = "panier.db";

    // Version de la base de données. Si on change le schéma, on incrémente cette valeur
    // pour déclencher onUpgrade() automatiquement
    private static final int DATABASE_VERSION = 1;

    // Nom de la table
    private static final String TABLE_PANIER = "panier";

    // Colonnes de la table (constantes pour éviter les fautes de frappe dans les requêtes)
    private static final String COLUMN_ID = "film_id";
    private static final String COLUMN_TITRE = "titre";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_ANNEE = "annee";
    private static final String COLUMN_DUREE = "duree";
    private static final String COLUMN_LANGUE = "langue";
    private static final String COLUMN_CLASSIFICATION = "classification";
    private static final String COLUMN_REALISATEURS = "realisateurs";
    private static final String COLUMN_ACTEURS = "acteurs";
    private static final String COLUMN_CATEGORIES = "categories";

    // Requête SQL de création de la table (construite comme une String)
    // "create table panier(film_id integer primary key, titre text not null, ...)"
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PANIER + "("
            + COLUMN_ID + " integer primary key, "
            + COLUMN_TITRE + " text not null, "
            + COLUMN_DESCRIPTION + " text, "
            + COLUMN_ANNEE + " integer, "
            + COLUMN_DUREE + " integer, "
            + COLUMN_LANGUE + " text, "
            + COLUMN_CLASSIFICATION + " text, "
            + COLUMN_REALISATEURS + " text, "
            + COLUMN_ACTEURS + " text, "
            + COLUMN_CATEGORIES + " text);";

    /**
     * Constructeur du helper.
     * Appelle le constructeur parent avec le nom et la version de la base.
     *
     * @param context Contexte Android pour accéder au système de fichiers
     */
    public DatabaseHelper(Context context) {
        // null = pas de CursorFactory personnalisée (on utilise la factory par défaut)
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Appelée par Android la première fois que la base de données est créée.
     * Exécute la requête SQL de création de la table "panier".
     *
     * @param database L'objet représentant la base de données SQLite
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        // execSQL() exécute une requête SQL qui ne retourne pas de résultat (DDL/DML)
        database.execSQL(DATABASE_CREATE);
        Log.d("mydebug", ">>>DatabaseHelper - Table panier créée");
    }

    /**
     * Appelée quand DATABASE_VERSION est augmentée (mise à jour du schéma).
     * Stratégie simple : supprimer l'ancienne table et recréer (perd les données).
     * Pour une app de production, on migrerait les données plutôt que de les supprimer.
     *
     * @param db         La base de données à mettre à jour
     * @param oldVersion L'ancienne version du schéma
     * @param newVersion La nouvelle version du schéma
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("mydebug", ">>>DatabaseHelper - Mise à jour de " + oldVersion + " vers " + newVersion);
        // "DROP TABLE IF EXISTS" supprime la table si elle existe (ne plante pas si elle n'existe pas)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANIER);
        // Recréer la table avec le nouveau schéma
        onCreate(db);
    }

    /**
     * Ajoute un film dans la table "panier" de SQLite.
     * Vérifie d'abord si le film est déjà présent pour éviter les doublons.
     *
     * @param film Le film à ajouter
     * @return true si l'ajout a réussi, false si le film était déjà dans le panier
     */
    // Ajouter un film au panier
    public boolean ajouterFilm(Film film) {
        // getWritableDatabase() ouvre la base en écriture (crée le fichier si nécessaire)
        SQLiteDatabase db = this.getWritableDatabase();

        // Vérifier si le film existe déjà (évite les doublons)
        if (filmExiste(film.getId())) {
            Log.d("mydebug", ">>>DatabaseHelper - Film déjà dans le panier");
            return false;
        }

        // ContentValues = dictionnaire clé-valeur pour les données à insérer
        // Equivalent d'un HashMap<String, Object> typé pour SQLite
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, film.getId());
        values.put(COLUMN_TITRE, film.getTitre());
        values.put(COLUMN_DESCRIPTION, film.getDescription());
        values.put(COLUMN_ANNEE, film.getAnnee());
        values.put(COLUMN_DUREE, film.getDuree());
        values.put(COLUMN_LANGUE, film.getLangueOriginaleName());
        values.put(COLUMN_CLASSIFICATION, film.getClassification());
        // Les listes sont converties en String pour le stockage
        values.put(COLUMN_REALISATEURS, film.getRealisateursString());
        values.put(COLUMN_ACTEURS, film.getActeursString());
        values.put(COLUMN_CATEGORIES, film.getCategoriesString());

        // db.insert(table, nullColumnHack, values) retourne l'ID de la ligne insérée
        // ou -1 en cas d'erreur (ex: contrainte violée)
        // null = si values est vide, Android insère une ligne avec des valeurs nulles
        long result = db.insert(TABLE_PANIER, null, values);
        Log.d("mydebug", ">>>DatabaseHelper - Film ajouté : " + film.getTitre());

        // L'insertion a réussi si result != -1
        return result != -1;
    }

    /**
     * Vérifie si un film est déjà présent dans le panier.
     * Requête SELECT sur la colonne film_id.
     *
     * @param filmId L'identifiant du film à chercher
     * @return true si le film existe, false sinon
     */
    // Vérifier si un film existe déjà
    private boolean filmExiste(int filmId) {
        // getReadableDatabase() ouvre en lecture seule (suffisant pour une vérification)
        SQLiteDatabase db = this.getReadableDatabase();

        // db.query() génère et exécute une requête SELECT
        // Paramètres : table, colonnes, clause WHERE, valeurs WHERE, groupBy, having, orderBy
        // Ici : SELECT film_id FROM panier WHERE film_id = ?
        Cursor cursor = db.query(TABLE_PANIER,
                new String[]{COLUMN_ID},          // Colonnes à sélectionner
                COLUMN_ID + "=?",                  // Clause WHERE ("?" = paramètre)
                new String[]{String.valueOf(filmId)}, // Valeurs des paramètres
                null, null, null);

        // cursor.getCount() retourne le nombre de lignes résultantes
        boolean existe = cursor.getCount() > 0;
        cursor.close();  // IMPORTANT : toujours fermer le Cursor pour libérer la mémoire
        return existe;
    }

    /**
     * Récupère tous les films stockés dans le panier.
     * Parcourt le Cursor ligne par ligne pour construire la liste de films.
     *
     * @return La liste de tous les films dans le panier (vide si aucun)
     */
    // Obtenir tous les films du panier
    public List<Film> obtenirFilms() {
        List<Film> films = new ArrayList<>();
        // getReadableDatabase() : lecture seule suffit
        SQLiteDatabase db = this.getReadableDatabase();

        // SELECT * FROM panier (null = toutes les colonnes, pas de filtre)
        Cursor cursor = db.query(TABLE_PANIER, null, null, null, null, null, null);

        // moveToFirst() : positionne le curseur sur la première ligne
        // Retourne false si la table est vide
        if (cursor.moveToFirst()) {
            do {
                // Créer un nouvel objet Film pour chaque ligne
                Film film = new Film();

                // cursor.getColumnIndexOrThrow(nom) retourne l'indice de la colonne
                // cursor.getInt(indice) lit la valeur entière à cet indice
                film.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                film.setTitre(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITRE)));
                film.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                film.setAnnee(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ANNEE)));
                film.setDuree(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DUREE)));
                film.setClassification(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSIFICATION)));

                // Pour les listes (réalisateurs, acteurs, catégories), on stocke juste les strings formatés
                // car on a juste besoin de les afficher (pas besoin de les reparser en objets)

                films.add(film);
            } while (cursor.moveToNext()); // moveToNext() avance à la ligne suivante, retourne false à la fin
        }

        cursor.close();  // Libérer le Cursor
        Log.d("mydebug", ">>>DatabaseHelper - " + films.size() + " films récupérés");
        return films;
    }

    /**
     * Supprime un film du panier par son identifiant.
     *
     * @param filmId L'identifiant du film à supprimer
     * @return true si la suppression a affecté au moins une ligne, false sinon
     */
    // Supprimer un film du panier
    public boolean supprimerFilm(int filmId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // DELETE FROM panier WHERE film_id = ?
        // db.delete() retourne le nombre de lignes supprimées
        int result = db.delete(TABLE_PANIER, COLUMN_ID + "=?", new String[]{String.valueOf(filmId)});
        Log.d("mydebug", ">>>DatabaseHelper - Film supprimé : " + filmId);
        return result > 0;  // true si au moins une ligne a été supprimée
    }

    /**
     * Supprime tous les films du panier (DELETE sans clause WHERE).
     * Appelée lors de la validation du panier ou du vidage manuel.
     */
    // Vider le panier
    public void viderPanier() {
        SQLiteDatabase db = this.getWritableDatabase();
        // null comme clause WHERE = pas de condition = supprime TOUT
        db.delete(TABLE_PANIER, null, null);
        Log.d("mydebug", ">>>DatabaseHelper - Panier vidé");
    }

    /**
     * Retourne le nombre total de films dans le panier.
     * Utilise une requête SQL directe avec rawQuery() (plus flexible que query()).
     *
     * @return Le nombre de films dans le panier
     */
    // Obtenir le nombre de films dans le panier
    public int getNombreFilms() {
        SQLiteDatabase db = this.getReadableDatabase();
        // rawQuery() permet d'écrire du SQL libre (utile pour COUNT(*))
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PANIER, null);
        cursor.moveToFirst();      // Se positionner sur la première (et seule) ligne
        int count = cursor.getInt(0); // La valeur COUNT(*) est dans la colonne 0
        cursor.close();
        return count;
    }
}
