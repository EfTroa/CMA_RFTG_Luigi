package com.example.applicationrftgcma;

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
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListefilmsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private String listeFilmsResultat = "";
    private ListView listeFilms;
    private ConstraintLayout filterPanel;
    private boolean isFilterVisible = false;

    private List<Film> tousLesFilms = new ArrayList<>();
    private List<Film> filmsAffiches = new ArrayList<>();
    private FilmAdapter adapter;
    private EditText searchBar;
    private Spinner spinnerCategorie;
    private Spinner spinnerAnnee;
    private String categorieSelectionnee = "Toutes";
    private String anneeSelectionnee = "Toutes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilms);

        listeFilms = findViewById(R.id.listeFilms);
        progressBar = findViewById(R.id.progressBar);
        filterPanel = findViewById(R.id.filterPanel);
        searchBar = findViewById(R.id.searchBar);
        spinnerCategorie = findViewById(R.id.spinnerCategorie);
        spinnerAnnee = findViewById(R.id.spinnerAnnee);
        Button btnFiltre = findViewById(R.id.btnFiltre);
        Button btnResetFilters = findViewById(R.id.btnResetFilters);

        // Activer le champ de recherche au toucher
        searchBar.setOnTouchListener((v, event) -> {
            searchBar.setFocusable(true);
            searchBar.setFocusableInTouchMode(true);
            return false;
        });

        // Ajouter un TextWatcher pour la recherche en temps réel
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

        // Gérer le clic sur le bouton filtre
        btnFiltre.setOnClickListener(v -> toggleFilterPanel());

        // Gérer le clic sur le bouton réinitialiser
        btnResetFilters.setOnClickListener(v -> reinitialiserFiltres());

        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/films");
            new ListefilmsTask(this).execute(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode pour afficher/masquer le panneau de filtre
    private void toggleFilterPanel() {
        if (isFilterVisible) {
            filterPanel.setVisibility(View.GONE);
            isFilterVisible = false;
        } else {
            filterPanel.setVisibility(View.VISIBLE);
            isFilterVisible = true;
        }
    }

    // affichage / masquage de la barre de chargement
    public void showProgressBar(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // callback après l’appel REST
    public void mettreAJourActivityApresAppelRest(String resultatAppelRest) {
        listeFilmsResultat = resultatAppelRest; // callback
        Log.d("mydebug", ">>>Pour ListefilmsActivity - mettreAJourActivityApresAppelRest=" + listeFilmsResultat);
        afficherListeFilms(listeFilmsResultat);
    }

    // méthode équivalente à "afficherListeCommunes" dans le cours
    public void afficherListeFilms(String resultat) {
        TextView tvHeader = findViewById(R.id.tvTitre);

        if (resultat == null || resultat.isEmpty()) {
            tvHeader.setText("Aucun résultat reçu");
            Toast.makeText(this, "Aucune donnée reçue de l'API", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parser le JSON avec Gson
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Film>>(){}.getType();
            List<Film> films = gson.fromJson(resultat, listType);

            // Vérifier si la liste contient des films
            if (films != null && !films.isEmpty()) {
                // Sauvegarder tous les films
                tousLesFilms = new ArrayList<>(films);
                filmsAffiches = new ArrayList<>(films);

                tvHeader.setText("Liste des films (" + films.size() + " films)");

                // Créer l'adapter et le connecter à la ListView
                adapter = new FilmAdapter(this, filmsAffiches);
                listeFilms.setAdapter(adapter);

                // Ajouter un écouteur de clic sur chaque élément de la liste
                listeFilms.setOnItemClickListener((parent, view, position, id) -> {
                    Film filmSelectionne = filmsAffiches.get(position);
                    ouvrirPageDetailfilm(filmSelectionne);
                });

                // Initialiser les spinners de filtres
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

    // Méthode pour filtrer les films
    private void filtrerFilms() {
        String query = searchBar.getText().toString().toLowerCase().trim();

        // Commencer avec tous les films
        List<Film> filmsTemporaires = new ArrayList<>();

        // Parcourir tous les films pour appliquer les filtres
        for (Film film : tousLesFilms) {
            boolean inclure = true;

            // Filtre par recherche texte
            if (!query.isEmpty()) {
                boolean correspondRecherche = film.getTitre().toLowerCase().contains(query) ||
                        film.getRealisateur().toLowerCase().contains(query);
                if (!correspondRecherche) {
                    inclure = false;
                }
            }

            // Filtre par catégorie
            if (inclure && !categorieSelectionnee.equals("Toutes")) {
                boolean correspondCategorie = false;
                if (film.getCategories() != null) {
                    for (Film.Category cat : film.getCategories()) {
                        if (cat.getName().equals(categorieSelectionnee)) {
                            correspondCategorie = true;
                            break;
                        }
                    }
                }
                if (!correspondCategorie) {
                    inclure = false;
                }
            }

            // Filtre par année
            if (inclure && !anneeSelectionnee.equals("Toutes")) {
                int annee = Integer.parseInt(anneeSelectionnee);
                if (film.getAnnee() != annee) {
                    inclure = false;
                }
            }

            // Ajouter le film si tous les filtres sont passés
            if (inclure) {
                filmsTemporaires.add(film);
            }
        }

        // Mettre à jour la liste affichée
        filmsAffiches = filmsTemporaires;

        // Mettre à jour l'affichage
        if (adapter != null) {
            adapter.clear();
            adapter.addAll(filmsAffiches);
            adapter.notifyDataSetChanged();
        }

        // Mettre à jour le compteur
        TextView tvHeader = findViewById(R.id.tvTitre);
        tvHeader.setText("Films (" + filmsAffiches.size() + ")");
    }

    // Méthode pour initialiser les spinners
    private void initialiserSpinners() {
        // Extraire toutes les catégories uniques
        Set<String> categories = new HashSet<>();
        categories.add("Toutes");
        for (Film film : tousLesFilms) {
            if (film.getCategories() != null) {
                for (Film.Category cat : film.getCategories()) {
                    categories.add(cat.getName());
                }
            }
        }
        List<String> listeCategories = new ArrayList<>(categories);
        java.util.Collections.sort(listeCategories);

        // Configurer le spinner des catégories
        ArrayAdapter<String> adapterCategorie = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeCategories);
        adapterCategorie.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorie.setAdapter(adapterCategorie);

        spinnerCategorie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categorieSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Extraire toutes les années uniques
        Set<String> annees = new HashSet<>();
        annees.add("Toutes");
        for (Film film : tousLesFilms) {
            annees.add(String.valueOf(film.getAnnee()));
        }
        List<String> listeAnnees = new ArrayList<>(annees);
        java.util.Collections.sort(listeAnnees, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                if (a.equals("Toutes")) return -1;
                if (b.equals("Toutes")) return 1;
                return b.compareTo(a); // Ordre décroissant
            }
        });

        // Configurer le spinner des années
        ArrayAdapter<String> adapterAnnee = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeAnnees);
        adapterAnnee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnnee.setAdapter(adapterAnnee);

        spinnerAnnee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                anneeSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Méthode pour réinitialiser les filtres
    private void reinitialiserFiltres() {
        searchBar.setText("");
        spinnerCategorie.setSelection(0);
        spinnerAnnee.setSelection(0);
        categorieSelectionnee = "Toutes";
        anneeSelectionnee = "Toutes";
        filtrerFilms();
    }

    // navigation vers les autres pages
    public void ouvrirPagePanier(View view) {
        Intent intent = new Intent(this, PanierActivity.class);
        startActivity(intent);
    }

    // Méthode pour ouvrir la page de détail avec les données du film
    public void ouvrirPageDetailfilm(Film film) {
        Intent intent = new Intent(this, DetailfilmActivity.class);

        // Passer toutes les données du film via JSON
        Gson gson = new Gson();
        String filmJson = gson.toJson(film);
        intent.putExtra("filmJson", filmJson);

        startActivity(intent);
    }
}
