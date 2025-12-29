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
            if (!liste.isEmpty()) tournoiSelect.setValue(liste.get(0));
        } catch (SQLException e) { e.printStackTrace(); }

        IntegerField nbEquipesField = new IntegerField("Nombre d'équipes par match");
        nbEquipesField.setWidth("300px");
        nbEquipesField.setValue(2);
        
        // --- NOUVEAU CHAMP ---
        IntegerField dureeField = new IntegerField("Durée de la ronde (minutes)");
        dureeField.setWidth("300px");
        dureeField.setValue(15); 

        Button btnGenerer = new Button("Générer un nouveau round");
        btnGenerer.addClickListener(e -> {
            Tournoi t = tournoiSelect.getValue();
            if (t == null) {
                Notification.show("Veuillez sélectionner un tournoi !");
                return;
            }
            try (Connection con = ConnectionPool.getConnection()) {
                int nbEquipes = nbEquipesField.getValue() != null ? nbEquipesField.getValue() : 2;
                int duree = dureeField.getValue() != null ? dureeField.getValue() : 15;
                
                ServiceGestionTournoi.genererNouvelleRonde(con, t.getId(), nbEquipes, duree);
                Notification.show("Ronde générée (" + duree + " min)");
            } catch (Exception ex) {
                Notification.show("Erreur : " + ex.getMessage());
            }
        });

        add(new Paragraph("Configuration de la nouvelle ronde :"), 
            tournoiSelect, 
            nbEquipesField, 
            dureeField, // Placé juste en dessous
            btnGenerer);
    }
}