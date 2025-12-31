package fr.insa.toto.webui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button; // Nouvel import
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon; // Nouvel import pour l'icône
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.security.SessionInfo;

public class MainLayout extends AppLayout {

    public MainLayout() {
        this.addToDrawer(new MainMenu());

        DrawerToggle toggle = new DrawerToggle();

        String surnom = SessionInfo.getUtilisateurConnecte()
                        .map(Utilisateur::getSurnom) 
                        .orElse("Invité");

        H2 bienvenue = new H2("Bienvenue " + surnom);
        bienvenue.getStyle().set("font-size", "var(--lumo-font-size-l)");
        bienvenue.getStyle().set("margin", "0");
        bienvenue.getStyle().set("color", "black"); 

        // --- AJOUT DU BOUTON AIDE ---
        Button aideBtn = new Button("Aide", VaadinIcon.QUESTION_CIRCLE.create());
        aideBtn.addClickListener(e -> {
            AideDialog dialog = new AideDialog();
            dialog.open();
        });
        // Style pour que le bouton soit à droite
        aideBtn.getStyle().set("margin-left", "auto");
        // ----------------------------

        // Ajout du bouton aideBtn dans le constructeur HorizontalLayout
        HorizontalLayout navbarContainer = new HorizontalLayout(toggle, bienvenue, aideBtn);
        navbarContainer.setWidthFull();
        navbarContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        
        navbarContainer.getStyle().set("background-color", "#F5F5DC");
        navbarContainer.getStyle().set("padding", "0 1em");

        this.addToNavbar(navbarContainer);

        this.getElement().executeJs(
            "const drawer = this.shadowRoot.querySelector('[part=\"drawer\"]');" +
            "if (drawer) {" +
            "   drawer.style.backgroundColor = '#EADDCA';" +
            "}"
        );
    }
}