package fr.insa.toto.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceGestionTournoi {

    // SIGNATURE MODIFIÉE avec le paramètre tempsMatch
    public static void genererNouvelleRonde(Connection con, int idTournoi, int nbEquipesParMatch, int tempsMatch) throws SQLException {
        
        // 1. Déterminer le numéro de la prochaine ronde
        int prochainNumero = 1;
        String sqlNum = "SELECT MAX(numero) FROM ronde WHERE idTournoi = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlNum)) {
            ps.setInt(1, idTournoi);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                prochainNumero = rs.getInt(1) + 1;
            }
        }

        // 2. Créer la nouvelle ronde avec le TEMPS_MATCH
        int idRondeGeneree = -1;
        // REQUÊTE MODIFIÉE pour inclure Temps_Match
        String sqlRonde = "INSERT INTO ronde (numero, statut, idTournoi, Temps_Match) VALUES (?, 'EN_COURS', ?, ?)";
        
        try (PreparedStatement pst = con.prepareStatement(sqlRonde, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, prochainNumero);
            pst.setInt(2, idTournoi);
            pst.setInt(3, tempsMatch); // Injection de la durée
            pst.executeUpdate();
            
            ResultSet rsKeys = pst.getGeneratedKeys();
            if (rsKeys.next()) {
                idRondeGeneree = rsKeys.getInt(1);
            }
        }

        // 3. Logique de création des matchs (Exemple simplifié)
        // Ici, vous récupérez vos équipes et vous créez les lignes dans la table 'matchs'
        // rattachées à idRondeGeneree...
        creerMatchsPourRonde(con, idRondeGeneree, idTournoi, nbEquipesParMatch);
    }

    private static void creerMatchsPourRonde(Connection con, int idRonde, int idTournoi, int nbParMatch) throws SQLException {
        // Votre logique actuelle de création de matchs (récupération équipes, mélange, INSERT matchs)
        // ...
    }
    
    public static void verifierEtCloturerRonde(Connection con, int idRonde) throws SQLException {
        // Utiliser 'CLOSE' ici comme discuté précédemment
        String sql = "UPDATE ronde r SET r.statut = 'CLOSE' " +
                     "WHERE r.id = ? AND NOT EXISTS (SELECT 1 FROM matchs m WHERE m.idRonde = r.id AND m.statut != 'CLOSE')";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idRonde);
            pst.executeUpdate();
        }
    }
}