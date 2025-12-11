package com.example.applicationrftg;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private EditText etEmail;
    private EditText etPassword;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupérer les références des champs
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        // Déconnecter l'utilisateur à chaque lancement de l'app
        TokenManager.getInstance(this).clearToken();
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
