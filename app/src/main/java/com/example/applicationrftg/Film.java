package com.example.applicationrftg;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Film {

    // Constructeur vide (requis pour Gson)
    public Film() {
    }
    @SerializedName("filmId")
    private int id;

    @SerializedName("title")
    private String titre;

    @SerializedName("description")
    private String description;

    @SerializedName("releaseYear")
    private int annee;

    @SerializedName("originalLanguageId")
    private Integer languageId;

    @SerializedName("length")
    private int duree;

    @SerializedName("rating")
    private String rating;

    @SerializedName("rentalRate")
    private double prix;

    @SerializedName("directors")
    private List<Director> directors;

    @SerializedName("actors")
    private List<Actor> actors;

    @SerializedName("categories")
    private List<Category> categories;

    // Classe interne pour les réalisateurs
    public static class Director {
        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    // Classe interne pour les acteurs
    public static class Actor {
        @SerializedName("actorId")
        private int id;

        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    // Classe interne pour les catégories
    public static class Category {
        @SerializedName("categoryId")
        private int id;

        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }
    }



    // Getters
    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre != null ? titre : "";
    }

    public String getDescription() {
        return description != null ? description : "Aucune description disponible";
    }

    public int getAnnee() {
        return annee;
    }

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

    public String getDureeFormatee() {
        int heures = duree / 60;
        int minutes = duree % 60;
        if (heures > 0) {
            return heures + "h" + (minutes > 0 ? String.format("%02d", minutes) : "");
        } else {
            return minutes + " min";
        }
    }

    public String getRating() {
        return rating != null ? rating : "Non classé";
    }

    public String getRealisateur() {
        if (directors != null && !directors.isEmpty()) {
            return directors.get(0).getFullName();
        }
        return "Réalisateur inconnu";
    }

    public List<Director> getDirectors() {
        return directors;
    }

    public List<Actor> getActors() {
        return actors;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public double getPrix() {
        return prix;
    }

    public int getDuree() {
        return duree;
    }

    public String getClassification() {
        return rating;
    }

    public String getLangueOriginaleName() {
        return getLangue();
    }

    // Méthodes pour obtenir les listes sous forme de string (pour SQLite)
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

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public void setClassification(String classification) {
        this.rating = classification;
    }

    // toString pour affichage simple
    @Override
    public String toString() {
        return titre + " - " + getRealisateur() + " (" + prix + "€)";
    }
}