package fr.insa.toto.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Matchs {

    private Integer id;
    private String statut;   // EN_COURS / CLOSE
    private int idRonde;
    private Integer idTerrain;

    public Matchs(String statut, int idRonde, Integer idTerrain) {
        this.id = null;
        this.statut = statut;
        this.idRonde = idRonde;
        this.idTerrain = idTerrain;
    }

    public Matchs(Integer id, String statut, int idRonde, Integer idTerrain) {
        this.id = id;
        this.statut = statut;
        this.idRonde = idRonde;
        this.idTerrain = idTerrain;
    }

    public Integer getId() { return id; }
    public String getStatut() { return statut; }
    public int getIdRonde() { return idRonde; }
    public Integer getIdTerrain() { return idTerrain; }

    public static List<Matchs> findAll(Connection con, int idRonde) throws SQLException {
        List<Matchs> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT * FROM matchs WHERE idRonde = ?")) {
            pst.setInt(1, idRonde);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Matchs(rs.getInt("id"), rs.getString("statut"), rs.getInt("idRonde"), rs.getInt("idTerrain")));
                }
            }
        }
        return res;
    }
}