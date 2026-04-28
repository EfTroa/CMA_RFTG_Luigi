package com.example.applicationrftgcma.activity;

/**
 * RÔLE DE CE FICHIER :
 * Écran de détail d'un film sélectionné.
 * Cette activité reçoit les données d'un film (sérialisées en JSON via l'Intent)
 * depuis ListefilmsActivity, et les affiche de manière complète :
 *   - Titre, année, durée formatée, classification
 *   - Description / synopsis
 *   - Langue originale
 *   - Liste des réalisateurs
 *   - Liste des acteurs
 *   - Liste des catégories/genres
 *
 * Elle permet aussi d'ajouter le film au panier directement depuis cette page.
 * Le parsing JSON est délégué à DetailfilmTask (AsyncTask) pour ne pas bloquer l'UI.
 */

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.PanierManager;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.task.AddToCartTask;
import com.example.applicationrftgcma.task.DetailfilmTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class DetailfilmActivity extends AppCompatActivity {

    // Barre de chargement affichée pendant le parsing JSON par DetailfilmTask
    private ProgressBar progressBar;

    // Référence au film actuellement affiché (nécessaire pour le bouton "Ajouter au panier")
    private Film filmActuel;

    /**
     * Méthode du cycle de vie Android appelée à la création de l'activité.
     * Récupère le JSON du film depuis l'Intent, lance le parsing asynchrone,
     * et configure les boutons Retour et Ajouter au panier.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailfilm);

        progressBar = findViewById(R.id.progressBarDetail);

        // Récupérer le JSON du film transmis par ListefilmsActivity via l'Intent
        // (le film est sérialisé en JSON car Film n'implémente pas Parcelable)
        Intent intent = getIntent();
        String filmJson = intent.getStringExtra("filmJson");

        // Lancer le parsing du JSON en arrière-plan (retourne un objet Film)
        // Le résultat sera transmis à mettreAJourActivityAvecFilm() via onPostExecute()
        new DetailfilmTask(this).execute(filmJson);

        // Bouton Retour : ferme cette activité et revient à ListefilmsActivity
        Button btnRetour = findViewById(R.id.btnRetour);
        btnRetour.setOnClickListener(v -> finish());

        // Bouton Ajouter au panier : ajoute filmActuel au panier SQLite
        // filmActuel est null jusqu'à ce que mettreAJourActivityAvecFilm() soit appelé,
        // d'où la vérification if (filmActuel != null) pour éviter un NullPointerException
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

                    JSONObject requestBody = new JSONObject();
                    try {
                        requestBody.put("customerId", customerId);
                        requestBody.put("filmId", filmActuel.getId());
                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur interne", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    android.util.Log.d("DetailfilmActivity", ">>> Avant création AddToCartTask");
                    // Appeler l'API pour ajouter au panier (crée rental avec status_id = 2)
                    new AddToCartTask(this, requestBody, new AddToCartTask.AddToCartCallback() {
                    @Override
                    public void onAddToCartSuccess(int rentalId) {
                        // Stocker le rentalId sur le film avant de l'insérer en SQLite
                        filmActuel.setRentalId(rentalId);

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

    /**
     * Affiche ou masque la ProgressBar.
     * Appelé par DetailfilmTask.onPreExecute() (true) et onPostExecute() (false).
     *
     * @param visible true pour afficher, false pour masquer
     */
    public void showProgressBar(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Callback appelé par DetailfilmTask.onPostExecute() une fois le JSON parsé.
     * Remplit tous les TextViews du layout avec les données du film.
     * En cas d'erreur de parsing (film == null), affiche un message d'erreur.
     *
     * @param film L'objet Film désérialisé par DetailfilmTask (ou null si erreur)
     */
    public void mettreAJourActivityAvecFilm(Film film) {
        if (film == null) {
            // Cas d'erreur : le JSON était invalide ou le parsing a échoué
            TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
            tvTitreFilmDetail.setText("Erreur : Impossible de charger le film");
            return;
        }

        // Mémoriser le film pour le bouton "Ajouter au panier" (défini dans onCreate)
        this.filmActuel = film;

        // Récupérer toutes les vues texte du layout activity_detailfilm.xml
        TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
        TextView tvInfosFilm       = findViewById(R.id.tvInfosFilm);
        TextView tvDescription     = findViewById(R.id.tvDescription);
        TextView tvLangue          = findViewById(R.id.tvLangue);
        TextView tvRealisateurs    = findViewById(R.id.tvRealisateurs);
        TextView tvActeurs         = findViewById(R.id.tvActeurs);
        TextView tvCategories      = findViewById(R.id.tvCategories);

        // Afficher le titre du film
        tvTitreFilmDetail.setText(film.getTitre());

        // Afficher les métadonnées sur une seule ligne : "2006 • 1h54 • PG-13"
        String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
        tvInfosFilm.setText(infos);

        // Afficher le synopsis
        tvDescription.setText(film.getDescription());

        // Afficher la langue originale (convertie depuis l'ID numérique par getLangue())
        tvLangue.setText(film.getLangue());

        // Afficher les réalisateurs, un par ligne avec un bullet "•"
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

        // Afficher les acteurs, un par ligne avec un bullet "•"
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

        // Afficher les catégories/genres, une par ligne avec un bullet "•"
        StringBuilder categoriesBuilder = new StringBuilder();
        List<Film.Category> cats = film.getCategories();
        if (cats != null && !cats.isEmpty()) {
            for (int i = 0; i < cats.size(); i++) {
                categoriesBuilder.append("• ").append(cats.get(i).getName());
                if (i < cats.size() - 1) {
                    categoriesBuilder.append("\n");
                }
            }
            tvCategories.setText(categoriesBuilder.toString());
        } else {
            tvCategories.setText("Aucune catégorie");
        }
    }
}
