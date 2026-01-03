package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import fr.insa.toto.webui.security.SessionInfo;

public class AideDialog extends Dialog {

    public AideDialog() {
        this.setHeaderTitle("Guide d'utilisation - Tournoi App");
        this.setWidth("600px");
        this.setDraggable(true);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        // --- SECTION 1 : INTRODUCTION ---
        content.add(new H2("Bienvenue !"));
        content.add(new Paragraph("Cette application permet d'organiser et de suivre des tournois amicaux où la durée des matchs est fixée."));

        // --- SECTION 2 : CONSULTATION (TOUS UTILISATEURS) ---
        content.add(new H3("Fonctionnalités de consultation"));
        UnorderedList consultList = new UnorderedList();
        consultList.add(new ListItem("Classement : Consultez le score global de tous les joueurs ou filtrez par tournoi spécifique."));
        consultList.add(new ListItem("Matchs & Résultats : Visualisez l'état des rencontres passées ou en cours."));
        consultList.add(new ListItem("Joueurs : Accédez à la fiche détaillée d'un participant pour voir son historique de matchs."));
        content.add(consultList);

        // --- SECTION 3 : ADMINISTRATION (RESTREINT) ---
        if (SessionInfo.isCurUserAdmin()) {
            content.add(new H3("Outils d'Administration"));
            content.add(new Paragraph("En tant qu'administrateur, vous avez accès aux fonctions de gestion suivantes :"));
            
            UnorderedList adminList = new UnorderedList();
            adminList.add(new ListItem("Tournois : Créez de nouveaux tournois et générez les rondes."));
            adminList.add(new ListItem("Génération de Ronde : Affecte aléatoirement les joueurs aux équipes et aux terrains disponibles."));
            adminList.add(new ListItem("Table de Match : L'interface d'arbitrage en temps réel avec chronomètre intégré pour saisir les scores."));
            adminList.add(new ListItem("Gestion des Utilisateurs : Créez de nouveaux comptes (Admin ou Utilisateur)."));
            content.add(adminList);

            content.add(new Paragraph("Note : Pour clore une ronde, tous les matchs de celle-ci doivent être terminés."));
        } else {
            content.add(new Paragraph("Certaines fonctions (création de tournois, arbitrage) ne sont accessibles qu'aux administrateurs."));
        }

        // --- SECTION 4 : INFORMATIONS TECHNIQUES ---
        content.add(new H3("À savoir"));
        content.add(new Paragraph("Les joueurs gagnent des points en fonction de leur score à chaque match. Le classement général est basé sur la somme des points obtenus sur l'ensemble des tournois."));

        this.add(content);

        // Bouton de fermeture
        Button closeButton = new Button("J'ai compris", VaadinIcon.CHECK.create(), e -> this.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.getFooter().add(closeButton);
    }
}