package com.example.applicationrftg;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class FilmAdapter extends ArrayAdapter<Film> {

    private Context context;
    private List<Film> films;

    public FilmAdapter(Context context, List<Film> films) {
        super(context, R.layout.item_film, films);
        this.context = context;
        this.films = films;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupérer le film à la position actuelle
        Film film = films.get(position);

        // Réutiliser la vue si elle existe déjà (optimisation)
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_film, parent, false);
        }

        // Récupérer les éléments du layout
        TextView tvTitre = convertView.findViewById(R.id.tvTitreFilm);
        TextView tvRealisateur = convertView.findViewById(R.id.tvRealisateurFilm);
        TextView tvAnnee = convertView.findViewById(R.id.tvAnneeFilm);
        Button btnAjouterPanier = convertView.findViewById(R.id.btnAjouterPanierItem);

        // Remplir les TextViews avec les données du film
        tvTitre.setText(film.getTitre());
        tvRealisateur.setText(film.getRealisateur());
        tvAnnee.setText(String.valueOf(film.getAnnee()));

        // Gérer le clic sur le bouton Ajouter au panier
        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("FilmAdapter", ">>> Bouton Ajouter au panier cliqué pour: " + film.getTitre());

            // Récupérer le customerId
            TokenManager tokenManager = TokenManager.getInstance(context);
            Integer customerId = tokenManager.getCustomerId();

            android.util.Log.d("FilmAdapter", ">>> CustomerId: " + customerId + ", FilmId: " + film.getId());

            if (customerId == null) {
                Toast.makeText(context, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                return;
            }

            // Appeler l'API pour ajouter au panier (crée rental avec status_id = 2)
            new AddToCartTask(context, customerId, film.getId(), new AddToCartTask.AddToCartCallback() {
                @Override
                public void onAddToCartSuccess() {
                    // Ajouter aussi au panier local pour l'affichage
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

        // S'assurer que le bouton ne bloque pas le clic sur l'item
        btnAjouterPanier.setFocusable(false);
        btnAjouterPanier.setFocusableInTouchMode(false);

        return convertView;
    }
}