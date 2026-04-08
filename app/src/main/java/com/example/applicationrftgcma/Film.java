package com.example.applicationrftgcma;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * MODÈLE DE DONNÉES : Film
 *
 * Cette classe représente un film dans l'application.
 * Elle joue le rôle de "modèle" dans le pattern MVC (Modèle-Vue-Contrôleur).
 *
 * GSON et @SerializedName :
 * Gson est une bibliothèque Google qui convertit automatiquement du JSON en objets Java.
 * L'annotation @SerializedName("nomDansLeJSON") indique à Gson quel champ JSON
 * correspond à quelle variable Java. Si le nom JSON est identique au nom Java,
 * l'annotation n'est pas obligatoire, mais ici l'API renvoie des noms en anglais
 * (ex: "filmId") et on veut les stocker dans des variables en français (ex: id).
 *
 * Exemple de JSON reçu depuis l'API :
 * {
 *   "filmId": 1,
 *   "title": "ACADEMY DINOSAUR",
 *   "description": "A Epic Drama...",
 *   "releaseYear": 2006,
 *   ...
 * }
 */
public class Film {

    // Constructeur vide (requis pour Gson)
    // Gson a besoin d'un constructeur sans argument pour créer l'objet avant de remplir ses champs
    public Film() {
    }

    // L'identifiant unique du film dans la base de données
    // @SerializedName("filmId") = dans le JSON l'API appelle ça "filmId", on le stocke dans "id"
    @SerializedName("filmId")
    private int id;

    // Le titre du film (ex: "ACADEMY DINOSAUR")
    @SerializedName("title")
    private String titre;

    // La description/synopsis du film
    @SerializedName("description")
    private String description;

    // L'année de sortie du film (ex: 2006)
    @SerializedName("releaseYear")
    private int annee;

    // L'identifiant de la langue originale (1=Anglais, 2=Italien, etc.)
    // Integer (avec majuscule) plutôt que int pour pouvoir accepter la valeur null
    @SerializedName("originalLanguageId")
    private Integer languageId;

    // La durée du film en minutes (ex: 86 pour 1h26)
    @SerializedName("length")
    private int duree;

    // La classification (ex: "PG", "R", "NC-17" - système américain de classification)
    @SerializedName("rating")
    private String rating;

    // Le tarif de location en euros
    @SerializedName("rentalRate")
    private double prix;

    // Liste des réalisateurs du film (peut en avoir plusieurs)
    @SerializedName("directors")
    private List<Director> directors;

    // Liste des acteurs du film
    @SerializedName("actors")
    private List<Actor> actors;

    // Liste des catégories/genres du film (ex: Action, Comedy...)
    @SerializedName("categories")
    private List<Category> categories;

    /**
     * CLASSE INTERNE : Director (Réalisateur)
     *
     * Une classe interne (nested class) est une classe définie à l'intérieur d'une autre.
     * Ici Director est "static" car elle n'a pas besoin d'accéder aux champs de Film.
     * Elle représente un réalisateur avec prénom et nom.
     *
     * Exemple JSON reçu :
     * "directors": [{"firstName": "John", "lastName": "Doe"}]
     */
    public static class Director {
        // Prénom du réalisateur
        @SerializedName("firstName")
        private String firstName;

        // Nom de famille du réalisateur
        @SerializedName("lastName")
        private String lastName;

        /**
         * Retourne le nom complet du réalisateur (prénom + espace + nom)
         * Utilisé pour l'affichage dans les listes et le détail du film
         */
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    /**
     * CLASSE INTERNE : Actor (Acteur)
     *
     * Représente un acteur avec son identifiant, prénom et nom.
     * Même principe que Director.
     */
    public static class Actor {
        // Identifiant unique de l'acteur en base de données
        @SerializedName("actorId")
        private int id;

        // Prénom de l'acteur
        @SerializedName("firstName")
        private String firstName;

        // Nom de famille de l'acteur
        @SerializedName("lastName")
        private String lastName;

        /**
         * Retourne le nom complet de l'acteur
         * Utilisé pour construire la liste des acteurs dans DetailfilmActivity
         */
        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    /**
     * CLASSE INTERNE : Category (Catégorie/Genre)
     *
     * Représente une catégorie de film (ex: Action, Comedy, Drama...)
     * Utilisée à la fois pour l'affichage et pour le filtre par catégorie.
     */
    public static class Category {
        // Identifiant unique de la catégorie
        @SerializedName("categoryId")
        private int id;

        // Nom de la catégorie (ex: "Action", "Comedy")
        @SerializedName("name")
        private String name;

        /**
         * Getter du nom de la catégorie
         * Utilisé dans les spinners de filtre et l'affichage du détail
         */
        public String getName() {
            return name;
        }
    }


    // =========================================================
    // GETTERS - méthodes permettant de lire les valeurs privées
    // En Java, les attributs sont private (encapsulation), donc
    // on crée des getters publics pour y accéder depuis l'extérieur
    // =========================================================

    /** Retourne l'identifiant du film */
    public int getId() {
        return id;
    }

    /**
     * Retourne le titre du film.
     * Si le titre est null (cas rare mais possible), retourne une chaîne vide
     * pour éviter un NullPointerException lors de l'affichage.
     */
    public String getTitre() {
        return titre != null ? titre : "";
    }

    /**
     * Retourne la description/synopsis du film.
     * Si la description est null, retourne un texte par défaut pour informer l'utilisateur.
     */
    public String getDescription() {
        return description != null ? description : "Aucune description disponible";
    }

