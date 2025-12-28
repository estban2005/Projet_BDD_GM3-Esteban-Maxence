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

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.security.SessionInfo;

/**
 *
 * @author maxen
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        this.addToDrawer(new MainMenu());
        
        DrawerToggle toggle = new DrawerToggle();
        String surnom = SessionInfo.getUtilisateurConnecte()
                        .map(Utilisateur::getSurnom) 
                        .orElse("Invité");
        H2 bienvenue = new H2("Bienvenue " + surnom);
        this.addToNavbar(toggle, bienvenue);
    }

}