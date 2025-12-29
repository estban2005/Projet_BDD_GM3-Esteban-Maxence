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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Ronde {

    private int id;
    private int numero;
    private String statut;
    private int idTournoi;
    public Ronde(int id, int numero, String statut, int idTournoi) {
        this.id = id;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
    }
    public Ronde(int numero, String statut, int idTournoi) {
        this.id = -1;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
    }

    public void insertInDB(Connection con) throws SQLException {
        String sql = "INSERT INTO ronde (numero, statut, idTournoi) VALUES (?, ?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, this.numero);
            pst.setString(2, this.statut);
            pst.setInt(3, this.idTournoi);
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }
    public static List<Ronde> findAll(Connection con, int idTournoi) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        String sql = "SELECT * FROM ronde WHERE idTournoi = ? ORDER BY numero ASC";
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Ronde(
                        rs.getInt("id"),
                        rs.getInt("numero"),
                        rs.getString("statut"),
                        rs.getInt("idTournoi")
                    ));
                }
            }
        }
        return res;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdTournoi() {
        return idTournoi;
    }

    public void setIdTournoi(int idTournoi) {
        this.idTournoi = idTournoi;
    }
}