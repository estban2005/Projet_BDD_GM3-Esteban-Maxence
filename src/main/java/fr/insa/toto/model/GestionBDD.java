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

public class GestionBDD {

    public static void creeSchema(Connection con) throws SQLException {
        try {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {

                // ----- table tournoi -----
                st.executeUpdate(
                        "create table tournoi ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(50) not null,"
                        + " nbTerrains integer not null,"
                        + " nbJoueursParEquipe integer not null"
                        + ")"
                );

                // ----- table terrain -----
                st.executeUpdate(
                        "create table terrain ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(30) not null"
                        + ")"
                );

                // ----- table ronde -----
                st.executeUpdate(
                        "create table ronde ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " numero integer not null,"
                        + " statut varchar(20) not null default 'EN_COURS',"
                        + " idTournoi integer not null,"
                        + " constraint fk_ronde_tournoi "
                        + "   foreign key (idTournoi) references tournoi(id)"
                        + ")"
                );

                // ----- table joueur -----
                st.executeUpdate(
                        "create table joueur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(30),"
                        + " prenom varchar(30),"
                        + " surnom varchar(30) not null unique,"
                        + " sexe char(1),"
                        + " dateNaissance date,"
                        + " scoreTotal integer not null default 0"
                        + ")"
                );

                // ----- table matchs -----
                st.executeUpdate(
                        "create table matchs ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " statut varchar(20) not null default 'EN_COURS',"
                        + " idRonde integer not null,"
                        + " idTerrain integer,"
                        + " constraint fk_matchs_ronde "
                        + "   foreign key (idRonde) references ronde(id),"
                        + " constraint fk_matchs_terrain "
                        + "   foreign key (idTerrain) references terrain(id)"
                        + ")"
                );

                // ----- table equipe -----
                st.executeUpdate(
                        "create table equipe ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " num integer not null,"
                        + " score integer,"
                        + " idMatch integer not null,"
                        + " constraint fk_equipe_match "
                        + "   foreign key (idMatch) references matchs(id)"
                        + ")"
                );

                // ----- table composition -----
                st.executeUpdate(
                        "create table composition ( "
                        + " idEquipe integer not null,"
                        + " idJoueur integer not null,"
                        + " constraint pk_composition primary key (idEquipe, idJoueur),"
                        + " constraint fk_compo_equipe "
                        + "   foreign key (idEquipe) references equipe(id),"
                        + " constraint fk_compo_joueur "
                        + "   foreign key (idJoueur) references joueur(id)"
                        + ")"
                );

                // ----- table utilisateur -----
                st.executeUpdate(
                        "create table utilisateur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " surnom varchar(50) not null unique,"
                        + " pass varchar(50) not null,"
                        + " role integer not null"
                        + ")"
                );

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
            // Suppression dans l'ordre inverse des dépendances
            try { st.executeUpdate("drop table composition"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table equipe"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table matchs"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table ronde"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table terrain"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table tournoi"); } catch (SQLException ex) {}
            
            // Suppression utilisateur
            try { st.executeUpdate("drop table utilisateur"); } catch (SQLException ex) {}
        }
    }

    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }
}