package fr.insa.toto.webui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import fr.insa.beuvron.utils.database.ConnectionPool; 
import fr.insa.toto.model.BdDTest;                 
import fr.insa.toto.model.GestionBDD;             
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@Theme("default")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
//        try (Connection con = ConnectionPool.getConnection()) {
//            GestionBDD.razBdd(con);
            
//            BdDTest.createBdDTest(con);
            
//            System.out.println("--- Base de données initialisée avec succès ---");
            
//        } catch (SQLException ex) {
//            throw new Error("Impossible d'initialiser la BDD", ex);
//        }

        SpringApplication.run(Application.class, args);
}
}