package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    /**
     * Génère une nouvelle ronde en créant autant de matchs que possible 
     * selon le nombre de terrains et de joueurs disponibles.
     */
    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch, int duree) throws SQLException {
        // 1. Récupération des paramètres du tournoi
        List<Tournoi> tournois = Tournoi.findAll(con);
        Tournoi tournoi = tournois.stream()
                .filter(t -> t.getId() == idTournoi)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tournoi introuvable (ID " + idTournoi + ")"));

        int tailleEquipe = tournoi.getNbJoueursParEquipe();
        int nbTerrainsDisponibles = tournoi.getNbTerrains();
        
        // 2. Récupération et mélange des joueurs pour l'affectation aléatoire
        List<Joueur> tousLesJoueurs = Joueur.findAll(con);
        Collections.shuffle(tousLesJoueurs);

        // 3. Calcul de la capacité de la ronde
        int joueursParMatch = nbEquipesParMatch * tailleEquipe;
        if (joueursParMatch <= 0) throw new IllegalArgumentException("Configuration d'équipes invalide");
        
        // Nombre de matchs que les joueurs peuvent former
        int nbMatchsPossiblesParJoueurs = tousLesJoueurs.size() / joueursParMatch;
        // On ne peut pas dépasser le nombre de terrains
        int nbMatchsACreer = Math.min(nbMatchsPossiblesParJoueurs, nbTerrainsDisponibles);

        if (nbMatchsACreer == 0) {
            throw new IllegalStateException("Pas assez de joueurs (" + tousLesJoueurs.size() + 
                ") pour former un match de " + joueursParMatch + " personnes sur les terrains disponibles.");
        }

        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            // 4. Détermination du numéro de la nouvelle ronde
            int numRonde = 1;
            try (PreparedStatement pst = con.prepareStatement("SELECT MAX(numero) FROM ronde WHERE idTournoi = ?")) {
                pst.setInt(1, idTournoi);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) numRonde = rs.getInt(1) + 1;
                }
            }

            // 5. Insertion de la ronde
            Ronde ronde = new Ronde(numRonde, "EN_COURS", idTournoi, duree);
            ronde.insertInDB(con);

            // 6. Boucle de création des matchs (C'est ici que la multiplicité se joue)
            int indexJoueur = 0;
            for (int i = 0; i < nbMatchsACreer; i++) {
                int idTerrain = i + 1; // On affecte un terrain par match
                Matchs match = new Matchs("EN_COURS", ronde.getId(), idTerrain);
                match.insertInDB(con);

                // Création des équipes pour ce match spécifique
                for (int numEquipe = 1; numEquipe <= nbEquipesParMatch; numEquipe++) {
                    Equipe equipe = new Equipe(numEquipe, 0, match.getId());
                    equipe.insertInDB(con);

                    // Affectation des joueurs à l'équipe
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

    public static void verifierEtCloturerRonde(Connection con, int idRonde) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM matchs WHERE idRonde = ? AND statut != 'CLOSE'";
        boolean rondeFinie = false;
        try (PreparedStatement pst = con.prepareStatement(sqlCheck)) {
            pst.setInt(1, idRonde);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) rondeFinie = (rs.getInt(1) == 0);
            }
        }
        if (rondeFinie) {
            String sqlUpdate = "UPDATE ronde SET statut = 'TERMINÉ' WHERE id = ?";
            try (PreparedStatement pstUpdate = con.prepareStatement(sqlUpdate)) {
                pstUpdate.setInt(1, idRonde);
                pstUpdate.executeUpdate();
            }
        }
    }
}