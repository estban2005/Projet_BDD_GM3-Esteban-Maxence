package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.webui.security.SessionInfo;
import fr.insa.toto.model.ServiceGestionTournoi;

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
    
    // Conteneurs
    private VerticalLayout containerRondes = new VerticalLayout();
    private VerticalLayout containerMatchs = new VerticalLayout();
    
    private Tab tabMatchs;
    private Tab tabClassement;
    private Tabs tabs;

    // Grilles
    private Grid<RondeInfo> gridRondes;
    private Grid<MatchInfo> gridMatchs;
    
    private H3 titreRondeEnCours = new H3("");

    public VueDetailTournoi() {
        add(titreVue);

        tabMatchs = new Tab("Rondes & Matchs");
        tabClassement = new Tab("Classement Local");
        tabs = new Tabs(tabMatchs, tabClassement);

        VerticalLayout contentMatchsTab = new VerticalLayout();
        contentMatchsTab.setPadding(false);
        
        containerRondes.setSizeFull();
        containerMatchs.setVisible(false);
        containerMatchs.setSizeFull();

        contentMatchsTab.add(containerRondes, containerMatchs);

        tabs.addSelectedChangeListener(event -> {
            contentMatchsTab.setVisible(event.getSelectedTab().equals(tabMatchs));
            if (event.getSelectedTab().equals(tabMatchs)) {
                afficherVueRondes();
            } else {
                afficherClassementLocal();
            }
        });

        add(tabs, contentMatchsTab);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.idTournoi = parameter;
        chargerInfosTournoi();
        tabs.setSelectedTab(tabMatchs);
        afficherVueRondes(); 
    }

    private void chargerInfosTournoi() {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement pst = con.prepareStatement("SELECT nom FROM tournoi WHERE id = ?")) {
                pst.setInt(1, idTournoi);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) titreVue.setText("Tournoi : " + rs.getString("nom"));
                    else titreVue.setText("Tournoi inconnu");
                }
            }
        } catch (SQLException e) {
            titreVue.setText("Erreur : " + e.getMessage());
        }
    }

    // =========================================================================
    // VUE 1 : LISTE DES RONDES + GENERATION INTELLIGENTE
    // =========================================================================

    private void afficherVueRondes() {
        containerMatchs.setVisible(false);
        containerRondes.setVisible(true);
        containerRondes.removeAll(); 

        HorizontalLayout headerRondes = new HorizontalLayout();
        headerRondes.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRondes.setWidthFull();
        headerRondes.add(new H3("Liste des Rondes"));
        
        if (SessionInfo.isCurUserAdmin()) {
            Button btnGenerer = new Button("Générer la ronde suivante", VaadinIcon.PLUS.create());
            btnGenerer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnGenerer.addClickListener(e -> preparerGenerationRonde());
            
            headerRondes.add(btnGenerer);
            headerRondes.setFlexGrow(1, btnGenerer);
            headerRondes.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        }
        
        containerRondes.add(headerRondes);

        gridRondes = new Grid<>(RondeInfo.class, false);
        gridRondes.addColumn(r -> "Ronde " + r.numero).setHeader("Nom").setSortable(true);
        
        gridRondes.addComponentColumn(r -> {
            Span badge = new Span(r.estTerminee ? "Terminée" : "En cours");
            badge.getElement().getThemeList().add(r.estTerminee ? "badge success" : "badge");
            return badge;
        }).setHeader("Statut");

        gridRondes.addComponentColumn(r -> {
            Button btnVoir = new Button("Voir les matchs", VaadinIcon.ARROW_RIGHT.create());
            btnVoir.addClickListener(e -> afficherVueMatchs(r));
            return btnVoir;
        });

        try (Connection con = ConnectionPool.getConnection()) {
            List<RondeInfo> rondes = recupererRondes(con);
            if (rondes.isEmpty()) {
                containerRondes.add(new Span("Aucune ronde. Cliquez sur 'Générer' pour lancer le tournoi."));
            } else {
                gridRondes.setItems(rondes);
                containerRondes.add(gridRondes);
            }
        } catch (SQLException e) {
            containerRondes.add(new Span("Erreur : " + e.getMessage()));
        }
    }

    private void preparerGenerationRonde() {
        try (Connection con = ConnectionPool.getConnection()) {
            if (!isDerniereRondeTerminee(con, idTournoi)) {
                Notification.show("Impossible : Terminez les matchs de la ronde actuelle d'abord.")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            int nbRondesExistantes = recupererNombreRondes(con);

            if (nbRondesExistantes == 0) {
                afficherDialogConfigurationInitiale();
            } else {
                int nbEquipesParMatch = recupererConfigEquipesParMatch(con);
                lancerGeneration(nbEquipesParMatch);
            }
            
        } catch (Exception ex) {
            Notification.show("Erreur pré-génération : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void afficherDialogConfigurationInitiale() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Démarrage du Tournoi");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Span("C'est la première ronde. Veuillez configurer les matchs."));
        
        IntegerField nbEquipesField = new IntegerField("Nombre d'équipes par match");
        nbEquipesField.setValue(2);
        nbEquipesField.setMin(2);
        nbEquipesField.setStepButtonsVisible(true);
        nbEquipesField.setHelperText("Ex: 2 pour des duels, 3 pour des triangulaires...");
        
        layout.add(nbEquipesField);

        Button btnConfirmer = new Button("Générer la ronde 1", e -> {
            if (nbEquipesField.getValue() != null && nbEquipesField.getValue() >= 2) {
                lancerGeneration(nbEquipesField.getValue());
                dialog.close();
            } else {
                Notification.show("Valeur invalide");
            }
        });
        btnConfirmer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button btnAnnuler = new Button("Annuler", e -> dialog.close());

        dialog.getFooter().add(btnAnnuler, btnConfirmer);
        dialog.add(layout);
        dialog.open();
    }

    private void lancerGeneration(int nbEquipesParMatch) {
        try (Connection con = ConnectionPool.getConnection()) {
            ServiceGestionTournoi.genererNouvelleRonde(con, idTournoi, nbEquipesParMatch);
            Notification.show("Nouvelle ronde générée (" + nbEquipesParMatch + " équipes/match) !")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            afficherVueRondes();
        } catch (Exception ex) {
            Notification.show("Erreur génération : " + ex.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            ex.printStackTrace();
        }
    }

    // --- REQUETES SQL UTILES ---

    private int recupererNombreRondes(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ronde WHERE idTournoi = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private int recupererConfigEquipesParMatch(Connection con) throws SQLException {
        String sql = "SELECT COUNT(e.id) " +
                     "FROM equipe e " +
                     "JOIN matchs m ON e.idMatch = m.id " +
                     "JOIN ronde r ON m.idRonde = r.id " +
                     "WHERE r.idTournoi = ? " +
                     "GROUP BY m.id " +
                     "LIMIT 1"; 
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 2;
    }

    private boolean isDerniereRondeTerminee(Connection con, int idTournoi) throws SQLException {
        String sql = "SELECT COUNT(*) FROM matchs m " +
                     "JOIN ronde r ON m.idRonde = r.id " +
                     "WHERE r.idTournoi = ? " +
                     "AND r.numero = (SELECT MAX(numero) FROM ronde WHERE idTournoi = ?) " +
                     "AND m.statut != 'CLOSE'"; 
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            pst.setInt(2, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        }
        return true; 
    }

    private List<RondeInfo> recupererRondes(Connection con) throws SQLException {
        List<RondeInfo> res = new ArrayList<>();
        String sql = "SELECT r.id, r.numero, " +
                     "(SELECT COUNT(*) FROM matchs m WHERE m.idRonde = r.id AND m.statut != 'CLOSE') as encours " +
                     "FROM ronde r WHERE r.idTournoi = ? ORDER BY r.numero";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    boolean terminee = rs.getInt("encours") == 0;
                    res.add(new RondeInfo(rs.getInt("id"), rs.getInt("numero"), terminee));
                }
            }
        }
        return res;
    }

    // =========================================================================
    // VUE 2 : LISTE DES MATCHS
    // =========================================================================

    private void afficherVueMatchs(RondeInfo ronde) {
        containerRondes.setVisible(false);
        containerMatchs.setVisible(true);
        containerMatchs.removeAll();

        HorizontalLayout header = new HorizontalLayout();
        Button btnRetour = new Button("Retour aux rondes", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> afficherVueRondes());
        
        titreRondeEnCours.setText("Matchs de la Ronde " + ronde.numero);
        header.add(btnRetour, titreRondeEnCours);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        gridMatchs = new Grid<>(MatchInfo.class, false);
        gridMatchs.addColumn(MatchInfo::getDescriptionDuel).setHeader("Rencontre").setAutoWidth(true).setFlexGrow(1);

        gridMatchs.addColumn(new ComponentRenderer<>(match -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);

            if ("CLOSE".equals(match.statut)) {
                Span scoreTxt = new Span(match.score1 + " - " + match.score2);
                scoreTxt.getStyle().set("font-weight", "bold");
                layout.add(scoreTxt);

                if (SessionInfo.isCurUserAdmin()) {
                    Button btnEdit = new Button(VaadinIcon.EDIT.create());
                    btnEdit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                    btnEdit.setTooltipText("Corriger (Annule les points et réouvre)");
                    btnEdit.addClickListener(e -> corrigerMatch(match));
                    layout.add(btnEdit);
                }
            } else {
                if (SessionInfo.isCurUserAdmin()) {
                    IntegerField fieldS1 = new IntegerField();
                    fieldS1.setValue(match.score1 != null ? match.score1 : 0);
                    fieldS1.setWidth("70px");

                    IntegerField fieldS2 = new IntegerField();
                    fieldS2.setValue(match.score2 != null ? match.score2 : 0);
                    fieldS2.setWidth("70px");

                    Button btnValider = new Button(VaadinIcon.CHECK.create());
                    btnValider.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
                    btnValider.addClickListener(e -> validerMatch(match, fieldS1.getValue(), fieldS2.getValue()));

                    layout.add(fieldS1, new Span("-"), fieldS2, btnValider);
                } else {
                    layout.add(new Span("En attente..."));
                }
            }
            return layout;
        })).setHeader("Score").setWidth("300px");

        gridMatchs.addComponentColumn(match -> {
            Span badge = new Span("CLOSE".equals(match.statut) ? "Terminé" : "En cours");
            badge.getElement().getThemeList().add("CLOSE".equals(match.statut) ? "badge success" : "badge");
            return badge;
        }).setHeader("Statut").setWidth("120px");

        try (Connection con = ConnectionPool.getConnection()) {
            List<MatchInfo> matchs = recupererMatchsDeLaRonde(con, ronde.id);
            gridMatchs.setItems(matchs);
        } catch (SQLException e) {
            containerMatchs.add(new Span("Erreur : " + e.getMessage()));
        }
        containerMatchs.add(header, gridMatchs);
    }

    private void validerMatch(MatchInfo match, Integer s1, Integer s2) {
        int finalS1 = s1 != null ? s1 : 0;
        int finalS2 = s2 != null ? s2 : 0;

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            try {
                updateEquipeScore(con, match.idEquipe1, finalS1);
                updateEquipeScore(con, match.idEquipe2, finalS2);
                try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'CLOSE' WHERE id = ?")) {
                    pst.setInt(1, match.idMatch);
                    pst.executeUpdate();
                }
                ajouterPointsAuxJoueurs(con, match.idEquipe1, finalS1);
                ajouterPointsAuxJoueurs(con, match.idEquipe2, finalS2);
                con.commit();
                
                match.score1 = finalS1;
                match.score2 = finalS2;
                match.statut = "CLOSE";
                gridMatchs.getDataProvider().refreshItem(match);
                Notification.show("Match validé !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                con.rollback(); throw ex;
            } finally { con.setAutoCommit(true); }
        } catch (Exception ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    private void corrigerMatch(MatchInfo match) {
        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (match.score1 != null) retirerPointsAuxJoueurs(con, match.idEquipe1, match.score1);
                if (match.score2 != null) retirerPointsAuxJoueurs(con, match.idEquipe2, match.score2);
                try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'EN_COURS' WHERE id = ?")) {
                    pst.setInt(1, match.idMatch);
                    pst.executeUpdate();
                }
                con.commit();
                match.statut = "EN_COURS";
                gridMatchs.getDataProvider().refreshItem(match);
                Notification.show("Match réouvert.").addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            } catch (Exception ex) {
                con.rollback(); throw ex;
            } finally { con.setAutoCommit(true); }
        } catch (Exception ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    // --- SQL HELPERS ---
    private void updateEquipeScore(Connection con, int idEquipe, int score) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?")) {
            pst.setInt(1, score); pst.setInt(2, idEquipe); pst.executeUpdate();
        }
    }
    private void ajouterPointsAuxJoueurs(Connection con, int idEquipe, int points) throws SQLException {
        modifierPointsJoueurs(con, idEquipe, points, true);
    }
    private void retirerPointsAuxJoueurs(Connection con, int idEquipe, int points) throws SQLException {
        modifierPointsJoueurs(con, idEquipe, points, false);
    }
    private void modifierPointsJoueurs(Connection con, int idEquipe, int points, boolean ajouter) throws SQLException {
        String sql = "SELECT idJoueur FROM composition WHERE idEquipe = ?";
        try (PreparedStatement pstSelect = con.prepareStatement(sql)) {
            pstSelect.setInt(1, idEquipe);
            try (ResultSet rs = pstSelect.executeQuery()) {
                while (rs.next()) {
                    try (PreparedStatement pstUpdate = con.prepareStatement(
                            "UPDATE joueur SET scoreTotal = scoreTotal " + (ajouter ? "+" : "-") + " ? WHERE id = ?")) {
                        pstUpdate.setInt(1, points); pstUpdate.setInt(2, rs.getInt("idJoueur")); pstUpdate.executeUpdate();
                    }
                }
            }
        }
    }

    private List<MatchInfo> recupererMatchsDeLaRonde(Connection con, int idRonde) throws SQLException {
        List<MatchInfo> res = new ArrayList<>();
        String sql = "SELECT m.id, m.idTerrain, m.statut FROM matchs m WHERE m.idRonde = ? ORDER BY m.id";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idRonde);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    MatchInfo info = new MatchInfo();
                    info.idMatch = rs.getInt("id");
                    info.idTerrain = rs.getInt("idTerrain");
                    info.statut = rs.getString("statut");
                    remplirEquipes(con, info);
                    res.add(info);
                }
            }
        }
        return res;
    }
    private void remplirEquipes(Connection con, MatchInfo info) throws SQLException {
        String sql = "SELECT e.id, e.score, GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                     "FROM equipe e JOIN composition c ON c.idEquipe = e.id JOIN joueur j ON j.id = c.idJoueur " +
                     "WHERE e.idMatch = ? GROUP BY e.id ORDER BY e.num";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, info.idMatch);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) { 
                    info.idEquipe1 = rs.getInt("id"); info.nomEquipe1 = rs.getString("joueurs");
                    info.score1 = rs.getObject("score") != null ? rs.getInt("score") : null;
                }
                if (rs.next()) { 
                    info.idEquipe2 = rs.getInt("id"); info.nomEquipe2 = rs.getString("joueurs");
                    info.score2 = rs.getObject("score") != null ? rs.getInt("score") : null;
                }
            }
        }
    }

    // --- VUE 3 : CLASSEMENT ---
    private void afficherClassementLocal() {
        containerRondes.setVisible(false); containerMatchs.setVisible(false); containerRondes.removeAll();
        Grid<JoueurScoreLocal> grid = new Grid<>(JoueurScoreLocal.class, false);
        grid.addColumn(j -> j.surnom).setHeader("Joueur");
        grid.addColumn(j -> j.scoreLocal).setHeader("Points (Ce tournoi)").setSortable(true);
        try (Connection con = ConnectionPool.getConnection()) {
            List<JoueurScoreLocal> liste = new ArrayList<>();
            String sql = "SELECT j.surnom, COALESCE(SUM(e.score), 0) as total FROM joueur j " +
                         "JOIN composition c ON j.id = c.idJoueur JOIN equipe e ON c.idEquipe = e.id " +
                         "JOIN matchs m ON e.idMatch = m.id JOIN ronde r ON m.idRonde = r.id " +
                         "WHERE r.idTournoi = ? GROUP BY j.id, j.surnom ORDER BY total DESC";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, idTournoi);
                try(ResultSet rs = pst.executeQuery()){
                    while(rs.next()) liste.add(new JoueurScoreLocal(rs.getString("surnom"), rs.getInt("total")));
                }
            }
            grid.setItems(liste); containerRondes.add(grid); containerRondes.setVisible(true);
        } catch (SQLException e) { containerRondes.add(new Span("Erreur : " + e.getMessage())); containerRondes.setVisible(true); }
    }
}