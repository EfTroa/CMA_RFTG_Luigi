package com.example.applicationrftgcma.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.adapter.FilmAdapter;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.task.ListefilmsTask;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RÔLE DE CE FICHIER :
 * Écran principal affichant la liste de tous les films disponibles à la location.
 * Récupère les films via l'API REST (GET /films) grâce à ListefilmsTask,
 * puis les affiche dans une ListView avec FilmAdapter.
 *
 * Fonctionnalités :
 *   - Recherche en temps réel par titre ou réalisateur (TextWatcher)
 *   - Filtres par catégorie et par année (deux Spinners dans un panneau rétractable)
 *   - Bouton "Ajouter au panier" sur chaque item (délégué à FilmAdapter)
 *   - Navigation vers DetailfilmActivity au clic sur un film
 *   - Navigation vers PanierActivity via le bouton panier
 *
 * Architecture :
 *   - tousLesFilms   : liste complète reçue de l'API (jamais modifiée après chargement)
 *   - filmsAffiches  : sous-ensemble de tousLesFilms après application des filtres
 *   Cette séparation permet de réinitialiser les filtres sans rappeler l'API.
 */
public class ListefilmsActivity extends AppCompatActivity {

    // Barre de progression circulaire affichée pendant le chargement
    private ProgressBar progressBar;

    // Overlay semi-transparent qui couvre l'écran pendant le chargement (contient progressBar)
    private View loadingOverlay;

    // Dernière réponse JSON brute reçue de l'API (conservée pour référence)
    private String listeFilmsResultat = "";

    // ListView qui affiche les films via FilmAdapter
    private ListView listeFilms;

    // Panneau de filtres (catégorie + année) — affiché/masqué par toggleFilterPanel()
    private ConstraintLayout filterPanel;

    // État de visibilité du panneau de filtres (true = visible)
    private boolean isFilterVisible = false;

    // Liste complète de tous les films reçus de l'API — jamais filtrée directement
    private List<Film> tousLesFilms = new ArrayList<>();

    // Sous-ensemble de tousLesFilms correspondant aux filtres actifs — affiché dans la ListView
    private List<Film> filmsAffiches = new ArrayList<>();

    // Adapter qui fait le lien entre filmsAffiches et la ListView
    private FilmAdapter adapter;

    // Champ de recherche textuelle (filtre par titre et réalisateur)
    private EditText searchBar;

    // Spinner de sélection de catégorie (ex: "Action", "Comedy"…)
    private Spinner spinnerCategorie;

    // Spinner de sélection d'année de sortie
    private Spinner spinnerAnnee;

    // Valeur de la catégorie actuellement sélectionnée ("Toutes" = pas de filtre)
    private String categorieSelectionnee = "Toutes";

    // Valeur de l'année actuellement sélectionnée ("Toutes" = pas de filtre)
    private String anneeSelectionnee = "Toutes";

    /**
     * Méthode du cycle de vie Android appelée à la création de l'activité.
     * Initialise les vues, configure les listeners, et lance le chargement des films.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilms);

        // Récupérer les références de toutes les vues du layout
        listeFilms = findViewById(R.id.listeFilms);
        progressBar = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        filterPanel = findViewById(R.id.filterPanel);
        searchBar = findViewById(R.id.searchBar);
        spinnerCategorie = findViewById(R.id.spinnerCategorie);
        spinnerAnnee = findViewById(R.id.spinnerAnnee);
        Button btnFiltre = findViewById(R.id.btnFiltre);
        Button btnResetFilters = findViewById(R.id.btnResetFilters);

        // Workaround Android : le champ de recherche est initialement non-focusable
        // pour éviter qu'il capture le focus au lancement. On l'active au toucher.
        searchBar.setOnTouchListener((v, event) -> {
            searchBar.setFocusable(true);
            searchBar.setFocusableInTouchMode(true);
            return false;
        });

        // TextWatcher : déclenche filtrerFilms() à chaque modification du champ de recherche
        // onTextChanged est appelé après chaque frappe — les deux autres méthodes ne sont pas utilisées
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrerFilms();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Bouton "Filtre" : affiche ou masque le panneau de filtres
        btnFiltre.setOnClickListener(v -> toggleFilterPanel());

        // Bouton "Réinitialiser" : remet tous les filtres à leur valeur par défaut
        btnResetFilters.setOnClickListener(v -> reinitialiserFiltres());

        // Lancer le chargement asynchrone de la liste des films depuis l'API
        // L'URL est construite directement dans ListefilmsTask.doInBackground() via UrlManager,
        // donc execute() n'a pas besoin de paramètre (AsyncTask<Void, Void, String>)
        new ListefilmsTask(this).execute();
    }

    /**
     * Bascule la visibilité du panneau de filtres (catégorie + année).
     * Utilisé par le bouton "Filtre" dans le layout.
     */
    private void toggleFilterPanel() {
        if (isFilterVisible) {
            filterPanel.setVisibility(View.GONE);
            isFilterVisible = false;
        } else {
            filterPanel.setVisibility(View.VISIBLE);
            isFilterVisible = true;
        }
    }

