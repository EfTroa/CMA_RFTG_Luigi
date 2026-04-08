package com.example.applicationrftgcma;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * GESTIONNAIRE DE SESSION : TokenManager
 *
 * Cette classe gère la session de l'utilisateur connecté.
 * Elle stocke et récupère le token d'authentification et l'identifiant client
 * via SharedPreferences (système de stockage clé-valeur persistant d'Android).
 *
 * PATRON DE CONCEPTION - Singleton :
 * Un singleton est une classe dont il n'existe qu'UNE SEULE instance dans toute l'application.
 * Avantage : on accède à la session depuis n'importe où (MainActivity, ListefilmsActivity, etc.)
 * sans avoir à passer des objets en paramètre.
 *
 * Utilisation :
 *   TokenManager tm = TokenManager.getInstance(context);
 *   tm.saveToken("monToken");
 *   String token = tm.getToken();
 *
 * SharedPreferences vs SQLite :
 * SharedPreferences est adapté pour stocker de petites données simples (token, préférences).
 * SQLite est utilisé pour des données structurées en tables (le panier de films).
 */
public class TokenManager {

    // L'unique instance de la classe (static = partagée par toute l'application)
    private static TokenManager instance;

    // Nom du fichier de préférences (chaque app a son propre espace de stockage)
    private static final String PREFS_NAME = "RFTGPrefs";

    // Clé pour retrouver le token dans SharedPreferences (comme un dictionnaire)
    private static final String KEY_TOKEN = "jwt_token";

    // Clé pour retrouver l'identifiant client dans SharedPreferences
    private static final String KEY_CUSTOMER_ID = "customer_id";

    // Le token en mémoire (cache pour éviter de lire les prefs à chaque fois)
    private String token;

    // L'identifiant du client connecté (Integer avec majuscule pour accepter null)
    private Integer customerId;

    // Objet d'accès aux SharedPreferences (lecture/écriture)
    private SharedPreferences sharedPreferences;

    /**
     * Constructeur PRIVÉ : empêche la création de l'objet avec "new TokenManager()"
     * depuis l'extérieur. Seul getInstance() peut créer l'instance.
     *
     * @param context Contexte Android (pour accéder aux SharedPreferences)
     */
    private TokenManager(Context context) {
        // getApplicationContext() évite les fuites mémoire liées aux Activity Context
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Charger les données existantes depuis SharedPreferences au démarrage
        // getString(KEY_TOKEN, null) : retourne null si la clé n'existe pas
        token = sharedPreferences.getString(KEY_TOKEN, null);

        // getInt(KEY_CUSTOMER_ID, -1) : retourne -1 si la clé n'existe pas
        // (car int ne peut pas être null, on utilise -1 comme valeur sentinelle)
        customerId = sharedPreferences.getInt(KEY_CUSTOMER_ID, -1);
        if (customerId == -1) {
            customerId = null;  // Convertir la valeur sentinelle en null Java
        }
    }

    /**
     * Retourne l'unique instance de TokenManager (en la créant si nécessaire).
     *
     * synchronized : garantit qu'un seul thread peut exécuter cette méthode à la fois.
     * Important en Android où plusieurs threads peuvent s'exécuter simultanément.
     *
     * @param context Contexte Android (utilisé uniquement à la création)
     * @return L'instance unique du TokenManager
     */
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            // Première fois : on crée l'instance
            instance = new TokenManager(context);
        } else {
            // Instance existante : recharger les données au cas où elles auraient changé
            instance.reloadFromPreferences();
        }
        return instance;
    }

    /**
     * Recharge les données depuis SharedPreferences si elles sont absentes en mémoire.
     * Utile si une autre partie du code a sauvegardé des données entre deux appels.
     */
    private void reloadFromPreferences() {
        if (token == null) {
            token = sharedPreferences.getString(KEY_TOKEN, null);
        }
        if (customerId == null) {
            int storedId = sharedPreferences.getInt(KEY_CUSTOMER_ID, -1);
            if (storedId != -1) {
                customerId = storedId;
            }
        }
    }

    /**
     * Sauvegarde le token JWT dans la mémoire et dans SharedPreferences.
     * apply() est asynchrone (non bloquant) contrairement à commit().
     *
     * @param token Le token JWT reçu après la connexion (ou "logged_in" dans cette version)
     */
    public void saveToken(String token) {
        this.token = token;
        // edit() ouvre une session d'écriture, putString() modifie la valeur, apply() valide
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    /**
     * Retourne le token actuel (peut être null si l'utilisateur n'est pas connecté).
     *
     * @return Le token ou null
     */
    public String getToken() {
        return token;
    }

    /**
     * Sauvegarde l'identifiant client dans la mémoire et dans SharedPreferences.
     *
     * @param customerId L'identifiant du client retourné par l'API lors de la connexion
     */
    public void saveCustomerId(Integer customerId) {
        this.customerId = customerId;
        if (customerId != null) {
            sharedPreferences.edit().putInt(KEY_CUSTOMER_ID, customerId).apply();
        }
    }

    /**
     * Retourne l'identifiant du client connecté (peut être null).
     *
     * @return Le customerId ou null si non connecté
     */
    public Integer getCustomerId() {
        return customerId;
    }

    /**
     * Efface toutes les données de session (token + customerId).
     * Appelée à chaque lancement de l'app (MainActivity.onCreate) pour forcer la reconnexion.
     * Aussi utile pour implémenter un bouton "Se déconnecter".
     */
    public void clearToken() {
        this.token = null;
        this.customerId = null;
        // remove() supprime les deux clés du fichier SharedPreferences
        sharedPreferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_CUSTOMER_ID)
                .apply();
    }

    /**
     * Vérifie si l'utilisateur est actuellement connecté.
     * L'utilisateur est considéré connecté si un token non vide est présent.
     *
     * @return true si connecté, false sinon
     */
    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
}
