package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Fenêtre d'aide personnalisée.
 */
public class AideDialog extends Dialog {

    public AideDialog() {
        this.setHeaderTitle("Aide");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(new Paragraph("Bonjour, comment puis-je vous aider ?"));
        
        this.add(dialogLayout);

        Button closeButton = new Button("Fermer", e -> this.close());
        this.getFooter().add(closeButton);
    }
}