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

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import java.awt.TextField;
import java.util.List;

/**
 *
 * @author maxen
 */
@Route(value = "utilisateurs/creationAdmin",layout=MainLayout.class")
@PageTitle("Likes")
public class CreationAdmine extends FormLayout {

    private TextField surnom;
    private PasswordField password;
    private ComboBox<String> role;

    public CreationAdmine() {
        this.surnom = new TextField("surnom");
        this.password = new PasswordField("Password");
        this.role = new ComboBox<>("role");
        
        // Remplit la liste déroulante avec deux choix
        this.role.setItems(List.of("utilisateur", "admin"));

        // Note : Ces méthodes semblent spécifiques à une librairie de ton école 
        // ou à une classe personnalisée, car elles n'existent pas dans le Vaadin standard.
        this.setAutoResponsive(true);
        this.addFormRow(this.surnom, this.password);
        this.addFormRow(this.role);
    }
}