package com.example.applicationrftgcma.manager;

/**
 * RÔLE DE CE FICHIER :
 * Gestionnaire de session utilisateur (pattern Singleton).
 * Ce fichier est responsable de :
 *   - Stocker et récupérer le token de connexion et l'ID client
 *     dans les SharedPreferences (stockage persistant Android)
 *   - Savoir si l'utilisateur est actuellement connecté (isLoggedIn())
 *   - Effacer la session lors de la déconnexion (clearToken())
 *
 * En tant que Singleton, une seule instance existe pour toute l'application,
 * ce qui garantit la cohérence de la session entre toutes les activités.
 *
 * Note : dans cette version, le "token" est une valeur fictive "logged_in"
 * (pas de JWT réel), mais l'architecture est prête pour un vrai token JWT.
 */

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    // Instance unique du Singleton (volatile pour la sécurité en multi-thread)
    private static TokenManager instance;

    // Nom du fichier SharedPreferences où les données sont persistées
    private static final String PREFS_NAME = "RFTGPrefs";

    // Clés utilisées pour lire/écrire dans les SharedPreferences
    private static final String KEY_TOKEN       = "jwt_token";
    private static final String KEY_CUSTOMER_ID = "customer_id";

    // Token de session en mémoire (chargé depuis SharedPreferences au démarrage)
    private String token;

    // ID du client connecté en mémoire (null si non connecté)
    private Integer customerId;

    // Référence aux SharedPreferences pour la persistance entre sessions
    private SharedPreferences sharedPreferences;

    /**
     * Constructeur privé (Singleton) — initialise les SharedPreferences
     * et charge les données éventuellement déjà sauvegardées (reconnexion auto).
     */
    private TokenManager(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Charger les données persistées (si l'utilisateur était déjà connecté)
        token = sharedPreferences.getString(KEY_TOKEN, null);
        customerId = sharedPreferences.getInt(KEY_CUSTOMER_ID, -1);
        if (customerId == -1) {
            customerId = null; // -1 est la valeur sentinelle "absent"
        }
    }

    /**
     * Point d'accès unique au Singleton.
     * Crée l'instance si elle n'existe pas encore, sinon la retourne.
     * Synchronized pour éviter les problèmes de concurrence.
     *
     * @param context Contexte Android nécessaire aux SharedPreferences
     * @return L'instance unique de TokenManager
     */
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        } else {
            // Si l'instance existe déjà, recharger les données depuis les préférences
            // au cas où elles auraient été modifiées depuis une autre activité
            instance.reloadFromPreferences();
        }
        return instance;
    }

    /**
     * Recharge token et customerId depuis les SharedPreferences si les valeurs
     * en mémoire sont null. Évite d'écraser une valeur déjà chargée.
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
     * Sauvegarde le token de session en mémoire ET dans les SharedPreferences.
     * Appelé après une connexion réussie dans MainActivity.
     *
     * @param token Le token à sauvegarder (ici "logged_in" comme valeur fictive)
     */
    public void saveToken(String token) {
        this.token = token;
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    /**
     * Retourne le token actuellement en mémoire.
     * Utilisé par ListefilmsTask pour construire l'en-tête Authorization des requêtes HTTP.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sauvegarde l'identifiant du client connecté.
     * Appelé après une connexion réussie pour mémoriser qui est connecté.
     *
     * @param customerId L'ID du client retourné par l'API
     */
    public void saveCustomerId(Integer customerId) {
        this.customerId = customerId;
        if (customerId != null) {
            sharedPreferences.edit().putInt(KEY_CUSTOMER_ID, customerId).apply();
        }
    }

    /**
     * Retourne l'identifiant du client connecté.
     * Peut être null si l'utilisateur n'est pas connecté.
     */
    public Integer getCustomerId() {
        return customerId;
    }

    /**
     * Efface la session : supprime token et customerId en mémoire et dans les SharedPreferences.
     * Appelé à chaque lancement de MainActivity pour forcer la reconnexion,
     * et lors d'une réponse 401 (token expiré ou invalide).
     */
    public void clearToken() {
        this.token = null;
        this.customerId = null;
        sharedPreferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_CUSTOMER_ID)
                .apply();
    }

    /**
     * Indique si l'utilisateur est actuellement connecté.
     * Se base sur la présence d'un token non vide.
     *
     * @return true si un token de session existe (utilisateur connecté)
     */
    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
}
