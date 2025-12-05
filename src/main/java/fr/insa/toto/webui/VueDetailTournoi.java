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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "tournoi", layout = MainLayout.class)
@PageTitle("Détail Tournoi")
public class VueDetailTournoi extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idTournoi;
    private H2 titreVue = new H2("Chargement...");
    private VerticalLayout contenuOnglet = new VerticalLayout();
    
    private Tab tabMatchs;
    private Tab tabClassement;
    private Tabs tabs;

    public VueDetailTournoi() {
        add(titreVue);

        tabMatchs = new Tab("Matchs");
        tabClassement = new Tab("Classement Local");
        tabs = new Tabs(tabMatchs, tabClassement);

        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(tabMatchs)) {
                afficherMatchs();
            } else {
                afficherClassementLocal();
            }
        });

        add(tabs, contenuOnglet);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.idTournoi = parameter;
        chargerInfosTournoi();
        tabs.setSelectedTab(tabMatchs);
        afficherMatchs(); 
    }

    private void chargerInfosTournoi() {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement pst = con.prepareStatement("SELECT nom FROM tournoi WHERE id = ?")) {
                pst.setInt(1, idTournoi);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        titreVue.setText("Tournoi : " + rs.getString("nom"));
                    } else {
                        titreVue.setText("Tournoi inconnu");
                    }
                }
            }
        } catch (SQLException e) {
            titreVue.setText("Erreur : " + e.getMessage());
        }
    }

    private void afficherMatchs() {
        contenuOnglet.removeAll();
        
        Grid<MatchInfo> grid = new Grid<>(MatchInfo.class, false);
        grid.addColumn(m -> "Ronde " + m.numRonde).setHeader("Ronde").setSortable(true).setWidth("100px");
        grid.addColumn(MatchInfo::getDescriptionDuel).setHeader("Rencontre").setAutoWidth(true);
        grid.addColumn(MatchInfo::getScoreText).setHeader("Score").setWidth("150px");
        grid.addColumn(MatchInfo::getStatut).setHeader("Statut").setWidth("120px");
        
        try (Connection con = ConnectionPool.getConnection()) {
            List<MatchInfo> matchs = recupererMatchsDuTournoi(con);
            if (matchs.isEmpty()) {
                contenuOnglet.add(new Span("Aucun match trouvé pour ce tournoi."));
            } else {
                grid.setItems(matchs);
                contenuOnglet.add(grid);
            }
        } catch (SQLException e) {
            contenuOnglet.add(new Span("Erreur lors du chargement des matchs : " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private List<MatchInfo> recupererMatchsDuTournoi(Connection con) throws SQLException {
        List<MatchInfo> res = new ArrayList<>();
        String sql = "SELECT m.id, m.idTerrain, m.statut, r.numero " +
                     "FROM matchs m " +
                     "JOIN ronde r ON m.idRonde = r.id " +
                     "WHERE r.idTournoi = ? " +
                     "ORDER BY r.numero DESC, m.id";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    MatchInfo info = new MatchInfo();
                    info.idMatch = rs.getInt("id");
                    info.idTerrain = rs.getInt("idTerrain");
                    info.statut = rs.getString("statut");
                    info.numRonde = rs.getInt("numero");
                    
                    remplirEquipes(con, info);
                    
                    res.add(info);
                }
            }
        }
        return res;
    }
    
    private void remplirEquipes(Connection con, MatchInfo info) throws SQLException {
        String sql = "SELECT e.id, e.score, GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                     "FROM equipe e " +
                     "JOIN composition c ON c.idEquipe = e.id " +
                     "JOIN joueur j ON j.id = c.idJoueur " +
                     "WHERE e.idMatch = ? " +
                     "GROUP BY e.id ORDER BY e.num";
                     
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, info.idMatch);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) { 
                    info.idEquipe1 = rs.getInt("id");
                    info.nomEquipe1 = rs.getString("joueurs");
                    info.score1 = rs.getInt("score");
                    if (rs.wasNull()) info.score1 = null;
                }
                if (rs.next()) { 
                    info.idEquipe2 = rs.getInt("id");
                    info.nomEquipe2 = rs.getString("joueurs");
                    info.score2 = rs.getInt("score");
                    if (rs.wasNull()) info.score2 = null;
                }
            }
        }
    }

    private void afficherClassementLocal() {
        contenuOnglet.removeAll();
        
        Grid<JoueurScoreLocal> grid = new Grid<>(JoueurScoreLocal.class, false);
        grid.addColumn(j -> j.surnom).setHeader("Joueur");
        grid.addColumn(j -> j.scoreLocal).setHeader("Points (Ce tournoi)").setSortable(true);
        
        try (Connection con = ConnectionPool.getConnection()) {
            List<JoueurScoreLocal> classement = calculerClassementLocal(con);
            if (classement.isEmpty()) {
                contenuOnglet.add(new Span("Aucun point marqué dans ce tournoi pour l'instant."));
            } else {
                grid.setItems(classement);
                contenuOnglet.add(grid);
            }
        } catch (SQLException e) {
            contenuOnglet.add(new Span("Erreur lors du calcul du classement : " + e.getMessage()));
        }
    }

    private List<JoueurScoreLocal> calculerClassementLocal(Connection con) throws SQLException {
        List<JoueurScoreLocal> liste = new ArrayList<>();
        String sql = "SELECT j.surnom, COALESCE(SUM(e.score), 0) as total " +
                     "FROM joueur j " +
                     "JOIN composition c ON j.id = c.idJoueur " +
                     "JOIN equipe e ON c.idEquipe = e.id " +
                     "JOIN matchs m ON e.idMatch = m.id " +
                     "JOIN ronde r ON m.idRonde = r.id " +
                     "WHERE r.idTournoi = ? " +
                     "GROUP BY j.id, j.surnom " +
                     "ORDER BY total DESC";
                     
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    liste.add(new JoueurScoreLocal(rs.getString("surnom"), rs.getInt("total")));
                }
            }
        }
        return liste;
    }

    public static class MatchInfo {
        public int idMatch;
        public int idTerrain;
        public String statut;
        public int numRonde;
        
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

    public static class JoueurScoreLocal {
        public String surnom;
        public int scoreLocal;
        
        public JoueurScoreLocal(String n, int s) { 
            this.surnom = n; 
            this.scoreLocal = s; 
        }
    }
}