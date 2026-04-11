package com.example.applicationrftgcma.activity;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.adapter.PanierAdapter;
import com.example.applicationrftgcma.manager.PanierManager;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.task.CheckoutTask;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * RÔLE DE CE FICHIER :
 * Écran du panier — affiche les films que l'utilisateur a mis en attente de location.
 * Les données sont lues depuis la base SQLite locale via PanierManager.
 *
 * Fonctionnalités :
 *   - Affichage de la liste des films du panier (PanierAdapter + ListView)
 *   - Suppression individuelle d'un film (avec dialogue de confirmation)
 *   - Vidage complet du panier (avec dialogue de confirmation)
 *   - Validation du panier : appel API POST /cart/checkout via CheckoutTask
 *     → change le status_id de 2 (dans le panier) à 3 (location active)
 *
 * La méthode onResume() recharge le panier à chaque retour sur cet écran,
 * pour refléter les ajouts éventuels faits depuis DetailfilmActivity ou FilmAdapter.
 */
public class PanierActivity extends AppCompatActivity {

    // ListView affichant les films du panier
    private ListView listePanier;

    // Texte affichant "N film(s)" en haut de l'écran
    private TextView tvNombreFilms;

    // Adapter reliant la liste de films à la ListView (layout item_panier.xml)
    private PanierAdapter adapter;

    // Liste des films actuellement dans le panier (synchronisée avec SQLite)
    private List<Film> films;

    // Facade vers SQLite — utilisé pour toutes les opérations CRUD sur le panier
    private PanierManager panierManager;

    /**
     * Méthode du cycle de vie Android appelée à la création de l'activité.
     * Initialise les vues et configure les boutons Retour et Vider le panier.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        // Récupérer l'instance unique de PanierManager (Singleton)
        panierManager = PanierManager.getInstance(this);

        // Récupérer les références des vues
        listePanier = findViewById(R.id.listePanier);
        tvNombreFilms = findViewById(R.id.tvNombreFilms);
        Button btnViderPanier = findViewById(R.id.btnViderPanier);
        Button btnRetourPanier = findViewById(R.id.btnRetourPanier);

        // Charger et afficher les films du panier depuis SQLite
        chargerPanier();

        // Bouton Retour : ferme cette activité et revient à ListefilmsActivity
        btnRetourPanier.setOnClickListener(v -> finish());

        // Bouton "Vider le panier" : demande confirmation avant suppression totale
        btnViderPanier.setOnClickListener(v -> {
            if (films.isEmpty()) {
                Toast.makeText(this, "Le panier est déjà vide", Toast.LENGTH_SHORT).show();
            } else {
                afficherDialogueConfirmationVider();
            }
        });
    }

    /**
     * Appelé à chaque fois que l'activité revient au premier plan (après un retour
     * depuis DetailfilmActivity par exemple). Recharge le panier pour afficher
     * les films éventuellement ajoutés entre-temps.
     */
    @Override
    protected void onResume() {
        super.onResume();
        chargerPanier();
    }

    /**
     * Charge les films depuis SQLite et configure l'adapter de la ListView.
     * Le callback lambda transmis à PanierAdapter sera invoqué quand l'utilisateur
     * clique sur "Supprimer" sur un item — l'activité affiche alors la confirmation.
     */
    private void chargerPanier() {
        // Lire la liste des films depuis la base de données locale
        films = panierManager.obtenirFilms();

        // Mettre à jour le compteur affiché
        tvNombreFilms.setText(films.size() + " film(s)");

        // Créer l'adapter en passant un callback de suppression (pattern délégation)
        // Quand l'utilisateur clique sur "Supprimer", PanierAdapter appelle ce lambda
        adapter = new PanierAdapter(this, films, (film, position) -> {
            afficherDialogueConfirmationSupprimer(film, position);
        });

        listePanier.setAdapter(adapter);
    }

    /**
     * Affiche un AlertDialog pour confirmer la suppression d'un film individuel.
     * Si confirmé, supprime de SQLite ET de la liste locale, puis rafraîchit l'affichage.
     *
     * @param film     Le film à supprimer
     * @param position L'index du film dans la liste (pour films.remove())
     */
    private void afficherDialogueConfirmationSupprimer(Film film, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer du panier")
                .setMessage("Voulez-vous supprimer \"" + film.getTitre() + "\" du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Supprimer de la base de données SQLite via PanierManager
                    boolean supprime = panierManager.supprimerFilm(film.getId());

                    if (supprime) {
                        // Synchroniser la liste locale avec la base (évite un rechargement complet)
                        films.remove(position);
                        adapter.notifyDataSetChanged();
                        tvNombreFilms.setText(films.size() + " film(s)");
                        Toast.makeText(this, "Film supprimé du panier", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Non", null) // null = fermer le dialogue sans action
                .show();
    }

    /**
     * Affiche un AlertDialog pour confirmer la suppression de tous les films du panier.
     * Si confirmé, vide SQLite ET la liste locale, puis rafraîchit l'affichage.
     */
    private void afficherDialogueConfirmationVider() {
        new AlertDialog.Builder(this)
                .setTitle("Vider le panier")
                .setMessage("Voulez-vous supprimer tous les films du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Vider la base de données SQLite
                    panierManager.viderPanier();

                    // Vider la liste locale et rafraîchir la ListView
                    films.clear();
                    adapter.notifyDataSetChanged();
                    tvNombreFilms.setText("0 film(s)");
                    Toast.makeText(this, "Panier vidé", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    /**
     * Déclenché par le bouton "Valider" (android:onClick dans le layout).
     * Affiche une confirmation, puis lance CheckoutTask pour valider le panier via l'API.
     * En cas de succès, le panier local est vidé et l'utilisateur est informé.
     *
     * @param view La vue du bouton (non utilisée)
     */
    public void validerPanier(View view) {
        if (films.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Valider le panier")
                .setMessage("Confirmer la validation de " + films.size() + " film(s) ?")
                .setPositiveButton("Valider", (dialog, which) -> {
                    // Récupérer le customerId nécessaire pour l'API
                    TokenManager tokenManager = TokenManager.getInstance(this);
                    Integer customerId = tokenManager.getCustomerId();

                    if (customerId == null) {
                        Toast.makeText(this, "Erreur: Customer ID non trouvé", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Lancer la validation en arrière-plan via CheckoutTask
                    // Le customerId est passé à execute() → récupéré dans doInBackground(params[0])
                    new CheckoutTask(this, new CheckoutTask.CheckoutCallback() {
                        @Override
                        public void onCheckoutSuccess(int itemsCount) {
                            // Checkout réussi → vider le panier local (SQLite + liste + affichage)
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
                            // Erreur réseau ou serveur → on ne vide pas le panier local
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
