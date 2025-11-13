package com.example.applicationrftg;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class PanierAdapter extends ArrayAdapter<Film> {

    private Context context;
    private List<Film> films;
    private OnFilmSupprimerListener listener;

    // Interface pour gérer la suppression d'un film
    public interface OnFilmSupprimerListener {
        void onFilmSupprimer(Film film, int position);
    }

    public PanierAdapter(Context context, List<Film> films, OnFilmSupprimerListener listener) {
        super(context, 0, films);
        this.context = context;
        this.films = films;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupérer le film à afficher
        Film film = getItem(position);

        // Vérifier si la vue est réutilisée, sinon on l'infle
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_panier, parent, false);
        }

        // Récupérer les éléments de la vue
        TextView tvTitreFilmPanier = convertView.findViewById(R.id.tvTitreFilmPanier);
        TextView tvInfoFilmPanier = convertView.findViewById(R.id.tvInfoFilmPanier);
        Button btnSupprimerFilm = convertView.findViewById(R.id.btnSupprimerFilm);

        // Remplir les données
        if (film != null) {
            tvTitreFilmPanier.setText(film.getTitre());

            // Afficher les infos (année, durée, classification)
            String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
            tvInfoFilmPanier.setText(infos);

            // Gérer le clic sur le bouton Supprimer
            btnSupprimerFilm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFilmSupprimer(film, position);
                }
            });
        }

        return convertView;
    }
}