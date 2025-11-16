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

public class Ronde {

    private Integer id;
    private int numero;
    private String statut;   // EN_COURS / CLOSE
    private int idTournoi;

    public Ronde(int numero, String statut, int idTournoi) {
        this.id = null;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
    }

    public Ronde(Integer id, int numero, String statut, int idTournoi) {
        this.id = id;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
    }

    public Integer getId() { return id; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getIdTournoi() { return idTournoi; }
    public void setIdTournoi(int idTournoi) { this.idTournoi = idTournoi; }

    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Rounde déjà insérée");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into ronde (numero, statut, idTournoi) values (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
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

    public static List<Ronde> findAll(Connection con) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, numero, statut, idTournoi from ronde");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Ronde(
                        rs.getInt("id"),
                        rs.getInt("numero"),
                        rs.getString("statut"),
                        rs.getInt("idTournoi")));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Rounde{" + "id=" + id + ", numero=" + numero + ", statut=" + statut + '}';
    }
}

