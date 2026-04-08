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

/**
 * ACTIVITÉ : Panier (gestion du panier de location)
 *
 * Affiche les films ajoutés au panier par l'utilisateur.
 * Le panier est stocké localement dans une base SQLite via PanierManager/DatabaseHelper.
 *
 * Fonctionnalités :
 *  - Affichage de la liste des films dans le panier
 *  - Suppression d'un film individuel (avec confirmation)
 *  - Vidage complet du panier (avec confirmation)
 *  - Validation du panier : appelle l'API /cart/checkout pour finaliser les locations
 *
 * IMPORTANT : La synchronisation avec le serveur se fait en deux temps :
 *  1. Lors de l'ajout (AddToCartTask) : le serveur crée un rental avec status_id=2 (en panier)
 *  2. Lors de la validation (CheckoutTask) : le serveur passe les rentals à status_id=3 (loué)
 */
public class PanierActivity extends AppCompatActivity {

    // ListView qui affiche les films du panier
    private ListView listePanier;

    // TextView qui affiche le nombre de films (ex: "3 film(s)")
    private TextView tvNombreFilms;

    // Adaptateur reliant la liste de films à la ListView
    private PanierAdapter adapter;

    // Liste des films actuellement dans le panier (chargée depuis SQLite)
    private List<Film> films;

    // Singleton d'accès au panier SQLite local
    private PanierManager panierManager;

    /**
     * Méthode du cycle de vie appelée à la création de l'activité.
     * Initialise les composants et charge les films du panier.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        // Initialiser le PanierManager (singleton)
        // getInstance() retourne toujours la même instance (patron singleton)
        panierManager = PanierManager.getInstance(this);

        // Récupérer les vues depuis le layout XML
        listePanier = findViewById(R.id.listePanier);
        tvNombreFilms = findViewById(R.id.tvNombreFilms);
        Button btnViderPanier = findViewById(R.id.btnViderPanier);
        Button btnRetourPanier = findViewById(R.id.btnRetourPanier);

        // Charger les films du panier depuis la base SQLite et afficher
        chargerPanier();

        // Gérer le clic sur le bouton Retour
        // finish() ferme cette activité et revient à ListefilmsActivity
        btnRetourPanier.setOnClickListener(v -> finish());

        // Gérer le clic sur le bouton "Vider le panier"
        btnViderPanier.setOnClickListener(v -> {
            if (films.isEmpty()) {
                Toast.makeText(this, "Le panier est déjà vide", Toast.LENGTH_SHORT).show();
            } else {
                // Demander une confirmation avant de vider (action irréversible)
                afficherDialogueConfirmationVider();
            }
        });
    }

    /**
     * Méthode du cycle de vie appelée quand l'activité redevient visible.
     * onResume() est appelé après onCreate() mais aussi après un retour depuis une autre activité.
     * On recharge le panier ici pour prendre en compte les films ajoutés
     * depuis DetailfilmActivity pendant que PanierActivity était en pause.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Recharger le panier quand on revient sur cette activité
        chargerPanier();
    }

    /**
     * Charge les films depuis SQLite et met à jour l'interface.
     * Appelée au démarrage (onCreate) et au retour sur l'activité (onResume).
     */
    private void chargerPanier() {
        // Récupérer les films depuis SQLite via le PanierManager
        films = panierManager.obtenirFilms();

        // Mettre à jour le texte compteur
        tvNombreFilms.setText(films.size() + " film(s)");

        // Créer l'adapter avec un listener de suppression
        // OnFilmSupprimerListener est une interface de PanierAdapter :
        // elle est implémentée ici avec une expression lambda
        adapter = new PanierAdapter(this, films, (film, position) -> {
            // Afficher un dialogue de confirmation avant de supprimer
            afficherDialogueConfirmationSupprimer(film, position);
        });

        // Connecter l'adapter à la ListView
        listePanier.setAdapter(adapter);
    }

    /**
     * Affiche une boîte de dialogue de confirmation avant de supprimer un film.
     * AlertDialog.Builder permet de construire une fenêtre de dialogue native Android.
     *
     * @param film     Le film à supprimer
     * @param position Sa position dans la liste (pour la supprimer visuellement)
     */
    private void afficherDialogueConfirmationSupprimer(Film film, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer du panier")
                .setMessage("Voulez-vous supprimer \"" + film.getTitre() + "\" du panier ?")
                // Bouton de confirmation : exécute la suppression
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Supprimer de la base de données SQLite
                    boolean supprime = panierManager.supprimerFilm(film.getId());

                    if (supprime) {
                        // Supprimer aussi de la liste locale en mémoire
                        films.remove(position);
                        // Notifier l'adapter pour rafraîchir l'affichage de la ListView
                        adapter.notifyDataSetChanged();
                        // Mettre à jour le compteur
                        tvNombreFilms.setText(films.size() + " film(s)");
                        Toast.makeText(this, "Film supprimé du panier", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                // Bouton d'annulation : ne fait rien (null = ferme juste le dialogue)
                .setNegativeButton("Non", null)
                .show();
    }

    /**
     * Affiche une boîte de dialogue de confirmation avant de vider tout le panier.
     */
    private void afficherDialogueConfirmationVider() {
        new AlertDialog.Builder(this)
                .setTitle("Vider le panier")
                .setMessage("Voulez-vous supprimer tous les films du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Vider la base de données SQLite complètement
                    panierManager.viderPanier();

                    // Vider aussi la liste locale en mémoire
                    films.clear();
                    // Rafraîchir l'affichage
                    adapter.notifyDataSetChanged();
                    tvNombreFilms.setText("0 film(s)");
                    Toast.makeText(this, "Panier vidé", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    /**
     * Valide le panier : finalise les locations auprès du serveur.
     * Déclarée dans activity_panier.xml via android:onClick="validerPanier".
     *
     * Processus :
     * 1. Vérifie que le panier n'est pas vide
     * 2. Demande confirmation à l'utilisateur
     * 3. Lance CheckoutTask qui appelle POST /cart/checkout
     * 4. En cas de succès : vide le panier local et affiche un message
     *
     * @param view La vue (bouton "Valider le panier") qui a déclenché l'événement
     */
    public void validerPanier(View view) {
        if (films.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show();
            return;
        }

        // Afficher un dialogue de confirmation avec le nombre de films
        new AlertDialog.Builder(this)
                .setTitle("Valider le panier")
                .setMessage("Confirmer la validation de " + films.size() + " film(s) ?")
                .setPositiveButton("Valider", (dialog, which) -> {
                    // Récupérer le customerId depuis le TokenManager
                    TokenManager tokenManager = TokenManager.getInstance(this);
                    Integer customerId = tokenManager.getCustomerId();

                    if (customerId == null) {
                        Toast.makeText(this, "Erreur: Customer ID non trouvé", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Appeler l'API pour valider le panier (change status_id de 2 à 3)
                    // CheckoutTask.execute(customerId) passe le customerId à doInBackground()
                    new CheckoutTask(this, new CheckoutTask.CheckoutCallback() {

                        /**
                         * Callback de succès : le serveur a confirmé la location de tous les films.
                         * @param itemsCount Nombre de films validés (retourné par l'API)
                         */
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

                        /**
                         * Callback d'erreur : le serveur a refusé ou une erreur réseau est survenue.
                         * @param errorMessage Le message d'erreur à afficher
                         */
                        @Override
                        public void onCheckoutError(String errorMessage) {
                            Toast.makeText(PanierActivity.this,
                                "Erreur: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        }
                    }).execute(customerId);  // On passe customerId en paramètre à execute()
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
