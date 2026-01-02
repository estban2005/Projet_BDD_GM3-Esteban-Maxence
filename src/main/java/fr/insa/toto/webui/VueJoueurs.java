package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "joueurs", layout = MainLayout.class)
@PageTitle("Gestion des Joueurs")
public class VueJoueurs extends VerticalLayout {

    private Grid<Joueur> grid;
    private TextField filtreSurnom = new TextField("Rechercher par surnom");

    private TextField nom = new TextField("Nom");
    private TextField prenom = new TextField("Prénom");
    private TextField surnom = new TextField("Surnom");
    private TextField sexe = new TextField("Sexe (M/F)");
    private DatePicker dateNaissance = new DatePicker("Date de naissance");

    public VueJoueurs() {
        add(new H2("Liste des Joueurs"));

        filtreSurnom.setPlaceholder("Tapez un surnom...");
        filtreSurnom.setClearButtonVisible(true);
        filtreSurnom.setValueChangeMode(ValueChangeMode.EAGER);
        filtreSurnom.addValueChangeListener(e -> rafraichirGrille());
        add(filtreSurnom);

        grid = new Grid<>(Joueur.class, false);
        grid.addColumn(Joueur::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Joueur::getSurnom).setHeader("Surnom").setSortable(true);
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");
        grid.addColumn(Joueur::getDateNaissance).setHeader("Date Naissance");
        grid.addColumn(Joueur::getScoreTotal).setHeader("Score Total");

        grid.addComponentColumn(joueur -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button infoBtn = new Button(VaadinIcon.SEARCH.create(), e -> ouvrirDialogResume(joueur));
            infoBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            actions.add(infoBtn);

            if (SessionInfo.isCurUserAdmin()) {
                Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> ouvrirDialogEdition(joueur));
                Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> supprimerJoueur(joueur));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                actions.add(editBtn, deleteBtn);
            }
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        add(grid);
        rafraichirGrille();
    }

    private void ouvrirDialogResume(Joueur joueur) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Historique : " + joueur.getSurnom());
        dialog.setWidth("800px");

        Grid<LigneResume> gridHisto = new Grid<>();
        gridHisto.addColumn(h -> h.nomTournoi).setHeader("Tournoi");
        gridHisto.addColumn(h -> "Ronde " + h.numRonde).setHeader("Ronde");
        gridHisto.addColumn(h -> "Match " + h.idMatch).setHeader("Match");
        gridHisto.addColumn(h -> h.score).setHeader("Score");
        gridHisto.addColumn(h -> h.statut).setHeader("Statut");

        List<LigneResume> donnees = new ArrayList<>();
        String sql = "SELECT t.nom, r.numero, m.id, e.score, m.statut FROM composition c " +
                     "JOIN equipe e ON c.idEquipe = e.id JOIN matchs m ON e.idMatch = m.id " +
                     "JOIN ronde r ON m.idRonde = r.id JOIN tournoi t ON r.idTournoi = t.id " +
                     "WHERE c.idJoueur = ? ORDER BY t.id DESC, r.numero DESC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, joueur.getId());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                donnees.add(new LigneResume(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getString(5)));
            }
        } catch (SQLException e) { Notification.show("Erreur historique"); }

        gridHisto.setItems(donnees);
        dialog.add(new VerticalLayout(new H3("Participations"), gridHisto));
        dialog.getFooter().add(new Button("Fermer", e -> dialog.close()));
        dialog.open();
    }

    private void rafraichirGrille() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Joueur> joueurs = Joueur.findAll(con);
            String search = filtreSurnom.getValue();
            if (search != null && !search.isEmpty()) {
                joueurs = joueurs.stream().filter(j -> j.getSurnom().toLowerCase().contains(search.toLowerCase())).collect(Collectors.toList());
            }
            grid.setItems(joueurs);
        } catch (SQLException e) { Notification.show("Erreur"); }
    }

    private void ouvrirDialogEdition(Joueur joueur) { /* ... identique au code précédent ... */ }
    private void sauvegarderJoueur(Joueur joueur) { /* ... identique au code précédent ... */ }
    private void supprimerJoueur(Joueur joueur) { /* ... identique au code précédent ... */ }
    private void updateJoueurInDB(Connection con, Joueur j) throws SQLException { /* ... identique au code précédent ... */ }

    private static class LigneResume {
        String nomTournoi; int numRonde; int idMatch; int score; String statut;
        public LigneResume(String t, int r, int m, int s, String st) {
            this.nomTournoi = t; this.numRonde = r; this.idMatch = m; this.score = s; this.statut = st;
        }
    }
}