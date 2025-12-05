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
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;

@Route(value = "login")
@PageTitle("Connexion")
public class VueLogin extends VerticalLayout {

    public VueLogin() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 titre = new H1("Tournoi App - Connexion");

        TextField surnomField = new TextField("Surnom");
        PasswordField passField = new PasswordField("Mot de passe");
        
        Button loginButton = new Button("Se connecter", event -> {
            try (Connection con = ConnectionPool.getConnection()) {
                String surnom = surnomField.getValue();
                String pass = passField.getValue();

                Utilisateur user = Utilisateur.login(con, surnom, pass);

                if (user != null) {
                    SessionInfo.setUtilisateurConnecte(user);
                    getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class));
                } else {
                    Notification.show("Identifiants incorrects", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (SQLException e) {
                Notification.show("Erreur BDD: " + e.getMessage());
            }
        });

        loginButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

        add(titre, surnomField, passField, loginButton);
    }
}