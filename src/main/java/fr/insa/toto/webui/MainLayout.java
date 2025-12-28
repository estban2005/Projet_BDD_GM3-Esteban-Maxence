package fr.insa.toto.webui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.security.SessionInfo;

/**
 * @author maxen
 */
@Push // INDISPENSABLE pour que le chronomètre de VueTableMatch s'actualise en direct
public class MainLayout extends AppLayout {

    public MainLayout() {
        // 1. Menu latéral (Drawer)
        this.addToDrawer(new MainMenu());

        // 2. Bouton Menu (Toggle)
        DrawerToggle toggle = new DrawerToggle();

        // 3. Récupération du surnom depuis la session (BDD)
        String surnom = SessionInfo.getUtilisateurConnecte()
                        .map(Utilisateur::getSurnom) 
                        .orElse("Invité");

        H2 bienvenue = new H2("Bienvenue " + surnom);
        bienvenue.getStyle().set("font-size", "var(--lumo-font-size-l)");
        bienvenue.getStyle().set("margin", "0");
        bienvenue.getStyle().set("color", "black"); 

        // 4. Navbar (Bandeau du haut)
        HorizontalLayout navbarContainer = new HorizontalLayout(toggle, bienvenue);
        navbarContainer.setWidthFull();
        navbarContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        
        // Couleur Beige/Marron clair pour le haut
        navbarContainer.getStyle().set("background-color", "#F5F5DC");
        navbarContainer.getStyle().set("padding", "0 1em");

        this.addToNavbar(navbarContainer);

        // 5. Couleur du menu latéral (Drawer) via JavaScript (Shadow DOM)
        this.getElement().executeJs(
            "const drawer = this.shadowRoot.querySelector('[part=\"drawer\"]');" +
            "if (drawer) {" +
            "   drawer.style.backgroundColor = '#EADDCA';" +
            "}"
        );
    }
}