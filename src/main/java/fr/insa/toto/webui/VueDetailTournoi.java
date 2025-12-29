package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
                }
            }
        } catch (SQLException e) { titreVue.setText("Erreur : " + e.getMessage()); }
    }

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
            gridRondes.setItems(rondes);
            containerRondes.add(gridRondes);
        } catch (SQLException e) { containerRondes.add(new Span("Erreur : " + e.getMessage())); }
    }

    private void preparerGenerationRonde() {
        try (Connection con = ConnectionPool.getConnection()) {
            if (!isDerniereRondeTerminee(con, idTournoi)) {
                Notification.show("Impossible : Terminez les matchs de la ronde actuelle d'abord.").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            int nbRondesExistantes = recupererNombreRondes(con);
            if (nbRondesExistantes == 0) {
                afficherDialogConfigurationInitiale();
            } else {
                int nbEquipesParMatch = recupererConfigEquipesParMatch(con);
                int tempsMatchPrecedent = recupererTempsDerniereRonde(con); 
                lancerGeneration(nbEquipesParMatch, tempsMatchPrecedent);
            }
        } catch (Exception ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    private void lancerGeneration(int nbEquipesParMatch, int tempsMatch) {
        try (Connection con = ConnectionPool.getConnection()) {
            ServiceGestionTournoi.genererNouvelleRonde(con, idTournoi, nbEquipesParMatch, tempsMatch);
            Notification.show("Ronde générée !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            afficherVueRondes();
        } catch (Exception ex) { Notification.show("Erreur génération : " + ex.getMessage()); }
    }

    private void afficherVueMatchs(RondeInfo ronde) {
        containerRondes.setVisible(false);
        containerMatchs.setVisible(true);
        containerMatchs.removeAll();

        HorizontalLayout header = new HorizontalLayout();
        Button btnRetour = new Button("Retour", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> afficherVueRondes());
        titreRondeEnCours.setText("Matchs de la Ronde " + ronde.numero);
        header.add(btnRetour, titreRondeEnCours);

        gridMatchs = new Grid<>(MatchInfo.class, false);
        gridMatchs.addColumn(MatchInfo::getDescriptionDuel).setHeader("Rencontre").setAutoWidth(true);

        gridMatchs.addColumn(new ComponentRenderer<>(match -> {
            HorizontalLayout layout = new HorizontalLayout();
            if ("CLOSE".equals(match.statut)) {
                String txt = match.equipes.stream().map(e -> String.valueOf(e.score)).collect(Collectors.joining(" - "));
                layout.add(new Span(txt));
            } else {
                if (SessionInfo.isCurUserAdmin()) {
                    List<IntegerField> inputs = new ArrayList<>();
                    for (EquipeInfo eq : match.equipes) {
                        IntegerField f = new IntegerField();
                        f.setValue(eq.score != null ? eq.score : 0);
                        f.setWidth("60px");
                        inputs.add(f);
                        layout.add(f);
                    }
                    Button btnValider = new Button(VaadinIcon.CHECK.create(), e -> validerMatch(match, inputs));
                    btnValider.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                    layout.add(btnValider);
                } else { layout.add(new Span("En cours...")); }
            }
            return layout;
        })).setHeader("Score").setWidth("350px");

        try (Connection con = ConnectionPool.getConnection()) {
            gridMatchs.setItems(recupererMatchsDeLaRonde(con, ronde.id));
        } catch (SQLException e) { containerMatchs.add(new Span("Erreur : " + e.getMessage())); }
        containerMatchs.add(header, gridMatchs);
    }

    private void remplirEquipes(Connection con, MatchInfo info) throws SQLException {
        String sql = "SELECT e.id, e.score, GROUP_CONCAT(j.surnom SEPARATOR ', ') as joueurs " +
                     "FROM equipe e JOIN composition c ON c.idEquipe = e.id " +
                     "JOIN joueur j ON j.id = c.idJoueur WHERE e.idMatch = ? " +
                     "GROUP BY e.id ORDER BY e.num";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, info.idMatch);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    info.equipes.add(new EquipeInfo(rs.getInt("id"), rs.getString("joueurs"), 
                                    rs.getObject("score") != null ? rs.getInt("score") : null));
                }
            }
        }
    }

    private void afficherClassementLocal() {
        containerRondes.setVisible(false); containerMatchs.setVisible(false);
        Grid<JoueurScoreLocal> grid = new Grid<>(JoueurScoreLocal.class, false);
        grid.addColumn(j -> j.surnom).setHeader("Joueur");
        grid.addColumn(j -> j.scoreLocal).setHeader("Points").setSortable(true);
        try (Connection con = ConnectionPool.getConnection()) {
            List<JoueurScoreLocal> list = new ArrayList<>();
            String sql = "SELECT j.surnom, SUM(e.score) as total FROM joueur j " +
                         "JOIN composition c ON j.id = c.idJoueur JOIN equipe e ON c.idEquipe = e.id " +
                         "JOIN matchs m ON e.idMatch = m.id JOIN ronde r ON m.idRonde = r.id " +
                         "WHERE r.idTournoi = ? GROUP BY j.id ORDER BY total DESC";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, idTournoi);
                ResultSet rs = pst.executeQuery();
                while(rs.next()) list.add(new JoueurScoreLocal(rs.getString("surnom"), rs.getInt("total")));
            }
            grid.setItems(list); containerRondes.removeAll(); containerRondes.add(grid); containerRondes.setVisible(true);
        } catch (SQLException e) { Notification.show(e.getMessage()); }
    }
    
    // Les autres méthodes privées (recupererRondes, updateEquipeScore, etc.) suivent ici
}