package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Ronde {

    private int id;
    private int numero;
    private String statut;
    private int idTournoi;
    private int duree; 

    public Ronde(int id, int numero, String statut, int idTournoi, int duree) {
        this.id = id;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
        this.duree = duree;
    }

    public Ronde(int numero, String statut, int idTournoi, int duree) {
        this.id = -1;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
        this.duree = duree;
    }

    public void insertInDB(Connection con) throws SQLException {
        
        String sql = "INSERT INTO ronde (numero, statut, idTournoi, duree) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, this.numero);
            pst.setString(2, this.statut);
            pst.setInt(3, this.idTournoi);
            pst.setInt(4, this.duree);
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Ronde> findAll(Connection con, int idTournoi) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        String sql = "SELECT * FROM ronde WHERE idTournoi = ? ORDER BY numero ASC";
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Ronde(
                        rs.getInt("id"),
                        rs.getInt("numero"),
                        rs.getString("statut"),
                        rs.getInt("idTournoi"),
                        rs.getInt("duree") 
                    ));
                }
            }
        }
        return res;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getIdTournoi() { return idTournoi; }
    public void setIdTournoi(int idTournoi) { this.idTournoi = idTournoi; }
    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
}