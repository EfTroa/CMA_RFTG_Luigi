package com.example.applicationrftgcma;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * TÂCHE ASYNCHRONE : LoginTask
 *
 * Effectue l'authentification de l'utilisateur en appelant l'API REST.
 * Endpoint : POST /customers/verify
 * Corps de la requête JSON : { "email": "...", "password": "hashMD5..." }
 * Réponse JSON : { "customerId": 42 }
 *
 * POURQUOI AsyncTask ?
 * Android interdit les opérations réseau sur le thread principal (UI thread)
 * car elles bloquent l'interface et provoquent une erreur NetworkOnMainThreadException.
 * AsyncTask exécute doInBackground() sur un thread séparé, puis retourne sur
 * le thread UI via onPostExecute() pour mettre à jour l'interface.
 *
 * PARAMÈTRES GÉNÉRIQUES AsyncTask<Params, Progress, Result> :
 *   - Params : Void = doInBackground ne prend pas de paramètre
 *   - Progress : Void = pas de notification de progression intermédiaire
 *   - Result : Integer = retourne l'ID du client (ou -1 si échec)
 *
 * @deprecated AsyncTask est déprécié depuis Android 11 (API 30).
 * Les alternatives modernes sont Coroutines (Kotlin) ou Executors/LiveData (Java).
 * Il reste fonctionnel et lisible pour l'apprentissage.
 */
public class LoginTask extends AsyncTask<Void, Void, Integer> {

    // Tag de log pour identifier les messages de cette classe dans Logcat
    private static final String TAG = "LoginTask";

    // Email de l'utilisateur saisi dans MainActivity
    private String email;

    // Mot de passe saisi dans MainActivity (sera hashé en MD5 avant envoi)
    private String password;

    // Référence au callback (interface) pour notifier MainActivity du résultat
    private LoginCallback callback;

    /**
     * INTERFACE : LoginCallback
     *
     * Définit les méthodes de retour que MainActivity doit implémenter.
     * Le pattern "callback" permet à LoginTask de notifier MainActivity
     * sans avoir de référence directe à cette classe.
     *
     * Avantage : LoginTask est réutilisable avec n'importe quelle classe
     * qui implémente LoginCallback.
     */
    public interface LoginCallback {
        /**
         * Appelée si la connexion a réussi.
         * @param customerId L'identifiant du client retourné par l'API
         */
        void onLoginSuccess(Integer customerId);

        /**
         * Appelée si la connexion a échoué.
         * @param errorMessage Le message d'erreur à afficher à l'utilisateur
         */
        void onLoginError(String errorMessage);
    }

    /**
     * Constructeur : initialise la tâche avec les données de connexion.
     *
     * @param email    L'email saisi par l'utilisateur
     * @param password Le mot de passe saisi (sera hashé dans doInBackground)
     * @param callback L'objet qui recevra les résultats (MainActivity)
     */
    public LoginTask(String email, String password, LoginCallback callback) {
        this.email = email;
        this.password = password;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread en arrière-plan (hors UI thread).
     * Réalise la requête HTTP POST vers l'API d'authentification.
     *
     * @param voids Pas de paramètre (Void...)
     * @return L'identifiant du client (> 0) si succès, -1 sinon
     */
    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            // Construire l'URL complète de l'endpoint d'authentification
            URL url = new URL(UrlManager.getURLConnexion() + "/customers/verify");

            // Ouvrir une connexion HTTP
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");  // Méthode HTTP POST
            connection.setRequestProperty("Content-Type", "application/json"); // Corps en JSON
            connection.setDoOutput(true); // Activer l'envoi d'un corps de requête

            // Créer le JSON body avec l'email et le mot de passe hashé
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            // On envoie le hash MD5 du mot de passe (jamais le mot de passe en clair)
            requestBody.put("password", encrypterChaineMD5(password));

            // Envoyer le corps JSON via le flux de sortie (OutputStream)
            // getBytes("UTF-8") convertit la String en tableau d'octets pour le réseau
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            // Récupérer le code de statut HTTP de la réponse (ex: 200, 401, 500)
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            // HTTP 200 = OK (authentification acceptée)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse JSON caractère par caractère via un BufferedReader
                // InputStreamReader convertit les octets en caractères (selon l'encodage)
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                // Lire toutes les lignes de la réponse et les concaténer
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parser la réponse JSON pour extraire le customerId
                // JSONObject est la classe standard Android pour le JSON (alternative à Gson)
                JSONObject jsonResponse = new JSONObject(response.toString());
                int customerId = jsonResponse.getInt("customerId");

                Log.d(TAG, "Response customerId: " + customerId);

                // Vérifier si le customerId est valide (> 0 = client trouvé)
                // Un customerId de -1 signifie que les identifiants sont incorrects
                if (customerId > 0) {
                    Log.d(TAG, "Login successful. CustomerId: " + customerId);
                    return customerId;
                } else {
                    Log.e(TAG, "Login failed: Invalid credentials (customerId = -1)");
                    return -1;
                }
            } else {
                // Code HTTP différent de 200 (ex: 401 Unauthorized, 500 Server Error)
                Log.e(TAG, "Login failed with code: " + responseCode);
                return -1;
            }
        } catch (Exception e) {
            // Capture toute exception (IOException réseau, JSONException, etc.)
            Log.e(TAG, "Error during login", e);
            return -1;
        }
    }

    /**
     * Exécuté sur le thread UI après la fin de doInBackground().
     * Appelle le callback approprié selon le résultat de la connexion.
     *
     * @param customerId Le résultat retourné par doInBackground()
     */
    @Override
    protected void onPostExecute(Integer customerId) {
        if (callback != null) {
            // Si customerId > 0, la connexion est réussie
            if (customerId != null && customerId > 0) {
                callback.onLoginSuccess(customerId);
            } else {
                callback.onLoginError("Échec de connexion. Vérifiez vos identifiants.");
            }
        }
    }

    /**
     * ENCRYPTAGE EN MD5
     *
     * Transforme une chaîne de caractères en son empreinte (hash) MD5.
     * MD5 est un algorithme de hachage qui produit une chaîne hexadécimale de 32 caractères.
     * Exemple : "1234" → "81dc9bdb52d04dc20036dbd8313ed055"
     *
     * POURQUOI hasher le mot de passe ?
     * Pour ne jamais envoyer le mot de passe en clair sur le réseau.
     * Même si quelqu'un intercepte la requête, il ne voit que le hash, pas le mot de passe.
     *
     * NOTE : MD5 est considéré faible par les standards modernes (collisions possibles).
     * Pour un projet réel, préférer SHA-256 ou bcrypt.
     *
     * @param chaine Le mot de passe en clair à hasher
     * @return Le hash MD5 de la chaîne (32 caractères hexadécimaux)
     */
    // ENCRYPTAGE EN MD5
    private String encrypterChaineMD5(String chaine) {
        // Convertir la chaîne en tableau d'octets (bytes)
        byte[] chaineBytes = chaine.getBytes();
        byte[] hash = null;
        try {
            // MessageDigest.getInstance("MD5") crée un digesteur MD5
            // digest() calcule l'empreinte et retourne un tableau de 16 octets
            hash = MessageDigest.getInstance("MD5").digest(chaineBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Convertir le tableau d'octets en chaîne hexadécimale lisible
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            // Integer.toHexString() convertit un octet en sa représentation hexadécimale
            // (ex: 15 → "f", 255 → "ff")
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                // Un seul caractère hex : ajouter un '0' de padding (ex: "f" → "0f")
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                // Prendre les 2 derniers caractères
                // (nécessaire car les octets négatifs produisent plus de 2 caractères hex en Java)
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
