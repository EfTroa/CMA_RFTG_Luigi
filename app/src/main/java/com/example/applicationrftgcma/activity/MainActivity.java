package com.example.applicationrftgcma.activity;

/**
 * RÔLE DE CE FICHIER :
 * Écran de connexion — point d'entrée de l'application (activité lancée au démarrage).
 * Permet à l'utilisateur de :
 *   - Saisir son email et son mot de passe
 *   - Choisir l'URL du serveur REST via un Spinner (ou la saisir manuellement)
 *   - Se connecter via LoginTask (appel API POST /customers/verify)
 *
 * En cas de succès, le customerId et un token fictif sont sauvegardés dans TokenManager,
 * puis l'utilisateur est redirigé vers ListefilmsActivity.
 *
 * À chaque lancement, la session est effacée (clearToken()) pour forcer la reconnexion.
 *
 * Implémente AdapterView.OnItemSelectedListener pour réagir aux changements du Spinner d'URL.
 */

import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.manager.UrlManager;
import com.example.applicationrftgcma.task.LoginTask;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // Champ de saisie de l'email
    private EditText etEmail;

    // Champ de saisie du mot de passe
    private EditText etPassword;

    // Champ de saisie manuelle de l'URL du serveur (pré-rempli par le Spinner)
    private EditText editTextURL;

    // Dialogue de chargement affiché pendant l'appel réseau de connexion
    private ProgressDialog progressDialog;

    // Liste des URLs prédéfinies chargée depuis res/values/arrays.xml (R.array.listeURLs)
    private String[] listeURLs;

    /**
     * Méthode du cycle de vie Android appelée à la création de l'activité.
     * Initialise les vues, configure le Spinner d'URLs, et efface la session existante.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupérer les références des champs de saisie
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        editTextURL = findViewById(R.id.editTextURL);

        // Configurer le Spinner avec la liste d'URLs prédéfinies depuis arrays.xml
        listeURLs = getResources().getStringArray(R.array.listeURLs);
        Spinner spinnerURLs = findViewById(R.id.spinnerURLs);
        spinnerURLs.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapterListeURLs = ArrayAdapter.createFromResource(
                this, R.array.listeURLs, android.R.layout.simple_spinner_item);
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);

        // Forcer la déconnexion à chaque lancement pour éviter les sessions fantômes
        TokenManager.getInstance(this).clearToken();
    }

    /**
     * Appelé quand l'utilisateur sélectionne une URL dans le Spinner.
     * Met à jour le champ texte ET l'URL active dans UrlManager.
     *
     * @param parent   Le Spinner qui a déclenché l'événement
     * @param view     La vue de l'item sélectionné
     * @param position L'index de l'item sélectionné dans listeURLs
     * @param id       L'identifiant de ligne de l'item
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        editTextURL.setText(listeURLs[position]);
        UrlManager.setURLConnexion(listeURLs[position]);
    }

    /** Requis par l'interface OnItemSelectedListener — rien à faire si aucun item sélectionné */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Déclenché par le bouton "Se connecter" (android:onClick dans le layout).
     * Valide les champs, met à jour l'URL si saisie manuellement,
     * puis lance LoginTask pour authentifier l'utilisateur via l'API.
     *
     * @param view La vue du bouton qui a déclenché l'appel (non utilisée)
     */
    public void ouvrirPageListefilms(View view) {
        // Récupérer les valeurs saisies (trim() pour supprimer les espaces parasites)
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation côté client avant tout appel réseau
        if (email.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre mot de passe", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si l'utilisateur a modifié l'URL manuellement, on l'enregistre dans UrlManager
        String urlSaisie = editTextURL.getText().toString().trim();
        if (!urlSaisie.isEmpty()) {
            UrlManager.setURLConnexion(urlSaisie);
        }

        // Afficher un ProgressDialog non-annulable pendant l'appel réseau
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connexion en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Lancer la tâche de login en arrière-plan
        LoginTask loginTask = new LoginTask(email, password, new LoginTask.LoginCallback() {
            @Override
            public void onLoginSuccess(Integer customerId) {
                // Fermer le ProgressDialog avant toute action UI
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Persister le customerId et un token fictif dans les SharedPreferences
                // (il n'y a pas de JWT réel dans cette version, "logged_in" sert de marqueur)
                TokenManager tokenManager = TokenManager.getInstance(MainActivity.this);
                tokenManager.saveCustomerId(customerId);
                tokenManager.saveToken("logged_in");

                Toast.makeText(MainActivity.this, "Connexion réussie! (Customer ID: " + customerId + ")", Toast.LENGTH_SHORT).show();

                // Naviguer vers la liste des films et fermer MainActivity (finish)
                // pour qu'un appui sur Retour ne revienne pas à l'écran de login
                Intent intent = new Intent(MainActivity.this, ListefilmsActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onLoginError(String errorMessage) {
                // Fermer le ProgressDialog et afficher l'erreur
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        loginTask.execute();
    }
}
