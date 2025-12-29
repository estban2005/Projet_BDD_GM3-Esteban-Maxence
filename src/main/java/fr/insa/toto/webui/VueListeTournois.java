package fr.insa.toto.webui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
            HorizontalLayout actions = new HorizontalLayout();
            
            Button btnDetails = new Button("Voir", VaadinIcon.EYE.create());
            btnDetails.addClickListener(e -> UI.getCurrent().navigate(VueDetailTournoi.class, tournoi.getId()));
            actions.add(btnDetails);

            if (SessionInfo.isCurUserAdmin()) {
                Button btnDelete = new Button(VaadinIcon.TRASH.create());
                btnDelete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                btnDelete.addClickListener(e -> {
                    ConfirmDialog dialog = new ConfirmDialog();
                    dialog.setHeader("Suppression définitive");
                    dialog.setText("Supprimer " + tournoi.getNom() + " ? "
                                   + "Ceci supprimera aussi les rondes, les matchs, les équipes et les compositions.");
                    dialog.setCancelable(true);
                    dialog.setConfirmText("Confirmer la suppression");
                    dialog.setConfirmButtonTheme("error primary");
                    
                    dialog.addConfirmListener(event -> supprimerTournoi(tournoi.getId()));
                    dialog.open();
                });
                actions.add(btnDelete);
            }
            return actions;
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

    private void supprimerTournoi(int idTournoi) {
        try (Connection con = ConnectionPool.getConnection()) {
            Tournoi.deleteById(con, idTournoi);
            Notification.show("Tournoi supprimé avec succès")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            rafraichirGrille();
        } catch (SQLException ex) {
            Notification.show("Erreur SQL : " + ex.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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
        dialog.setHeaderTitle("Nouveau tournoi");
        TextField nom = new TextField("Nom");
        IntegerField nbTerrains = new IntegerField("Nb Terrains");
        nbTerrains.setValue(2);
        IntegerField nbJoueurs = new IntegerField("Joueurs par équipe");
        nbJoueurs.setValue(1);

        Button save = new Button("Créer", e -> {
            if (nom.isEmpty()) return;
            creerTournoi(nom.getValue(), nbTerrains.getValue(), nbJoueurs.getValue());
            dialog.close();
            rafraichirGrille();
        });
        
        dialog.add(new FormLayout(nom, nbTerrains, nbJoueurs), save);
        dialog.open();
    }

    private void creerTournoi(String nom, int terrains, int joueurs) {
        try (Connection con = ConnectionPool.getConnection()) {
            Tournoi t = new Tournoi(nom, terrains, joueurs);
            t.insertInDB(con);
            Notification.show("Tournoi créé !");
        } catch (SQLException ex) {
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}