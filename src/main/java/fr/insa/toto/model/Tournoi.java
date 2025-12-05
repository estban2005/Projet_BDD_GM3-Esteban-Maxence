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

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Tournoi {

    private Integer id;
    private String nom;
    private int nbTerrains;
    private int nbJoueursParEquipe;

    // --------- Constructeurs ---------
    public Tournoi(String nom, int nbTerrains, int nbJoueursParEquipe) {
        this.id = null;
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    public Tournoi(Integer id, String nom, int nbTerrains, int nbJoueursParEquipe) {
        this.id = id;
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    // --------- Getters / setters ---------
    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getNbTerrains() { return nbTerrains; }
    public void setNbTerrains(int nbTerrains) { this.nbTerrains = nbTerrains; }
    public int getNbJoueursParEquipe() { return nbJoueursParEquipe; }
    public void setNbJoueursParEquipe(int nbJoueursParEquipe) { this.nbJoueursParEquipe = nbJoueursParEquipe; }

    // --------- Persistence ---------
    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Tournoi déjà inséré (id != null)");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into tournoi (nom, nbTerrains, nbJoueursParEquipe) values (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, this.nom);
            pst.setInt(2, this.nbTerrains);
            pst.setInt(3, this.nbJoueursParEquipe);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Tournoi> findAll(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, nom, nbTerrains, nbJoueursParEquipe from tournoi");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Tournoi(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nbTerrains"),
                        rs.getInt("nbJoueursParEquipe")));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Tournoi{" + "id=" + id + ", nom=" + nom + '}';
    }
}

