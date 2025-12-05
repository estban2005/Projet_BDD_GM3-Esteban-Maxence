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
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Route(value = "configuration", layout = MainLayout.class)
@PageTitle("Configuration Tournoi")
public class VueConfigurationTournoi extends VerticalLayout {

    private TextField nomTournoi;
    private IntegerField nbTerrains;
    private IntegerField nbJoueursParEquipe;
    
    private final int ID_TOURNOI = 1;

    public VueConfigurationTournoi() {
        add(new H2("Paramètres du Tournoi"));
        add(new Paragraph("Modifiez ici les règles globales. Attention : changer la taille des équipes en cours de route peut déséquilibrer le classement !"));

        nomTournoi = new TextField("Nom du Tournoi");
        nbTerrains = new IntegerField("Nombre de terrains disponibles");
        nbTerrains.setMin(1);
        nbTerrains.setHelperText("Détermine le nombre maximum de matchs simultanés");
        
        nbJoueursParEquipe = new IntegerField("Joueurs par équipe");
        nbJoueursParEquipe.setMin(1);
        nbJoueursParEquipe.setHelperText("Ex: 1 pour simple, 2 pour double, 11 pour foot...");

        Button btnSave = new Button("Enregistrer les modifications");
        btnSave.addClickListener(e -> sauvegarderConfig());

        FormLayout form = new FormLayout();
        form.add(nomTournoi, nbTerrains, nbJoueursParEquipe);
        
        chargerConfig();

        add(form, btnSave);
    }

    private void chargerConfig() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> tournois = Tournoi.findAll(con);
            Tournoi t = tournois.stream()
                    .filter(tour -> tour.getId() == ID_TOURNOI)
                    .findFirst()
                    .orElse(null);

            if (t != null) {
                nomTournoi.setValue(t.getNom());
                nbTerrains.setValue(t.getNbTerrains());
                nbJoueursParEquipe.setValue(t.getNbJoueursParEquipe());
            } else {
                Notification.show("Aucun tournoi trouvé (ID 1 manquants)");
            }
        } catch (SQLException e) {
            Notification.show("Erreur chargement : " + e.getMessage());
        }
    }

    private void sauvegarderConfig() {
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "UPDATE tournoi SET nom = ?, nbTerrains = ?, nbJoueursParEquipe = ? WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, nomTournoi.getValue());
                pst.setInt(2, nbTerrains.getValue());
                pst.setInt(3, nbJoueursParEquipe.getValue());
                pst.setInt(4, ID_TOURNOI);
                
                int res = pst.executeUpdate();
                if (res > 0) {
                    Notification.show("Configuration sauvegardée !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Erreur : Tournoi ID 1 introuvable").addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        } catch (SQLException e) {
            Notification.show("Erreur sauvegarde : " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
