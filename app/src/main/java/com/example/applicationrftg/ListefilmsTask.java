package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ListefilmsTask extends AsyncTask<URL, Integer, String> {

    private volatile ListefilmsActivity screen;  // Référence à l'activité

    public ListefilmsTask(ListefilmsActivity s) {
        this.screen = s;
    }

    @Override
    protected void onPreExecute() {
        // Prétraitement de l'appel (ex: afficher ProgressBar)
        screen.showProgressBar(true);
    }

    @Override
    protected String doInBackground(URL... urls) {
        String resultat = null;
        URL urlAAppeler = urls[0];
        resultat = appelerServiceRestHttp(urlAAppeler);
        return resultat;
    }

    @Override
    protected void onPostExecute(String resultat) {
        System.out.println(">>>onPostExecute / resultat=" + resultat);

        // IMPORTANT : plusieurs possibilités pour traiter les données reçues :
        // possibilité-1 : mettre à jour directement la liste dans l'activité principale
        // possibilité-2 : passer les données à l'activité principale et laisser cette dernière mettre à jour la liste
        // possibilité-3 : appeler une méthode de l'activité principale pour qu'elle mette à jour la liste ou les composants

        // Ici on choisit la possibilité-3 : appeler la méthode de mise à jour
        screen.mettreAJourActivityApresAppelRest(resultat);

        // Cacher la ProgressBar
        screen.showProgressBar(false);
    }

    private String appelerServiceRestHttp(URL urlAAppeler ) {
        HttpURLConnection urlConnection = null;
        int responseCode = -1;
        String sResultatAppel = "";
        try {
            //Exemple pour un appel GET
            urlConnection = (HttpURLConnection) urlAAppeler.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", System.getProperty("http.agent"));
            urlConnection.setRequestProperty("Authorization","Bearer " + "eyJhbGciOiJIUzI1NiJ9.e30.jg2m4pLbAlZv1h5uPQ6fU38X23g65eXMX8q-SXuIPDg");


            responseCode = urlConnection.getResponseCode();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            //lecture du résulat de l'appel et alimentation de la chaine de caractères à renvoyer vers l'appelant
            int codeCaractere = -1;
            while ((codeCaractere = in.read()) != -1) {
                sResultatAppel = sResultatAppel + (char) codeCaractere;
            }
            in.close();
        } catch (IOException ioe) {
            Log.d("mydebug", ">>>Pour appelerServiceRestHttp - IOException ioe =" + ioe.toString());
        } catch (Exception e) {
            Log.d("mydebug",">>>Pour appelerServiceRestHttp - Excep-tion="+e.toString());
        } finally {
            urlConnection.disconnect();
        }
        return sResultatAppel;
    }

}
