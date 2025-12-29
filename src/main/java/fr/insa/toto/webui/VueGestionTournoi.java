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
        tournoiSelect.setWidth("400px");
        
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> liste = Tournoi.findAll(con);
            tournoiSelect.setItems(liste);
            if (!liste.isEmpty()) tournoiSelect.setValue(liste.get(0));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        IntegerField nbEquipesField = new IntegerField("Nombre d'équipes par match");
        nbEquipesField.setWidth("400px");
        nbEquipesField.setValue(2);
        nbEquipesField.setMin(2);
        nbEquipesField.setStepButtonsVisible(true);

        IntegerField tempsMatchField = new IntegerField("Durée du round (en secondes)");
        tempsMatchField.setWidth("400px");
        tempsMatchField.setPlaceholder("Ex: 180 pour 3 minutes");
        tempsMatchField.setValue(60); 
        tempsMatchField.setMin(1);
        tempsMatchField.setStepButtonsVisible(true);
        
        Button btnGenerer = new Button("Générer un nouveau round");
        btnGenerer.getStyle().set("margin-top", "20px");
        
        btnGenerer.addClickListener(e -> {
            Tournoi t = tournoiSelect.getValue();
            if (t == null) {
                Notification.show("Veuillez sélectionner un tournoi !");
                return;
            }

            try (Connection con = ConnectionPool.getConnection()) {
                int idTournoi = t.getId(); 
                int nbEquipes = nbEquipesField.getValue() != null ? nbEquipesField.getValue() : 2;
                int tempsMatch = tempsMatchField.getValue() != null ? tempsMatchField.getValue() : 60;
                
                ServiceGestionTournoi.genererNouvelleRonde(con, idTournoi, nbEquipes, tempsMatch);
                Notification.show("Ronde générée avec succès (" + tempsMatch + "s) pour : " + t.getNom());
                
            } catch (Exception ex) {
                Notification.show("Erreur lors de la génération : " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        add(
            new Paragraph("Sélectionnez le tournoi et configurez les paramètres de la ronde."), 
            tournoiSelect, 
            nbEquipesField, 
            tempsMatchField, 
            btnGenerer
        );
    }
}