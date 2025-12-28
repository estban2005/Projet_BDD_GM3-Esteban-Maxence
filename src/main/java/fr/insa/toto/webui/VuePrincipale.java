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

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 *
 * @author francois
 */
@Route(value = "",layout=MainLayout.class)
@PageTitle("Likes")
public class VuePrincipale extends VerticalLayout {

    public VuePrincipale() {
        setSizeFull();
        
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 titrePrincipal = new H1("Gestion de Tournoi");
        
        titrePrincipal.getStyle().set("color", "black"); 
        titrePrincipal.getStyle().set("font-weight", "bold");
        titrePrincipal.getStyle().set("font-size", "4em");

        add(titrePrincipal);
    }

}
