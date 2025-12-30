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
package fr.insa.toto.webui.utilisateur;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.SQLException;

@Route(value = "utilisateurs/creation", layout = MainLayout.class)
@PageTitle("Création Utilisateur")
public class CreationAdmine extends VerticalLayout {

    public CreationAdmine() {
        // --- MODIFICATION : AJOUT DE LA COULEUR MAUVE ---
        setSizeFull(); // Pour que la couleur couvre tout l'écran
        getStyle().set("background-color", "#E6E6FA"); // Mauve (Mauve clair/Lavender)
        // -----------------------------------------------

        add(new H2("Créer un nouvel utilisateur"));

        TextField surnomField = new TextField("Surnom");
        PasswordField passField = new PasswordField("Mot de passe");
        
        ComboBox<String> roleSelect = new ComboBox<>("Rôle");
        roleSelect.setItems("Utilisateur", "Administrateur");
        roleSelect.setValue("Utilisateur"); // Valeur par défaut

        Button saveButton = new Button("Enregistrer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                String surnom = surnomField.getValue();
                String pass = passField.getValue();
                String roleStr = roleSelect.getValue();
                
                if (surnom.isEmpty() || pass.isEmpty() || roleStr == null) {
                    Notification.show("Veuillez remplir tous les champs");
                    return;
                }

                int roleId = "Administrateur".equals(roleStr) ? 1 : 2;

                Utilisateur nouveau = new Utilisateur(surnom, pass, roleId);
                nouveau.saveInDB(con);

                Notification.show("Utilisateur " + surnom + " créé avec succès !")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                surnomField.clear();
                passField.clear();
                roleSelect.setValue("Utilisateur");

            } catch (SQLException ex) {
                Notification.show("Erreur BDD : " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        FormLayout form = new FormLayout();
        form.add(surnomField, passField, roleSelect);
        
        add(form, saveButton);
    }
}