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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Route(value = "tournois", layout = MainLayout.class)
@PageTitle("Liste des Tournois")
public class VueListeTournois extends VerticalLayout {

    private Grid<Tournoi> grid;

    public VueListeTournois() {
        add(new H2("Tournois disponibles"));

        grid = new Grid<>(Tournoi.class, false);
        grid.addColumn(Tournoi::getId).setHeader("ID").setWidth("50px").setFlexGrow(0);
        grid.addColumn(Tournoi::getNom).setHeader("Nom");
        grid.addColumn(Tournoi::getNbTerrains).setHeader("Terrains");
        grid.addColumn(Tournoi::getNbJoueursParEquipe).setHeader("J/Équipe");
        
        grid.addComponentColumn(tournoi -> {
            Button btnDetails = new Button("Voir", VaadinIcon.EYE.create());
            btnDetails.addClickListener(e -> {
                UI.getCurrent().navigate(VueDetailTournoi.class, tournoi.getId());
            });
            return btnDetails;
        }).setHeader("Actions");

        add(grid);

        if (SessionInfo.isCurUserAdmin()) {
            Button btnCreate = new Button("Nouveau Tournoi", VaadinIcon.PLUS.create());
            btnCreate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnCreate.addClickListener(e -> ouvrirDialogCreation());
            add(btnCreate);
        }

        rafraichirGrille();
    }

    private void rafraichirGrille() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> liste = Tournoi.findAll(con);
            grid.setItems(liste);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ouvrirDialogCreation() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Créer un nouveau tournoi");

        TextField nom = new TextField("Nom");
        IntegerField nbTerrains = new IntegerField("Nb Terrains");
        nbTerrains.setValue(2);
        nbTerrains.setMin(1);
        
        IntegerField nbJoueurs = new IntegerField("Joueurs par équipe");
        nbJoueurs.setValue(1);
        nbJoueurs.setMin(1);

        Button save = new Button("Créer", e -> {
            if (nom.isEmpty() || nbTerrains.isEmpty() || nbJoueurs.isEmpty()) {
                Notification.show("Remplissez tous les champs");
                return;
            }
            creerTournoi(nom.getValue(), nbTerrains.getValue(), nbJoueurs.getValue());
            dialog.close();
            rafraichirGrille();
        });
        
        FormLayout form = new FormLayout(nom, nbTerrains, nbJoueurs);
        dialog.add(form, save);
        dialog.open();
    }

    private void creerTournoi(String nom, int terrains, int joueurs) {
        try (Connection con = ConnectionPool.getConnection()) {
            Tournoi t = new Tournoi(nom, terrains, joueurs);
            t.insertInDB(con);
            Notification.show("Tournoi créé ! ID: " + t.getId())
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (SQLException ex) {
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}