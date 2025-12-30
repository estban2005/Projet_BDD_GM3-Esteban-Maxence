package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Matchs;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Route(value = "matchs", layout = MainLayout.class)
@PageTitle("Liste des Matchs")
public class VueListeMatchs extends VerticalLayout {

    private Grid<MatchInfo> grid;
    private ComboBox<Tournoi> comboTournois = new ComboBox<>("Tournoi");
    private ComboBox<Integer> comboRondes = new ComboBox<>("Ronde");
    private ComboBox<String> comboStatut = new ComboBox<>("Statut");

    public VueListeMatchs() {
        add(new H2("Matchs et Résultats"));

        // --- CONFIGURATION DES FILTRES ---
        HorizontalLayout filtres = new HorizontalLayout();
        filtres.setAlignItems(Alignment.BASELINE);

        comboTournois.setPlaceholder("Tous");
        comboTournois.setItemLabelGenerator(Tournoi::getNom);
        comboTournois.setClearButtonVisible(true);
        chargerTournois();

        comboRondes.setPlaceholder("Toutes");
        comboRondes.setEnabled(false);
        comboRondes.setClearButtonVisible(true);

        // Nouveau filtre Statut
        comboStatut.setPlaceholder("Tous");
        comboStatut.setItems("EN_COURS", "CLOSE");
        comboStatut.setClearButtonVisible(true);

        // Écouteurs pour rafraîchir la grille
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

        // --- GRILLE ---
        grid = new Grid<>(MatchInfo.class, false);
        grid.addColumn(m -> m.nomTournoi).setHeader("Tournoi").setAutoWidth(true);
        grid.addColumn(m -> "Ronde " + m.numRonde).setHeader("Ronde").setWidth("100px");
        grid.addColumn(m -> "Terrain " + m.idTerrain).setHeader("Lieu").setWidth("100px");
        grid.addColumn(MatchInfo::getDescriptionDuel).setHeader("Rencontre").setAutoWidth(true);
        grid.addColumn(MatchInfo::getScoreText).setHeader("Score").setWidth("150px");
        grid.addColumn(MatchInfo::getStatut).setHeader("Statut").setWidth("120px");

        if (SessionInfo.isCurUserAdmin()) {
            grid.addComponentColumn(match -> {
                if ("CLOSE".equals(match.statut)) return new Span("Terminé");
                Button btnScore = new Button("Saisir Score", VaadinIcon.EDIT.create());
                btnScore.addClickListener(e -> ouvrirDialogScore(match));
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
        try (Connection con = ConnectionPool.getConnection()) {
            grid.setItems(recupererMatchsFiltres(con));
        } catch (SQLException e) { Notification.show("Erreur : " + e.getMessage()); }
    }

    private List<MatchInfo> recupererMatchsFiltres(Connection con) throws SQLException {
        List<MatchInfo> res = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT m.id, m.idTerrain, m.statut, r.numero as numR, t.nom as nomT " +
            "FROM matchs m JOIN ronde r ON m.idRonde = r.id JOIN tournoi t ON r.idTournoi = t.id " +
            "WHERE 1=1 ");

        if (comboTournois.getValue() != null) sql.append(" AND t.id = ").append(comboTournois.getValue().getId());
        if (comboRondes.getValue() != null) sql.append(" AND r.numero = ").append(comboRondes.getValue());
        if (comboStatut.getValue() != null) sql.append(" AND m.statut = '").append(comboStatut.getValue()).append("'");
        
        sql.append(" ORDER BY t.id DESC, r.numero DESC");

        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                MatchInfo info = new MatchInfo();
                info.idMatch = rs.getInt("id");
                info.idTerrain = rs.getInt("idTerrain");
                info.statut = rs.getString("statut");
                info.numRonde = rs.getInt("numR");
                info.nomTournoi = rs.getString("nomT");

                try (PreparedStatement pstEq = con.prepareStatement(
                        "SELECT e.id, e.score, GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                        "FROM equipe e JOIN composition c ON c.idEquipe = e.id JOIN joueur j ON j.id = c.idJoueur " +
                        "WHERE e.idMatch = ? GROUP BY e.id ORDER BY e.num")) {
                    pstEq.setInt(1, info.idMatch);
                    try (ResultSet rsEq = pstEq.executeQuery()) {
                        if (rsEq.next()) {
                            info.idEquipe1 = rsEq.getInt("id");
                            info.nomEquipe1 = rsEq.getString("joueurs");
                            info.score1 = rsEq.getObject("score") != null ? rsEq.getInt("score") : null;
                        }
                        if (rsEq.next()) {
                            info.idEquipe2 = rsEq.getInt("id");
                            info.nomEquipe2 = rsEq.getString("joueurs");
                            info.score2 = rsEq.getObject("score") != null ? rsEq.getInt("score") : null;
                        }
                    }
                }
                res.add(info);
            }
        }
        return res;
    }

    private void ouvrirDialogScore(MatchInfo match) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Saisie Score");
        IntegerField s1 = new IntegerField("Score " + match.nomEquipe1);
        IntegerField s2 = new IntegerField("Score " + match.nomEquipe2);
        Button save = new Button("Enregistrer", e -> {
            if (s1.getValue() != null && s2.getValue() != null) {
                sauvegarderScore(match, s1.getValue(), s2.getValue());
                dialog.close();
            }
        });
        dialog.add(new VerticalLayout(s1, s2, save));
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
                    pst.setInt(1, match.idMatch); pst.executeUpdate();
                }
                con.commit();
                Notification.show("Match terminé");
                rafraichirGrille();
            } catch (Exception ex) { con.rollback(); throw ex; }
        } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    private void updateEquipeScore(Connection con, int idEq, int sc) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?")) {
            pst.setInt(1, sc); pst.setInt(2, idEq); pst.executeUpdate();
        }
    }

    private void ajouterPointsAuxJoueurs(Connection con, int idEq, int pts) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "UPDATE joueur SET scoreTotal = scoreTotal + ? WHERE id IN (SELECT idJoueur FROM composition WHERE idEquipe = ?)")) {
            pst.setInt(1, pts); pst.setInt(2, idEq); pst.executeUpdate();
        }
    }

    public static class MatchInfo {
        public int idMatch, idTerrain, numRonde, idEquipe1, idEquipe2;
        public String statut, nomTournoi, nomEquipe1, nomEquipe2;
        public Integer score1, score2;
        public String getDescriptionDuel() { return nomEquipe1 + "  VS  " + nomEquipe2; }
        public String getScoreText() { return (score1 == null) ? "-" : score1 + " - " + score2; }
        public String getStatut() { return statut; }
    }
}