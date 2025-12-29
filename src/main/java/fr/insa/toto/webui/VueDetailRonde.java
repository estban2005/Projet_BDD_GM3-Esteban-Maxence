package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.ServiceGestionTournoi;
import java.sql.Connection;
import java.util.Optional;

@Route(value = "detail-ronde", layout = MainLayout.class)
public class VueDetailRonde extends VerticalLayout implements HasUrlParameter<Integer> {

    private int idRonde;

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.idRonde = parameter;
        refresh();
    }

    private void refresh() {
        this.removeAll();
        try (Connection con = ConnectionPool.getConnection()) {
            Optional<Ronde> rondeOpt = Ronde.findAll(con).stream()
                    .filter(r -> r.getId().equals(idRonde)).findFirst();

            if (rondeOpt.isPresent()) {
                Ronde r = rondeOpt.get();
                add(new H2("Détails de la Ronde n°" + r.getNumero()));
                add(new Span("Statut : " + r.getStatut()));
                // Affichage du temps défini à la création
                add(new Span("Durée des matchs : " + r.getTempsMatch() + " minutes"));

                if (r.getStatut().equals("EN_COURS")) {
                    Button btnCloturer = new Button("Clôturer la ronde", e -> {
                        try (Connection con2 = ConnectionPool.getConnection()) {
                            ServiceGestionTournoi.verifierEtCloturerRonde(con2, idRonde);
                            Notification.show("Ronde clôturée !");
                            refresh();
                        } catch (Exception ex) {
                            Notification.show("Erreur : " + ex.getMessage());
                        }
                    });
                    add(btnCloturer);
                }
            }
        } catch (Exception ex) {
            Notification.show("Erreur de chargement.");
        }
    }
}