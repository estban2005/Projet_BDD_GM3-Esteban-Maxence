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
package fr.insa.toto.webui;

/**
 *
 * @author maxen
 */
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Matchs;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "matchs", layout = MainLayout.class)
@PageTitle("Liste des Matchs")
public class VueListeMatchs extends VerticalLayout {

    private Grid<MatchInfo> grid;

    public VueListeMatchs() {
        add(new H2("Matchs en cours"));

        grid = new Grid<>(MatchInfo.class, false);
        
        // Colonne Terrain
        grid.addColumn(m -> "Terrain " + m.idTerrain).setHeader("Lieu").setSortable(true).setWidth("100px");

        // Colonne Affichage du match (Equipe A vs Equipe B)
        grid.addColumn(MatchInfo::getDescriptionDuel).setHeader("Rencontre").setAutoWidth(true);

        // Colonne Score
        grid.addColumn(MatchInfo::getScoreText).setHeader("Score").setWidth("150px");

        // Colonne Statut
        grid.addColumn(MatchInfo::getStatut).setHeader("Statut").setWidth("120px");

        // Colonne Action (Visible seulement pour les ADMINS)
        if (SessionInfo.isCurUserAdmin()) {
            grid.addComponentColumn(match -> {
                if ("CLOSE".equals(match.statut)) {
                    return new Span("Terminé");
                }
                Button btnScore = new Button("Saisir Score", VaadinIcon.EDIT.create());
                btnScore.addClickListener(e -> ouvrirDialogScore(match));
                return btnScore;
            }).setHeader("Actions");
        }

        add(grid);
        rafraichirGrille();
    }

    private void rafraichirGrille() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<MatchInfo> matchs = recupererMatchsComplets(con);
            grid.setItems(matchs);
        } catch (SQLException e) {
            Notification.show("Erreur de chargement : " + e.getMessage());
        }
    }

    // --- LOGIQUE DE SAISIE DE SCORE (ADMIN) ---
    private void ouvrirDialogScore(MatchInfo match) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Résultat du match (Terrain " + match.idTerrain + ")");
        IntegerField score1 = new IntegerField("Score " + match.nomEquipe1);
        IntegerField score2 = new IntegerField("Score " + match.nomEquipe2);

        Button save = new Button("Valider et Clore", e -> {
            if (score1.getValue() != null && score2.getValue() != null) {
                sauvegarderScore(match, score1.getValue(), score2.getValue());
                dialog.close();
                rafraichirGrille();
            }
        });

        VerticalLayout layout = new VerticalLayout(score1, score2, save);
        dialog.add(layout);
        dialog.open();
    }

private void sauvegarderScore(MatchInfo match, int s1, int s2) {
        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            try {
                updateEquipeScore(con, match.idEquipe1, s1);
                updateEquipeScore(con, match.idEquipe2, s2);

                ajouterPointsAuxJoueurs(con, match.idEquipe1, s1);
                ajouterPointsAuxJoueurs(con, match.idEquipe2, s2);


                try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'CLOSE' WHERE id = ?")) {
                    pst.setInt(1, match.idMatch);
                    pst.executeUpdate();
                }
                
                con.commit();
                Notification.show("Match terminé et scores mis à jour !");
                rafraichirGrille();
                
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            Notification.show("Erreur sauvegarde : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void updateEquipeScore(Connection con, int idEquipe, int score) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?")) {
            pst.setInt(1, score);
            pst.setInt(2, idEquipe);
            pst.executeUpdate();
        }
    }

    private List<MatchInfo> recupererMatchsComplets(Connection con) throws SQLException {
        List<MatchInfo> res = new ArrayList<>();
        
        List<Matchs> listeMatchs = Matchs.findAll(con);
        for (Matchs m : listeMatchs) {
            MatchInfo info = new MatchInfo();
            info.idMatch = m.getId();
            info.idTerrain = m.getIdTerrain();
            info.statut = m.getStatut();
            
            try (PreparedStatement pst = con.prepareStatement(
                    "SELECT e.id, e.score, e.num, " +
                    "GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                    "FROM equipe e " +
                    "JOIN composition c ON c.idEquipe = e.id " +
                    "JOIN joueur j ON j.id = c.idJoueur " +
                    "WHERE e.idMatch = ? " +
                    "GROUP BY e.id ORDER BY e.num")) {
                
                pst.setInt(1, m.getId());
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) { // Equipe 1
                        info.idEquipe1 = rs.getInt("id");
                        info.nomEquipe1 = rs.getString("joueurs");
                        info.score1 = rs.getInt("score");
                        if (rs.wasNull()) info.score1 = null;
                    }
                    if (rs.next()) { // Equipe 2
                        info.idEquipe2 = rs.getInt("id");
                        info.nomEquipe2 = rs.getString("joueurs");
                        info.score2 = rs.getInt("score");
                        if (rs.wasNull()) info.score2 = null;
                    }
                }
            }
            res.add(info);
        }
        return res;
    }

    public static class MatchInfo {
        public int idMatch;
        public int idTerrain;
        public String statut;
        
        public int idEquipe1;
        public String nomEquipe1 = "Equipe 1";
        public Integer score1;
        
        public int idEquipe2;
        public String nomEquipe2 = "Equipe 2";
        public Integer score2;

        public String getDescriptionDuel() {
            return nomEquipe1 + "  VS  " + nomEquipe2;
        }

        public String getScoreText() {
            if (score1 == null || score2 == null) return "-";
            return score1 + " - " + score2;
        }
        
        public String getStatut() { return statut; }
    }
    private void ajouterPointsAuxJoueurs(Connection con, int idEquipe, int pointsGagnes) throws SQLException {
        String sql = "SELECT idJoueur FROM composition WHERE idEquipe = ?";
        
        try (PreparedStatement pstSelect = con.prepareStatement(sql)) {
            pstSelect.setInt(1, idEquipe);
            try (ResultSet rs = pstSelect.executeQuery()) {
                while (rs.next()) {
                    int idJoueur = rs.getInt("idJoueur");
                    
                    try (PreparedStatement pstUpdate = con.prepareStatement(
                            "UPDATE joueur SET scoreTotal = scoreTotal + ? WHERE id = ?")) {
                        pstUpdate.setInt(1, pointsGagnes);
                        pstUpdate.setInt(2, idJoueur);
                        pstUpdate.executeUpdate();
                    }
                }
            }
        }
    }
}