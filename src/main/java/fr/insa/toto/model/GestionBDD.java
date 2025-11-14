/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is ecole of CoursBeuvron.

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
import java.sql.Statement;

/**
 *
 * @author francois
 */
public class GestionBDD {

    /**
     *
     * @param con
     * @throws SQLException
     */
public static void creeSchema(Connection con) throws SQLException {
    try {
        con.setAutoCommit(false);
        try (Statement st = con.createStatement()) {

            // ----- table joueur -----
            st.executeUpdate(
                    "create table joueur ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " surnom varchar(30) not null unique,"
                    + " categorie char(1),"
                    + " tailleCm integer"
                    + ")"
            )
                    ;

            // ----- table matchs -----
            st.executeUpdate(
                    "create table matchs ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " ronde integer not null"
                    + ")"
            ) 
                    ;

            // ----- table equipe -----
            st.executeUpdate(
                    "create table equipe ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " num integer not null,"
                    + " score integer,"
                    + " idMatch integer not null,"
                    + " constraint fk_equipe_idmatch "
                    + "   foreign key (idMatch) references matchs(id)"
                    + ")"
            )
                    ;

            // ----- table composition -----
            st.executeUpdate(
                    "create table composition ( "
                    + " idEquipe integer not null,"
                    + " idJoueur integer not null,"
                    + " constraint fk_compo_idequipe "
                    + "   foreign key (idEquipe) references equipe(id),"
                    + " constraint fk_compo_idjoueur "
                    + "   foreign key (idJoueur) references joueur(id)"
                    + ")"
            )
                    ;

            con.commit();
        }
    } catch (SQLException ex) {
        con.rollback();
        throw ex;
    } finally {
        con.setAutoCommit(true);
    }
}

public static void deleteSchema(Connection con) throws SQLException {
    try (Statement st = con.createStatement()) {
        // on supprime dans l'ordre enfant -> parent
        try {
            st.executeUpdate("drop table composition");
        } catch (SQLException ex) {
        }
        try {
            st.executeUpdate("drop table equipe");
        } catch (SQLException ex) {
        }
        try {
            st.executeUpdate("drop table matchs");
        } catch (SQLException ex) {
        }
        try {
            st.executeUpdate("drop table joueur");
        } catch (SQLException ex) {
        }
    }
}

public static void razBdd(Connection con) throws SQLException {
    deleteSchema(con);
    creeSchema(con);
}

public static void main(String[] args) {
    try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
        razBdd(con);
    } catch (SQLException ex) {
        throw new Error(ex);
    }
}
}