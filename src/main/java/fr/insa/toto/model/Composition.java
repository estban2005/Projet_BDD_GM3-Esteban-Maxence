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

public class Composition {

    private int idEquipe;
    private int idJoueur;

    public Composition(int idEquipe, int idJoueur) {
        this.idEquipe = idEquipe;
        this.idJoueur = idJoueur;
    }

    public int getIdEquipe() { return idEquipe; }
    public void setIdEquipe(int idEquipe) { this.idEquipe = idEquipe; }
    public int getIdJoueur() { return idJoueur; }
    public void setIdJoueur(int idJoueur) { this.idJoueur = idJoueur; }

    public void insertInDB(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "insert into composition (idEquipe, idJoueur) values (?,?)")) {
            pst.setInt(1, this.idEquipe);
            pst.setInt(2, this.idJoueur);
            pst.executeUpdate();
        }
    }

    public static List<Composition> findAll(Connection con) throws SQLException {
        List<Composition> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select idEquipe, idJoueur from composition");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Composition(
                        rs.getInt("idEquipe"),
                        rs.getInt("idJoueur")));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Composition{" + "idEquipe=" + idEquipe + ", idJoueur=" + idJoueur + '}';
    }
}
