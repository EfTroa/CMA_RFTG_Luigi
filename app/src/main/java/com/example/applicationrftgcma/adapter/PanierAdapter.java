package com.example.applicationrftgcma.adapter;

/**
 * RÔLE DE CE FICHIER :
 * Adaptateur entre la liste de films du panier (données) et la ListView (affichage).
 * Similaire à FilmAdapter, mais adapté pour l'affichage dans le panier :
 *   - Layout différent (item_panier.xml) avec un bouton "Supprimer"
 *   - Pas de bouton "Ajouter au panier" (on est déjà dans le panier)
 *   - La suppression est notifiée à PanierActivity via l'interface OnFilmSupprimerListener
 *     (pattern callback / délégation) pour que ce soit l'activité qui gère la confirmation
 *     et la mise à jour de la base de données
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.model.Film;

public class PanierAdapter extends ArrayAdapter<Film> {

    private Context context;
    private List<Film> films; // La liste des films dans le panier
    private OnFilmSupprimerListener listener; // Callback vers PanierActivity pour la suppression

    /**
     * Interface de callback pour notifier PanierActivity qu'un utilisateur
     * a cliqué sur le bouton "Supprimer" d'un item.
     * PanierActivity affichera une boîte de dialogue de confirmation avant suppression réelle.
     */
    public interface OnFilmSupprimerListener {
        // Appelé quand l'utilisateur clique sur le bouton Supprimer d'un item
        void onFilmSupprimer(Film film, int position);
    }

    /**
     * Constructeur — reçoit le contexte, la liste des films et le listener de suppression.
     *
     * @param context  Le contexte Android (pour inflater les vues)
     * @param films    La liste des films dans le panier
     * @param listener Le callback à appeler lors d'un clic sur "Supprimer"
     */
    public PanierAdapter(Context context, List<Film> films, OnFilmSupprimerListener listener) {
        super(context, 0, films); // 0 = pas de layout par défaut, on gère nous-mêmes dans getView
        this.context  = context;
        this.films    = films;
        this.listener = listener;
    }

    /**
     * Méthode principale appelée par Android pour chaque item visible dans la ListView.
     * Crée ou réutilise une vue et y injecte les données du film à la position donnée.
     *
     * @param position    L'index du film dans la liste du panier
     * @param convertView Vue recyclée depuis un item précédent (null si première création)
     * @param parent      Le ViewGroup parent (la ListView)
     * @return La vue remplie avec les données du film
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupérer le film à la position donnée via getItem() (méthode d'ArrayAdapter)
        Film film = getItem(position);

        // Optimisation : réutiliser la vue si elle a déjà été créée pour une autre position
        if (convertView == null) {
            // Inflater le layout item_panier.xml pour créer une nouvelle vue
            convertView = LayoutInflater.from(context).inflate(R.layout.item_panier, parent, false);
        }

        // Récupérer les widgets du layout item_panier.xml
        TextView tvTitreFilmPanier = convertView.findViewById(R.id.tvTitreFilmPanier);
        TextView tvInfoFilmPanier  = convertView.findViewById(R.id.tvInfoFilmPanier);
        Button   btnSupprimerFilm  = convertView.findViewById(R.id.btnSupprimerFilm);

        if (film != null) {
            // Afficher le titre du film
            tvTitreFilmPanier.setText(film.getTitre());

            // Afficher les métadonnées sur une seule ligne : "2006 • 1h54 • PG-13"
            String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
            tvInfoFilmPanier.setText(infos);

            // Clic sur "Supprimer" : notifier PanierActivity via le listener
            // PanierActivity affichera une boîte de dialogue de confirmation avant suppression
            btnSupprimerFilm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFilmSupprimer(film, position);
                }
            });
        }

        return convertView;
    }
}
