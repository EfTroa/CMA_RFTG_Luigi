package com.example.applicationrftg;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "panier.db";
    private static final int DATABASE_VERSION = 1;

    // Nom de la table
    private static final String TABLE_PANIER = "panier";

    // Colonnes de la table
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

    // Requête de création de la table
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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        Log.d("mydebug", ">>>DatabaseHelper - Table panier créée");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("mydebug", ">>>DatabaseHelper - Mise à jour de " + oldVersion + " vers " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANIER);
        onCreate(db);
    }

    // Ajouter un film au panier
    public boolean ajouterFilm(Film film) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Vérifier si le film existe déjà
        if (filmExiste(film.getId())) {
            Log.d("mydebug", ">>>DatabaseHelper - Film déjà dans le panier");
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, film.getId());
        values.put(COLUMN_TITRE, film.getTitre());
        values.put(COLUMN_DESCRIPTION, film.getDescription());
        values.put(COLUMN_ANNEE, film.getAnnee());
        values.put(COLUMN_DUREE, film.getDuree());
        values.put(COLUMN_LANGUE, film.getLangueOriginaleName());
        values.put(COLUMN_CLASSIFICATION, film.getClassification());
        values.put(COLUMN_REALISATEURS, film.getRealisateursString());
        values.put(COLUMN_ACTEURS, film.getActeursString());
        values.put(COLUMN_CATEGORIES, film.getCategoriesString());

        long result = db.insert(TABLE_PANIER, null, values);
        Log.d("mydebug", ">>>DatabaseHelper - Film ajouté : " + film.getTitre());

        return result != -1;
    }

    // Vérifier si un film existe déjà
    private boolean filmExiste(int filmId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PANIER,
                new String[]{COLUMN_ID},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(filmId)},
                null, null, null);

        boolean existe = cursor.getCount() > 0;
        cursor.close();
        return existe;
    }

    // Obtenir tous les films du panier
    public List<Film> obtenirFilms() {
        List<Film> films = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PANIER, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Film film = new Film();
                film.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                film.setTitre(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITRE)));
                film.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                film.setAnnee(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ANNEE)));
                film.setDuree(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DUREE)));
                film.setClassification(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSIFICATION)));

                // Pour les listes (réalisateurs, acteurs, catégories), on stocke juste les strings formatés
                // car on a juste besoin de les afficher

                films.add(film);
            } while (cursor.moveToNext());
        }

        cursor.close();
        Log.d("mydebug", ">>>DatabaseHelper - " + films.size() + " films récupérés");
        return films;
    }

    // Supprimer un film du panier
    public boolean supprimerFilm(int filmId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PANIER, COLUMN_ID + "=?", new String[]{String.valueOf(filmId)});
        Log.d("mydebug", ">>>DatabaseHelper - Film supprimé : " + filmId);
        return result > 0;
    }

    // Vider le panier
    public void viderPanier() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PANIER, null, null);
        Log.d("mydebug", ">>>DatabaseHelper - Panier vidé");
    }

    // Obtenir le nombre de films dans le panier
    public int getNombreFilms() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PANIER, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }
}