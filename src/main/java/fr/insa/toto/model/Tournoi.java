package fr.insa.toto.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Tournoi {

    private Integer id;
    private String nom;
    private int nbTerrains;
    private int nbJoueursParEquipe;

    public Tournoi(String nom, int nbTerrains, int nbJoueursParEquipe) {
        this.id = null;
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    public Tournoi(Integer id, String nom, int nbTerrains, int nbJoueursParEquipe) {
        this.id = id;
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    // Getters et Setters
    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getNbTerrains() { return nbTerrains; }
    public void setNbTerrains(int nbTerrains) { this.nbTerrains = nbTerrains; }
    public int getNbJoueursParEquipe() { return nbJoueursParEquipe; }
    public void setNbJoueursParEquipe(int nbJoueursParEquipe) { this.nbJoueursParEquipe = nbJoueursParEquipe; }

    public void insertInDB(Connection con) throws SQLException {
        if (this.id != null) {
            throw new IllegalStateException("Tournoi déjà inséré");
        }
        try (PreparedStatement pst = con.prepareStatement(
                "insert into tournoi (nom, nbTerrains, nbJoueursParEquipe) values (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, this.nom);
            pst.setInt(2, this.nbTerrains);
            pst.setInt(3, this.nbJoueursParEquipe);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Tournoi> findAll(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, nom, nbTerrains, nbJoueursParEquipe from tournoi");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Tournoi(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nbTerrains"),
                        rs.getInt("nbJoueursParEquipe")));
            }
        }
        return res;
    }

    
    public static void deleteById(Connection con, int idTournoi) throws SQLException {
        boolean initialAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false); 

        try {
            // 1. Supprimer les COMPOSITIONS
            String sqlDeleteCompo = "DELETE c FROM composition c " +
                                    "JOIN equipe e ON c.idEquipe = e.id " +
                                    "JOIN matchs m ON e.idMatch = m.id " +
                                    "JOIN ronde r ON m.idRonde = r.id " +
                                    "WHERE r.idTournoi = ?";
            try (PreparedStatement pstC = con.prepareStatement(sqlDeleteCompo)) {
                pstC.setInt(1, idTournoi);
                pstC.executeUpdate();
            }

            // 2. Supprimer les EQUIPES
            String sqlDeleteEquipes = "DELETE e FROM equipe e " +
                                      "JOIN matchs m ON e.idMatch = m.id " +
                                      "JOIN ronde r ON m.idRonde = r.id " +
                                      "WHERE r.idTournoi = ?";
            try (PreparedStatement pstE = con.prepareStatement(sqlDeleteEquipes)) {
                pstE.setInt(1, idTournoi);
                pstE.executeUpdate();
            }

            // 3. Supprimer les MATCHS
            String sqlDeleteMatchs = "DELETE m FROM matchs m " +
                                     "JOIN ronde r ON m.idRonde = r.id " +
                                     "WHERE r.idTournoi = ?";
            try (PreparedStatement pstM = con.prepareStatement(sqlDeleteMatchs)) {
                pstM.setInt(1, idTournoi);
                pstM.executeUpdate();
            }

            // 4. Supprimer les RONDES
            String sqlDeleteRondes = "DELETE FROM ronde WHERE idTournoi = ?";
            try (PreparedStatement pstR = con.prepareStatement(sqlDeleteRondes)) {
                pstR.setInt(1, idTournoi);
                pstR.executeUpdate();
            }

            // 5. Supprimer le TOURNOI
            String sqlDeleteTournoi = "DELETE FROM tournoi WHERE id = ?";
            try (PreparedStatement pstT = con.prepareStatement(sqlDeleteTournoi)) {
                pstT.setInt(1, idTournoi);
                pstT.executeUpdate();
            }

            con.commit(); 
        } catch (SQLException e) {
            con.rollback(); 
            throw e;
        } finally {
            con.setAutoCommit(initialAutoCommit);
        }
    }

    @Override
    public String toString() {
        return "Tournoi{" + "id=" + id + ", nom=" + nom + '}';
    }
}