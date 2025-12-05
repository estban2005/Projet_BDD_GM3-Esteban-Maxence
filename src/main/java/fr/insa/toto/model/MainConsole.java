/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Date;
import java.util.List;
import java.util.Scanner;

public class MainConsole {

    private final Connection con;
    private final Scanner in;

    public MainConsole(Connection con) {
        this.con = con;
        this.in = new Scanner(System.in);
    }

    // ================== Méthode principale de menu ==================
    public void menuPrincipal() {
        boolean fini = false;
        while (!fini) {
            try {
                System.out.println("========== MENU TOURNOI ==========");
                System.out.println("1) Afficher tous les joueurs");
                System.out.println("2) Ajouter un joueur");
                System.out.println("3) Afficher tous les matchs");
                System.out.println("4) Afficher toutes les équipes");
                System.out.println("5) Afficher la composition des équipes");
                System.out.println("6) Réinitialiser la base avec les données de test");
                System.out.println("0) Quitter");
                System.out.print("Votre choix : ");

                String choix = in.nextLine().trim();
                switch (choix) {
                    case "1":
                        afficheTousLesJoueurs();
                        break;
                    case "2":
                        ajouteJoueur();
                        break;
                    case "3":
                        afficheTousLesMatchs();
                        break;
                    case "4":
                        afficheToutesLesEquipes();
                        break;
                    case "5":
                        afficheComposition();
                        break;
                    case "6":
                        razAvecDonneesTest();
                        break;
                    case "0":
                        fini = true;
                        break;
                    default:
                        System.out.println("Choix inconnu.");
                }
            } catch (SQLException ex) {
                System.out.println("Erreur SQL : " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        System.out.println("Au revoir.");
    }

    // ================== Actions du menu ==================

    private void afficheTousLesJoueurs() throws SQLException {
        List<Joueur> joueurs = Joueur.findAll(con);
        System.out.println("---- Liste des joueurs ----");
        for (Joueur j : joueurs) {
            System.out.println(j);
        }
        System.out.println("---------------------------");
    }

    private void ajouteJoueur() throws SQLException {
        System.out.println("---- Ajout d'un joueur ----");
        System.out.print("Nom (peut être vide) : ");
        String nom = in.nextLine().trim();
        if (nom.isEmpty()) nom = null;

        System.out.print("Prénom (peut être vide) : ");
        String prenom = in.nextLine().trim();
        if (prenom.isEmpty()) prenom = null;

        System.out.print("Surnom (obligatoire, unique) : ");
        String surnom = in.nextLine().trim();

        System.out.print("Sexe (M/F ou vide) : ");
        String sexe = in.nextLine().trim();
        if (sexe.isEmpty()) sexe = null;

        // Pour simplifier on ne demande pas la date : on met null
        Date dateN = null;

        Joueur j = new Joueur(nom, prenom, surnom, sexe, dateN, 0);
        j.insertInDB(con);
        System.out.println("Joueur ajouté avec id = " + j.getId());
    }

    private void afficheTousLesMatchs() throws SQLException {
        List<Matchs> matchs = Matchs.findAll(con);
        System.out.println("---- Liste des matchs ----");
        for (Matchs m : matchs) {
            System.out.println(m);
        }
        System.out.println("--------------------------");
    }

    private void afficheToutesLesEquipes() throws SQLException {
        List<Equipe> equipes = Equipe.findAll(con);
        System.out.println("---- Liste des équipes ----");
        for (Equipe e : equipes) {
            System.out.println(e);
        }
        System.out.println("---------------------------");
    }

    private void afficheComposition() throws SQLException {
        List<Composition> compos = Composition.findAll(con);
        System.out.println("---- Composition des équipes ----");
        for (Composition c : compos) {
            System.out.println(c);
        }
        System.out.println("---------------------------------");
    }

    private void razAvecDonneesTest() throws SQLException {
        System.out.println("Réinitialisation complète de la base + données de test...");
        GestionBDD.razBdd(con);
        BdDTest.createBdDTest(con);
        System.out.println("Base réinitialisée.");
    }

    // ================== main ==================

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            MainConsole mc = new MainConsole(con);
            mc.menuPrincipal();
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}