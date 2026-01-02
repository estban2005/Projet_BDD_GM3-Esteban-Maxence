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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Route(value = "matchs", layout = MainLayout.class)
@PageTitle("Liste des Matchs")
public class VueListeMatchs extends VerticalLayout {

    private Grid<MatchSimpleInfo> grid;
    private ComboBox<Tournoi> comboTournois = new ComboBox<>("Tournoi");
    private ComboBox<Integer> comboRondes = new ComboBox<>("Ronde");
    private ComboBox<String> comboStatut = new ComboBox<>("Statut");

    public VueListeMatchs() {
        add(new H2("Matchs et Résultats"));

      
        HorizontalLayout filtres = new HorizontalLayout();
        filtres.setAlignItems(Alignment.BASELINE);

        comboTournois.setPlaceholder("Tous");
        comboTournois.setItemLabelGenerator(Tournoi::getNom);
        comboTournois.setClearButtonVisible(true);
        chargerTournois();

        comboRondes.setPlaceholder("Toutes");
        comboRondes.setEnabled(false);
        comboRondes.setClearButtonVisible(true);

        comboStatut.setPlaceholder("Tous");
        comboStatut.setItems("EN_COURS", "CLOSE");
        comboStatut.setClearButtonVisible(true);

        comboTournois.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                chargerNumerosRondes(e.getValue().getId());
                comboRondes.setEnabled(true);
            } else {
                comboRondes.clear();
                comboRondes.setEnabled(false);
            }
            rafraichirGrille();
        });
        comboRondes.addValueChangeListener(e -> rafraichirGrille());
        comboStatut.addValueChangeListener(e -> rafraichirGrille());

        filtres.add(comboTournois, comboRondes, comboStatut);
        add(filtres);

     
        grid = new Grid<>(MatchSimpleInfo.class, false);
        grid.addColumn(m -> m.nomTournoi).setHeader("Tournoi").setAutoWidth(true);
        grid.addColumn(m -> "Ronde " + m.numRonde).setHeader("Ronde").setWidth("100px");
        grid.addColumn(m -> "Terrain " + m.idTerrain).setHeader("Terrain").setWidth("100px");

        
        grid.addColumn(new ComponentRenderer<>(match -> {
            VerticalLayout layoutMatch = new VerticalLayout();
            layoutMatch.setPadding(false);
            layoutMatch.setSpacing(false);
            try (Connection con = ConnectionPool.getConnection()) {
                String sql = "SELECT e.id, e.num, e.score, GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                             "FROM equipe e " +
                             "JOIN composition c ON c.idEquipe = e.id " +
                             "JOIN joueur j ON j.id = c.idJoueur " +
                             "WHERE e.idMatch = ? " +
                             "GROUP BY e.id ORDER BY e.num";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setInt(1, match.idMatch);
                    try (ResultSet rs = pst.executeQuery()) {
                        while (rs.next()) {
                            String scoreStr = rs.getObject("score") != null ? rs.getString("score") : "?";
                            Span line = new Span("Équipe " + rs.getInt("num") + " [" + rs.getString("joueurs") + "] - Score: " + scoreStr);
                            layoutMatch.add(line);
                        }
                    }
                }
            } catch (Exception e) {
                layoutMatch.add(new Span("Erreur de chargement"));
            }
            return layoutMatch;
        })).setHeader("Équipes & Scores").setAutoWidth(true);

        grid.addColumn(m -> m.statut).setHeader("Statut").setWidth("120px");

        if (SessionInfo.isCurUserAdmin()) {
            grid.addComponentColumn(match -> {
                if ("CLOSE".equals(match.statut)) return new Span("Terminé");
                Button btnScore = new Button("Saisir Scores", VaadinIcon.EDIT.create());
                btnScore.addClickListener(e -> ouvrirDialogSaisieDynamique(match));
                return btnScore;
            }).setHeader("Actions");
        }

        add(grid);
        rafraichirGrille();
    }

    private void chargerTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            comboTournois.setItems(Tournoi.findAll(con));
        } catch (SQLException e) { Notification.show("Erreur tournois"); }
    }

    private void chargerNumerosRondes(int idTournoi) {
        List<Integer> numeros = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT numero FROM ronde WHERE idTournoi = ? ORDER BY numero";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idTournoi);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) numeros.add(rs.getInt("numero"));
            comboRondes.setItems(numeros);
        } catch (SQLException e) { Notification.show("Erreur rondes"); }
    }

    private void rafraichirGrille() {
        List<MatchSimpleInfo> res = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.idTerrain, m.statut, r.numero, t.nom " +
                "FROM matchs m JOIN ronde r ON m.idRonde = r.id JOIN tournoi t ON r.idTournoi = t.id " +
                "WHERE 1=1 ");
            if (comboTournois.getValue() != null) sql.append(" AND t.id = ").append(comboTournois.getValue().getId());
            if (comboRondes.getValue() != null) sql.append(" AND r.numero = ").append(comboRondes.getValue());
            if (comboStatut.getValue() != null) sql.append(" AND m.statut = '").append(comboStatut.getValue()).append("'");
            sql.append(" ORDER BY t.id DESC, r.numero DESC");

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql.toString())) {
                while (rs.next()) {
                    res.add(new MatchSimpleInfo(rs.getInt("id"), rs.getInt("idTerrain"), rs.getString("statut"), rs.getInt("numero"), rs.getString("nom")));
                }
            }
            grid.setItems(res);
        } catch (SQLException e) { Notification.show(e.getMessage()); }
    }

    private void ouvrirDialogSaisieDynamique(MatchSimpleInfo match) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Saisie des scores - Match " + match.idMatch);
        VerticalLayout form = new VerticalLayout();
        List<EquipeSaisie> listeSaisie = new ArrayList<>();

        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT id, num FROM equipe WHERE idMatch = ? ORDER BY num";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, match.idMatch);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        IntegerField field = new IntegerField("Score Équipe " + rs.getInt("num"));
                        form.add(field);
                        listeSaisie.add(new EquipeSaisie(rs.getInt("id"), field));
                    }
                }
            }
        } catch (Exception e) { Notification.show("Erreur chargement équipes"); }

        Button save = new Button("Enregistrer", e -> {
            sauvegarderScoresDynamiques(match.idMatch, listeSaisie);
            dialog.close();
            rafraichirGrille();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(form, save);
        dialog.open();
    }

    private void sauvegarderScoresDynamiques(int idMatch, List<EquipeSaisie> saisies) {
        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            try {
                for (EquipeSaisie s : saisies) {
                    int score = s.field.getValue() != null ? s.field.getValue() : 0;
                    
                    try (PreparedStatement pst = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?")) {
                        pst.setInt(1, score); pst.setInt(2, s.idEquipe); pst.executeUpdate();
                    }
                    
                    try (PreparedStatement pst = con.prepareStatement("UPDATE joueur SET scoreTotal = scoreTotal + ? WHERE id IN (SELECT idJoueur FROM composition WHERE idEquipe = ?)")) {
                        pst.setInt(1, score); pst.setInt(2, s.idEquipe); pst.executeUpdate();
                    }
                }
                try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'CLOSE' WHERE id = ?")) {
                    pst.setInt(1, idMatch); pst.executeUpdate();
                }
                con.commit();
                Notification.show("Match terminé !");
            } catch (Exception ex) { con.rollback(); throw ex; }
        } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    
    public static class MatchSimpleInfo {
        public int idMatch, idTerrain, numRonde;
        public String statut, nomTournoi;
        public MatchSimpleInfo(int id, int t, String s, int r, String nt) {
            this.idMatch = id; this.idTerrain = t; this.statut = s; this.numRonde = r; this.nomTournoi = nt;
        }
    }

    private static class EquipeSaisie {
        int idEquipe; IntegerField field;
        EquipeSaisie(int id, IntegerField f) { this.idEquipe = id; this.field = f; }
    }
}