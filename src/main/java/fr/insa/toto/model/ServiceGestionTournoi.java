package fr.insa.toto.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    /**
     * Génère une nouvelle ronde, crée les matchs et répartit les équipes.
     */
    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch, int tempsMatch) throws SQLException {
        
        System.out.println("Tentative de création de ronde pour le tournoi ID : " + idTournoi);
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

        // 2. Créer la nouvelle ronde avec le TEMPS_MATCH
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

        // 3. Création effective des matchs et assignation des équipes
        if (idRondeGeneree != -1) {
            creerMatchsPourRonde(con, idRondeGeneree, idTournoi, nbEquipesParMatch);
        } else {
            throw new SQLException("Échec de la création de la ronde.");
        }
    }

    /**
     * Logique de répartition des équipes dans les matchs pour la ronde donnée.
     */
    private static void creerMatchsPourRonde(Connection con, int idRonde, int idTournoi, int nbParMatch) throws SQLException {
        // A. Récupérer toutes les équipes du tournoi
        List<Integer> listeEquipes = new ArrayList<>();
        String sqlGetEquipes = "SELECT id FROM equipe WHERE idTournoi = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlGetEquipes)) {
            ps.setInt(1, idTournoi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listeEquipes.add(rs.getInt("id"));
                }
            }
        }

        // B. Vérification de sécurité
        if (listeEquipes.isEmpty()) {
            throw new SQLException("Aucune équipe trouvée pour le tournoi ID " + idTournoi);
        }
        
        if (listeEquipes.size() < nbParMatch) {
            throw new SQLException("Nombre d'équipes insuffisant (" + listeEquipes.size() + ") pour créer un match de " + nbParMatch);
        }

        // C. Mélanger les équipes pour un tirage aléatoire
        Collections.shuffle(listeEquipes);

        // D. Créer les matchs et lier les équipes
        // On utilise i + nbParMatch <= size pour ne prendre que des groupes complets
        for (int i = 0; i + nbParMatch <= listeEquipes.size(); i += nbParMatch) {
            
            int idMatchCree = -1;
            // Insertion du match : On met NULL pour idTerrain par défaut pour éviter les erreurs FK
            String sqlInsertMatch = "INSERT INTO matchs (idRonde, idTerrain, statut) VALUES (?, ?, 'EN_COURS')";
            try (PreparedStatement psM = con.prepareStatement(sqlInsertMatch, Statement.RETURN_GENERATED_KEYS)) {
                psM.setInt(1, idRonde);
                psM.setNull(2, java.sql.Types.INTEGER); // Terrain mis à NULL pour la sécurité
                psM.executeUpdate();
                
                try (ResultSet rsM = psM.getGeneratedKeys()) {
                    if (rsM.next()) {
                        idMatchCree = rsM.getInt(1);
                    }
                }
            }

            // E. Mettre à jour les équipes pour les lier au match créé
            if (idMatchCree != -1) {
                for (int j = 0; j < nbParMatch; j++) {
                    int idEquipe = listeEquipes.get(i + j);
                    String sqlUpdateEquipe = "UPDATE equipe SET idMatch = ?, num = ? WHERE id = ?";
                    try (PreparedStatement psUE = con.prepareStatement(sqlUpdateEquipe)) {
                        psUE.setInt(1, idMatchCree);
                        psUE.setInt(2, j + 1); // num 1, 2...
                        psUE.setInt(3, idEquipe);
                        psUE.executeUpdate();
                    }
                }
            }
        }
    }
    
    /**
     * Vérifie si tous les matchs d'une ronde sont terminés et clôture la ronde.
     */
    public static void verifierEtCloturerRonde(Connection con, int idRonde) throws SQLException {
        String sql = "UPDATE ronde r SET r.statut = 'CLOSE' " +
                     "WHERE r.id = ? AND NOT EXISTS (SELECT 1 FROM matchs m WHERE m.idRonde = r.id AND m.statut != 'CLOSE')";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idRonde);
            pst.executeUpdate();
        }
    }
}