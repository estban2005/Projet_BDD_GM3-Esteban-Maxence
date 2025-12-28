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
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.webui.security.SessionInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route(value = "table-match", layout = MainLayout.class)
public class VueTableMatch extends VerticalLayout implements BeforeEnterObserver {

    private int scoreE1 = 0;
    private int scoreE2 = 0;
    private int secondesRestantes = 10; 
    private boolean enCours = false;
    private MatchInfo matchSelectionne = null;

    private ComboBox<MatchInfo> selectMatch = new ComboBox<>("Sélectionner le match à arbitrer");
    private Span messageVide = new Span("Aucun match prévu pour le moment.");
    private VerticalLayout zoneArbitrage = new VerticalLayout();
    
    private H1 labelScore1 = new H1("0");
    private H1 labelScore2 = new H1("0");
    private H2 labelNomE1 = new H2("Équipe 1");
    private H2 labelNomE2 = new H2("Équipe 2");
    private Span labelChrono = new Span();
    private Button btnStart;
    private ScheduledExecutorService timer;

    public static class MatchInfo {
        int idMatch;
        int idEquipe1, idEquipe2;
        String nomE1, nomE2;

        public MatchInfo(int idM, int idE1, int idE2, String n1, String n2) {
            this.idMatch = idM; 
            this.idEquipe1 = idE1; 
            this.idEquipe2 = idE2;
            this.nomE1 = n1;
            this.nomE2 = n2;
        }

        @Override
        public String toString() { return "Match n°" + idMatch + " (" + nomE1 + " vs " + nomE2 + ")"; }
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
        getStyle().set("background-color", "#D2B48C");

        messageVide.getStyle().set("color", "red").set("font-weight", "bold").set("font-size", "1.5em");
        messageVide.setVisible(false);

        configurerZoneArbitrage();

        selectMatch.setWidth("500px");
        selectMatch.setItemLabelGenerator(MatchInfo::toString);
        
        selectMatch.addValueChangeListener(e -> {
            matchSelectionne = e.getValue();
            if (timer != null) timer.shutdown();
            
            secondesRestantes = 10;
            scoreE1 = 0; 
            scoreE2 = 0;
            enCours = false;
            
            if (matchSelectionne != null) {
                labelNomE1.setText(matchSelectionne.nomE1);
                labelNomE2.setText(matchSelectionne.nomE2);
                labelScore1.setText("0");
                labelScore2.setText("0");
                btnStart.setText("DÉMARRER");
                btnStart.setEnabled(true);
                actualiserAffichageChrono();
                zoneArbitrage.setVisible(true);
            } else {
                zoneArbitrage.setVisible(false);
            }
        });

        add(selectMatch, messageVide, zoneArbitrage);
        actualiserListeMatchs();
    }

    private void configurerZoneArbitrage() {
        zoneArbitrage.setAlignItems(Alignment.CENTER);
        zoneArbitrage.setVisible(false);

        labelChrono.getStyle().set("font-size", "6em").set("font-weight", "bold");
        btnStart = new Button("DÉMARRER", e -> togglerChrono(e.getSource()));
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        actualiserAffichageChrono();

        HorizontalLayout layoutScores = new HorizontalLayout(
            createZoneScore(labelNomE1, labelScore1, true),
            new H2(" VS "),
            createZoneScore(labelNomE2, labelScore2, false)
        );
        layoutScores.setWidthFull();
        layoutScores.setJustifyContentMode(JustifyContentMode.AROUND);

        Button btnEnregistrer = new Button("Enregistrer le score final", e -> finaliserMatch());
        btnEnregistrer.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        btnEnregistrer.getStyle().set("margin-top", "2em");

        zoneArbitrage.add(labelChrono, btnStart, layoutScores, btnEnregistrer);
    }

