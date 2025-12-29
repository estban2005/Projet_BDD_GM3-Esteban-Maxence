package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Matchs;
import fr.insa.toto.model.ServiceGestionTournoi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Route(value = "ronde", layout = MainLayout.class)
public class VueDetailRonde extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idRonde;
    private H2 titreVue;
    private Grid<Matchs> gridMatchs;
    private Button btnRetour;

    public VueDetailRonde() {
        titreVue = new H2("Détails de la Ronde");
        
        btnRetour = new Button("Retour", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(VueListeTournois.class)));

        gridMatchs = new Grid<>(Matchs.class, false);
        gridMatchs.addColumn(Matchs::getId).setHeader("ID Match").setWidth("100px");
        gridMatchs.addColumn(m -> "Terrain n°" + m.getIdTerrain()).setHeader("Terrain");
        gridMatchs.addColumn(Matchs::getStatut).setHeader("Statut");

        gridMatchs.addComponentColumn(match -> {
            Button btnAction = new Button();
            if ("TERMINÉ".equals(match.getStatut())) {
                btnAction.setText("Terminé");
                btnAction.setEnabled(false);
            } else {
                btnAction.setText("Finir Match");
                btnAction.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                btnAction.addClickListener(e -> terminerLeMatch(match));
            }
            return btnAction;
        }).setHeader("Action");

        add(btnRetour, titreVue, gridMatchs);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.idRonde = parameter;
        refresh();
    }

    private void refresh() {
        try (Connection con = ConnectionPool.getConnection()) {
            // --- RECUPERATION INFOS RONDE (NUMERO + DUREE) ---
            String sqlRonde = "SELECT numero, duree FROM ronde WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sqlRonde)) {
                pst.setInt(1, idRonde);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        this.titreVue.setText("Ronde #" + rs.getInt("numero") + " (" + rs.getInt("duree") + " min)");
                    }
                }
            }
            
            List<Matchs> matchs = Matchs.findAll(con, idRonde);
            gridMatchs.setItems(matchs);
            
        } catch (Exception ex) {
            Notification.show("Erreur : " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void terminerLeMatch(Matchs match) {
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "UPDATE matchs SET statut = 'TERMINÉ' WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, match.getId());
                pst.executeUpdate();
            }
            ServiceGestionTournoi.verifierEtCloturerRonde(con, idRonde);
            refresh(); 
        } catch (Exception ex) {
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}