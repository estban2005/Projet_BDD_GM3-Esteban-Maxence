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

import fr.insa.beuvron.utils.ConsoleFdB;
import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author francois
 */
public class Utilisateur extends ClasseMiroir {

    private String surnom;
    private String pass;
    private int role;

    /**
     * pour nouvel utilisateur en mémoire
     */
    public Utilisateur(String surnom, String pass, int role) {
        super();
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }

    /**
     * pour utilisateur récupéré de la base de données
     */
    public Utilisateur(int id, String surnom, String pass, int role) {
        super(id);
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "insert into utilisateur (surnom,pass,role) values (?,?,?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setString(1, this.getSurnom());
        insert.setString(2, this.getPass());
        insert.setInt(3, role);
        insert.executeUpdate();
        return insert;
    }

    public static List<Utilisateur> tousLesUtilisateur(Connection con) throws SQLException {
        List<Utilisateur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("select id,surnom,pass,role from utilisateur")) {
            try (ResultSet allU = pst.executeQuery()) {
                while (allU.next()) {
                    res.add(new Utilisateur(allU.getInt("id"), allU.getString("surnom"),
                            allU.getString("pass"), allU.getInt("role")));
                }
            }
        }
        return res;
    }

    /**
     * supprime l'utilisateur de la BdD. Attention : supprime d'abord les
     * éventuelles dépendances.
     *
     * @param con
     * @throws SQLException
     */
    public void deleteInDB(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        try {
            con.setAutoCommit(false);
            try (PreparedStatement pst = con.prepareStatement(
                    "delete from pratique where idutilisateur = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = con.prepareStatement(
                    "delete from apprecie where u1 = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = con.prepareStatement(
                    "delete from apprecie where u2 = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }

            try (PreparedStatement pst = con.prepareStatement(
                    "delete from utilisateur where id = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            this.entiteSupprimee();
            con.commit();
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public static Utilisateur entreeConsole() {
        String nom = ConsoleFdB.entreeString("surnom de l'utilisateur : ");
        String pass = ConsoleFdB.entreeString("password : ");
        return new Utilisateur(nom, pass, 2);
    }

    /**
     * @return the surnom
     */
    public String getSurnom() {
        return surnom;
    }

    /**
     * @param surnom the surnom to set
     */
    public void setSurnom(String surnom) {
        this.surnom = surnom;
    }

    /**
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * @param pass the pass to set
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

}
