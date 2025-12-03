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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    /**
     * Génère une ronde aléatoire.
     * @param nbEquipesParMatch : Le nombre d'équipes qui s'affrontent (ex: 2).
     */
    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch) throws SQLException {
        // 1. Récupération des infos du tournoi
        List<Tournoi> tournois = Tournoi.findAll(con);
        Tournoi tournoi = tournois.stream()
                .filter(t -> t.getId() == idTournoi)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tournoi introuvable (ID " + idTournoi + ")"));

        int tailleEquipe = tournoi.getNbJoueursParEquipe();
        int nbTerrains = tournoi.getNbTerrains();
        
        // 2. Récupération et mélange des joueurs
        List<Joueur> tousLesJoueurs = Joueur.findAll(con);
        Collections.shuffle(tousLesJoueurs);

        // 3. Calculs des limites
        int joueursParMatch = nbEquipesParMatch * tailleEquipe;
        if (joueursParMatch == 0) throw new IllegalArgumentException("Configuration impossible (0 joueurs par match)");
        
        int nbMatchsPossibles = tousLesJoueurs.size() / joueursParMatch;
        int nbMatchsReels = Math.min(nbMatchsPossibles, nbTerrains);

        if (nbMatchsReels == 0) throw new IllegalStateException("Pas assez de joueurs ou de terrains !");

        // 4. Transaction BDD
        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            // Création Ronde (On met le numéro 1 arbitrairement pour l'instant)
            Ronde ronde = new Ronde(1, "EN_COURS", idTournoi);
            ronde.insertInDB(con);

            int indexJoueur = 0;

            // Boucle pour créer chaque MATCH
            for (int i = 0; i < nbMatchsReels; i++) {
                // Création du match (avec un ID terrain fictif i+1)
                Matchs match = new Matchs("EN_COURS", ronde.getId(), i + 1);
                match.insertInDB(con);

                // Boucle pour créer les EQUIPES du match (de 1 à nbEquipesParMatch)
                for (int numEquipe = 1; numEquipe <= nbEquipesParMatch; numEquipe++) {
                    Equipe equipe = new Equipe(numEquipe, 0, match.getId());
                    equipe.insertInDB(con);

                    // Boucle pour remplir l'équipe avec des JOUEURS
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
}