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

/**
 * ACTIVITÉ : Liste des films
 *
 * C'est l'écran principal de navigation après la connexion.
 * Elle affiche tous les films disponibles à la location.
 *
 * Fonctionnalités :
 *  - Chargement des films depuis l'API REST via ListefilmsTask
 *  - Affichage dans une ListView avec FilmAdapter
 *  - Recherche en temps réel (par titre ou réalisateur)
 *  - Filtrage par catégorie et par année (spinners)
 *  - Navigation vers le détail d'un film (clic sur un item)
 *  - Navigation vers le panier
 *  - Bouton pour quitter l'application
 *
 * Relation avec ListefilmsTask :
 * ListefilmsTask appelle mettreAJourActivityApresAppelRest() et showProgressBar()
 * sur cette activité une fois la requête HTTP terminée.
 * On dit que ListefilmsActivity est le "screen" de ListefilmsTask.
 */
public class ListefilmsActivity extends AppCompatActivity {

    // Barre de progression circulaire affichée pendant le chargement
    private ProgressBar progressBar;

    // Vue semi-transparente qui recouvre l'écran pendant le chargement (overlay)
    private View loadingOverlay;

    // Stockage brut du résultat JSON reçu depuis l'API (chaîne de texte)
    private String listeFilmsResultat = "";

    // Le composant ListView qui affiche la liste des films
    private ListView listeFilms;

    // Panneau de filtres (initialement masqué, affiché au clic du bouton "Filtrer")
    private ConstraintLayout filterPanel;

    // Indique si le panneau de filtres est actuellement visible
    private boolean isFilterVisible = false;

    // La liste complète de tous les films reçus de l'API (source de vérité)
    private List<Film> tousLesFilms = new ArrayList<>();

    // La liste des films actuellement affichés (après application des filtres)
    private List<Film> filmsAffiches = new ArrayList<>();

    // L'adaptateur qui connecte la liste de films à la ListView
    private FilmAdapter adapter;

    // Champ de recherche textuelle
    private EditText searchBar;

    // Spinner (liste déroulante) pour filtrer par catégorie
    private Spinner spinnerCategorie;

    // Spinner (liste déroulante) pour filtrer par année
    private Spinner spinnerAnnee;

    // Catégorie actuellement sélectionnée dans le spinner ("Toutes" = pas de filtre)
    private String categorieSelectionnee = "Toutes";

    // Année actuellement sélectionnée dans le spinner ("Toutes" = pas de filtre)
    private String anneeSelectionnee = "Toutes";

