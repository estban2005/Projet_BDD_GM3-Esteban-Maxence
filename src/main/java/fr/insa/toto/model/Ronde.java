package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Ronde {

    private Integer id;
    private int numero;
    private String statut;
    private int idTournoi;
    private int tempsMatch; // Nouvelle colonne BDD

    public Ronde(int numero, String statut, int idTournoi, int tempsMatch) {
        this(null, numero, statut, idTournoi, tempsMatch);
    }

    public Ronde(Integer id, int numero, String statut, int idTournoi, int tempsMatch) {
        this.id = id;
        this.numero = numero;
        this.statut = statut;
        this.idTournoi = idTournoi;
        this.tempsMatch = tempsMatch;
    }

    public void insertInDB(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "insert into ronde (numero, statut, idTournoi, temps_match) values (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, this.numero);
            pst.setString(2, this.statut);
            pst.setInt(3, this.idTournoi);
            pst.setInt(4, this.tempsMatch);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    this.id = rs.getInt(1);
                }
            }
        }
    }

    public static List<Ronde> findAll(Connection con) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("select id, numero, statut, idTournoi, temps_match from ronde")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Ronde(
                        rs.getInt("id"), 
                        rs.getInt("numero"), 
                        rs.getString("statut"), 
                        rs.getInt("idTournoi"), 
                        rs.getInt("temps_match")
                    ));
                }
            }
        }
        return res;
    }

    // Getters nécessaires pour VueDetailRonde
    public Integer getId() { return id; }
    public int getNumero() { return numero; }
    public String getStatut() { return statut; }
    public int getIdTournoi() { return idTournoi; }
    public int getTempsMatch() { return tempsMatch; }
}