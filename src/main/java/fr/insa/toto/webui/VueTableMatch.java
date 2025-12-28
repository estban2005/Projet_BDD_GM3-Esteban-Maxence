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

    private static int scoreE1 = 0;
    private static int scoreE2 = 0;
    private static int secondesRestantes = 10; 
    private static boolean enCours = false;
    private static MatchInfo matchSelectionne = null;

    private ComboBox<MatchInfo> selectMatch = new ComboBox<>("Sélectionner le match à arbitrer");
    private Span messageVide = new Span("Aucun match prévu pour le moment.");
    private VerticalLayout zoneArbitrage = new VerticalLayout();
    
    private H1 labelScore1 = new H1("0");
    private H1 labelScore2 = new H1("0");
    private Span labelChrono = new Span();
    private Button btnStart; // Déclaré ici
    private ScheduledExecutorService timer;

    public static class MatchInfo {
        int idMatch;
        int idEquipe1, idEquipe2;
        String texte;
        public MatchInfo(int idM, int idE1, int idE2, String txt) {
            this.idMatch = idM; this.idEquipe1 = idE1; this.idEquipe2 = idE2; this.texte = txt;
        }
        @Override
        public String toString() { return texte; }
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

        // CORRECTION : On initialise les composants AVANT d'appeler actualiserAffichageChrono
        configurerZoneArbitrage();

        selectMatch.setWidth("500px");
        selectMatch.setItemLabelGenerator(MatchInfo::toString);
        
        selectMatch.addValueChangeListener(e -> {
            matchSelectionne = e.getValue();
            if (timer != null) {
                timer.shutdown();
                enCours = false;
            }
            secondesRestantes = 10;
            scoreE1 = 0;
            scoreE2 = 0;
            enCours = false;
            
            labelScore1.setText("0");
            labelScore2.setText("0");
            btnStart.setText("DÉMARRER");
            btnStart.setEnabled(true);
            actualiserAffichageChrono();
            
            zoneArbitrage.setVisible(matchSelectionne != null);
        });

        add(selectMatch, messageVide, zoneArbitrage);
        actualiserListeMatchs();
    }

    private void configurerZoneArbitrage() {
        zoneArbitrage.setAlignItems(Alignment.CENTER);
        zoneArbitrage.setVisible(false);

        // 1. D'abord on crée les objets
        labelChrono.getStyle().set("font-size", "6em").set("font-weight", "bold");
        btnStart = new Button("DÉMARRER", e -> togglerChrono(e.getSource()));
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // 2. ENSUITE on peut appeler la méthode qui les utilise
        actualiserAffichageChrono();

        HorizontalLayout layoutScores = new HorizontalLayout(
            createZoneScore("Équipe 1", labelScore1, true),
            new H2(" VS "),
            createZoneScore("Équipe 2", labelScore2, false)
        );
        layoutScores.setWidthFull();
        layoutScores.setJustifyContentMode(JustifyContentMode.AROUND);

        Button btnEnregistrer = new Button("Enregistrer le score final", e -> finaliserMatch());
        btnEnregistrer.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        btnEnregistrer.getStyle().set("margin-top", "2em");

        zoneArbitrage.add(labelChrono, btnStart, layoutScores, btnEnregistrer);
    }

    private void actualiserAffichageChrono() {
        if (secondesRestantes > 0) {
            labelChrono.setText(String.format("%02d:%02d", secondesRestantes / 60, secondesRestantes % 60));
            labelChrono.getStyle().set("color", "black");
            if (btnStart != null) btnStart.setEnabled(true); // Vérification de sécurité
        } else {
            labelChrono.setText("MATCH TERMINÉ");
            labelChrono.getStyle().set("color", "red");
            if (btnStart != null) btnStart.setEnabled(false); // Plus de crash car btnStart existe
        }
    }

    // ... (Le reste des méthodes reste inchangé : actualiserListeMatchs, finaliserMatch, togglerChrono, lancerTimer)
    
    private void actualiserListeMatchs() {
        List<MatchInfo> matchs = new ArrayList<>();
        try (Connection con = ConnectionSimpleSGBD.connectMySQL("92.222.25.165", 3306, "m3_emorlet01", "m3_emorlet01", "a1d6060b")) {
            String sql = "SELECT m.id, e1.id, e2.id FROM matchs m " +
                         "JOIN equipe e1 ON m.id = e1.idMatch AND e1.num = 1 " +
                         "JOIN equipe e2 ON m.id = e2.idMatch AND e2.num = 2 " +
                         "WHERE m.statut = 'EN_COURS'";
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                matchs.add(new MatchInfo(rs.getInt(1), rs.getInt(2), rs.getInt(3), "Match n°" + rs.getInt(1)));
            }
            if (matchs.isEmpty()) {
                selectMatch.setVisible(false);
                messageVide.setVisible(true);
            } else {
                selectMatch.setItems(matchs);
                selectMatch.setVisible(true);
                messageVide.setVisible(false);
            }
        } catch (Exception e) {
            Notification.show("Erreur BDD : " + e.getMessage());
        }
    }

    private void finaliserMatch() {
        if (matchSelectionne == null) return;
        try (Connection con = ConnectionSimpleSGBD.connectMySQL("92.222.25.165", 3306, "m3_emorlet01", "m3_emorlet01", "a1d6060b")) {
            con.setAutoCommit(false);
            PreparedStatement ps1 = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps1.setInt(1, scoreE1); ps1.setInt(2, matchSelectionne.idEquipe1);
            ps1.executeUpdate();
            PreparedStatement ps2 = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps2.setInt(1, scoreE2); ps2.setInt(2, matchSelectionne.idEquipe2);
            ps2.executeUpdate();
            PreparedStatement psM = con.prepareStatement("UPDATE matchs SET statut = 'TERMINE' WHERE id = ?");
            psM.setInt(1, matchSelectionne.idMatch);
            psM.executeUpdate();
            con.commit();
            Notification.show("Match enregistré !");
            UI.getCurrent().getPage().reload();
        } catch (Exception e) {
            Notification.show("Erreur lors de l'enregistrement.");
        }
    }

    private VerticalLayout createZoneScore(String nom, H1 label, boolean isE1) {
        label.setText("0");
        Button bPlus = new Button("+1", e -> {
            if(isE1) scoreE1++; else scoreE2++;
            label.setText(String.valueOf(isE1 ? scoreE1 : scoreE2));
        });
        Button bMoins = new Button("-1", e -> {
            if(isE1) { if(scoreE1 > 0) scoreE1--; } else { if(scoreE2 > 0) scoreE2--; }
            label.setText(String.valueOf(isE1 ? scoreE1 : scoreE2));
        });
        VerticalLayout v = new VerticalLayout(new H2(nom), label, bPlus, bMoins);
        v.setAlignItems(Alignment.CENTER);
        return v;
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