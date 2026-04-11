package com.example.applicationrftgcma.manager;

/**
 * RÔLE DE CE FICHIER :
 * Stockage global de l'URL du serveur REST.
 * Cette classe utilitaire (pas de Singleton, que des méthodes statiques) centralise
 * l'URL de connexion utilisée par toutes les tâches réseau (ListefilmsTask,
 * LoginTask, AddToCartTask, CheckoutTask).
 *
 * L'URL est modifiable en cours d'exécution : l'utilisateur peut la changer
 * depuis le Spinner ou le champ texte de MainActivity, et toutes les tâches
 * réseau lancées ensuite utiliseront automatiquement la nouvelle valeur.
 *
 * Valeur par défaut : 10.0.2.2:8180 = l'hôte local vu depuis l'émulateur Android
 * (l'émulateur ne peut pas utiliser "localhost" pour atteindre la machine hôte).
 */
public class UrlManager {

    // URL courante du serveur — modifiable dynamiquement via setURLConnexion()
    // 10.0.2.2 est l'adresse spéciale qui pointe vers localhost de la machine hôte dans l'émulateur
    private static String URLConnexion = "http://10.0.2.2:8180";

    /**
     * Retourne l'URL courante du serveur REST.
     * Appelée par toutes les tâches réseau avant chaque requête HTTP.
     *
     * @return L'URL complète (ex: "http://10.0.2.2:8180")
     */
    public static String getURLConnexion() {
        return URLConnexion;
    }

    /**
     * Modifie l'URL du serveur REST pour toutes les prochaines requêtes.
     * Appelée par MainActivity quand l'utilisateur choisit une URL dans le Spinner
     * ou saisit manuellement une URL dans le champ texte.
     *
     * @param url La nouvelle URL à utiliser (ex: "http://192.168.1.10:8180")
     */
    public static void setURLConnexion(String url) {
        URLConnexion = url;
    }
}
