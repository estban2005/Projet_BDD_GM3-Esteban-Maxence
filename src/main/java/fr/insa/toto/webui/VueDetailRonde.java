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
import java.util.List;

@Route(value = "ronde", layout = MainLayout.class)
public class VueDetailRonde extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idRonde;
    private H2 titreVue;
    private Grid<Matchs> gridMatchs;
    private Button btnRetour;

    public VueDetailRonde() {
        titreVue = new H2("Détails de la Ronde");
        
        btnRetour = new Button("Retour à la liste des tournois", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(VueListeTournois.class)));

        gridMatchs = new Grid<>(Matchs.class, false);
        
        gridMatchs.addColumn(Matchs::getId)
                  .setHeader("ID Match")
                  .setWidth("100px").setFlexGrow(0);
        
        gridMatchs.addColumn(m -> "Terrain n°" + m.getIdTerrain())
                  .setHeader("Terrain");

        gridMatchs.addColumn(Matchs::getStatut)
                  .setHeader("Statut");

        gridMatchs.addComponentColumn(match -> {
            Button btnAction = new Button();

            if ("TERMINÉ".equals(match.getStatut())) {
                btnAction.setText("Terminé");
                btnAction.setEnabled(false);
                btnAction.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            } else {
                btnAction.setText("Valider Scores & Finir");
                btnAction.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                
                btnAction.addClickListener(e -> {
                    terminerLeMatch(match);
                });
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
            this.titreVue.setText("Matchs de la Ronde #" + idRonde);
            
            List<Matchs> matchs = Matchs.findAll(con, idRonde);
            gridMatchs.setItems(matchs);
            
        } catch (Exception ex) {
            Notification.show("Erreur de chargement : " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
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

            Notification.show("Match terminé avec succès !")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            refresh(); 

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur lors de la validation : " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}