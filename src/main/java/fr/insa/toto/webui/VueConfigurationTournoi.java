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
        
        nomTournoi = new TextField("Nom du Tournoi");
        nbTerrains = new IntegerField("Nombre de terrains");
        nbJoueursParEquipe = new IntegerField("Joueurs par équipe");

        Button btnSave = new Button("Enregistrer", e -> sauvegarderConfig());

        FormLayout form = new FormLayout(nomTournoi, nbTerrains, nbJoueursParEquipe);
        chargerConfig();
        add(form, btnSave);
    }

    private void chargerConfig() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> tournois = Tournoi.findAll(con);
            Tournoi t = tournois.stream().filter(tour -> tour.getId() == ID_TOURNOI).findFirst().orElse(null);
            if (t != null) {
                nomTournoi.setValue(t.getNom());
                nbTerrains.setValue(t.getNbTerrains());
                nbJoueursParEquipe.setValue(t.getNbJoueursParEquipe());
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void sauvegarderConfig() {
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "UPDATE tournoi SET nom = ?, nbTerrains = ?, nbJoueursParEquipe = ? WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, nomTournoi.getValue());
                pst.setInt(2, nbTerrains.getValue());
                pst.setInt(3, nbJoueursParEquipe.getValue());
                pst.setInt(4, ID_TOURNOI);
                pst.executeUpdate();
                Notification.show("Sauvegardé !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}