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
import java.util.stream.Collectors;

@Route(value = "tournoi", layout = MainLayout.class)
@PageTitle("Détail Tournoi")
public class VueDetailTournoi extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idTournoi;
    private H2 titreVue = new H2("Chargement...");
    
    private VerticalLayout containerRondes = new VerticalLayout();
    private VerticalLayout containerMatchs = new VerticalLayout();
    
    private Tab tabMatchs;
    private Tab tabClassement;
    private Tabs tabs;

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

    // --- VUE 1 : RONDES ---

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
        
        layout.add(nbEquipesField);

        Button btnConfirmer = new Button("Générer", e -> {
            if (nbEquipesField.getValue() != null && nbEquipesField.getValue() >= 2) {
                lancerGeneration(nbEquipesField.getValue());
                dialog.close();
            }
        });
        btnConfirmer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Annuler", e -> dialog.close()), btnConfirmer);
        dialog.add(layout);
        dialog.open();
    }

    private void lancerGeneration(int nbEquipesParMatch) {
        try (Connection con = ConnectionPool.getConnection()) {
            ServiceGestionTournoi.genererNouvelleRonde(con, idTournoi, nbEquipesParMatch,1);
            Notification.show("Ronde générée !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            afficherVueRondes();
        } catch (Exception ex) {
            Notification.show("Erreur génération : " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // --- VUE 2 : MATCHS (Dynamique N équipes) ---

    private void afficherVueMatchs(RondeInfo ronde) {
        containerRondes.setVisible(false);
        containerMatchs.setVisible(true);
        containerMatchs.removeAll();

        HorizontalLayout header = new HorizontalLayout();
        Button btnRetour = new Button("Retour", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> afficherVueRondes());
        
        titreRondeEnCours.setText("Matchs de la Ronde " + ronde.numero);
        header.add(btnRetour, titreRondeEnCours);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        gridMatchs = new Grid<>(MatchInfo.class, false);
        
        // Colonne Description (Equipe 1 vs Equipe 2 vs ...)
        gridMatchs.addColumn(MatchInfo::getDescriptionDuel)
                  .setHeader("Rencontre")
                  .setAutoWidth(true)
                  .setFlexGrow(1);

        // Colonne Score DYNAMIQUE
        gridMatchs.addColumn(new ComponentRenderer<>(match -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);

            // MODE LECTURE (CLOSE)
            if ("CLOSE".equals(match.statut)) {
                // Affiche "12 - 15 - 8"
                String txt = match.equipes.stream()
                        .map(e -> String.valueOf(e.score))
                        .collect(Collectors.joining(" - "));
                Span scoreTxt = new Span(txt);
                scoreTxt.getStyle().set("font-weight", "bold");
                layout.add(scoreTxt);

                if (SessionInfo.isCurUserAdmin()) {
                    Button btnEdit = new Button(VaadinIcon.EDIT.create());
                    btnEdit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                    btnEdit.setTooltipText("Corriger (Réouvrir)");
                    btnEdit.addClickListener(e -> corrigerMatch(match));
                    layout.add(btnEdit);
                }
            } 
            // MODE ÉDITION (EN_COURS)
            else {
                if (SessionInfo.isCurUserAdmin()) {
                    List<IntegerField> inputs = new ArrayList<>();
                    
                    // On crée un champ pour CHAQUE équipe
                    for (EquipeInfo eq : match.equipes) {
                        IntegerField f = new IntegerField();
                        f.setValue(eq.score != null ? eq.score : 0);
                        f.setWidth("60px");
                        f.setTooltipText(eq.nom); // Pour savoir qui est qui si besoin
                        inputs.add(f);
                        layout.add(f);
                        
                        // Séparateur "-" sauf pour le dernier
                        if (match.equipes.indexOf(eq) < match.equipes.size() - 1) {
                            layout.add(new Span("-"));
                        }
                    }

                    Button btnValider = new Button(VaadinIcon.CHECK.create());
                    btnValider.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
                    
                    // On passe la liste des champs à la validation
                    btnValider.addClickListener(e -> validerMatch(match, inputs));
                    layout.add(btnValider);
                } else {
                    layout.add(new Span("En attente..."));
                }
            }
            return layout;
        })).setHeader("Score").setWidth("350px"); // Un peu plus large pour N équipes

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

    private void validerMatch(MatchInfo match, List<IntegerField> inputs) {
        if (match.equipes.size() != inputs.size()) return;

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Pour chaque équipe, on met à jour le score et on ajoute les points
                for (int i = 0; i < match.equipes.size(); i++) {
                    EquipeInfo eq = match.equipes.get(i);
                    Integer val = inputs.get(i).getValue();
                    int score = val != null ? val : 0;
                    
                    updateEquipeScore(con, eq.id, score);
                    ajouterPointsAuxJoueurs(con, eq.id, score);
                    
                    // Mise à jour objet local
                    eq.score = score;
                }

                try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'CLOSE' WHERE id = ?")) {
                    pst.setInt(1, match.idMatch);
                    pst.executeUpdate();
                }
                match.statut = "CLOSE";
                
                con.commit();
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
                // On retire les points pour tout le monde
                for (EquipeInfo eq : match.equipes) {
                    if (eq.score != null) retirerPointsAuxJoueurs(con, eq.id, eq.score);
                }

                try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'EN_COURS' WHERE id = ?")) {
                    pst.setInt(1, match.idMatch);
                    pst.executeUpdate();
                }
                
                match.statut = "EN_COURS";
                con.commit();
                gridMatchs.getDataProvider().refreshItem(match);
                Notification.show("Match réouvert.").addThemeVariants(NotificationVariant.LUMO_CONTRAST);

            } catch (Exception ex) {
                con.rollback(); throw ex;
            } finally { con.setAutoCommit(true); }
        } catch (Exception ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    // --- REQUÊTES SQL & OUTILS ---

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
                    remplirEquipes(con, info); // C'est ici que la magie opère
                    res.add(info);
                }
            }
        }
        return res;
    }
    
    private void remplirEquipes(Connection con, MatchInfo info) throws SQLException {
        // Cette requête récupère TOUTES les équipes du match, peu importe le nombre (2, 3, 4...)
        String sql = "SELECT e.id, e.score, GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                     "FROM equipe e " +
                     "JOIN composition c ON c.idEquipe = e.id " +
                     "JOIN joueur j ON j.id = c.idJoueur " +
                     "WHERE e.idMatch = ? " +
                     "GROUP BY e.id ORDER BY e.num";
                     
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, info.idMatch);
            try (ResultSet rs = pst.executeQuery()) {
                // Boucle while au lieu de if/if : on prend tout ce qui vient !
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nom = rs.getString("joueurs");
                    Integer score = rs.getObject("score") != null ? rs.getInt("score") : null;
                    info.equipes.add(new EquipeInfo(id, nom, score));
                }
            }
        }
    }

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
        String sql = "SELECT COUNT(e.id) FROM equipe e JOIN matchs m ON e.idMatch = m.id " +
                     "JOIN ronde r ON m.idRonde = r.id WHERE r.idTournoi = ? GROUP BY m.id LIMIT 1"; 
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 2;
    }
    private boolean isDerniereRondeTerminee(Connection con, int idTournoi) throws SQLException {
        String sql = "SELECT COUNT(*) FROM matchs m JOIN ronde r ON m.idRonde = r.id " +
                     "WHERE r.idTournoi = ? AND r.numero = (SELECT MAX(numero) FROM ronde WHERE idTournoi = ?) AND m.statut != 'CLOSE'"; 
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi); pst.setInt(2, idTournoi);
            try (ResultSet rs = pst.executeQuery()) { if (rs.next()) return rs.getInt(1) == 0; }
        }
        return true; 
    }
    private List<RondeInfo> recupererRondes(Connection con) throws SQLException {
        List<RondeInfo> res = new ArrayList<>();
        String sql = "SELECT r.id, r.numero, (SELECT COUNT(*) FROM matchs m WHERE m.idRonde = r.id AND m.statut != 'CLOSE') as encours " +
                     "FROM ronde r WHERE r.idTournoi = ? ORDER BY r.numero";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) res.add(new RondeInfo(rs.getInt("id"), rs.getInt("numero"), rs.getInt("encours") == 0));
            }
        }
        return res;
    }
    
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