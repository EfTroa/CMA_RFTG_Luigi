package com.example.applicationrftgcma;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * ACTIVITÉ PRINCIPALE : Écran de connexion (Login)
 *
 * C'est le point d'entrée de l'application (LAUNCHER dans le Manifest).
 * Elle affiche un formulaire de connexion avec :
 *  - Un champ email
 *  - Un champ mot de passe
 *  - Un spinner (liste déroulante) pour choisir l'URL du serveur
 *  - Un champ texte pour saisir une URL personnalisée
 *
 * Flux d'authentification :
 * 1. L'utilisateur saisit ses identifiants
 * 2. On appelle LoginTask (AsyncTask) qui envoie les identifiants à l'API
 * 3. Si succès → on sauvegarde le customerId et on ouvre ListefilmsActivity
 * 4. Si échec → on affiche un message d'erreur
 *
 * implements AdapterView.OnItemSelectedListener :
 * Cette Activity implémente une interface pour écouter les changements du spinner.
 * Cela oblige à implémenter onItemSelected() et onNothingSelected().
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // Champ de saisie de l'email
    private EditText etEmail;

    // Champ de saisie du mot de passe
    private EditText etPassword;

    // Champ pour saisir une URL de serveur manuellement
    private EditText editTextURL;

    // Boîte de dialogue affichée pendant la connexion (indicateur de chargement)
    private ProgressDialog progressDialog;

    // Tableau des URLs prédéfinies (chargées depuis res/values/strings.xml)
    private String[] listeURLs;

    /**
     * onCreate() est la méthode de cycle de vie appelée quand l'activité est créée.
     * C'est ici qu'on initialise l'interface et les composants.
     *
     * @param savedInstanceState Contient l'état sauvegardé si l'activité est recréée
     *                           (ex: rotation de l'écran). Peut être null au premier lancement.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Associe le fichier XML de layout (activity_main.xml) à cette activité
        setContentView(R.layout.activity_main);

        // Récupérer les références des champs
        // findViewById() cherche un composant par son identifiant défini dans le XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        editTextURL = findViewById(R.id.editTextURL);

        // Configuration du Spinner (liste déroulante)
        // On charge le tableau d'URLs depuis les ressources strings.xml
        listeURLs = getResources().getStringArray(R.array.listeURLs);
        Spinner spinnerURLs = findViewById(R.id.spinnerURLs);

        // L'Activity elle-même écoute les événements du spinner (implements OnItemSelectedListener)
        spinnerURLs.setOnItemSelectedListener(this);

        // Création d'un ArrayAdapter pour connecter les données (listeURLs) au spinner
        // android.R.layout.simple_spinner_item = layout standard Android pour les items du spinner
        ArrayAdapter<CharSequence> adapterListeURLs = ArrayAdapter.createFromResource(
                this, R.array.listeURLs, android.R.layout.simple_spinner_item);

        // Définir le layout pour la liste déroulante (différent de l'item sélectionné)
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);

        // Déconnecter l'utilisateur à chaque lancement de l'app
        // Bonne pratique de sécurité : on efface le token pour forcer une nouvelle authentification
        TokenManager.getInstance(this).clearToken();
    }

    /**
     * Méthode de l'interface OnItemSelectedListener.
     * Appelée automatiquement quand l'utilisateur sélectionne une URL dans le spinner.
     *
     * @param parent   L'AdapterView où la sélection s'est produite
     * @param view     La vue correspondant à l'item sélectionné
     * @param position L'indice (0-based) de l'item sélectionné dans la liste
     * @param id       L'identifiant de la ligne dans l'adaptateur
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Met à jour le champ URL avec la valeur sélectionnée dans le spinner
        editTextURL.setText(listeURLs[position]);
        // Met aussi à jour l'UrlManager (classe utilitaire globale)
        UrlManager.setURLConnexion(listeURLs[position]);
    }

    /**
     * Méthode obligatoire de l'interface OnItemSelectedListener.
     * Appelée quand aucun item n'est sélectionné (cas rare, spinner toujours pré-sélectionné).
     * Ici on ne fait rien dans ce cas.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Méthode déclenchée lors du clic sur le bouton "Se connecter".
     * Déclarée dans activity_main.xml via android:onClick="ouvrirPageListefilms".
     *
     * @param view La vue (bouton) qui a déclenché l'événement (non utilisée ici)
     */
    public void ouvrirPageListefilms(View view) {
        // Récupérer les valeurs des champs
        // trim() supprime les espaces en début et fin de chaîne (évite les erreurs de saisie)
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Valider que les champs ne sont pas vides
        // Si le champ email est vide, on affiche un Toast (message court) et on arrête
        if (email.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre email", Toast.LENGTH_SHORT).show();
            return;  // return sans valeur = sortir de la méthode ici
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre mot de passe", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sauvegarder l'URL saisie manuellement
        // Si l'utilisateur a modifié l'URL dans le champ texte, on l'utilise
        String urlSaisie = editTextURL.getText().toString().trim();
        if (!urlSaisie.isEmpty()) {
            UrlManager.setURLConnexion(urlSaisie);
        }

        // Afficher ProgressDialog
        // ProgressDialog est une boîte de dialogue de chargement (dépréciée mais fonctionnelle)
        // setCancelable(false) empêche de la fermer en appuyant derrière
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connexion en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Lancer la tâche de login
        // LoginTask est un AsyncTask : le réseau ne peut pas s'exécuter sur le thread principal (UI thread)
        // On utilise donc un AsyncTask pour le faire en arrière-plan
        // Les callbacks (onLoginSuccess / onLoginError) sont appelés sur le thread UI une fois terminé
        LoginTask loginTask = new LoginTask(email, password, new LoginTask.LoginCallback() {

            /**
             * Callback de succès : appelé quand le serveur confirme l'authentification.
             * @param customerId L'identifiant du client retourné par l'API
             */
            @Override
            public void onLoginSuccess(Integer customerId) {
                // Masquer le ProgressDialog
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Sauvegarder le customerId (pas de token JWT dans cette version)
                TokenManager tokenManager = TokenManager.getInstance(MainActivity.this);
                tokenManager.saveCustomerId(customerId);
                // Sauvegarder un token fictif pour que isLoggedIn() fonctionne
                // isLoggedIn() vérifie que le token n'est pas null : on lui donne une valeur fixe
                tokenManager.saveToken("logged_in");

                // Afficher un message de succès
                Toast.makeText(MainActivity.this, "Connexion réussie! (Customer ID: " + customerId + ")", Toast.LENGTH_SHORT).show();

                // Rediriger vers ListefilmsActivity
                // Intent = "intention de faire quelque chose" → ici naviguer vers une autre activité
                Intent intent = new Intent(MainActivity.this, ListefilmsActivity.class);
                startActivity(intent);
                // finish() ferme MainActivity pour qu'elle ne reste pas dans la pile de navigation
                // (évite que l'utilisateur revienne à la connexion avec le bouton Retour)
                finish();
            }

            /**
             * Callback d'erreur : appelé quand l'authentification a échoué.
             * @param errorMessage Le message d'erreur à afficher à l'utilisateur
             */
            @Override
            public void onLoginError(String errorMessage) {
                // Masquer le ProgressDialog
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Afficher le message d'erreur
                // Toast.LENGTH_LONG = le message reste affiché plus longtemps qu'avec SHORT
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Lancer l'exécution de la tâche asynchrone
        loginTask.execute();
    }
}
