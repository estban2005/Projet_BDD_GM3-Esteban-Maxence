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

public class Matchs {

    private Integer id;
    private String statut;   // EN_COURS / CLOSE
    private int idRonde;
    private Integer idTerrain; // peut être null

    public Matchs(String statut, int idRonde, Integer idTerrain) {
        this.id = null;
        this.statut = statut;
        this.idRonde = idRonde;
        this.idTerrain = idTerrain;
    }

    public Matchs(Integer id, String statut, int idRonde, Integer idTerrain) {
        this.id = id;
        this.statut = statut;
        this.idRonde = idRonde;
        this.idTerrain = idTerrain;
    }

    public Integer getId() { return id; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getIdRonde() { return idRonde; }
    public void setIdRonde(int idRonde) { this.idRonde = idRonde; }
    public Integer getIdTerrain() { return idTerrain; }
    public void setIdTerrain(Integer idTerrain) { this.idTerrain = idTerrain; }

    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Match déjà inséré");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into matchs (statut, idRonde, idTerrain) values (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, this.statut);
            pst.setInt(2, this.idRonde);
            if (this.idTerrain == null) {
                pst.setNull(3, Types.INTEGER);
            } else {
                pst.setInt(3, this.idTerrain);
            }
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Matchs> findAll(Connection con) throws SQLException {
        List<Matchs> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, statut, idRonde, idTerrain from matchs");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Integer idTer = rs.getObject("idTerrain") == null ? null : rs.getInt("idTerrain");
                res.add(new Matchs(
                        rs.getInt("id"),
                        rs.getString("statut"),
                        rs.getInt("idRonde"),
                        idTer));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Matchs{" + "id=" + id + ", statut=" + statut + '}';
    }
}

