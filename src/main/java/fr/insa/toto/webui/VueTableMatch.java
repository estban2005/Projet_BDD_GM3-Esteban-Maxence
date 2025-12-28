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
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route(value = "table-match", layout = MainLayout.class)
public class VueTableMatch extends VerticalLayout {

    private static int scoreE1 = 0;
    private static int scoreE2 = 0;
    private static int secondesRestantes = 600;
    private static boolean enCours = false;
    private static MatchInfo matchSelectionne = null;

    private ComboBox<MatchInfo> selectMatch = new ComboBox<>("Sélectionner le match en cours");
    private H1 labelScore1 = new H1("0");
    private H1 labelScore2 = new H1("0");
    private Span labelChrono = new Span();
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

    public VueTableMatch() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        getStyle().set("background-color", "#D2B48C");

        actualiserAffichageChrono();
        actualiserListeMatchs();

        selectMatch.setItemLabelGenerator(MatchInfo::toString);
        selectMatch.setWidth("500px");
        if (matchSelectionne != null) selectMatch.setValue(matchSelectionne);
        selectMatch.addValueChangeListener(e -> matchSelectionne = e.getValue());

        labelChrono.getStyle().set("font-size", "6em").set("font-weight", "bold");
        Button btnStart = new Button(enCours ? "PAUSE" : "DÉMARRER", e -> togglerChrono(e.getSource()));
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout layoutScores = new HorizontalLayout(
            createZoneScore("Équipe 1", labelScore1, true),
            new H2(" VS "),
            createZoneScore("Équipe 2", labelScore2, false)
        );

        Button btnEnregistrer = new Button("Valider le score et terminer le match", e -> finaliserMatch());
        btnEnregistrer.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        add(selectMatch, labelChrono, btnStart, layoutScores, btnEnregistrer);
        if (enCours) lancerTimer(UI.getCurrent());
    }

    private void actualiserListeMatchs() {
        List<MatchInfo> matchs = new ArrayList<>();
        try (Connection con = ConnectionSimpleSGBD.connection("localhost", 3306, "m3_emorlet01", "root", "")) {
            // Requête complexe pour récupérer les noms des joueurs par équipe pour chaque match
            String sql = "SELECT m.id, e1.id, e2.id, " +
                         "(SELECT GROUP_CONCAT(j.surnom) FROM composition c JOIN joueur j ON c.idJoueur = j.id WHERE c.idEquipe = e1.id) as nomsE1, " +
                         "(SELECT GROUP_CONCAT(j.surnom) FROM composition c JOIN joueur j ON c.idJoueur = j.id WHERE c.idEquipe = e2.id) as nomsE2 " +
                         "FROM matchs m " +
                         "JOIN equipe e1 ON m.id = e1.idMatch AND e1.num = 1 " +
                         "JOIN equipe e2 ON m.id = e2.idMatch AND e2.num = 2 " +
                         "WHERE m.statut = 'EN_COURS'";
            
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                String label = "Match " + rs.getInt(1) + " : " + rs.getString("nomsE1") + " VS " + rs.getString("nomsE2");
                matchs.add(new MatchInfo(rs.getInt(1), rs.getInt(2), rs.getInt(3), label));
            }
            selectMatch.setItems(matchs);
        } catch (Exception e) {
            Notification.show("Erreur chargement matchs : " + e.getMessage());
        }
    }

    private void finaliserMatch() {
        if (selectMatch.getValue() == null) {
            Notification.show("Erreur : Sélectionnez un match !");
            return;
        }
        MatchInfo mi = selectMatch.getValue();
        try (Connection con = ConnectionSimpleSGBD.connection("localhost", 3306, "m3_emorlet01", "root", "")) {
            con.setAutoCommit(false);
            // 1. Update Score Equipe 1
            PreparedStatement ps1 = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps1.setInt(1, scoreE1); ps1.setInt(2, mi.idEquipe1);
            ps1.executeUpdate();

            // 2. Update Score Equipe 2
            PreparedStatement ps2 = con.prepareStatement("UPDATE equipe SET score = ? WHERE id = ?");
            ps2.setInt(1, scoreE2); ps2.setInt(2, mi.idEquipe2);
            ps2.executeUpdate();

            // 3. Clôturer le match
            PreparedStatement psM = con.prepareStatement("UPDATE matchs SET statut = 'TERMINE' WHERE id = ?");
            psM.setInt(1, mi.idMatch);
            psM.executeUpdate();

            con.commit();
            Notification.show("Match terminé ! Les scores ont été enregistrés.");
            
            // Reset pour le match suivant
            scoreE1 = 0; scoreE2 = 0; secondesRestantes = 600; enCours = false; matchSelectionne = null;
            UI.getCurrent().getPage().reload();
        } catch (Exception e) {
            Notification.show("Erreur SQL : " + e.getMessage());
        }
    }

    private VerticalLayout createZoneScore(String nom, H1 label, boolean isE1) {
        label.setText(String.valueOf(isE1 ? scoreE1 : scoreE2));
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

    private void actualiserAffichageChrono() {
        labelChrono.setText(String.format("%02d:%02d", secondesRestantes / 60, secondesRestantes % 60));
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
                secondesRestantes--; actualiserAffichageChrono();
            }
        }), 0, 1, TimeUnit.SECONDS);
    }
}