    /**
     * Méthode du cycle de vie appelée à la création de l'activité.
     * Initialise l'interface et déclenche le chargement des films depuis l'API.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilms);

        // Récupération de tous les composants définis dans activity_listefilms.xml
        listeFilms = findViewById(R.id.listeFilms);
        progressBar = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        filterPanel = findViewById(R.id.filterPanel);
        searchBar = findViewById(R.id.searchBar);
        spinnerCategorie = findViewById(R.id.spinnerCategorie);
        spinnerAnnee = findViewById(R.id.spinnerAnnee);
        Button btnFiltre = findViewById(R.id.btnFiltre);
        Button btnResetFilters = findViewById(R.id.btnResetFilters);

        // Activer le champ de recherche au toucher
        // Par défaut la barre de recherche n'est pas focusable (pour ne pas gêner la navigation)
        // On l'active manuellement quand l'utilisateur la touche
        searchBar.setOnTouchListener((v, event) -> {
            searchBar.setFocusable(true);
            searchBar.setFocusableInTouchMode(true);
            return false;  // false = l'événement n'est pas consommé, le focus normal se fait aussi
        });

        // Ajouter un TextWatcher pour la recherche en temps réel
        // TextWatcher est une interface qui observe les changements dans un champ texte
        // Chaque modification du texte déclenche filtrerFilms()
        searchBar.addTextChangedListener(new TextWatcher() {
            // Appelé AVANT la modification du texte (non utilisé ici)
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            // Appelé PENDANT la modification (à chaque caractère tapé ou effacé)
            // C'est ici qu'on met à jour la liste en temps réel
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrerFilms();
            }

            // Appelé APRÈS la modification du texte (non utilisé ici)
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Gérer le clic sur le bouton filtre
        // Expression lambda : v -> toggleFilterPanel() est équivalent à new OnClickListener() {...}
        btnFiltre.setOnClickListener(v -> toggleFilterPanel());

        // Gérer le clic sur le bouton réinitialiser
        btnResetFilters.setOnClickListener(v -> reinitialiserFiltres());

        // Construire l'URL de l'API et lancer la requête en arrière-plan
        // UrlManager.getURLConnexion() retourne l'URL de base (ex: "http://10.0.2.2:8180")
        // On y ajoute le chemin de l'endpoint "/films"
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/films");
            // execute() démarre l'exécution de l'AsyncTask en arrière-plan
            new ListefilmsTask(this).execute(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche ou masque le panneau de filtres.
     * Utilise la propriété visibility : VISIBLE = affiché, GONE = masqué (n'occupe pas d'espace)
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
     * Affiche ou masque l'indicateur de chargement (ProgressBar + overlay).
     * Appelée par ListefilmsTask depuis onPreExecute() et onPostExecute().
     *
     * @param visible true = afficher, false = masquer
     */
    public void showProgressBar(boolean visible) {
        // Opérateur ternaire : condition ? valeurSiVrai : valeurSiFaux
        int visibility = visible ? View.VISIBLE : View.GONE;
        loadingOverlay.setVisibility(visibility);
        progressBar.setVisibility(visibility);
    }

    /**
     * Callback appelée par ListefilmsTask après la requête HTTP.
     * C'est le "retour" de l'appel REST : ListefilmsTask a terminé son travail
     * et passe le résultat JSON brut (String) à l'activité pour qu'elle l'affiche.
     *
     * @param resultatAppelRest Le JSON brut reçu de l'API (liste de films)
     */
    public void mettreAJourActivityApresAppelRest(String resultatAppelRest) {
        listeFilmsResultat = resultatAppelRest; // callback
        Log.d("mydebug", ">>>Pour ListefilmsActivity - mettreAJourActivityApresAppelRest=" + listeFilmsResultat);
        afficherListeFilms(listeFilmsResultat);
    }