    /**
     * Affiche ou masque l'overlay de chargement et la ProgressBar.
     * Appelé par ListefilmsTask.onPreExecute() (true) et onPostExecute() (false).
     *
     * @param visible true pour afficher, false pour masquer
     */
    public void showProgressBar(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        loadingOverlay.setVisibility(visibility);
        progressBar.setVisibility(visibility);
    }

    /**
     * Callback appelé par ListefilmsTask.onPostExecute() avec le JSON reçu de l'API.
     * Sauvegarde le résultat brut puis délègue l'affichage à afficherListeFilms().
     *
     * @param resultatAppelRest Le JSON brut retourné par l'API REST
     */
    public void mettreAJourActivityApresAppelRest(String resultatAppelRest) {
        listeFilmsResultat = resultatAppelRest;
        Log.d("mydebug", ">>>Pour ListefilmsActivity - mettreAJourActivityApresAppelRest=" + listeFilmsResultat);
        afficherListeFilms(listeFilmsResultat);
    }

    /**
     * Parse le JSON de la liste des films et configure la ListView avec FilmAdapter.
     * Initialise aussi les spinners de filtres une fois les films chargés.
     *
     * @param resultat Le JSON brut contenant le tableau de films
     */
    public void afficherListeFilms(String resultat) {
        TextView tvHeader = findViewById(R.id.tvTitre);

        if (resultat == null || resultat.isEmpty()) {
            tvHeader.setText("Aucun résultat reçu");
            Toast.makeText(this, "Aucune donnée reçue de l'API", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Désérialiser le JSON en List<Film> grâce à Gson et à son TypeToken
            // TypeToken est nécessaire car Java efface les génériques à l'exécution (type erasure)
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Film>>(){}.getType();
            List<Film> films = gson.fromJson(resultat, listType);

            if (films != null && !films.isEmpty()) {
                // Stocker la liste complète (référence de base pour les filtres)
                tousLesFilms = new ArrayList<>(films);
                filmsAffiches = new ArrayList<>(films);

                tvHeader.setText("Liste des films");

                // Créer l'adapter et le connecter à la ListView
                adapter = new FilmAdapter(this, filmsAffiches);
                listeFilms.setAdapter(adapter);

                // Clic sur un film → ouvrir la page de détail
                listeFilms.setOnItemClickListener((parent, view, position, id) -> {
                    Film filmSelectionne = filmsAffiches.get(position);
                    ouvrirPageDetailfilm(filmSelectionne);
                });

                // Peupler les spinners de filtres avec les valeurs extraites des films chargés
                initialiserSpinners();

                Log.d("mydebug", ">>>Films affichés avec succès : " + films.size() + " films");
            } else {
                tvHeader.setText("Aucun film trouvé");
                Toast.makeText(this, "La liste des films est vide", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            tvHeader.setText("Erreur lors du parsing des données");
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("mydebug", ">>>Erreur parsing JSON : " + e.getMessage());
        }
    }

    /**
     * Applique les trois filtres actifs (texte, catégorie, année) sur tousLesFilms
     * et met à jour filmsAffiches + l'adapter.
     * Appelée à chaque changement de recherche ou de spinner.
     */
    private void filtrerFilms() {
        String query = searchBar.getText().toString().toLowerCase().trim();

        // Construire une nouvelle liste filtrée à partir de tousLesFilms
        List<Film> filmsTemporaires = new ArrayList<>();

        for (Film film : tousLesFilms) {
            boolean inclure = true;

            // Filtre texte : le titre ou le réalisateur doit contenir la saisie
            if (!query.isEmpty()) {
                boolean correspondRecherche = film.getTitre().toLowerCase().contains(query) ||
                        film.getRealisateur().toLowerCase().contains(query);
                if (!correspondRecherche) {
                    inclure = false;
                }
            }

            // Filtre catégorie : au moins une catégorie du film doit correspondre
            if (inclure && !categorieSelectionnee.equals("Toutes")) {
                boolean correspondCategorie = false;
                if (film.getCategories() != null) {
                    for (Film.Category cat : film.getCategories()) {
                        if (cat.getName().equals(categorieSelectionnee)) {
                            correspondCategorie = true;
                            break; // Inutile de continuer dès qu'une catégorie correspond
                        }
                    }
                }
                if (!correspondCategorie) {
                    inclure = false;
                }
            }

            // Filtre année : l'année du film doit correspondre exactement
            if (inclure && !anneeSelectionnee.equals("Toutes")) {
                int annee = Integer.parseInt(anneeSelectionnee);
                if (film.getAnnee() != annee) {
                    inclure = false;
                }
            }

            if (inclure) {
                filmsTemporaires.add(film);
            }
        }

        // Remplacer la liste affichée et notifier l'adapter pour rafraîchir la ListView
        filmsAffiches = filmsTemporaires;
        if (adapter != null) {
            adapter.clear();
            adapter.addAll(filmsAffiches);
            adapter.notifyDataSetChanged();
        }

        TextView tvHeader = findViewById(R.id.tvTitre);
        tvHeader.setText("Films");
    }

    /**
     * Peuple les deux Spinners (catégorie et année) avec les valeurs extraites des films chargés.
     * Les valeurs sont dédupliquées via un Set, puis triées :
     *   - Catégories : ordre alphabétique
     *   - Années     : ordre décroissant (plus récent en premier), "Toutes" toujours en tête
     */
    private void initialiserSpinners() {
        // --- Spinner des catégories ---
        // HashSet pour dédupliquer (un film peut avoir plusieurs catégories identiques à celles d'autres)
        Set<String> categories = new HashSet<>();
        categories.add("Toutes"); // Option par défaut = pas de filtre
        for (Film film : tousLesFilms) {
            if (film.getCategories() != null) {
                for (Film.Category cat : film.getCategories()) {
                    categories.add(cat.getName());
                }
            }
        }
        List<String> listeCategories = new ArrayList<>(categories);
        java.util.Collections.sort(listeCategories); // Ordre alphabétique

        ArrayAdapter<String> adapterCategorie = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeCategories);
        adapterCategorie.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorie.setAdapter(adapterCategorie);

        spinnerCategorie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categorieSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms(); // Relancer le filtre à chaque changement
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // --- Spinner des années ---
        Set<String> annees = new HashSet<>();
        annees.add("Toutes");
        for (Film film : tousLesFilms) {
            annees.add(String.valueOf(film.getAnnee()));
        }
        List<String> listeAnnees = new ArrayList<>(annees);
        // Tri personnalisé : "Toutes" en tête, puis années en ordre décroissant
        java.util.Collections.sort(listeAnnees, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                if (a.equals("Toutes")) return -1;
                if (b.equals("Toutes")) return 1;
                return b.compareTo(a); // Ordre décroissant
            }
        });

        ArrayAdapter<String> adapterAnnee = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeAnnees);
        adapterAnnee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnnee.setAdapter(adapterAnnee);

