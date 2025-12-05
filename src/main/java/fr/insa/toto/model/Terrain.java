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

public class Terrain {

    private Integer id;
    private String nom;

    public Terrain(String nom) {
        this.id = null;
        this.nom = nom;
    }

    public Terrain(Integer id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Terrain déjà inséré");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into terrain (nom) values (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, this.nom);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Terrain> findAll(Connection con) throws SQLException {
        List<Terrain> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, nom from terrain");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Terrain(
                        rs.getInt("id"),
                        rs.getString("nom")));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Terrain{" + "id=" + id + ", nom=" + nom + '}';
    }
}
