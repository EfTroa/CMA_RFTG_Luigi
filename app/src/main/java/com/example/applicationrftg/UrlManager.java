package com.example.applicationrftg;

/* Liste déroulante serveur */
public class UrlManager {
    private static String URLConnexion = "http://10.0.2.2:8180";

    public static String getURLConnexion() {

        return URLConnexion;
    }

    public static void setURLConnexion(String url) {

        URLConnexion = url;
    }
}
