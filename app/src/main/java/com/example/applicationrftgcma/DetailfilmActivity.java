package com.example.applicationrftgcma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class DetailfilmActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Film filmActuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailfilm);

        progressBar = findViewById(R.id.progressBarDetail);

        // Récupérer le JSON du film passé par l'Intent
        Intent intent = getIntent();
        String filmJson = intent.getStringExtra("filmJson");

        // Lancer le DetailfilmTask pour traiter les données en arrière-plan
        new DetailfilmTask(this).execute(filmJson);

        // Gérer le clic sur le bouton Retour
        Button btnRetour = findViewById(R.id.btnRetour);
        btnRetour.setOnClickListener(v -> finish());

        // Gérer le clic sur le bouton Ajouter au panier
        Button btnAjouterPanier = findViewById(R.id.btnAjouterPanier);
        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("DetailfilmActivity", ">>> Bouton Ajouter au panier cliqué");
            if (filmActuel != null) {
                // Récupérer le customerId
                TokenManager tokenManager = TokenManager.getInstance(this);
                Integer customerId = tokenManager.getCustomerId();

                android.util.Log.d("DetailfilmActivity", ">>> CustomerId: " + customerId + ", FilmId: " + filmActuel.getId());

                if (customerId == null) {
                    Toast.makeText(this, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    android.util.Log.d("DetailfilmActivity", ">>> Avant création AddToCartTask");
                    // Appeler l'API pour ajouter au panier (crée rental avec status_id = 2)
                    new AddToCartTask(this, customerId, filmActuel.getId(), new AddToCartTask.AddToCartCallback() {
                    @Override
                    public void onAddToCartSuccess() {
                        // Ajouter aussi au panier local pour l'affichage
                        PanierManager panierManager = PanierManager.getInstance(DetailfilmActivity.this);
                        panierManager.ajouterFilm(filmActuel);

                        Toast.makeText(DetailfilmActivity.this,
                            "Film ajouté au panier",
                            Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAddToCartError(String errorMessage) {
                        Toast.makeText(DetailfilmActivity.this,
                            errorMessage,
                            Toast.LENGTH_LONG).show();
                    }
                }).execute();
                    android.util.Log.d("DetailfilmActivity", ">>> Après execute AddToCartTask");
                } catch (Exception e) {
                    android.util.Log.e("DetailfilmActivity", ">>> ERREUR lors de l'ajout au panier", e);
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                android.util.Log.e("DetailfilmActivity", ">>> filmActuel est NULL!");
            }
        });
    }

    // Affichage / masquage de la barre de chargement
    public void showProgressBar(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    // Callback appelé par DetailfilmTask après le traitement
    public void mettreAJourActivityAvecFilm(Film film) {
        if (film == null) {
            // Gérer le cas où le film est null
            TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
            tvTitreFilmDetail.setText("Erreur : Impossible de charger le film");
            return;
        }

        // Conserver le film pour pouvoir l'ajouter au panier
        this.filmActuel = film;

        // Récupérer les TextViews
        TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
        TextView tvInfosFilm = findViewById(R.id.tvInfosFilm);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvLangue = findViewById(R.id.tvLangue);
        TextView tvRealisateurs = findViewById(R.id.tvRealisateurs);
        TextView tvActeurs = findViewById(R.id.tvActeurs);
        TextView tvCategories = findViewById(R.id.tvCategories);

        // Afficher le titre
        tvTitreFilmDetail.setText(film.getTitre());

        // Afficher les infos (année, durée, rating)
        String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
        tvInfosFilm.setText(infos);

        // Afficher la description
        tvDescription.setText(film.getDescription());

        // Afficher la langue
        tvLangue.setText(film.getLangue());

        // Afficher les réalisateurs
        StringBuilder realisateurs = new StringBuilder();
        List<Film.Director> directors = film.getDirectors();
        if (directors != null && !directors.isEmpty()) {
            for (int i = 0; i < directors.size(); i++) {
                realisateurs.append("• ").append(directors.get(i).getFullName());
                if (i < directors.size() - 1) {
                    realisateurs.append("\n");
                }
            }
            tvRealisateurs.setText(realisateurs.toString());
        } else {
            tvRealisateurs.setText("Aucun réalisateur");
        }

        // Afficher les acteurs
        StringBuilder acteurs = new StringBuilder();
        List<Film.Actor> actors = film.getActors();
        if (actors != null && !actors.isEmpty()) {
            for (int i = 0; i < actors.size(); i++) {
                acteurs.append("• ").append(actors.get(i).getFullName());
                if (i < actors.size() - 1) {
                    acteurs.append("\n");
                }
            }
            tvActeurs.setText(acteurs.toString());
        } else {
            tvActeurs.setText("Aucun acteur");
        }

        // Afficher les catégories
        StringBuilder categories = new StringBuilder();
        List<Film.Category> cats = film.getCategories();
        if (cats != null && !cats.isEmpty()) {
            for (int i = 0; i < cats.size(); i++) {
                categories.append("• ").append(cats.get(i).getName());
                if (i < cats.size() - 1) {
                    categories.append("\n");
                }
            }
            tvCategories.setText(categories.toString());
        } else {
            tvCategories.setText("Aucune catégorie");
        }
    }
}
