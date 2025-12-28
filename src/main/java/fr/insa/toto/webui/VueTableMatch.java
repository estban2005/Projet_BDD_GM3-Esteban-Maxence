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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route(value = "table-match", layout = MainLayout.class)
public class VueTableMatch extends VerticalLayout {

    // On utilise 'static' pour que les valeurs ne soient pas effacées 
    // si on change d'onglet dans le menu
    private static int scoreJ1 = 0;
    private static int scoreJ2 = 0;
    private static int secondesRestantes = 600; // 10 minutes
    private static boolean enCours = false;

    private ComboBox<String> selectMatch = new ComboBox<>("Match à arbitrer");
    private H1 labelScore1 = new H1(String.valueOf(scoreJ1));
    private H1 labelScore2 = new H1(String.valueOf(scoreJ2));
    private Span labelChrono = new Span();
    private ScheduledExecutorService timer;

    public VueTableMatch() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background-color", "#D2B48C");

        // 1. Initialisation de l'affichage du chrono au chargement
        actualiserAffichageChrono();

        // 2. Sélection du match
        selectMatch.setItems("Match 1 : Joueur A vs Joueur B", "Match 2 : Joueur C vs Joueur D");
        selectMatch.setWidth("400px");

        // 3. Style du Chronomètre
        labelChrono.getStyle()
                .set("font-size", "6em")
                .set("font-weight", "bold")
                .set("color", "black")
                .set("font-family", "monospace");
        
        Button btnStart = new Button(enCours ? "PAUSE" : "DÉMARRER", e -> togglerChrono(e.getSource()));
        btnStart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // 4. Zone de score
        HorizontalLayout layoutScores = new HorizontalLayout();
        layoutScores.setWidthFull();
        layoutScores.setJustifyContentMode(JustifyContentMode.AROUND);
        layoutScores.add(createZoneScore("Joueur 1", labelScore1, true), 
                          new H2(" VS "), 
                          createZoneScore("Joueur 2", labelScore2, false));

        // 5. Bouton d'enregistrement
        Button btnEnregistrer = new Button("Enregistrer le score final", e -> finaliserMatch());
        btnEnregistrer.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(selectMatch, labelChrono, btnStart, layoutScores, btnEnregistrer);

        // Si on revient sur la page et que le chrono tournait, on relance l'affichage
        if (enCours) {
            lancerTimer(UI.getCurrent());
        }
    }

    private void actualiserAffichageChrono() {
        int min = secondesRestantes / 60;
        int sec = secondesRestantes % 60;
        labelChrono.setText(String.format("%02d:%02d", min, sec));
    }

    private void togglerChrono(Button source) {
        if (!enCours) {
            enCours = true;
            source.setText("PAUSE");
            lancerTimer(UI.getCurrent());
        } else {
            enCours = false;
            source.setText("DÉMARRER");
            if (timer != null) timer.shutdown();
            UI.getCurrent().setPollInterval(-1);
            // Ici, secondesRestantes garde sa valeur actuelle, donc l'affichage reste fixe
            actualiserAffichageChrono();
        }
    }

    private void lancerTimer(UI ui) {
        ui.setPollInterval(1000); 
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> {
            ui.access(() -> {
                if (secondesRestantes > 0 && enCours) {
                    secondesRestantes--;
                    actualiserAffichageChrono();
                } else if (secondesRestantes <= 0) {
                    enCours = false;
                    ui.setPollInterval(-1);
                    timer.shutdown();
                    Notification.show("Temps écoulé !");
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private VerticalLayout createZoneScore(String nom, H1 label, boolean isJ1) {
        Button btnPlus = new Button("+1", e -> {
            if (isJ1) scoreJ1++; else scoreJ2++;
            label.setText(String.valueOf(isJ1 ? scoreJ1 : scoreJ2));
        });
        Button btnMoins = new Button("-1", e -> {
            if (isJ1) { if(scoreJ1 > 0) scoreJ1--; } else { if(scoreJ2 > 0) scoreJ2--; }
            label.setText(String.valueOf(isJ1 ? scoreJ1 : scoreJ2));
        });
        VerticalLayout layout = new VerticalLayout(new H2(nom), label, btnPlus, btnMoins);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    private void finaliserMatch() {
        enCours = false;
        if (timer != null) timer.shutdown();
        UI.getCurrent().setPollInterval(-1);
        Notification.show("Match enregistré : " + scoreJ1 + " - " + scoreJ2);
    }
}