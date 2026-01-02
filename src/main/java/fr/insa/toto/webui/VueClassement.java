package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.Tournoi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Route(value = "classement", layout = MainLayout.class)
@PageTitle("Classement Général et par Tournoi")
public class VueClassement extends VerticalLayout {

    private Grid<Joueur> grid = new Grid<>(Joueur.class, false);
    private ComboBox<Tournoi> selectTournoi = new ComboBox<>("Filtrer par Tournoi");
    private ComboBox<Ronde> selectRonde = new ComboBox<>("Filtrer par Ronde");
    // Nouveau bouton
    private Button btnEffacer = new Button("Effacer les filtres", VaadinIcon.CLOSE_CIRCLE.create());

    public VueClassement() {
        add(new H2("Classement du Tournoi"));

        // Configuration du bouton d'effacement
        btnEffacer.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        btnEffacer.addClickListener(e -> {
            selectTournoi.clear();
            selectRonde.clear();
            selectRonde.setEnabled(false);
            actualiserClassement();
        });

        // Configuration des filtres (ajout du bouton à côté)
        HorizontalLayout filtres = new HorizontalLayout(selectTournoi, selectRonde, btnEffacer);
        filtres.setVerticalComponentAlignment(Alignment.END, selectTournoi, selectRonde, btnEffacer);
        add(filtres);

        // Configuration de la grille
        grid.addColumn(Joueur::getSurnom).setHeader("Joueur");
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");
        grid.addColumn(Joueur::getScoreTotal).setHeader("Points").setSortable(true);
        add(grid);

        // Initialisation des données
        chargerTournois();
        selectRonde.setEnabled(false);

        // Listeners pour les filtres
        selectTournoi.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                chargerRondes(e.getValue().getId());
                selectRonde.setEnabled(true);
            } else {
                selectRonde.clear();
                selectRonde.setEnabled(false);
            }
            actualiserClassement();
        });

        selectRonde.addValueChangeListener(e -> actualiserClassement());

        // Premier affichage (Général)
        actualiserClassement();
    }

    private void chargerTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Tournoi> tournois = Tournoi.findAll(con);
            selectTournoi.setItems(tournois);
            selectTournoi.setItemLabelGenerator(Tournoi::getNom);
        } catch (SQLException e) {
            Notification.show("Erreur chargement tournois");
        }
    }

    private void chargerRondes(int idTournoi) {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Ronde> rondes = Ronde.findAll(con, idTournoi);
            selectRonde.setItems(rondes);
            selectRonde.setItemLabelGenerator(r -> "Ronde n°" + r.getNumero());
        } catch (SQLException e) {
            Notification.show("Erreur chargement rondes");
        }
    }

    private void actualiserClassement() {
        List<Joueur> classement = new ArrayList<>();
        Tournoi t = selectTournoi.getValue();
        Ronde r = selectRonde.getValue();

        try (Connection con = ConnectionPool.getConnection()) {
            if (r != null) {
                classement = calculerScoreFiltre(con, "r.id = ?", r.getId());
            } else if (t != null) {
                classement = calculerScoreFiltre(con, "r.idTournoi = ?", t.getId());
            } else {
                // Classement GÉNÉRAL
                classement = Joueur.findAll(con);
                classement.sort(Comparator.comparingInt(Joueur::getScoreTotal).reversed());
            }
            grid.setItems(classement);
        } catch (SQLException e) {
            e.printStackTrace();
            Notification.show("Erreur lors du calcul du classement");
        }
    }

    private List<Joueur> calculerScoreFiltre(Connection con, String conditionSql, int idFiltre) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT j.*, SUM(e.score) as scoreFiltre " +
                     "FROM joueur j " +
                     "JOIN composition c ON j.id = c.idJoueur " +
                     "JOIN equipe e ON c.idEquipe = e.id " +
                     "JOIN matchs m ON e.idMatch = m.id " +
                     "JOIN ronde r ON m.idRonde = r.id " +
                     "WHERE " + conditionSql + " " +
                     "GROUP BY j.id " +
                     "ORDER BY scoreFiltre DESC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idFiltre);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Joueur j = new Joueur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("surnom"),
                        rs.getString("sexe"),
                        rs.getDate("dateNaissance"),
                        rs.getInt("scoreFiltre")
                    );
                    res.add(j);
                }
            }
        }
        return res;
    }
}