    /**
     * Parse le JSON reçu de l'API et affiche les films dans la ListView.
     * Méthode équivalente à "afficherListeCommunes" dans le cours.
     *
     * @param resultat La chaîne JSON contenant le tableau de films
     */
    public void afficherListeFilms(String resultat) {
        TextView tvHeader = findViewById(R.id.tvTitre);

        // Vérifier si on a bien reçu quelque chose
        if (resultat == null || resultat.isEmpty()) {
            tvHeader.setText("Aucun résultat reçu");
            Toast.makeText(this, "Aucune donnée reçue de l'API", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parser le JSON avec Gson
            // TypeToken permet à Gson de connaître le type générique exact (List<Film>)
            // car Java efface les types génériques à la compilation (type erasure)
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Film>>(){}.getType();
            List<Film> films = gson.fromJson(resultat, listType);

            // Vérifier si la liste contient des films
            if (films != null && !films.isEmpty()) {
                // Sauvegarder tous les films dans la liste de référence (copie défensive)
                tousLesFilms = new ArrayList<>(films);
                // filmsAffiches commence avec tous les films (aucun filtre appliqué)
                filmsAffiches = new ArrayList<>(films);

                tvHeader.setText("Liste des films");

                // Créer l'adapter et le connecter à la ListView
                // FilmAdapter hérite de ArrayAdapter<Film> pour afficher chaque film
                adapter = new FilmAdapter(this, filmsAffiches);
                listeFilms.setAdapter(adapter);

                // Ajouter un écouteur de clic sur chaque élément de la liste
                // OnItemClickListener : interface avec une méthode onItemClick()
                listeFilms.setOnItemClickListener((parent, view, position, id) -> {
                    // position = indice du film cliqué dans filmsAffiches
                    Film filmSelectionne = filmsAffiches.get(position);
                    ouvrirPageDetailfilm(filmSelectionne);
                });

                // Initialiser les spinners de filtres avec les valeurs extraites des films
                initialiserSpinners();

                Log.d("mydebug", ">>>Films affichés avec succès : " + films.size() + " films");
            } else {
                tvHeader.setText("Aucun film trouvé");
                Toast.makeText(this, "La liste des films est vide", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Gestion des erreurs de parsing JSON (format inattendu, champ manquant, etc.)
            tvHeader.setText("Erreur lors du parsing des données");
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("mydebug", ">>>Erreur parsing JSON : " + e.getMessage());
        }
    }

    /**
     * Applique les filtres actifs (texte, catégorie, année) sur la liste complète.
     * Appelée à chaque changement dans la barre de recherche ou les spinners.
     *
     * Algorithme :
     * - On part de tousLesFilms (la liste complète, jamais modifiée)
     * - Pour chaque film, on vérifie s'il passe tous les filtres actifs
     * - Si oui, on l'ajoute à la liste temporaire
     * - On met à jour filmsAffiches et on rafraîchit l'adaptateur
     */
    private void filtrerFilms() {
        // Texte saisi dans la barre de recherche (en minuscules pour comparaison insensible à la casse)
        String query = searchBar.getText().toString().toLowerCase().trim();

        // Commencer avec une liste vide
        List<Film> filmsTemporaires = new ArrayList<>();

        // Parcourir tous les films pour appliquer les filtres
        for (Film film : tousLesFilms) {
            boolean inclure = true;  // Le film passe tous les filtres jusqu'à preuve du contraire

            // Filtre par recherche texte (sur le titre ET le nom du réalisateur)
            if (!query.isEmpty()) {
                boolean correspondRecherche = film.getTitre().toLowerCase().contains(query) ||
                        film.getRealisateur().toLowerCase().contains(query);
                if (!correspondRecherche) {
                    inclure = false;
                }
            }

            // Filtre par catégorie (seulement si une catégorie autre que "Toutes" est sélectionnée)
            if (inclure && !categorieSelectionnee.equals("Toutes")) {
                boolean correspondCategorie = false;
                // Un film peut avoir plusieurs catégories, on cherche si l'une correspond
                if (film.getCategories() != null) {
                    for (Film.Category cat : film.getCategories()) {
                        if (cat.getName().equals(categorieSelectionnee)) {
                            correspondCategorie = true;
                            break;  // Inutile de continuer la boucle si on a trouvé
                        }
                    }
                }
                if (!correspondCategorie) {
                    inclure = false;
                }
            }

            // Filtre par année (seulement si une année autre que "Toutes" est sélectionnée)
            if (inclure && !anneeSelectionnee.equals("Toutes")) {
                // parseInt() convertit la String "2006" en int 2006 pour comparer
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

        // Mettre à jour la liste affichée avec les résultats filtrés
        filmsAffiches = filmsTemporaires;

        // Mettre à jour l'affichage de la ListView
        if (adapter != null) {
            adapter.clear();             // Vider l'adapter
            adapter.addAll(filmsAffiches); // Ajouter les films filtrés
            adapter.notifyDataSetChanged(); // Signaler à la ListView de se rafraîchir
        }

        // Mettre à jour le compteur (affiché dans le titre)
        TextView tvHeader = findViewById(R.id.tvTitre);
        tvHeader.setText("Films");
    }

    /**
     * Initialise les deux spinners de filtre (catégories et années)
     * avec les valeurs extraites dynamiquement des films chargés.
     *
     * On utilise un Set<String> pour éviter les doublons
     * (plusieurs films peuvent avoir la même catégorie ou la même année).
     */
    private void initialiserSpinners() {
        // Extraire toutes les catégories uniques
        // HashSet = collection sans doublons, sans ordre particulier
        Set<String> categories = new HashSet<>();
        categories.add("Toutes");  // Option par défaut (pas de filtre)
        for (Film film : tousLesFilms) {
            if (film.getCategories() != null) {
                for (Film.Category cat : film.getCategories()) {
                    categories.add(cat.getName());  // HashSet ignore automatiquement les doublons
                }
            }
        }
        // Convertir en List pour pouvoir la trier
        List<String> listeCategories = new ArrayList<>(categories);
        java.util.Collections.sort(listeCategories);  // Tri alphabétique

        // Configurer le spinner des catégories avec un ArrayAdapter standard
        ArrayAdapter<String> adapterCategorie = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeCategories);
        adapterCategorie.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorie.setAdapter(adapterCategorie);

        // Écouter les changements de sélection du spinner catégorie
        spinnerCategorie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Mémoriser la catégorie sélectionnée et relancer le filtrage
                categorieSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Extraire toutes les années uniques
        Set<String> annees = new HashSet<>();
        annees.add("Toutes");  // Option par défaut
        for (Film film : tousLesFilms) {
            // String.valueOf() convertit l'int (ex: 2006) en String (ex: "2006")
            annees.add(String.valueOf(film.getAnnee()));
        }
        List<String> listeAnnees = new ArrayList<>(annees);

        // Tri personnalisé : "Toutes" en premier, puis années en ordre décroissant
        java.util.Collections.sort(listeAnnees, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                if (a.equals("Toutes")) return -1;  // "Toutes" toujours en premier
                if (b.equals("Toutes")) return 1;
                return b.compareTo(a); // Ordre décroissant (ex: 2010 avant 2006)
            }
        });

        // Configurer le spinner des années
        ArrayAdapter<String> adapterAnnee = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeAnnees);
        adapterAnnee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnnee.setAdapter(adapterAnnee);

        // Écouter les changements de sélection du spinner année
        spinnerAnnee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Mémoriser l'année sélectionnée et relancer le filtrage
                anneeSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Réinitialise tous les filtres à leur valeur par défaut.
     * Vide la barre de recherche, remet les spinners à "Toutes",
     * et rafraîchit la liste pour afficher tous les films.
     */
    private void reinitialiserFiltres() {
        searchBar.setText("");          // Vider la barre de recherche
        spinnerCategorie.setSelection(0); // Sélectionner le premier item ("Toutes")
        spinnerAnnee.setSelection(0);     // Sélectionner le premier item ("Toutes")
        categorieSelectionnee = "Toutes";
        anneeSelectionnee = "Toutes";
        filtrerFilms();                  // Relancer le filtre (affichera tous les films)
    }

    /**
     * Ferme l'application complètement.
     * finishAffinity() ferme l'activité courante ET toutes les activités parentes dans la pile.
     * Déclaré dans activity_listefilms.xml via android:onClick="quitterApplication".
     *
     * @param view La vue (bouton ×) qui a déclenché l'événement
     */
    public void quitterApplication(View view) {
        finishAffinity();
    }

    /**
     * Navigue vers le panier (PanierActivity).
     * Déclaré dans activity_listefilms.xml via android:onClick="ouvrirPagePanier".
     *
     * @param view La vue (bouton "Panier") qui a déclenché l'événement
     */
    public void ouvrirPagePanier(View view) {
        Intent intent = new Intent(this, PanierActivity.class);
        startActivity(intent);
    }

    /**
     * Navigue vers la page de détail d'un film.
     * Appelée lors du clic sur un film dans la liste.
     *
     * On sérialise l'objet Film en JSON pour le passer à DetailfilmActivity via l'Intent.
     * On ne peut pas passer directement un objet Java dans un Intent sans implémenter
     * Serializable ou Parcelable. Utiliser Gson est une alternative simple.
     *
     * @param film Le film sélectionné par l'utilisateur
     */
    public void ouvrirPageDetailfilm(Film film) {
        Intent intent = new Intent(this, DetailfilmActivity.class);

        // Convertir l'objet Film en chaîne JSON pour le passer via l'Intent
        // intent.putExtra(clé, valeur) = ajouter une donnée à l'Intent
        Gson gson = new Gson();
        String filmJson = gson.toJson(film);
        intent.putExtra("filmJson", filmJson);

        startActivity(intent);
    }
}
