package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch, int tempsMatch) throws SQLException {
        List<Tournoi> tournois = Tournoi.findAll(con);
        Tournoi tournoi = tournois.stream()
                .filter(t -> t.getId() == idTournoi).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tournoi introuvable"));

        int tailleEquipe = tournoi.getNbJoueursParEquipe();
        int nbTerrains = tournoi.getNbTerrains();

        // Les joueurs changent d'équipes à chaque ronde (mélange aléatoire)
        List<Joueur> tousLesJoueurs = Joueur.findAll(con);
        Collections.shuffle(tousLesJoueurs);

        int joueursParMatch = nbEquipesParMatch * tailleEquipe;
        int nbMatchsReels = Math.min(tousLesJoueurs.size() / joueursParMatch, nbTerrains);

        if (nbMatchsReels == 0) throw new IllegalStateException("Pas assez de joueurs ou de terrains.");

        con.setAutoCommit(false);
        try {
            int numRonde = 1;
            try (PreparedStatement pst = con.prepareStatement("SELECT MAX(numero) FROM ronde WHERE idTournoi = ?")) {
                pst.setInt(1, idTournoi);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) numRonde = rs.getInt(1) + 1;
                }
            }

            Ronde ronde = new Ronde(numRonde, "EN_COURS", idTournoi, tempsMatch);
            ronde.insertInDB(con);

            int indexJ = 0;
            for (int i = 0; i < nbMatchsReels; i++) {
                Matchs match = new Matchs("EN_COURS", ronde.getId(), i + 1);
                match.insertInDB(con);

                for (int nEq = 1; nEq <= nbEquipesParMatch; nEq++) {
                    Equipe eq = new Equipe(nEq, 0, match.getId());
                    eq.insertInDB(con);

                    for (int j = 0; j < tailleEquipe; j++) {
                        new Composition(eq.getId(), tousLesJoueurs.get(indexJ++).getId()).insertInDB(con);
                    }
                }
            }
            con.commit();
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    // Méthode pour clôturer la ronde appelée par VueDetailRonde
    public static void verifierEtCloturerRonde(Connection con, int idRonde) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("UPDATE ronde SET statut = 'TERMINE' WHERE id = ?")) {
            pst.setInt(1, idRonde);
            pst.executeUpdate();
        }
    }
}