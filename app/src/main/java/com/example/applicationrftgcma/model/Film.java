package com.example.applicationrftgcma.model;

/**
 * RÔLE DE CE FICHIER :
 * Modèle de données représentant un film dans l'application.
 * Cette classe est le reflet exact de la structure JSON renvoyée par l'API REST.
 * Elle est utilisée à trois niveaux :
 *   - Désérialisation JSON → objet Java via Gson (ListefilmsTask, DetailfilmTask)
 *   - Affichage dans les listes et la page de détail (FilmAdapter, DetailfilmActivity)
 *   - Stockage dans SQLite via DatabaseHelper (PanierManager)
 *
 * Les annotations @SerializedName font le lien entre les clés JSON de l'API
 * (camelCase anglais) et les noms de champs Java (français).
 *
 * Les trois classes internes (Director, Actor, Category) correspondent aux
 * tableaux imbriqués dans le JSON : "directors": [...], "actors": [...], etc.
 */
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Film {

    // Constructeur vide requis par Gson pour instancier l'objet lors de la désérialisation JSON
    public Film() {
    }

    // Identifiant unique du film dans la base de données (clé primaire côté API)
    @SerializedName("filmId")
    private int id;

    // Titre du film (champ "title" dans le JSON de l'API)
    @SerializedName("title")
    private String titre;

    // Synopsis / description du film
    @SerializedName("description")
    private String description;

    // Année de sortie du film
    @SerializedName("releaseYear")
    private int annee;

    // ID de la langue originale — Integer (pas int) pour pouvoir être null si absent du JSON
    @SerializedName("originalLanguageId")
    private Integer languageId;

    // Durée du film en minutes (converti en "Xh YY min" par getDureeFormatee())
    @SerializedName("length")
    private int duree;

    // Classification du film (ex: "PG", "PG-13", "R", "NC-17", "G")
    @SerializedName("rating")
    private String rating;

    // Prix de location journalier
    @SerializedName("rentalRate")
    private double prix;

    // Liste des réalisateurs du film (peut être vide ou null)
    @SerializedName("directors")
    private List<Director> directors;

    // Liste des acteurs du film (peut être vide ou null)
    @SerializedName("actors")
    private List<Actor> actors;

    // Liste des catégories/genres du film (ex: "Action", "Comedy"…)
    @SerializedName("categories")
    private List<Category> categories;

    /**
     * Classe interne représentant un réalisateur.
     * Mappée depuis le tableau JSON "directors" : [{ "firstName": "...", "lastName": "..." }]
     */
    public static class Director {
        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        /** Retourne le nom complet "Prénom Nom" pour l'affichage */
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    /**
     * Classe interne représentant un acteur.
     * Mappée depuis le tableau JSON "actors" : [{ "actorId": 1, "firstName": "...", "lastName": "..." }]
     */
    public static class Actor {
        @SerializedName("actorId")
        private int id;

        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        /** Retourne le nom complet "Prénom Nom" pour l'affichage */
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    /**
     * Classe interne représentant une catégorie/genre.
     * Mappée depuis le tableau JSON "categories" : [{ "categoryId": 1, "name": "Action" }]
     */
    public static class Category {
        @SerializedName("categoryId")
        private int id;

        @SerializedName("name")
        private String name;

        /** Retourne le nom de la catégorie (ex: "Action", "Comedy") */
        public String getName() {
            return name;
        }
    }


    // -----------------------------------------------------------------------------------------
    // GETTERS — utilisés par les adapters et activités pour afficher les données
    // -----------------------------------------------------------------------------------------

    /** @return L'identifiant unique du film (utilisé comme clé dans SQLite) */
    public int getId() {
        return id;
    }

    /** @return Le titre du film, ou chaîne vide si absent du JSON */
    public String getTitre() {
        return titre != null ? titre : "";
    }

    /** @return Le synopsis du film, ou message par défaut si absent */
    public String getDescription() {
        return description != null ? description : "Aucune description disponible";
    }

    /** @return L'année de sortie du film */
    public int getAnnee() {
        return annee;
    }

    /**
     * Convertit l'ID numérique de langue en nom lisible.
     * Le mapping correspond aux données de la table language de la base Sakila.
     *
     * @return Le nom de la langue originale (ex: "Anglais", "Français")
     */
    public String getLangue() {
        if (languageId == null) {
            return "Anglais"; // Par défaut
        }
        // Mapping des IDs de langue selon l'image fournie
        switch (languageId) {
            case 1: return "Anglais";
            case 2: return "Italien";
            case 3: return "Japonais";
            case 4: return "Mandarin";
            case 5: return "Français";
            case 6: return "Allemand";
            default: return "Anglais";
        }
    }

    /**
     * Formate la durée brute (en minutes) en chaîne lisible.
     * Exemples : 114 min → "1h54", 45 min → "45 min", 120 min → "2h"
     *
     * @return La durée formatée pour l'affichage (ex: "1h54")
     */
    public String getDureeFormatee() {
        int heures = duree / 60;
        int minutes = duree % 60;
        if (heures > 0) {
            // Affiche les minutes uniquement si elles ne sont pas nulles, avec zéro de remplissage
            return heures + "h" + (minutes > 0 ? String.format("%02d", minutes) : "");
        } else {
            return minutes + " min";
        }
    }

    /** @return La classification du film (ex: "PG-13"), ou "Non classé" si absente */
    public String getRating() {
        return rating != null ? rating : "Non classé";
    }

    /**
     * Retourne le nom du premier réalisateur de la liste.
     * Utilisé dans FilmAdapter pour afficher une ligne courte sous le titre.
     *
     * @return Nom du premier réalisateur, ou "Réalisateur inconnu" si la liste est vide
     */
    public String getRealisateur() {
        if (directors != null && !directors.isEmpty()) {
            return directors.get(0).getFullName();
        }
        return "Réalisateur inconnu";
    }

    /** @return La liste complète des réalisateurs (pour l'affichage détaillé) */
    public List<Director> getDirectors() {
        return directors;
    }

    /** @return La liste complète des acteurs (pour l'affichage détaillé) */
    public List<Actor> getActors() {
        return actors;
    }

    /** @return La liste complète des catégories/genres (pour les filtres et l'affichage) */
    public List<Category> getCategories() {
        return categories;
    }

    /** @return Le prix de location journalier */
    public double getPrix() {
        return prix;
    }

    /** @return La durée brute en minutes (utilisée par DatabaseHelper pour SQLite) */
    public int getDuree() {
        return duree;
    }

    /**
     * Alias de getRating() — utilisé par DatabaseHelper pour stocker la classification.
     * @return La classification du film (ex: "PG-13")
     */
    public String getClassification() {
        return rating;
    }

    /**
     * Alias de getLangue() — utilisé par DatabaseHelper pour stocker le nom de la langue.
     * @return Le nom de la langue originale
     */
    public String getLangueOriginaleName() {
        return getLangue();
    }

    // -----------------------------------------------------------------------------------------
    // MÉTHODES DE SÉRIALISATION POUR SQLITE
    // Ces méthodes convertissent les listes en chaînes CSV simples pour le stockage SQLite.
    // Les listes ne sont pas restaurées à la lecture car PanierActivity n'en a pas besoin.
    // -----------------------------------------------------------------------------------------

    /**
     * Sérialise la liste des réalisateurs en chaîne CSV.
     * Exemple : "Martin Scorsese, Francis Ford Coppola"
     * Utilisé par DatabaseHelper.ajouterFilm() pour la colonne "realisateurs".
     *
     * @return Les réalisateurs séparés par des virgules, ou "Aucun réalisateur"
     */
    public String getRealisateursString() {
        if (directors == null || directors.isEmpty()) {
            return "Aucun réalisateur";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < directors.size(); i++) {
            sb.append(directors.get(i).getFullName());
            if (i < directors.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Sérialise la liste des acteurs en chaîne CSV.
     * Utilisé par DatabaseHelper.ajouterFilm() pour la colonne "acteurs".
     *
     * @return Les acteurs séparés par des virgules, ou "Aucun acteur"
     */
    public String getActeursString() {
        if (actors == null || actors.isEmpty()) {
            return "Aucun acteur";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actors.size(); i++) {
            sb.append(actors.get(i).getFullName());
            if (i < actors.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Sérialise la liste des catégories en chaîne CSV.
     * Utilisé par DatabaseHelper.ajouterFilm() pour la colonne "categories".
     *
     * @return Les catégories séparées par des virgules, ou "Aucune catégorie"
     */
    public String getCategoriesString() {
        if (categories == null || categories.isEmpty()) {
            return "Aucune catégorie";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            sb.append(categories.get(i).getName());
            if (i < categories.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------------------------
    // SETTERS — utilisés par DatabaseHelper pour reconstruire un Film depuis SQLite
    // -----------------------------------------------------------------------------------------

    /** Définit l'identifiant du film */
    public void setId(int id) {
        this.id = id;
    }

    /** Définit le titre du film */
    public void setTitre(String titre) {
        this.titre = titre;
    }

    /** Définit le prix de location */
    public void setPrix(double prix) {
        this.prix = prix;
    }

    /** Définit le synopsis du film */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Définit l'année de sortie */
    public void setAnnee(int annee) {
        this.annee = annee;
    }

    /** Définit la durée en minutes */
    public void setDuree(int duree) {
        this.duree = duree;
    }

    /** Définit la classification (stockée dans le champ rating) */
    public void setClassification(String classification) {
        this.rating = classification;
    }

    /**
     * Représentation textuelle courte du film — utilisée pour les logs et le débogage.
     * @return "Titre - Réalisateur (prix€)"
     */
    @Override
    public String toString() {
        return titre + " - " + getRealisateur() + " (" + prix + "€)";
    }
}