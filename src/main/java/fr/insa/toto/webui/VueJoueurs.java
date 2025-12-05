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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Route(value = "joueurs", layout = MainLayout.class)
@PageTitle("Gestion des Joueurs")
public class VueJoueurs extends VerticalLayout {

    private Grid<Joueur> grid;
    
    private TextField nom = new TextField("Nom");
    private TextField prenom = new TextField("Prénom");
    private TextField surnom = new TextField("Surnom");
    private TextField sexe = new TextField("Sexe (M/F)");

    public VueJoueurs() {
        add(new H2("Liste des Joueurs"));

        grid = new Grid<>(Joueur.class, false);
        grid.addColumn(Joueur::getId).setHeader("ID").setWidth("50px").setFlexGrow(0);
        grid.addColumn(Joueur::getSurnom).setHeader("Surnom").setSortable(true);
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");
        grid.addColumn(Joueur::getSexe).setHeader("Sexe");
        grid.addColumn(Joueur::getScoreTotal).setHeader("Score Total");

        if (SessionInfo.isCurUserAdmin()) {
            grid.addComponentColumn(joueur -> {
                Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> ouvrirDialogEdition(joueur));
                Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> supprimerJoueur(joueur));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                
                return new HorizontalLayout(editBtn, deleteBtn);
            }).setHeader("Actions");
        }

        add(grid);

        if (SessionInfo.isCurUserAdmin()) {
            Button addBtn = new Button("Nouveau Joueur", VaadinIcon.PLUS.create());
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addBtn.addClickListener(e -> ouvrirDialogEdition(new Joueur("", "", "", "", null, 0)));
            add(addBtn);
        }

        rafraichirGrille();
    }

    private void rafraichirGrille() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Joueur> joueurs = Joueur.findAll(con);
            grid.setItems(joueurs);
        } catch (SQLException e) {
            Notification.show("Erreur de chargement : " + e.getMessage());
        }
    }

    private void ouvrirDialogEdition(Joueur joueur) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(joueur.getId() == null ? "Nouveau Joueur" : "Modifier Joueur");

        nom.setValue(joueur.getNom() != null ? joueur.getNom() : "");
        prenom.setValue(joueur.getPrenom() != null ? joueur.getPrenom() : "");
        surnom.setValue(joueur.getSurnom() != null ? joueur.getSurnom() : "");
        sexe.setValue(joueur.getSexe() != null ? joueur.getSexe() : "");

        FormLayout formLayout = new FormLayout(surnom, nom, prenom, sexe);
        
        Button saveButton = new Button("Enregistrer", e -> {
            if (surnom.getValue().isEmpty()) {
                Notification.show("Le surnom est obligatoire");
                return;
            }
            
            joueur.setNom(nom.getValue());
            joueur.setPrenom(prenom.getValue());
            joueur.setSurnom(surnom.getValue());
            joueur.setSexe(sexe.getValue());

            sauvegarderJoueur(joueur);
            dialog.close();
        });

        Button cancelButton = new Button("Annuler", e -> dialog.close());

        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void sauvegarderJoueur(Joueur joueur) {
        try (Connection con = ConnectionPool.getConnection()) {
            if (joueur.getId() == null) {
                joueur.insertInDB(con);
                Notification.show("Joueur créé avec succès");
            } else {
                updateJoueurInDB(con, joueur);
                Notification.show("Joueur modifié");
            }
            rafraichirGrille();
        } catch (SQLException e) {
            Notification.show("Erreur BDD : " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void supprimerJoueur(Joueur joueur) {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM composition WHERE idJoueur = ?")) {
                pst.setInt(1, joueur.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM joueur WHERE id = ?")) {
                pst.setInt(1, joueur.getId());
                pst.executeUpdate();
            }
            Notification.show("Joueur supprimé").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            rafraichirGrille();
        } catch (SQLException e) {
            Notification.show("Impossible de supprimer (Joueur engagé dans un match ?)");
        }
    }
    
    private void updateJoueurInDB(Connection con, Joueur j) throws SQLException {
        String sql = "UPDATE joueur SET nom=?, prenom=?, surnom=?, sexe=? WHERE id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, j.getNom());
            pst.setString(2, j.getPrenom());
            pst.setString(3, j.getSurnom());
            pst.setString(4, j.getSexe());
            pst.setInt(5, j.getId());
            pst.executeUpdate();
        }
    }
}
