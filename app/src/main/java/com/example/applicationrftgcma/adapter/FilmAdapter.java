package com.example.applicationrftgcma.adapter;

/**
 * RÔLE DE CE FICHIER :
 * Adaptateur entre la liste de films (données) et la ListView de ListefilmsActivity (affichage).
 * Hérite de ArrayAdapter<Film> : Android appelle getView() pour chaque ligne visible.
 *
 * Chaque ligne affiche :
 *   - Le titre du film
 *   - Le nom du premier réalisateur
 *   - L'année de sortie
 *   - Un bouton "Ajouter au panier" qui déclenche un appel API (AddToCartTask)
 *     puis enregistre le film dans SQLite (PanierManager)
 *
 * Différence avec PanierAdapter : ici on est dans la liste de tous les films,
 * donc le bouton ajoute au panier (et non supprime).
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.manager.PanierManager;
import com.example.applicationrftgcma.task.AddToCartTask;

import org.json.JSONException;
import org.json.JSONObject;

public class FilmAdapter extends ArrayAdapter<Film> {

    // Contexte Android nécessaire pour inflater les vues et accéder aux ressources
    private Context context;

    // Référence à la liste source des films — utilisée pour accéder aux données par position
    private List<Film> films;

    /**
     * Constructeur — initialise l'adapter avec la liste des films à afficher.
     *
     * @param context Le contexte Android (l'activité qui crée l'adapter)
     * @param films   La liste des films à afficher dans la ListView
     */
    public FilmAdapter(Context context, List<Film> films) {
        // item_film est le layout XML utilisé pour chaque ligne de la liste
        super(context, R.layout.item_film, films);
        this.context = context;
        this.films = films;
    }

    /**
     * Méthode principale appelée par Android pour chaque item visible dans la ListView.
     * Crée ou réutilise une vue et y injecte les données du film à la position donnée.
     *
     * @param position    L'index du film dans la liste
     * @param convertView Vue recyclée depuis un item précédent (null si première création)
     * @param parent      Le ViewGroup parent (la ListView)
     * @return La vue remplie avec les données du film
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupérer le film à la position actuelle
        Film film = films.get(position);

        // Optimisation : réutiliser une vue déjà créée plutôt que d'en instancier une nouvelle
        // (évite les allocations mémoire répétées lors du scroll)
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_film, parent, false);
        }

        // Récupérer les widgets du layout item_film.xml
        TextView tvTitre = convertView.findViewById(R.id.tvTitreFilm);
        TextView tvRealisateur = convertView.findViewById(R.id.tvRealisateurFilm);
        TextView tvAnnee = convertView.findViewById(R.id.tvAnneeFilm);
        Button btnAjouterPanier = convertView.findViewById(R.id.btnAjouterPanierItem);

        // Remplir les TextViews avec les données du film
        tvTitre.setText(film.getTitre());
        tvRealisateur.setText(film.getRealisateur());
        tvAnnee.setText(String.valueOf(film.getAnnee()));

        // Clic sur "Ajouter au panier" : appel API puis enregistrement local
        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("FilmAdapter", ">>> Bouton Ajouter au panier cliqué pour: " + film.getTitre());

            // Récupérer le customerId depuis la session (nécessaire pour l'API)
            TokenManager tokenManager = TokenManager.getInstance(context);
            Integer customerId = tokenManager.getCustomerId();

            android.util.Log.d("FilmAdapter", ">>> CustomerId: " + customerId + ", FilmId: " + film.getId());

            // Vérification de sécurité : l'utilisateur doit être connecté
            if (customerId == null) {
                Toast.makeText(context, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("customerId", customerId);
                requestBody.put("filmId", film.getId());
            } catch (JSONException e) {
                Toast.makeText(this.getContext(), "Erreur interne", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lancer l'appel API en arrière-plan (crée un rental avec status_id = 2 = "dans le panier")
            new AddToCartTask(context, requestBody, new AddToCartTask.AddToCartCallback() {
                @Override
                public void onAddToCartSuccess(int rentalId) {
                    // Stocker le rentalId sur le film avant de l'insérer en SQLite
                    // (nécessaire pour appeler DELETE /cart/{rentalId} lors de la suppression)
                    film.setRentalId(rentalId);

                    PanierManager panierManager = PanierManager.getInstance(context);
                    panierManager.ajouterFilm(film);

                    Toast.makeText(context, "Film ajouté au panier", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAddToCartError(String errorMessage) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                }
            }).execute();
            android.util.Log.d("FilmAdapter", ">>> Après execute AddToCartTask");
        });

        // Important : désactiver le focus du bouton pour que le clic sur la ligne entière
        // reste fonctionnel (sinon le bouton "capte" le focus et bloque le setOnItemClickListener)
        btnAjouterPanier.setFocusable(false);
        btnAjouterPanier.setFocusableInTouchMode(false);

        return convertView;
    }
}