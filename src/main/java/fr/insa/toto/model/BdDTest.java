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

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BdDTest {

    public static void createBdDTest(Connection con) throws SQLException {
        try {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {

                // ---------- TOURNOI ----------
                st.executeUpdate(
                        "insert into tournoi (id, nom, nbTerrains, nbJoueursParEquipe) "
                        + "values (1, 'Tournoi de test', 2, 2)");

                // ---------- TERRAINS ----------
                st.executeUpdate(
                        "insert into terrain (id, nom) values (1, 'Terrain 1')");
                st.executeUpdate(
                        "insert into terrain (id, nom) values (2, 'Terrain 2')");

                // ---------- RONDE ----------
                st.executeUpdate(
                        "insert into ronde (id, numero, statut, idTournoi) "
                        + "values (1, 1, 'CLOSE', 1)");

                // ---------- JOUEURS ----------
                // On met le scoreTotal égal à la somme des scores des équipes
                // auxquelles ils appartiennent
                st.executeUpdate(
                        "insert into joueur (id, nom, prenom, surnom, sexe, scoreTotal) "
                        + "values (1, 'Durand', 'Toto', 'Toto', 'M', 10)");
                st.executeUpdate(
                        "insert into joueur (id, nom, prenom, surnom, sexe, scoreTotal) "
                        + "values (2, 'Martin', 'Titi', 'Titi', 'M', 15)");
                st.executeUpdate(
                        "insert into joueur (id, nom, prenom, surnom, sexe, scoreTotal) "
                        + "values (3, 'Dupont', 'Tutu', 'Tutu', 'M', 27)");
                st.executeUpdate(
                        "insert into joueur (id, nom, prenom, surnom, sexe, scoreTotal) "
                        + "values (4, 'Bernard', 'Toti', 'Toti', 'M', 20)");
                st.executeUpdate(
                        "insert into joueur (id, nom, prenom, surnom, sexe, scoreTotal) "
                        + "values (5, 'Morel', 'Tuti', 'Tuti', 'M', 12)");

                // ---------- MATCHS ----------
                //2 matchs dans la ronde 1
                st.executeUpdate(
                        "insert into matchs (id, statut, idRonde, idTerrain) "
                        + "values (1, 'CLOSE', 1, 1)");
                st.executeUpdate(
                        "insert into matchs (id, statut, idRonde, idTerrain) "
                        + "values (2, 'CLOSE', 1, 2)");

                // ---------- EQUIPES ----------
                st.executeUpdate(
                        "insert into equipe (id, num, score, idMatch) "
                        + "values (1, 1, 10, 1)");
                st.executeUpdate(
                        "insert into equipe (id, num, score, idMatch) "
                        + "values (2, 2, 15, 1)");
                st.executeUpdate(
                        "insert into equipe (id, num, score, idMatch) "
                        + "values (3, 1, 12, 2)");
                st.executeUpdate(
                        "insert into equipe (id, num, score, idMatch) "
                        + "values (4, 2, 5, 2)");

                // ---------- COMPOSITION DES EQUIPES ----------
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (1, 1)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (1, 2)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (2, 3)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (2, 4)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (3, 5)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (3, 3)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (4, 4)");
                st.executeUpdate(
                        "insert into composition (idEquipe, idJoueur) values (4, 2)");

                con.commit();
            }
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            GestionBDD.razBdd(con);
            createBdDTest(con);
            System.out.println("Base de données de test créée avec succès.");
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}
