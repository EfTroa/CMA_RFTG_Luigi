package com.example.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ListefilmsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private String listeFilmsResultat = "";
    private ListView listeFilms;
    private ConstraintLayout filterPanel;
    private boolean isFilterVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilms);

        listeFilms = findViewById(R.id.listeFilms);
        progressBar = findViewById(R.id.progressBar);
        filterPanel = findViewById(R.id.filterPanel);
        Button btnFiltre = findViewById(R.id.btnFiltre);

        // Gérer le clic sur le bouton filtre
        btnFiltre.setOnClickListener(v -> toggleFilterPanel());

        try {
            URL url = new URL("http://10.0.2.2:8180/films");
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
                tvHeader.setText("Liste des films (" + films.size() + " films)");

                // Créer l'adapter et le connecter à la ListView
                FilmAdapter adapter = new FilmAdapter(this, films);
                listeFilms.setAdapter(adapter);

                // Ajouter un écouteur de clic sur chaque élément de la liste
                listeFilms.setOnItemClickListener((parent, view, position, id) -> {
                    Film filmSelectionne = films.get(position);
                    ouvrirPageDetailfilm(filmSelectionne);
                });

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
