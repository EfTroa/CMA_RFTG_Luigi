package com.example.applicationrftgcma;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * ADAPTATEUR : FilmAdapter
 *
 * Un adaptateur (Adapter) fait le lien entre une source de données et un composant d'affichage.
 * Ici FilmAdapter connecte une List<Film> à une ListView dans ListefilmsActivity.
 *
 * Il hérite de ArrayAdapter<Film> qui fournit les fonctionnalités de base
 * (ajout, suppression, gestion du nombre d'items, etc.).
 *
 * La méthode clé à implémenter est getView() : elle est appelée par Android
 * pour chaque item visible dans la liste. Elle crée ou réutilise une vue
 * et la remplit avec les données du film correspondant.
 *
 * Pattern ViewHolder (simplifié ici) :
 * convertView == null signifie qu'il n'y a pas de vue disponible à recycler.
 * Android recycle les vues scrollées hors de l'écran pour économiser la mémoire.
 */
public class FilmAdapter extends ArrayAdapter<Film> {

    // Contexte de l'activité (nécessaire pour accéder aux ressources et au LayoutInflater)
    private Context context;

    // La liste des films à afficher
    private List<Film> films;

    /**
     * Constructeur de l'adaptateur.
     *
     * @param context L'activité qui utilise cet adaptateur (ListefilmsActivity)
     * @param films   La liste des films à afficher
     */
    public FilmAdapter(Context context, List<Film> films) {
        // Appel du constructeur parent avec le layout d'un item et la liste de données
        super(context, R.layout.item_film, films);
        this.context = context;
        this.films = films;
    }

    /**
     * Crée ou réutilise la vue d'un item de la liste.
     * Appelée par Android pour chaque item visible dans la ListView.
     *
     * @param position    L'indice du film dans la liste (0 pour le premier)
     * @param convertView Une ancienne vue disponible pour être réutilisée (peut être null)
     * @param parent      Le ViewGroup parent (la ListView)
     * @return            La vue remplie avec les données du film à afficher
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupérer le film à la position actuelle
        Film film = films.get(position);

        // Réutiliser la vue si elle existe déjà (optimisation mémoire)
        // Android recycle les vues des items sortis de l'écran.
        // Si convertView est null, aucune vue n'est disponible : on l'infle depuis le XML.
        if (convertView == null) {
            // LayoutInflater "gonfle" (instancie) le layout XML en objet View Java
            LayoutInflater inflater = LayoutInflater.from(context);
            // false = ne pas attacher immédiatement au parent (Android s'en charge)
            convertView = inflater.inflate(R.layout.item_film, parent, false);
        }

        // Récupérer les éléments du layout item_film.xml
        TextView tvTitre = convertView.findViewById(R.id.tvTitreFilm);
        TextView tvRealisateur = convertView.findViewById(R.id.tvRealisateurFilm);
        TextView tvAnnee = convertView.findViewById(R.id.tvAnneeFilm);
        Button btnAjouterPanier = convertView.findViewById(R.id.btnAjouterPanierItem);

        // Remplir les TextViews avec les données du film
        tvTitre.setText(film.getTitre());
        tvRealisateur.setText(film.getRealisateur());
        // String.valueOf() convertit l'int (année) en String pour setText()
        tvAnnee.setText(String.valueOf(film.getAnnee()));

        // Gérer le clic sur le bouton "Ajouter au panier" de chaque item
        // ATTENTION : ce listener est ré-assigné à chaque appel de getView()
        // Il capture "film" par variable effectivement finale (Java 8+)
        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("FilmAdapter", ">>> Bouton Ajouter au panier cliqué pour: " + film.getTitre());

            // Récupérer le customerId depuis le TokenManager (singleton)
            TokenManager tokenManager = TokenManager.getInstance(context);
            Integer customerId = tokenManager.getCustomerId();

            android.util.Log.d("FilmAdapter", ">>> CustomerId: " + customerId + ", FilmId: " + film.getId());

            if (customerId == null) {
                Toast.makeText(context, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                return;
            }

            // Appeler l'API pour ajouter au panier (crée rental avec status_id = 2)
            // Même logique que dans DetailfilmActivity, mais depuis la liste
            new AddToCartTask(context, customerId, film.getId(), new AddToCartTask.AddToCartCallback() {

                /**
                 * Callback de succès : on stocke aussi le film en SQLite local
                 */
                @Override
                public void onAddToCartSuccess() {
                    // Ajouter aussi au panier local pour l'affichage dans PanierActivity
                    PanierManager panierManager = PanierManager.getInstance(context);
                    panierManager.ajouterFilm(film);

                    Toast.makeText(context, "Film ajouté au panier", Toast.LENGTH_SHORT).show();
                }

                /**
                 * Callback d'erreur : afficher le message d'erreur retourné par l'API
                 */
                @Override
                public void onAddToCartError(String errorMessage) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                }
            }).execute();

            android.util.Log.d("FilmAdapter", ">>> Après execute AddToCartTask");
        });

        // S'assurer que le bouton ne capte pas le focus et ne bloque pas le clic sur l'item
        // Par défaut un Button prend le focus, ce qui empêche le clic sur l'item de la ListView
        // setFocusable(false) délègue le focus à l'item parent (la ligne de la ListView)
        btnAjouterPanier.setFocusable(false);
        btnAjouterPanier.setFocusableInTouchMode(false);

        return convertView;
    }
}
