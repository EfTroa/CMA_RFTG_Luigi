package com.example.applicationrftgcma;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * ADAPTATEUR : PanierAdapter
 *
 * Connecte la liste de films du panier (List<Film>) à la ListView de PanierActivity.
 * Fonctionne sur le même principe que FilmAdapter (héritage de ArrayAdapter<Film>).
 *
 * Différences par rapport à FilmAdapter :
 *  - Affiche des infos différentes (durée, classification au lieu du réalisateur)
 *  - Dispose d'un bouton "Supprimer" au lieu de "Ajouter au panier"
 *  - Utilise le pattern "listener/callback" via l'interface OnFilmSupprimerListener
 *    pour déléguer la suppression à l'activité parente (PanierActivity)
 *
 * Pattern Listener (interface de callback) :
 * L'adaptateur ne sait pas comment supprimer un film (il ne connaît pas la logique métier).
 * Il délègue cette responsabilité à l'activité via l'interface OnFilmSupprimerListener.
 * C'est une bonne pratique de séparation des responsabilités.
 */
public class PanierAdapter extends ArrayAdapter<Film> {

    // Contexte de l'activité parente (PanierActivity)
    private Context context;

    // La liste des films à afficher dans le panier
    private List<Film> films;

    // Le listener de suppression (implémenté par PanierActivity)
    private OnFilmSupprimerListener listener;

    /**
     * INTERFACE : OnFilmSupprimerListener
     *
     * Une interface définit un "contrat" : toute classe qui l'implémente
     * DOIT fournir une implémentation de la méthode onFilmSupprimer().
     *
     * PanierActivity implémente cette interface (via une lambda) pour
     * afficher le dialogue de confirmation et effectuer la suppression.
     */
    public interface OnFilmSupprimerListener {
        /**
         * Appelée quand l'utilisateur clique sur "Supprimer" pour un film.
         * @param film     Le film à supprimer
         * @param position Sa position dans la liste (nécessaire pour la supprimer visuellement)
         */
        void onFilmSupprimer(Film film, int position);
    }

    /**
     * Constructeur de l'adaptateur.
     *
     * @param context  L'activité parente (PanierActivity)
     * @param films    La liste des films dans le panier
     * @param listener Le gestionnaire de suppression (implémenté par PanierActivity)
     */
    public PanierAdapter(Context context, List<Film> films, OnFilmSupprimerListener listener) {
        // super(context, 0, films) : 0 = pas de layout par défaut, on le fournit dans getView()
        super(context, 0, films);
        this.context = context;
        this.films = films;
        this.listener = listener;
    }

    /**
     * Crée ou réutilise la vue d'un item du panier.
     * Appelée par Android pour chaque item visible dans la ListView.
     *
     * @param position    L'indice du film dans la liste
     * @param convertView Une ancienne vue recyclable (peut être null)
     * @param parent      La ListView parente
     * @return            La vue remplie avec les données du film
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupérer le film à afficher à cette position
        // getItem() est une méthode de ArrayAdapter (accède à l'item via l'adaptateur)
        Film film = getItem(position);

        // Vérifier si la vue est réutilisée, sinon on l'infle depuis item_panier.xml
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_panier, parent, false);
        }

        // Récupérer les éléments de la vue item_panier.xml
        TextView tvTitreFilmPanier = convertView.findViewById(R.id.tvTitreFilmPanier);
        TextView tvInfoFilmPanier = convertView.findViewById(R.id.tvInfoFilmPanier);
        Button btnSupprimerFilm = convertView.findViewById(R.id.btnSupprimerFilm);

        // Remplir les données (film != null pour éviter un NullPointerException)
        if (film != null) {
            // Afficher le titre du film
            tvTitreFilmPanier.setText(film.getTitre());

            // Afficher les infos résumées : année, durée formatée, classification
            // Ex: "2006 • 1h26 • PG"
            String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
            tvInfoFilmPanier.setText(infos);

            // Gérer le clic sur le bouton Supprimer
            // On délègue la gestion au listener (PanierActivity)
            // afin que l'adapter ne gère pas directement la base de données
            btnSupprimerFilm.setOnClickListener(v -> {
                if (listener != null) {
                    // Appelle onFilmSupprimer() dans PanierActivity
                    listener.onFilmSupprimer(film, position);
                }
            });
        }

        return convertView;
    }
}
