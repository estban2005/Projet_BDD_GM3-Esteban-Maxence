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
/**
 *
 * @author maxen
 */
package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    /**
     * Génère une ronde aléatoire.
     * @param nbEquipesParMatch :
     */
    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch) throws SQLException {
        List<Tournoi> tournois = Tournoi.findAll(con);
        Tournoi tournoi = tournois.stream()
                .filter(t -> t.getId() == idTournoi)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tournoi introuvable (ID " + idTournoi + ")"));

        int tailleEquipe = tournoi.getNbJoueursParEquipe();
        int nbTerrains = tournoi.getNbTerrains();
        
        verifierEtCreerTerrains(con, nbTerrains);
        
        List<Joueur> tousLesJoueurs = Joueur.findAll(con);
        Collections.shuffle(tousLesJoueurs);

        int joueursParMatch = nbEquipesParMatch * tailleEquipe;
        if (joueursParMatch == 0) throw new IllegalArgumentException("Configuration impossible (0 joueurs par match)");
        
        int nbMatchsPossibles = tousLesJoueurs.size() / joueursParMatch;
        int nbMatchsReels = Math.min(nbMatchsPossibles, nbTerrains);

        if (nbMatchsReels == 0) throw new IllegalStateException("Pas assez de joueurs ou de terrains !");

        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);

        try {

            int numRonde = 1;
            try (PreparedStatement pst = con.prepareStatement("SELECT MAX(numero) FROM ronde WHERE idTournoi = ?")) {
                pst.setInt(1, idTournoi);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) numRonde = rs.getInt(1) + 1;
                }
            }

            Ronde ronde = new Ronde(numRonde, "EN_COURS", idTournoi);
            ronde.insertInDB(con);

            int indexJoueur = 0;

            for (int i = 0; i < nbMatchsReels; i++) {
                int idTerrain = i + 1;
                
                Matchs match = new Matchs("EN_COURS", ronde.getId(), idTerrain);
                match.insertInDB(con);

                for (int numEquipe = 1; numEquipe <= nbEquipesParMatch; numEquipe++) {
                    Equipe equipe = new Equipe(numEquipe, 0, match.getId());
                    equipe.insertInDB(con);

                    for (int j = 0; j < tailleEquipe; j++) {
                        if (indexJoueur < tousLesJoueurs.size()) {
                            Joueur joueur = tousLesJoueurs.get(indexJoueur++);
                            Composition compo = new Composition(equipe.getId(), joueur.getId());
                            compo.insertInDB(con);
                        }
                    }
                }
            }
            con.commit();
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }


    private static void verifierEtCreerTerrains(Connection con, int nbTerrainsNecessaires) throws SQLException {
        for (int i = 1; i <= nbTerrainsNecessaires; i++) {
            boolean existe = false;
            try (PreparedStatement pstCheck = con.prepareStatement("SELECT 1 FROM terrain WHERE id = ?")) {
                pstCheck.setInt(1, i);
                try (ResultSet rs = pstCheck.executeQuery()) {
                    if (rs.next()) existe = true;
                }
            }

            if (!existe) {
                try (PreparedStatement pstInsert = con.prepareStatement("INSERT INTO terrain (id, nom) VALUES (?, ?)")) {
                    pstInsert.setInt(1, i);
                    pstInsert.setString(2, "Terrain " + i);
                    pstInsert.executeUpdate();
                }
            }
        }
    }
public static void verifierEtCloturerRonde(Connection con, int idRonde) throws SQLException {
    String sqlCheck = "SELECT COUNT(*) FROM matchs WHERE idRonde = ? AND statut != 'TERMINÉ'";
    
    boolean rondeFinie = false;
    try (PreparedStatement pst = con.prepareStatement(sqlCheck)) {
        pst.setInt(1, idRonde);
        try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                rondeFinie = (rs.getInt(1) == 0);
            }
        }
    }

    if (rondeFinie) {
        String sqlUpdate = "UPDATE ronde SET statut = 'TERMINÉ' WHERE id = ?";
        try (PreparedStatement pstUpdate = con.prepareStatement(sqlUpdate)) {
            pstUpdate.setInt(1, idRonde);
            pstUpdate.executeUpdate();
            System.out.println("--- La ronde " + idRonde + " est maintenant TERMINÉE ---");
        }
    }
}
    }