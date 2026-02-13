package com.example.applicationrftgcma;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PanierActivity extends AppCompatActivity {

    private ListView listePanier;
    private TextView tvNombreFilms;
    private PanierAdapter adapter;
    private List<Film> films;
    private PanierManager panierManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        // Initialiser le PanierManager
        panierManager = PanierManager.getInstance(this);

        // Récupérer les vues
        listePanier = findViewById(R.id.listePanier);
        tvNombreFilms = findViewById(R.id.tvNombreFilms);
        Button btnViderPanier = findViewById(R.id.btnViderPanier);
        Button btnRetourPanier = findViewById(R.id.btnRetourPanier);

        // Charger les films du panier
        chargerPanier();

        // Gérer le clic sur le bouton Retour
        btnRetourPanier.setOnClickListener(v -> finish());

        // Gérer le clic sur le bouton "Vider le panier"
        btnViderPanier.setOnClickListener(v -> {
            if (films.isEmpty()) {
                Toast.makeText(this, "Le panier est déjà vide", Toast.LENGTH_SHORT).show();
            } else {
                afficherDialogueConfirmationVider();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger le panier quand on revient sur cette activité
        chargerPanier();
    }

    private void chargerPanier() {
        // Récupérer les films depuis SQLite
        films = panierManager.obtenirFilms();

        // Mettre à jour le nombre de films
        tvNombreFilms.setText(films.size() + " film(s)");

        // Créer l'adapter avec un listener pour la suppression
        adapter = new PanierAdapter(this, films, (film, position) -> {
            // Afficher un dialogue de confirmation avant de supprimer
            afficherDialogueConfirmationSupprimer(film, position);
        });

        // Affecter l'adapter à la ListView
        listePanier.setAdapter(adapter);
    }

    private void afficherDialogueConfirmationSupprimer(Film film, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer du panier")
                .setMessage("Voulez-vous supprimer \"" + film.getTitre() + "\" du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Supprimer de la base de données
                    boolean supprime = panierManager.supprimerFilm(film.getId());

                    if (supprime) {
                        // Supprimer de la liste locale
                        films.remove(position);
                        adapter.notifyDataSetChanged();
                        tvNombreFilms.setText(films.size() + " film(s)");
                        Toast.makeText(this, "Film supprimé du panier", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void afficherDialogueConfirmationVider() {
        new AlertDialog.Builder(this)
                .setTitle("Vider le panier")
                .setMessage("Voulez-vous supprimer tous les films du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Vider la base de données
                    panierManager.viderPanier();

                    // Vider la liste locale
                    films.clear();
                    adapter.notifyDataSetChanged();
                    tvNombreFilms.setText("0 film(s)");
                    Toast.makeText(this, "Panier vidé", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    public void validerPanier(View view) {
        if (films.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Valider le panier")
                .setMessage("Confirmer la validation de " + films.size() + " film(s) ?")
                .setPositiveButton("Valider", (dialog, which) -> {
                    // Récupérer le customerId
                    TokenManager tokenManager = TokenManager.getInstance(this);
                    Integer customerId = tokenManager.getCustomerId();

                    if (customerId == null) {
                        Toast.makeText(this, "Erreur: Customer ID non trouvé", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Appeler l'API pour valider le panier (change status_id de 2 à 3)
                    new CheckoutTask(this, new CheckoutTask.CheckoutCallback() {
                        @Override
                        public void onCheckoutSuccess(int itemsCount) {
                            // Vider le panier local après validation réussie
                            panierManager.viderPanier();
                            films.clear();
                            adapter.notifyDataSetChanged();
                            tvNombreFilms.setText("0 film(s)");
                            Toast.makeText(PanierActivity.this,
                                "Panier validé avec succès ! (" + itemsCount + " film(s))",
                                Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCheckoutError(String errorMessage) {
                            Toast.makeText(PanierActivity.this,
                                "Erreur: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        }
                    }).execute(customerId);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