    private void actualiserListeMatchs() {
        List<MatchInfo> matchs = new ArrayList<>();
        try (Connection con = ConnectionSimpleSGBD.connectMySQL("92.222.25.165", 3306, "m3_emorlet01", "m3_emorlet01", "a1d6060b")) {
            // Requête SQL corrigée : On récupère les IDs et on crée un nom par défaut (Équipe ID)
            // Car la colonne 'nom' n'existe pas dans ta table 'equipe'
            String sql = "SELECT m.id, e1.id as idE1, e2.id as idE2 FROM matchs m " +
                         "JOIN equipe e1 ON m.id = e1.idMatch AND e1.num = 1 " +
                         "JOIN equipe e2 ON m.id = e2.idMatch AND e2.num = 2 " +
                         "WHERE m.statut = 'EN_COURS'";
            
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                int idM = rs.getInt("id");
                int idE1 = rs.getInt("idE1");
                int idE2 = rs.getInt("idE2");
                // On affiche "Equipe [ID]" pour que ça fonctionne sans erreur
                matchs.add(new MatchInfo(idM, idE1, idE2, "Equipe " + idE1, "Equipe " + idE2));
            }
            selectMatch.setItems(matchs);
            selectMatch.setVisible(!matchs.isEmpty());
            messageVide.setVisible(matchs.isEmpty());
        } catch (Exception e) {
            Notification.show("Erreur de chargement : " + e.getMessage());
        }
    }

    private void finaliserMatch() {
        if (matchSelectionne == null) return;

        try (Connection con = ConnectionSimpleSGBD.connectMySQL("92.222.25.165", 3306, "m3_emorlet01", "m3_emorlet01", "a1d6060b")) {
            con.setAutoCommit(false); 
            
            PreparedStatement ps1 = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps1.setInt(1, scoreE1); 
            ps1.setInt(2, matchSelectionne.idEquipe1);
            ps1.executeUpdate();

            PreparedStatement ps2 = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps2.setInt(1, scoreE2); 
            ps2.setInt(2, matchSelectionne.idEquipe2);
            ps2.executeUpdate();

            PreparedStatement psM = con.prepareStatement("UPDATE matchs SET statut = 'TERMINE' WHERE id = ?");
            psM.setInt(1, matchSelectionne.idMatch);
            psM.executeUpdate();

            con.commit();
            Notification.show("Scores enregistrés pour le match " + matchSelectionne.idMatch);
            UI.getCurrent().getPage().reload();
        } catch (Exception e) {
            Notification.show("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    private VerticalLayout createZoneScore(H2 nomLabel, H1 scoreLabel, boolean isE1) {
        Button bPlus = new Button("+1", e -> {
            if(isE1) scoreE1++; else scoreE2++;
            scoreLabel.setText(String.valueOf(isE1 ? scoreE1 : scoreE2));
        });
        Button bMoins = new Button("-1", e -> {
            if(isE1) { if(scoreE1 > 0) scoreE1--; } else { if(scoreE2 > 0) scoreE2--; }
            scoreLabel.setText(String.valueOf(isE1 ? scoreE1 : scoreE2));
        });
        VerticalLayout v = new VerticalLayout(nomLabel, scoreLabel, bPlus, bMoins);
        v.setAlignItems(Alignment.CENTER);
        return v;
    }

    private void actualiserAffichageChrono() {
        if (secondesRestantes > 0) {
            labelChrono.setText(String.format("%02d:%02d", secondesRestantes / 60, secondesRestantes % 60));
            labelChrono.getStyle().set("color", "black");
        } else {
            labelChrono.setText("MATCH TERMINÉ");
            labelChrono.getStyle().set("color", "red");
            if (btnStart != null) btnStart.setEnabled(false);
        }
    }

    private void togglerChrono(Button source) {
        if (!enCours) {
            enCours = true; source.setText("PAUSE"); lancerTimer(UI.getCurrent());
        } else {
            enCours = false; source.setText("DÉMARRER"); if (timer != null) timer.shutdown();
        }
    }

    private void lancerTimer(UI ui) {
        ui.setPollInterval(1000);
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> ui.access(() -> {
            if (secondesRestantes > 0 && enCours) {
                secondesRestantes--; 
                actualiserAffichageChrono();
            } else if (secondesRestantes <= 0) {
                enCours = false;
                actualiserAffichageChrono();
                if (timer != null) timer.shutdown();
                ui.setPollInterval(-1);
            }
        }), 0, 1, TimeUnit.SECONDS);
    }
}