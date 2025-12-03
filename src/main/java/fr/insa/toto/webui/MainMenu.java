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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import fr.insa.toto.webui.security.SessionInfo;
import fr.insa.toto.webui.utilisateur.CreationAdmine;

public class MainMenu extends VerticalLayout {

    public MainMenu() {
        setHeightFull();
        setPadding(false);
        setSpacing(false);

        SideNav nav = new SideNav();
        
        SideNavItem accueil = new SideNavItem("Accueil", VuePrincipale.class);

        nav.addItem(accueil);

        if (SessionInfo.isCurUserAdmin()) {
            SideNavItem adminSection = new SideNavItem("Administration");
            adminSection.addItem(new SideNavItem("Créer Utilisateur", CreationAdmine.class));
            adminSection.addItem(new SideNavItem("Gérer Tournoi", VueGestionTournoi.class));
            
            nav.addItem(adminSection);
        }

        add(nav);
        Button logoutBtn = new Button("Se déconnecter", e -> {
            SessionInfo.logout();
            getUI().ifPresent(ui -> ui.navigate(VueLogin.class));
        });
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logoutBtn.getStyle().set("margin-top", "auto");
        logoutBtn.getStyle().set("margin-bottom", "10px");
        logoutBtn.getStyle().set("align-self", "center");
        
        add(logoutBtn);
    }
}