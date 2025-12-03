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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.ServiceGestionTournoi;
import fr.insa.toto.model.Tournoi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "gestion-tournoi", layout = MainLayout.class)
@PageTitle("Gestion Tournoi")
public class VueGestionTournoi extends VerticalLayout {

    public VueGestionTournoi() {
        add(new H2("Administration du Tournoi"));
        
        ComboBox<Tournoi> tournoiSelect = new ComboBox<>("Choisir le tournoi à gérer");
        tournoiSelect.setItemLabelGenerator(Tournoi::getNom);
        
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> liste = Tournoi.findAll(con);
            tournoiSelect.setItems(liste);
            if (!liste.isEmpty()) tournoiSelect.setValue(liste.get(0)); // Sélection par défaut
        } catch (SQLException e) {
            e.printStackTrace();
        }

        IntegerField nbEquipesField = new IntegerField("Nombre d'équipes par match");
        nbEquipesField.setWidth("300px");
        nbEquipesField.setValue(2);
        nbEquipesField.setMin(2);
        
        Button btnGenerer = new Button("Générer un nouveau round");
        
        btnGenerer.addClickListener(e -> {
            Tournoi t = tournoiSelect.getValue();
            if (t == null) {
                Notification.show("Veuillez sélectionner un tournoi !");
                return;
            }

            try (Connection con = ConnectionPool.getConnection()) {
                int idTournoi = t.getId(); 
                int nbEquipes = nbEquipesField.getValue() != null ? nbEquipesField.getValue() : 2;
                
                ServiceGestionTournoi.genererNouvelleRonde(con, idTournoi, nbEquipes);
                Notification.show("Ronde générée pour le tournoi : " + t.getNom());
                
            } catch (Exception ex) {
                Notification.show("Erreur : " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        add(new Paragraph("Sélectionnez le tournoi, puis lancez une ronde."), 
            tournoiSelect, 
            nbEquipesField, 
            btnGenerer);
    }
}