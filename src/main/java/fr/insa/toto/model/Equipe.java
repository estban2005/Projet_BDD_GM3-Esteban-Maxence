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

public class Equipe {

    private Integer id;
    private int num;
    private Integer score;  // peut être null si match pas terminé
    private int idMatch;

    public Equipe(int num, Integer score, int idMatch) {
        this.id = null;
        this.num = num;
        this.score = score;
        this.idMatch = idMatch;
    }

    public Equipe(Integer id, int num, Integer score, int idMatch) {
        this.id = id;
        this.num = num;
        this.score = score;
        this.idMatch = idMatch;
    }

    public Integer getId() { return id; }
    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public int getIdMatch() { return idMatch; }
    public void setIdMatch(int idMatch) { this.idMatch = idMatch; }

    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Equipe déjà insérée");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into equipe (num, score, idMatch) values (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, this.num);
            if (this.score == null) {
                pst.setNull(2, Types.INTEGER);
            } else {
                pst.setInt(2, this.score);
            }
            pst.setInt(3, this.idMatch);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Equipe> findAll(Connection con) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, num, score, idMatch from equipe");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Integer sc = rs.getObject("score") == null ? null : rs.getInt("score");
                res.add(new Equipe(
                        rs.getInt("id"),
                        rs.getInt("num"),
                        sc,
                        rs.getInt("idMatch")));
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Equipe{" + "id=" + id + ", num=" + num + ", score=" + score + '}';
    }
}

