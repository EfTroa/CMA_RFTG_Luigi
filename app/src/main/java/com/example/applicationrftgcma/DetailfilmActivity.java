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

/**
 * ACTIVITÉ : Détail d'un film
 *
 * Affiche toutes les informations d'un film sélectionné depuis ListefilmsActivity :
 *  - Titre, année, durée, classification
 *  - Synopsis (description)
 *  - Langue originale
 *  - Réalisateurs
 *  - Acteurs
 *  - Catégories
 *
 * Elle reçoit un objet Film sérialisé en JSON via l'Intent de ListefilmsActivity.
 * DetailfilmTask se charge de désérialiser ce JSON en objet Film (en arrière-plan).
 *
 * L'utilisateur peut :
 *  - Cliquer "Ajouter au panier" → AddToCartTask contacte l'API et PanierManager stocke en SQLite
 *  - Cliquer "Retour" → revient à ListefilmsActivity
 */
public class DetailfilmActivity extends AppCompatActivity {

    // Barre de progression affichée pendant le parsing du film
    private ProgressBar progressBar;

    // Le film actuellement affiché (initialisé par mettreAJourActivityAvecFilm)
    // Déclaré en champ de classe pour pouvoir l'utiliser dans le listener du bouton
    private Film filmActuel;

    /**
     * Méthode du cycle de vie appelée à la création de l'activité.
     * Récupère le JSON du film depuis l'Intent et lance le parsing en arrière-plan.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailfilm);

        progressBar = findViewById(R.id.progressBarDetail);

        // Récupérer le JSON du film passé par l'Intent
        // getIntent() retourne l'Intent qui a démarré cette activité
        // getStringExtra("filmJson") récupère la valeur associée à la clé "filmJson"
        Intent intent = getIntent();
        String filmJson = intent.getStringExtra("filmJson");

        // Lancer le DetailfilmTask pour traiter les données en arrière-plan
        // Même si ce n'est pas un appel réseau, on utilise AsyncTask pour
        // ne pas bloquer le thread UI pendant la désérialisation JSON
        new DetailfilmTask(this).execute(filmJson);

        // Gérer le clic sur le bouton Retour
        // finish() ferme cette activité et revient à la précédente dans la pile
        Button btnRetour = findViewById(R.id.btnRetour);
        btnRetour.setOnClickListener(v -> finish());

        // Gérer le clic sur le bouton Ajouter au panier
        Button btnAjouterPanier = findViewById(R.id.btnAjouterPanier);
        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("DetailfilmActivity", ">>> Bouton Ajouter au panier cliqué");

            // Vérifier que le film a bien été chargé avant d'essayer de l'ajouter
            if (filmActuel != null) {
                // Récupérer le customerId depuis le TokenManager (singleton)
                // Le customerId a été sauvegardé lors de la connexion dans MainActivity
                TokenManager tokenManager = TokenManager.getInstance(this);
                Integer customerId = tokenManager.getCustomerId();

                android.util.Log.d("DetailfilmActivity", ">>> CustomerId: " + customerId + ", FilmId: " + filmActuel.getId());

                // Si le customerId est null, l'utilisateur n'est pas connecté (ne devrait pas arriver)
                if (customerId == null) {
                    Toast.makeText(this, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    android.util.Log.d("DetailfilmActivity", ">>> Avant création AddToCartTask");

                    // Appeler l'API pour ajouter au panier (crée rental avec status_id = 2)
                    // AddToCartTask fait une requête POST /cart/add en arrière-plan
                    // Le callback est appelé sur le thread UI une fois la requête terminée
                    new AddToCartTask(this, customerId, filmActuel.getId(), new AddToCartTask.AddToCartCallback() {

                        /**
                         * Callback de succès : l'API a confirmé l'ajout au panier côté serveur.
                         * On stocke aussi le film en local (SQLite) pour l'affichage dans PanierActivity.
                         */
                        @Override
                        public void onAddToCartSuccess() {
                            // Ajouter aussi au panier local pour l'affichage
                            // PanierManager est le singleton qui gère la base SQLite locale
                            PanierManager panierManager = PanierManager.getInstance(DetailfilmActivity.this);
                            panierManager.ajouterFilm(filmActuel);

                            Toast.makeText(DetailfilmActivity.this,
                                "Film ajouté au panier",
                                Toast.LENGTH_SHORT).show();
                        }

                        /**
                         * Callback d'erreur : l'API a refusé l'ajout (ex: aucun exemplaire disponible)
                         * @param errorMessage Message d'erreur à afficher à l'utilisateur
                         */
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
                // filmActuel est null si DetailfilmTask n'a pas encore terminé
                android.util.Log.e("DetailfilmActivity", ">>> filmActuel est NULL!");
            }
        });
    }

    /**
     * Affiche ou masque la barre de chargement.
     * Appelée par DetailfilmTask depuis onPreExecute() et onPostExecute().
     *
     * @param visible true = afficher, false = masquer
     */
    public void showProgressBar(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Callback appelée par DetailfilmTask une fois le JSON désérialisé en objet Film.
     * Cette méthode remplit tous les TextViews avec les données du film.
     * Appelée sur le thread UI (depuis onPostExecute de DetailfilmTask).
     *
     * @param film L'objet Film désérialisé, ou null si une erreur est survenue
     */
    public void mettreAJourActivityAvecFilm(Film film) {
        if (film == null) {
            // Gérer le cas où le film est null (erreur de parsing)
            TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
            tvTitreFilmDetail.setText("Erreur : Impossible de charger le film");
            return;
        }

        // Conserver le film pour pouvoir l'ajouter au panier lors du clic sur le bouton
        // "this.filmActuel" au lieu de "filmActuel" est redondant ici mais clarifie l'intention
        this.filmActuel = film;

        // Récupérer les TextViews définis dans activity_detailfilm.xml
        TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
        TextView tvInfosFilm = findViewById(R.id.tvInfosFilm);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvLangue = findViewById(R.id.tvLangue);
        TextView tvRealisateurs = findViewById(R.id.tvRealisateurs);
        TextView tvActeurs = findViewById(R.id.tvActeurs);
        TextView tvCategories = findViewById(R.id.tvCategories);

        // Afficher le titre
        tvTitreFilmDetail.setText(film.getTitre());

        // Afficher les infos (année, durée, rating) sur une seule ligne séparée par "•"
        // Ex: "2006 • 1h26 • PG"
        String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
        tvInfosFilm.setText(infos);

        // Afficher la description/synopsis
        tvDescription.setText(film.getDescription());

        // Afficher la langue originale (ex: "Anglais")
        tvLangue.setText(film.getLangue());

        // Afficher les réalisateurs sous forme de liste à puces
        // Chaque réalisateur est précédé de "• " pour une meilleure lisibilité
        StringBuilder realisateurs = new StringBuilder();
        List<Film.Director> directors = film.getDirectors();
        if (directors != null && !directors.isEmpty()) {
            for (int i = 0; i < directors.size(); i++) {
                realisateurs.append("• ").append(directors.get(i).getFullName());
                // Ajouter un retour à la ligne entre chaque réalisateur (pas après le dernier)
                if (i < directors.size() - 1) {
                    realisateurs.append("\n");
                }
            }
            tvRealisateurs.setText(realisateurs.toString());
        } else {
            tvRealisateurs.setText("Aucun réalisateur");
        }

        // Afficher les acteurs sous forme de liste à puces (même principe)
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

        // Afficher les catégories sous forme de liste à puces (même principe)
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
