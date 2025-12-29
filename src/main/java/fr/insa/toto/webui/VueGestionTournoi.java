package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
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

@PageTitle("Gestion du Tournoi")
@Route(value = "gestion-tournoi", layout = MainLayout.class)
public class VueGestionTournoi extends VerticalLayout {

    private ComboBox<Tournoi> tournoiSelect;
    private IntegerField nbEquipesField;
    private IntegerField tempsMatchField; // Nouveau champ

    public VueGestionTournoi() {
        this.setAlignItems(Alignment.CENTER);
        this.add(new H2("Nouvelle Ronde"));

        tournoiSelect = new ComboBox<>("Tournoi");
        tournoiSelect.setItemLabelGenerator(Tournoi::getNom);

        nbEquipesField = new IntegerField("Nombre d'équipes par match");
        nbEquipesField.setValue(2);

        // Définition du temps juste en dessous du nombre d'équipes
        tempsMatchField = new IntegerField("Durée du match (minutes)");
        tempsMatchField.setValue(10);
        tempsMatchField.setMin(1);

        Button btnGenerer = new Button("Générer la ronde", e -> {
            Tournoi t = tournoiSelect.getValue();
            if (t != null && tempsMatchField.getValue() != null) {
                try (Connection con = ConnectionPool.getConnection()) {
                    ServiceGestionTournoi.genererNouvelleRonde(con, t.getId(), nbEquipesField.getValue(), tempsMatchField.getValue());
                    Notification.show("Ronde générée ! Les joueurs ont été répartis aléatoirement.");
                } catch (Exception ex) {
                    Notification.show("Erreur : " + ex.getMessage());
                }
            }
        });

        add(tournoiSelect, nbEquipesField, tempsMatchField, btnGenerer);
        actualiserTournois();
    }

    private void actualiserTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> tournois = Tournoi.findAll(con);
            tournoiSelect.setItems(tournois);
        } catch (SQLException ex) {
            Notification.show(ex.getMessage());
        }
    }
}