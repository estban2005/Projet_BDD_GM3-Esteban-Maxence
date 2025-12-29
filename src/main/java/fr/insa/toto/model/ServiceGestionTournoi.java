package fr.insa.toto.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    /**
     * Méthode de base pour générer une ronde.
     * Cette version est celle d'origine qui posait problème avec 'idTournoi' dans la table équipe.
     */
    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch, int tempsMatch) throws SQLException {
        
        // 1. Déterminer le numéro de la prochaine ronde
        int prochainNumero = 1;
        String sqlNum = "SELECT MAX(numero) FROM ronde WHERE idTournoi = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlNum)) {
            ps.setInt(1, idTournoi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    prochainNumero = rs.getInt(1) + 1;
                }
            }
        }

        // 2. Créer la nouvelle ronde
        int idRondeGeneree = -1;
        String sqlRonde = "INSERT INTO ronde (numero, statut, idTournoi, Temps_Match) VALUES (?, 'EN_COURS', ?, ?)";
        
        try (PreparedStatement pst = con.prepareStatement(sqlRonde, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, prochainNumero);
            pst.setInt(2, idTournoi);
            pst.setInt(3, tempsMatch); 
            pst.executeUpdate();
            
            try (ResultSet rsKeys = pst.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    idRondeGeneree = rsKeys.getInt(1);
                }
            }
        }

        // 3. Appel de la création des matchs
        if (idRondeGeneree != -1) {
            creerMatchsPourRonde(con, idRondeGeneree, idTournoi, nbEquipesParMatch);
        }
    }

    /**
     * Logique initiale de création des matchs.
     */
    private static void creerMatchsPourRonde(Connection con, int idRonde, int idTournoi, int nbParMatch) throws SQLException {
        // NOTE : C'est ici que se trouvait l'erreur "Unknown column idTournoi in equipe"
        // car le code cherchait des équipes existantes au lieu de les créer.
        
        String sqlTeams = "SELECT id FROM equipe WHERE idTournoi = ?"; 
        List<Integer> listeEquipes = new ArrayList<>();
        
        try (PreparedStatement ps = con.prepareStatement(sqlTeams)) {
            ps.setInt(1, idTournoi);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                listeEquipes.add(rs.getInt("id"));
            }
        }

        Collections.shuffle(listeEquipes);

        int i = 0;
        int numTerrain = 1;
        while (i + 1 < listeEquipes.size()) {
            String sqlMatch = "INSERT INTO matchs (idRonde, idTerrain, statut) VALUES (?, ?, 'EN_COURS')";
            try (PreparedStatement psM = con.prepareStatement(sqlMatch)) {
                psM.setInt(1, idRonde);
                psM.setInt(2, numTerrain++);
                psM.executeUpdate();
            }
            i += 2;
        }
    }

    public static void verifierEtCloturerRonde(Connection con, int idRonde) throws SQLException {
        String sql = "UPDATE ronde SET statut = 'CLOSE' WHERE id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idRonde);
            pst.executeUpdate();
        }
    }
}