package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Route(value = "ronde", layout = MainLayout.class)
public class VueDetailRonde extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idRonde;
    private H2 titreVue = new H2("Détails de la Ronde");
    private Grid<Matchs> gridMatchs;

    public VueDetailRonde() {
        Button btnRetour = new Button("Retour", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(VueListeTournois.class)));

        gridMatchs = new Grid<>(Matchs.class, false);
        gridMatchs.addColumn(Matchs::getId).setHeader("Match ID").setWidth("80px").setFlexGrow(0);
        gridMatchs.addColumn(m -> "Terrain " + m.getIdTerrain()).setHeader("Terrain");

        
        gridMatchs.addColumn(new ComponentRenderer<>(match -> {
            VerticalLayout layoutMatch = new VerticalLayout();
            layoutMatch.setPadding(false); 
            layoutMatch.setSpacing(false);

            try (Connection con = ConnectionPool.getConnection()) {
                
                String sqlEquipes = "SELECT id, num, score FROM equipe WHERE idMatch = ? ORDER BY num";
                try (PreparedStatement pstEq = con.prepareStatement(sqlEquipes)) {
                    pstEq.setInt(1, match.getId());
                    try (ResultSet rsEq = pstEq.executeQuery()) {
                        while (rsEq.next()) {
                            int idEq = rsEq.getInt("id");
                            int numEq = rsEq.getInt("num");
                            int scoreEq = rsEq.getInt("score");

                            
                            List<String> surnoms = new ArrayList<>();
                            String sqlJoueurs = "SELECT j.surnom FROM joueur j " +
                                               "JOIN composition c ON j.id = c.idJoueur " +
                                               "WHERE c.idEquipe = ?";
                            try (PreparedStatement pstJo = con.prepareStatement(sqlJoueurs)) {
                                pstJo.setInt(1, idEq);
                                try (ResultSet rsJo = pstJo.executeQuery()) {
                                    while (rsJo.next()) {
                                        surnoms.add(rsJo.getString("surnom"));
                                    }
                                }
                            }

                            String listeJoueurs = String.join(", ", surnoms);
                            Span line = new Span("Équipe " + numEq + " [" + listeJoueurs + "] - Score: " + scoreEq);
                            layoutMatch.add(line);
                        }
                    }
                }
            } catch (Exception e) { 
                layoutMatch.add(new Span("Erreur de chargement des données")); 
            }
            return layoutMatch;
        })).setHeader("Participants et Scores");

        gridMatchs.addColumn(Matchs::getStatut).setHeader("Statut");

        
        if (SessionInfo.isCurUserAdmin()) {
            gridMatchs.addComponentColumn(match -> {
                if ("CLOSE".equals(match.getStatut())) {
                    return new Span("Terminé");
                }
                Button btn = new Button("Clore", e -> terminerLeMatch(match));
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                return btn;
            }).setHeader("Action");
        }

        add(btnRetour, titreVue, gridMatchs);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        if (parameter != null) { 
            this.idRonde = parameter; 
            refresh(); 
        }
    }

    private void refresh() {
        try (Connection con = ConnectionPool.getConnection()) {
            gridMatchs.setItems(Matchs.findAll(con, idRonde));
        } catch (Exception ex) { 
            Notification.show(ex.getMessage()); 
        }
    }

    private void terminerLeMatch(Matchs match) {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement pst = con.prepareStatement("UPDATE matchs SET statut = 'CLOSE' WHERE id = ?")) {
                pst.setInt(1, match.getId()); 
                pst.executeUpdate();
            }
            ServiceGestionTournoi.verifierEtCloturerRonde(con, idRonde);
            refresh();
            Notification.show("Match clôturé").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) { 
            Notification.show("Erreur : " + ex.getMessage()); 
        }
    }
}