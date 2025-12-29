package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Matchs;
import fr.insa.toto.model.ServiceGestionTournoi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Vue détaillée d'une ronde affichant la liste des matchs.
 * L'implémentation de HasUrlParameter<Integer> permet de recevoir l'ID de la ronde via l'URL.
 */
@Route(value = "ronde", layout = MainLayout.class)
public class VueDetailRonde extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idRonde;
    private H2 titreVue = new H2("Détails de la Ronde");
    private Grid<Matchs> gridMatchs;

    public VueDetailRonde() {
        // Bouton pour revenir à la liste des tournois (ou à la vue précédente)
        Button btnRetour = new Button("Retour", VaadinIcon.ARROW_LEFT.create());
        btnRetour.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(VueListeTournois.class)));

        // Configuration de la grille des matchs
        gridMatchs = new Grid<>(Matchs.class, false);
        gridMatchs.addColumn(Matchs::getId).setHeader("ID Match").setFlexGrow(0).setWidth("100px");
        gridMatchs.addColumn(m -> "Terrain n°" + m.getIdTerrain()).setHeader("Terrain");
        gridMatchs.addColumn(Matchs::getStatut).setHeader("Statut");

        // Colonne d'action pour terminer un match individuellement
        gridMatchs.addComponentColumn(match -> {
            Button btnAction = new Button("Terminer Match");
            btnAction.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            // On désactive le bouton si le match est déjà clos
            btnAction.setEnabled(!"TERMINÉ".equals(match.getStatut()) && !"CLOSE".equals(match.getStatut()));
            
            btnAction.addClickListener(e -> terminerLeMatch(match));
            return btnAction;
        }).setHeader("Action");

        add(btnRetour, titreVue, gridMatchs);
    }

    /**
     * Méthode appelée par Vaadin lors de la navigation vers cette vue.
     * @param parameter L'ID de la ronde extrait de l'URL (ex: /ronde/5)
     */
    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        if (parameter != null) {
            this.idRonde = parameter;
            refresh(); // Charge les données dès que l'ID est connu
        } else {
            Notification.show("ID de ronde manquant", 3000, Notification.Position.MIDDLE);
        }
    }

    /**
     * Rafraîchit les données de la vue (Titre et Grille).
     */
    private void refresh() {
        try (Connection con = ConnectionPool.getConnection()) {
            // 1. Récupération des informations de la ronde (Numéro et Durée)
            String sqlRonde = "SELECT numero, duree FROM ronde WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sqlRonde)) {
                pst.setInt(1, idRonde);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int num = rs.getInt("numero");
                        int temps = rs.getInt("duree");
                        this.titreVue.setText("Ronde #" + num + " (Durée : " + temps + " min)");
                    }
                }
            }
            
            // 2. Chargement de la liste des matchs de cette ronde
            List<Matchs> matchs = Matchs.findAll(con, idRonde);
            gridMatchs.setItems(matchs);
            
        } catch (Exception ex) {
            Notification.show("Erreur lors du chargement : " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Termine un match et vérifie si la ronde doit être clôturée.
     */
    private void terminerLeMatch(Matchs match) {
        try (Connection con = ConnectionPool.getConnection()) {
            // Mise à jour du statut du match
            String sql = "UPDATE matchs SET statut = 'TERMINÉ' WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, match.getId());
                pst.executeUpdate();
            }
            
            // Appel au service pour vérifier si tous les matchs sont finis
            ServiceGestionTournoi.verifierEtCloturerRonde(con, idRonde);
            
            Notification.show("Match terminé").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refresh(); // Rechargement de la grille
        } catch (Exception ex) {
            Notification.show("Erreur : " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}