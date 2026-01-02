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
        }
        containerRondes.add(headerRondes);

        gridRondes = new Grid<>(RondeInfo.class, false);
        gridRondes.addColumn(r -> "Ronde " + r.numero).setHeader("Nom").setSortable(true);
        
        
        gridRondes.addColumn(r -> r.duree + " min").setHeader("Temps");

        gridRondes.addComponentColumn(r -> {
            Span badge = new Span(r.estTerminee ? "Terminée" : "En cours");
            badge.getElement().getThemeList().add(r.estTerminee ? "badge success" : "badge");
            return badge;
        }).setHeader("Statut");

        gridRondes.addComponentColumn(r -> {
            Button btnVoir = new Button("Voir les matchs", VaadinIcon.ARROW_RIGHT.create());
            btnVoir.addClickListener(e -> {
                
                getUI().ifPresent(ui -> ui.navigate(VueDetailRonde.class, r.id));
            });
            return btnVoir;
        }).setHeader("Action");

        try (Connection con = ConnectionPool.getConnection()) {
            List<RondeInfo> rondes = recupererRondes(con);
            gridRondes.setItems(rondes);
            containerRondes.add(gridRondes);
        } catch (SQLException e) { containerRondes.add(new Span("Erreur : " + e.getMessage())); }
    }

    private void preparerGenerationRonde() {
        try (Connection con = ConnectionPool.getConnection()) {
            if (!isDerniereRondeTerminee(con, idTournoi)) {
                Notification.show("Terminez les matchs de la ronde actuelle d'abord.").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            afficherDialogConfigurationInitiale();
        } catch (Exception ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    private void afficherDialogConfigurationInitiale() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Configuration de la Ronde");
        VerticalLayout layout = new VerticalLayout();
        
        IntegerField nbEquipesField = new IntegerField("Nombre d'équipes par match");
        nbEquipesField.setValue(2);
        
      
        IntegerField dureeField = new IntegerField("Durée (minutes)");
        dureeField.setValue(15);
        
        layout.add(nbEquipesField, dureeField);

        Button btnConfirmer = new Button("Générer", e -> {
            lancerGeneration(nbEquipesField.getValue(), dureeField.getValue());
            dialog.close();
        });
        btnConfirmer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Annuler", ev -> dialog.close()), btnConfirmer);
        dialog.add(layout);
        dialog.open();
    }

   
    private void lancerGeneration(int nbEquipes, int duree) {
        try (Connection con = ConnectionPool.getConnection()) {
            
            ServiceGestionTournoi.genererNouvelleRonde(con, idTournoi, nbEquipes, duree);
            Notification.show("Ronde générée avec succès !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            afficherVueRondes(); 
        } catch (Exception ex) {
            Notification.show("Erreur de génération : " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

 
    private List<RondeInfo> recupererRondes(Connection con) throws SQLException {
        List<RondeInfo> res = new ArrayList<>();
        String sql = "SELECT r.id, r.numero, r.duree, (SELECT COUNT(*) FROM matchs m WHERE m.idRonde = r.id AND m.statut != 'CLOSE') as encours " +
                     "FROM ronde r WHERE r.idTournoi = ? ORDER BY r.numero";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    res.add(new RondeInfo(rs.getInt("id"), rs.getInt("numero"), rs.getInt("encours") == 0, rs.getInt("duree")));
                }
            }
        }
        return res;
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

    private void afficherVueMatchs(RondeInfo ronde) {  }
    private void afficherClassementLocal() { }
}