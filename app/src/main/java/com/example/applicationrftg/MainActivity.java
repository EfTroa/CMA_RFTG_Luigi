package com.example.applicationrftg;

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

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private EditText etEmail;
    private EditText etPassword;
    private EditText editTextURL;
    private ProgressDialog progressDialog;
    private String[] listeURLs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupérer les références des champs
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        editTextURL = findViewById(R.id.editTextURL);

        // Configuration du Spinner
        listeURLs = getResources().getStringArray(R.array.listeURLs);
        Spinner spinnerURLs = findViewById(R.id.spinnerURLs);
        spinnerURLs.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapterListeURLs = ArrayAdapter.createFromResource(
                this, R.array.listeURLs, android.R.layout.simple_spinner_item);
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);

        // Déconnecter l'utilisateur à chaque lancement de l'app
        TokenManager.getInstance(this).clearToken();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        editTextURL.setText(listeURLs[position]);
        UrlManager.setURLConnexion(listeURLs[position]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void ouvrirPageListefilms(View view) {
        // Récupérer les valeurs des champs
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Valider que les champs ne sont pas vides
        if (email.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre mot de passe", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sauvegarder l'URL saisie manuellement
        String urlSaisie = editTextURL.getText().toString().trim();
        if (!urlSaisie.isEmpty()) {
            UrlManager.setURLConnexion(urlSaisie);
        }

        // Afficher ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connexion en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Lancer la tâche de login
        LoginTask loginTask = new LoginTask(email, password, new LoginTask.LoginCallback() {
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
                tokenManager.saveToken("logged_in");

                // Afficher un message de succès
                Toast.makeText(MainActivity.this, "Connexion réussie! (Customer ID: " + customerId + ")", Toast.LENGTH_SHORT).show();

                // Rediriger vers ListefilmsActivity
                Intent intent = new Intent(MainActivity.this, ListefilmsActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onLoginError(String errorMessage) {
                // Masquer le ProgressDialog
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Afficher le message d'erreur
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        loginTask.execute();
    }
}
