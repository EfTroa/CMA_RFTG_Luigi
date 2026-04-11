package com.example.applicationrftgcma.task;

/**
 * RÔLE DE CE FICHIER :
 * Tâche asynchrone pour authentifier un utilisateur via l'API REST POST /customers/verify.
 * L'API attend un email et un mot de passe hashé en MD5, et retourne le customerId si valide.
 *
 * Flux d'exécution :
 *   MainActivity.ouvrirPageListefilms()
 *   → new LoginTask(email, password, callback).execute()
 *   → doInBackground() : hash MD5 du mot de passe + appel HTTP POST
 *   → onPostExecute() : retour sur le thread UI, appel du callback avec le customerId
 *
 * Règle métier : si l'API retourne customerId > 0, la connexion est réussie.
 * Un customerId = -1 signifie identifiants incorrects.
 *
 * Note sécurité : MD5 est utilisé ici pour correspondre au format attendu par l'API
 * (hachage côté client avant envoi). Ce n'est pas recommandé en production.
 */

import android.os.AsyncTask;

import com.example.applicationrftgcma.manager.UrlManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class LoginTask extends AsyncTask<Void, Void, Integer> {

    // Tag pour les logs Logcat
    private static final String TAG = "LoginTask";

    // Email saisi par l'utilisateur dans MainActivity
    private String email;

    // Mot de passe saisi par l'utilisateur (sera hashé en MD5 avant envoi)
    private String password;

    // Callback pour notifier MainActivity du résultat (succès ou échec)
    private LoginCallback callback;

    /**
     * Interface de callback permettant à MainActivity de réagir au résultat de la connexion.
     */
    public interface LoginCallback {
        /**
         * Appelé si la connexion a réussi.
         * @param customerId L'identifiant du client retourné par l'API
         */
        void onLoginSuccess(Integer customerId);
        /** Appelé si la connexion a échoué (identifiants incorrects ou erreur réseau) */
        void onLoginError(String errorMessage);
    }

    /**
     * Constructeur — reçoit les identifiants et le callback de résultat.
     *
     * @param email    L'email saisi par l'utilisateur
     * @param password Le mot de passe en clair (sera hashé en MD5 dans doInBackground)
     * @param callback Listener à notifier une fois la requête terminée
     */
    public LoginTask(String email, String password, LoginCallback callback) {
        this.email = email;
        this.password = password;
        this.callback = callback;
    }

    /**
     * Exécuté sur un thread secondaire (pas le thread UI).
     * Hashe le mot de passe en MD5, puis envoie un POST à /customers/verify.
     *
     * @return Le customerId si connexion réussie (> 0), ou -1 en cas d'échec
     */
    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/customers/verify");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Construire le corps JSON : { "email": "...", "password": "hash_md5..." }
            // Le mot de passe est hashé en MD5 avant envoi (attendu par l'API)
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", encrypterChaineMD5(password));

            // Envoyer le corps de la requête en UTF-8
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Lire la réponse JSON
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parser la réponse JSON pour extraire le customerId
                JSONObject jsonResponse = new JSONObject(response.toString());
                int customerId = jsonResponse.getInt("customerId");

                Log.d(TAG, "Response customerId: " + customerId);

                // Vérifier si le customerId est valide (> 0 = succès, -1 = identifiants incorrects)
                if (customerId > 0) {
                    Log.d(TAG, "Login successful. CustomerId: " + customerId);
                    return customerId;
                } else {
                    Log.e(TAG, "Login failed: Invalid credentials (customerId = -1)");
                    return -1;
                }
            } else {
                Log.e(TAG, "Login failed with code: " + responseCode);
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during login", e);
            return -1;
        }
    }

    /**
     * Exécuté sur le thread UI après doInBackground().
     * Notifie le callback avec le résultat de la connexion.
     *
     * @param customerId Le customerId retourné (> 0 si succès, -1 si échec)
     */
    @Override
    protected void onPostExecute(Integer customerId) {
        if (callback != null) {
            if (customerId != null && customerId > 0) {
                callback.onLoginSuccess(customerId);
            } else {
                callback.onLoginError("Échec de connexion. Vérifiez vos identifiants.");
            }
        }
    }

    /**
     * Convertit une chaîne de caractères en son empreinte MD5 hexadécimale.
     * Utilisé pour hasher le mot de passe avant envoi à l'API.
     *
     * Fonctionnement :
     *   1. On obtient les bytes de la chaîne
     *   2. MessageDigest calcule le hash MD5 (tableau de 16 bytes)
     *   3. On convertit chaque byte en sa représentation hexadécimale sur 2 caractères
     *      (le padding '0' à gauche gère les bytes dont la valeur hex < 16)
     *
     * @param chaine Le mot de passe en clair à hasher
     * @return La représentation hexadécimale du hash MD5 (32 caractères)
     */
    private String encrypterChaineMD5(String chaine) {
        byte[] chaineBytes = chaine.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(chaineBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            // toHexString() des bytes négatifs donne 8 caractères (ex: "ffffff80")
            // On ne garde que les 2 derniers pour avoir la représentation correcte sur 1 byte
            if (hex.length() == 1) {
                // Cas d'un byte dont la valeur hex tient sur 1 chiffre → on ajoute un '0' devant
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