    /** Retourne l'année de sortie du film */
    public int getAnnee() {
        return annee;
    }

    /**
     * Retourne le nom de la langue originale du film.
     *
     * L'API renvoie un entier (ex: 1 pour Anglais), mais on veut afficher
     * un nom lisible. On utilise un switch-case pour faire la correspondance.
     * Si languageId est null ou inconnu, on retourne "Anglais" par défaut
     * car c'est la langue la plus courante dans la base de films.
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
     * Retourne la durée formatée de façon lisible (ex: "1h26" ou "45 min").
     *
     * La durée est stockée en minutes dans la base de données.
     * On divise par 60 pour obtenir les heures (division entière),
     * et on utilise le modulo (%) pour obtenir les minutes restantes.
     * String.format("%02d", minutes) affiche les minutes sur 2 chiffres (ex: "06" et pas "6").
     */
    public String getDureeFormatee() {
        int heures = duree / 60;      // Ex: 86 / 60 = 1 heure
        int minutes = duree % 60;     // Ex: 86 % 60 = 26 minutes
        if (heures > 0) {
            // Affiche "1h26" ou "1h00" si pas de minutes restantes
            return heures + "h" + (minutes > 0 ? String.format("%02d", minutes) : "");
        } else {
            // Si moins d'une heure, affiche juste "45 min"
            return minutes + " min";
        }
    }

    /**
     * Retourne la classification du film (ex: "PG", "R", "NC-17").
     * Si null, retourne "Non classé".
     */
    public String getRating() {
        return rating != null ? rating : "Non classé";
    }

    /**
     * Retourne le nom du premier réalisateur du film.
     *
     * Un film peut avoir plusieurs réalisateurs, mais on affiche
     * généralement le premier dans les listes pour simplifier.
     * Si la liste est vide ou null, retourne un message par défaut.
     */
    public String getRealisateur() {
        if (directors != null && !directors.isEmpty()) {
            return directors.get(0).getFullName();  // Premier réalisateur
        }
        return "Réalisateur inconnu";
    }

    /** Retourne la liste complète des réalisateurs (pour la page de détail) */
    public List<Director> getDirectors() {
        return directors;
    }

    /** Retourne la liste complète des acteurs (pour la page de détail) */
    public List<Actor> getActors() {
        return actors;
    }

    /** Retourne la liste complète des catégories (pour le filtre et la page de détail) */
    public List<Category> getCategories() {
        return categories;
    }

    /** Retourne le tarif de location */
    public double getPrix() {
        return prix;
    }

    /** Retourne la durée brute en minutes (pour les calculs et le stockage SQLite) */
    public int getDuree() {
        return duree;
    }

    /**
     * Retourne la classification (rating).
     * Méthode en double avec getRating() pour compatibilité avec DatabaseHelper
     * qui l'appelle sous ce nom.
     */
    public String getClassification() {
        return rating;
    }

    /**
     * Retourne le nom de la langue originale.
     * Alias de getLangue() pour clarté dans DatabaseHelper.
     */
    public String getLangueOriginaleName() {
        return getLangue();
    }

    // =========================================================
    // MÉTHODES DE SÉRIALISATION pour SQLite
    // SQLite ne peut pas stocker des listes d'objets directement.
    // On convertit donc les listes en chaînes de caractères séparées par ", "
    // pour les sauvegarder dans la base et les afficher facilement.
    // =========================================================

    /**
     * Retourne tous les réalisateurs sous forme d'une seule chaîne.
     * Ex: "John Ford, Orson Welles"
     * Utilisé pour stocker en SQLite et afficher dans PanierAdapter.
     *
     * StringBuilder est plus efficace que la concaténation de String (+)
     * en boucle car il ne crée pas d'objets intermédiaires à chaque itération.
     */
    public String getRealisateursString() {
        if (directors == null || directors.isEmpty()) {
            return "Aucun réalisateur";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < directors.size(); i++) {
            sb.append(directors.get(i).getFullName());
            // Ajouter une virgule après chaque réalisateur, sauf le dernier
            if (i < directors.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Retourne tous les acteurs sous forme d'une seule chaîne.
     * Ex: "Tom Hanks, Robin Wright, Gary Sinise"
     * Même principe que getRealisateursString().
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
     * Retourne toutes les catégories sous forme d'une seule chaîne.
     * Ex: "Action, Drama, Sci-Fi"
     * Même principe que getRealisateursString().
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

    // =========================================================
    // SETTERS - méthodes permettant de modifier les valeurs
    // Utilisés par DatabaseHelper pour reconstruire un Film depuis SQLite
    // =========================================================

    /** Modifie l'identifiant du film */
    public void setId(int id) {
        this.id = id;
    }

    /** Modifie le titre du film */
    public void setTitre(String titre) {
        this.titre = titre;
    }

    /** Modifie le tarif de location */
    public void setPrix(double prix) {
        this.prix = prix;
    }

    /** Modifie la description du film */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Modifie l'année de sortie */
    public void setAnnee(int annee) {
        this.annee = annee;
    }

    /** Modifie la durée (en minutes) */
    public void setDuree(int duree) {
        this.duree = duree;
    }

    /** Modifie la classification */
    public void setClassification(String classification) {
        this.rating = classification;
    }

    /**
     * Représentation textuelle de l'objet Film.
     * Redéfinition de la méthode toString() héritée de Object.
     * Utilisée pour l'affichage dans les logs et les debugs.
     */
    @Override
    public String toString() {
        return titre + " - " + getRealisateur() + " (" + prix + "€)";
    }
}