        spinnerAnnee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                anneeSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms(); // Relancer le filtre à chaque changement
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Remet tous les filtres à leur valeur par défaut et rafraîchit la liste.
     * Appelée par le bouton "Réinitialiser les filtres".
     */
    private void reinitialiserFiltres() {
        searchBar.setText("");
        spinnerCategorie.setSelection(0); // "Toutes" est toujours en position 0
        spinnerAnnee.setSelection(0);
        categorieSelectionnee = "Toutes";
        anneeSelectionnee = "Toutes";
        filtrerFilms();
    }

    /**
     * Ferme toutes les activités de la pile et quitte l'application.
     * Déclenché par le bouton "Quitter" (android:onClick dans le layout).
     *
     * @param view La vue du bouton (non utilisée)
     */
    public void quitterApplication(View view) {
        finishAffinity();
    }

    /**
     * Ouvre PanierActivity.
     * Déclenché par le bouton "Panier" (android:onClick dans le layout).
     *
     * @param view La vue du bouton (non utilisée)
     */
    public void ouvrirPagePanier(View view) {
        Intent intent = new Intent(this, PanierActivity.class);
        startActivity(intent);
    }

    /**
     * Ouvre DetailfilmActivity en passant le film sélectionné sérialisé en JSON.
     * Le film est passé via Intent.putExtra() car Film n'implémente pas Parcelable.
     *
     * @param film Le film sélectionné dans la ListView
     */
    public void ouvrirPageDetailfilm(Film film) {
        Intent intent = new Intent(this, DetailfilmActivity.class);

        // Sérialiser l'objet Film en JSON pour le passer via l'Intent
        // (alternative à Parcelable/Serializable — plus simple avec Gson déjà présent)
        Gson gson = new Gson();
        String filmJson = gson.toJson(film);
        intent.putExtra("filmJson", filmJson);

        startActivity(intent);
    }
}
