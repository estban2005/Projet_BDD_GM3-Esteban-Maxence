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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "joueurs", layout = MainLayout.class)
@PageTitle("Gestion des Joueurs")
public class VueJoueurs extends VerticalLayout implements BeforeEnterObserver {

    private Grid<Joueur> grid;
    private TextField filtreSurnom = new TextField("Rechercher par surnom");

    private TextField nom = new TextField("Nom");
    private TextField prenom = new TextField("Prénom");
    private TextField surnom = new TextField("Surnom");
    private TextField sexe = new TextField("Sexe (M/F)");
    private DatePicker dateNaissance = new DatePicker("Date de naissance");

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Lecture du paramètre 'surnom' dans l'URL
        event.getLocation().getQueryParameters().getParameters()
             .getOrDefault("surnom", List.of())
             .stream().findFirst()
             .ifPresent(s -> {
                 filtreSurnom.setValue(s);
                 rafraichirGrille();
             });
    }

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
        grid.addColumn(Joueur::getScoreTotal).setHeader("Score Total").setSortable(true);

        grid.addComponentColumn(joueur -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            Button infoBtn = new Button(VaadinIcon.SEARCH.create(), e -> ouvrirDialogResume(joueur));
            infoBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            infoBtn.setTooltipText("Voir l'historique des matchs");
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

        if (SessionInfo.isCurUserAdmin()) {
            Button addBtn = new Button("Nouveau Joueur", VaadinIcon.PLUS.create());
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addBtn.addClickListener(e -> ouvrirDialogEdition(new Joueur("", "", "", "", null, 0)));
            add(addBtn);
        }

        rafraichirGrille();
    }

    private void ouvrirDialogResume(Joueur joueur) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Historique : " + joueur.getSurnom());
        dialog.setWidth("800px");

        Grid<HistoriqueMatch> gridHisto = new Grid<>();
        gridHisto.addColumn(h -> h.tournoi).setHeader("Tournoi");
        gridHisto.addColumn(h -> "Ronde " + h.ronde).setHeader("Ronde");
        gridHisto.addColumn(h -> "Match " + h.idMatch).setHeader("Match");
        gridHisto.addColumn(h -> h.score).setHeader("Score obtenu");
        gridHisto.addColumn(h -> h.statut).setHeader("Statut Match");

        List<HistoriqueMatch> donnees = new ArrayList<>();

        String sql = "SELECT t.nom as tournoi, r.numero as ronde, m.id as idMatch, e.score, m.statut " +
                     "FROM composition c " +
                     "JOIN equipe e ON c.idEquipe = e.id " +
                     "JOIN matchs m ON e.idMatch = m.id " +
                     "JOIN ronde r ON m.idRonde = r.id " +
                     "JOIN tournoi t ON r.idTournoi = t.id " +
                     "WHERE c.idJoueur = ? " +
                     "ORDER BY t.id DESC, r.numero DESC";

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, joueur.getId());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                donnees.add(new HistoriqueMatch(
                    rs.getString("tournoi"),
                    rs.getInt("ronde"),
                    rs.getInt("idMatch"),
                    rs.getInt("score"),
                    rs.getString("statut")
                ));
            }
        } catch (SQLException e) {
            Notification.show("Erreur lors de la récupération de l'historique");
        }

        gridHisto.setItems(donnees);
        
        VerticalLayout content = new VerticalLayout(new H3("Participations aux matchs"), gridHisto);
        dialog.add(content);
        
        Button closeBtn = new Button("Fermer", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private static class HistoriqueMatch {
        String tournoi;
        int ronde;
        int idMatch;
        int score;
        String statut;

        public HistoriqueMatch(String tournoi, int ronde, int idMatch, int score, String statut) {
            this.tournoi = tournoi;
            this.ronde = ronde;
            this.idMatch = idMatch;
            this.score = score;
            this.statut = statut;
        }
    }

    private void rafraichirGrille() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Joueur> joueurs = Joueur.findAll(con);
            String search = filtreSurnom.getValue();
            if (search != null && !search.isEmpty()) {
                joueurs = joueurs.stream()
                        .filter(j -> j.getSurnom() != null && 
                                j.getSurnom().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
            }
            grid.setItems(joueurs);
        } catch (SQLException e) {
            Notification.show("Erreur de chargement : " + e.getMessage());
        }
    }

    private void ouvrirDialogEdition(Joueur joueur) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(joueur.getId() == null ? "Nouveau Joueur" : "Modifier Joueur");
        nom.setValue(joueur.getNom() != null ? joueur.getNom() : "");
        prenom.setValue(joueur.getPrenom() != null ? joueur.getPrenom() : "");
        surnom.setValue(joueur.getSurnom() != null ? joueur.getSurnom() : "");
        sexe.setValue(joueur.getSexe() != null ? joueur.getSexe() : "");
        if (joueur.getDateNaissance() != null) dateNaissance.setValue(joueur.getDateNaissance().toLocalDate());
        else dateNaissance.clear();

        FormLayout formLayout = new FormLayout(surnom, nom, prenom, sexe, dateNaissance);
        Button saveButton = new Button("Enregistrer", e -> {
            if (surnom.getValue().isEmpty()) { Notification.show("Le surnom est obligatoire"); return; }
            joueur.setNom(nom.getValue());
            joueur.setPrenom(prenom.getValue());
            joueur.setSurnom(surnom.getValue());
            joueur.setSexe(sexe.getValue());
            if (dateNaissance.getValue() != null) joueur.setDateNaissance(java.sql.Date.valueOf(dateNaissance.getValue()));
            sauvegarderJoueur(joueur);
            dialog.close();
        });
        dialog.add(formLayout);
        dialog.getFooter().add(new Button("Annuler", e -> dialog.close()), saveButton);
        dialog.open();
    }

    private void sauvegarderJoueur(Joueur joueur) {
        try (Connection con = ConnectionPool.getConnection()) {
            if (joueur.getId() == null) joueur.insertInDB(con);
            else updateJoueurInDB(con, joueur);
            rafraichirGrille();
        } catch (SQLException e) { Notification.show("Erreur BDD"); }
    }

    private void supprimerJoueur(Joueur joueur) {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM composition WHERE idJoueur = ?")) {
                pst.setInt(1, joueur.getId()); pst.executeUpdate();
            }
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM joueur WHERE id = ?")) {
                pst.setInt(1, joueur.getId()); pst.executeUpdate();
            }
            rafraichirGrille();
        } catch (SQLException e) { Notification.show("Erreur suppression"); }
    }

    private void updateJoueurInDB(Connection con, Joueur j) throws SQLException {
        String sql = "UPDATE joueur SET nom=?, prenom=?, surnom=?, sexe=?, dateNaissance=? WHERE id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, j.getNom()); pst.setString(2, j.getPrenom());
            pst.setString(3, j.getSurnom()); pst.setString(4, j.getSexe());
            pst.setDate(5, j.getDateNaissance()); pst.setInt(6, j.getId());
            pst.executeUpdate();
        }
    }
}