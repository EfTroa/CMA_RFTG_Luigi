package com.example.applicationrftgcma;

/**
 * GESTIONNAIRE D'URL : UrlManager
 *
 * Classe utilitaire qui centralise la gestion de l'URL de base du serveur API.
 * Toutes les tâches réseau (LoginTask, ListefilmsTask, etc.) appellent
 * getURLConnexion() pour construire l'URL complète de l'endpoint.
 *
 * Exemple d'utilisation :
 *   String url = UrlManager.getURLConnexion() + "/films";
 *   // Donne "http://10.0.2.2:8180/films"
 *
 * Pourquoi cette classe ?
 * Sans UrlManager, chaque tâche réseau devrait connaître l'URL courante,
 * ce qui rendrait difficile le changement de serveur (localhost vs production).
 * Ici, un seul appel à setURLConnexion() change l'URL pour toute l'application.
 *
 * Tous les champs et méthodes sont STATIC car on n'a pas besoin d'instancier cette classe.
 * C'est l'équivalent d'une variable globale, mais organisée dans une classe.
 *
 * Note : "10.0.2.2" est l'adresse spéciale de l'émulateur Android pour atteindre
 * "localhost" de la machine hôte (127.0.0.1 sur le PC de développement).
 */
/* Liste déroulante serveur */
public class UrlManager {

    // URL de base du serveur par défaut (émulateur Android → serveur local port 8180)
    // static = appartient à la classe, pas à une instance
    private static String URLConnexion = "http://10.0.2.2:8180";

    /**
     * Retourne l'URL de base actuellement configurée.
     * Appelée par toutes les tâches réseau pour construire leurs URLs.
     *
     * @return L'URL de base (ex: "http://10.0.2.2:8180" ou "http://rftg.mtb111.com")
     */
    public static String getURLConnexion() {

        return URLConnexion;
    }

    /**
     * Modifie l'URL de base du serveur.
     * Appelée depuis MainActivity quand l'utilisateur sélectionne ou saisit une URL.
     *
     * @param url La nouvelle URL de base (ex: "http://rftg.mtb111.com")
     */
    public static void setURLConnexion(String url) {

        URLConnexion = url;
    }
}
