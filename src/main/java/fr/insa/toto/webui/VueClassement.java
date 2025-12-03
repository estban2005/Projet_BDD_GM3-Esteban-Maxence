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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

@Route(value = "classement", layout = MainLayout.class)
@PageTitle("Classement Général")
public class VueClassement extends VerticalLayout {

    public VueClassement() {
        add(new H2("Classement du Tournoi"));

        Grid<Joueur> grid = new Grid<>(Joueur.class, false);
        
        grid.addColumn(Joueur::getSurnom).setHeader("Joueur");
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");
        
        grid.addColumn(Joueur::getScoreTotal).setHeader("Points Total").setSortable(true);

        try (Connection con = ConnectionPool.getConnection()) {
            List<Joueur> joueurs = Joueur.findAll(con);
            
            joueurs.sort(Comparator.comparingInt(Joueur::getScoreTotal).reversed());
            
            grid.setItems(joueurs);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }

        add(grid);
    }
}