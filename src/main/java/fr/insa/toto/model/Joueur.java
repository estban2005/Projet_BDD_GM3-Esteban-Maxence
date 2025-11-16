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

public class Joueur {

    private Integer id;
    private String nom;
    private String prenom;
    private String surnom;
    private String sexe;          
    private Date dateNaissance;   
    private int scoreTotal;

    public Joueur(String nom, String prenom, String surnom,
                  String sexe, Date dateNaissance, int scoreTotal) {
        this.id = null;
        this.nom = nom;
        this.prenom = prenom;
        this.surnom = surnom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
        this.scoreTotal = scoreTotal;
    }

    public Joueur(Integer id, String nom, String prenom, String surnom,
                  String sexe, Date dateNaissance, int scoreTotal) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.surnom = surnom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
        this.scoreTotal = scoreTotal;
    }

    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getSurnom() { return surnom; }
    public void setSurnom(String surnom) { this.surnom = surnom; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public Date getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(Date dateNaissance) { this.dateNaissance = dateNaissance; }
    public int getScoreTotal() { return scoreTotal; }
    public void setScoreTotal(int scoreTotal) { this.scoreTotal = scoreTotal; }

    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Joueur déjà inséré");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into joueur (nom, prenom, surnom, sexe, dateNaissance, scoreTotal) "
                + "values (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, this.nom);
            pst.setString(2, this.prenom);
            pst.setString(3, this.surnom);
            pst.setString(4, this.sexe);
            pst.setDate(5, this.dateNaissance);
            pst.setInt(6, this.scoreTotal);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Joueur> findAll(Connection con) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, nom, prenom, surnom, sexe, dateNaissance, scoreTotal from joueur");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Joueur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("surnom"),
                        rs.getString("sexe"),
                        rs.getDate("dateNaissance"),
                        rs.getInt("scoreTotal")));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Joueur{" + "id=" + id + ", surnom=" + surnom + ", scoreTotal=" + scoreTotal + '}';
    }
}
