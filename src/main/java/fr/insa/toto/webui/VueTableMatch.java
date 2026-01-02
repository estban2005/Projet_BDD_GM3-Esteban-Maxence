package fr.insa.toto.webui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route(value = "table-match", layout = MainLayout.class)
public class VueTableMatch extends VerticalLayout implements BeforeEnterObserver {

    private int secondesRestantes = 0; 
    private boolean enCours = false;
    private MatchInfo matchSelectionne = null;

    private ComboBox<Tournoi> selectTournoi = new ComboBox<>("1. Sélectionner le tournoi");
    private ComboBox<MatchInfo> selectMatch = new ComboBox<>("2. Sélectionner le match à arbitrer");
    
    private Span messageVide = new Span("Aucun match en cours pour ce tournoi.");
    private VerticalLayout zoneArbitrage = new VerticalLayout();
    private HorizontalLayout layoutEquipes = new HorizontalLayout(); // Zone dynamique pour les scores
    
    private Span labelChrono = new Span();
    private Button btnStart;
    private ScheduledExecutorService timer;

    // Classe locale pour stocker les infos de match simplifiées
    public static class MatchInfo {
        int idMatch;
        public MatchInfo(int idM) { this.idMatch = idM; }
        @Override
        public String toString() { return "Match n°" + idMatch; }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SessionInfo.isCurUserAdmin()) {
            Notification.show("Accès réservé aux administrateurs !");
            event.rerouteTo(VuePrincipale.class);
        }
    }

    public VueTableMatch() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        getStyle().set("background-color", "#E6E6FA"); // Mauve clair pour harmoniser

        messageVide.getStyle().set("color", "red").set("font-weight", "bold").set("font-size", "1.5em");
        messageVide.setVisible(false);

        configurerZoneArbitrage();

        selectTournoi.setWidth("500px");
        selectTournoi.setItemLabelGenerator(Tournoi::getNom);
        chargerListeTournois();

        selectMatch.setWidth("500px");
        selectMatch.setItemLabelGenerator(MatchInfo::toString);
        selectMatch.setEnabled(false); 
        
        selectTournoi.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                actualiserListeMatchs(e.getValue().getId());
                selectMatch.setEnabled(true);
            } else {
                selectMatch.setEnabled(false);
                selectMatch.clear();
            }
            zoneArbitrage.setVisible(false);
        });

        selectMatch.addValueChangeListener(e -> {
            matchSelectionne = e.getValue();
            if (timer != null) timer.shutdown();
            enCours = false;
            
            if (matchSelectionne != null) {
                chargerDonneesMatchDynamique();
                btnStart.setText("DÉMARRER");
                btnStart.setEnabled(true);
                actualiserAffichageChrono();
                zoneArbitrage.setVisible(true);
            } else {
                zoneArbitrage.setVisible(false);
            }
        });

        add(selectTournoi, selectMatch, messageVide, zoneArbitrage);
    }

    private void chargerListeTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            selectTournoi.setItems(Tournoi.findAll(con)); 
        } catch (Exception e) {
            Notification.show("Erreur chargement tournois");
        }
    }

    private void actualiserListeMatchs(int idTournoi) {
        List<MatchInfo> matchs = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT m.id FROM matchs m JOIN ronde r ON m.idRonde = r.id " +
                         "WHERE m.statut = 'EN_COURS' AND r.idTournoi = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idTournoi);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                matchs.add(new MatchInfo(rs.getInt("id")));
            }
            selectMatch.setItems(matchs);
            messageVide.setVisible(matchs.isEmpty());
        } catch (Exception e) { Notification.show("Erreur : " + e.getMessage()); }
    }

    /**
     * Récupère les noms des joueurs d'une équipe donnée pour l'affichage.
     */
    private String recupererNomsJoueurs(int idEquipe) {
        List<String> noms = new ArrayList<>();
        try (Connection con = ConnectionPool.getConnection()) {
            String sql = "SELECT j.nom FROM joueur j " +
                         "JOIN composition c ON j.id = c.idJoueur " +
                         "WHERE c.idEquipe = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idEquipe);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                noms.add(rs.getString("nom"));
            }
        } catch (SQLException e) {
            return "Erreur chargement joueurs";
        }
        return noms.isEmpty() ? "Aucun joueur" : String.join(", ", noms);
    }

    private void chargerDonneesMatchDynamique() {
        layoutEquipes.removeAll();
        try (Connection con = ConnectionPool.getConnection()) {
            // Temps du match
            String sqlT = "SELECT r.duree FROM ronde r JOIN matchs m ON m.idRonde = r.id WHERE m.id = ?";
            PreparedStatement psT = con.prepareStatement(sqlT);
            psT.setInt(1, matchSelectionne.idMatch);
            ResultSet rsT = psT.executeQuery();
            if (rsT.next()) this.secondesRestantes = rsT.getInt("duree") * 60;

            // Création dynamique des zones de score pour chaque équipe
            String sqlE = "SELECT id, num, score FROM equipe WHERE idMatch = ? ORDER BY num";
            PreparedStatement psE = con.prepareStatement(sqlE);
            psE.setInt(1, matchSelectionne.idMatch);
            ResultSet rsE = psE.executeQuery();
            
            while (rsE.next()) {
                int idEq = rsE.getInt("id");
                int numEq = rsE.getInt("num");
                int scoreInit = rsE.getInt("score");
                
                // Récupération des noms des joueurs
                String joueurs = recupererNomsJoueurs(idEq);
                
                layoutEquipes.add(creerZoneScoreDynamique(idEq, numEq, scoreInit, joueurs));
            }
        } catch (Exception e) { Notification.show("Erreur données match"); }
    }

    private void configurerZoneArbitrage() {
        zoneArbitrage.setAlignItems(Alignment.CENTER);
        zoneArbitrage.setVisible(false);
        labelChrono.getStyle().set("font-size", "5em").set("font-weight", "bold");
        btnStart = new Button("DÉMARRER", e -> togglerChrono(e.getSource()));
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        layoutEquipes.setWidthFull();
        layoutEquipes.setJustifyContentMode(JustifyContentMode.CENTER);
        layoutEquipes.getStyle().set("flex-wrap", "wrap"); // Pour que les équipes passent à la ligne si besoin

        Button btnEnd = new Button("Terminer le match", e -> finaliserMatch());
        btnEnd.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        zoneArbitrage.add(labelChrono, btnStart, layoutEquipes, btnEnd);
    }

    private VerticalLayout creerZoneScoreDynamique(int idEquipe, int num, int scoreActuel, String nomsJoueurs) {
        H2 nomLabel = new H2("Équipe " + num);
        
        // Composant pour afficher les noms des joueurs
        Span joueursLabel = new Span(nomsJoueurs);
        joueursLabel.getStyle()
            .set("font-style", "italic")
            .set("color", "#555555")
            .set("text-align", "center")
            .set("font-size", "0.9em")
            .set("margin-bottom", "10px");
        
        // Permet le retour à la ligne si la liste de noms est longue
        joueursLabel.setWidth("180px");

        H1 scoreLabel = new H1(String.valueOf(scoreActuel));
        
        // Stockage local du score pour cette instance de composant
        final int[] scoreLocal = {scoreActuel};

        Button bPlus = new Button("+1", e -> {
            scoreLocal[0]++;
            scoreLabel.setText(String.valueOf(scoreLocal[0]));
            sauvegarderScore(idEquipe, scoreLocal[0]);
        });
        Button bMoins = new Button("-1", e -> {
            if (scoreLocal[0] > 0) {
                scoreLocal[0]--;
                scoreLabel.setText(String.valueOf(scoreLocal[0]));
                sauvegarderScore(idEquipe, scoreLocal[0]);
            }
        });

        // Ajout du label des joueurs entre le nom de l'équipe et le score
        VerticalLayout v = new VerticalLayout(nomLabel, joueursLabel, scoreLabel, bPlus, bMoins);
        v.setAlignItems(Alignment.CENTER);
        v.getStyle().set("border", "1px solid gray").set("border-radius", "10px").set("padding", "10px");
        v.setWidth("220px");
        return v;
    }

    private void sauvegarderScore(int idEq, int score) {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps.setInt(1, score); ps.setInt(2, idEq);
            ps.executeUpdate();
        } catch (Exception e) { Notification.show("Erreur BDD"); }
    }

    private void finaliserMatch() {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE matchs SET statut = 'CLOSE' WHERE id = ?");
            ps.setInt(1, matchSelectionne.idMatch);
            ps.executeUpdate();
            Notification.show("Match terminé !");
            UI.getCurrent().getPage().reload();
        } catch (Exception e) { Notification.show("Erreur finalisation"); }
    }

    private void actualiserAffichageChrono() {
        if (secondesRestantes > 0) {
            labelChrono.setText(String.format("%02d:%02d", secondesRestantes / 60, secondesRestantes % 60));
            labelChrono.getStyle().set("color", "black");
        } else {
            labelChrono.setText("TERMINÉ");
            labelChrono.getStyle().set("color", "red");
            if (btnStart != null) btnStart.setEnabled(false);
        }
    }

    private void togglerChrono(Button source) {
        if (!enCours) { enCours = true; source.setText("PAUSE"); lancerTimer(UI.getCurrent()); } 
        else { enCours = false; source.setText("DÉMARRER"); if (timer != null) timer.shutdown(); }
    }

    private void lancerTimer(UI ui) {
        ui.setPollInterval(1000);
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> ui.access(() -> {
            if (secondesRestantes > 0 && enCours) { secondesRestantes--; actualiserAffichageChrono(); } 
            else if (secondesRestantes <= 0) {
                enCours = false; actualiserAffichageChrono();
                if (timer != null) timer.shutdown();
                ui.setPollInterval(-1);
            }
        }), 0, 1, TimeUnit.SECONDS);
    }
}