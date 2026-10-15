package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * PhytoScope application entry point.
 *
 * <p>Uses a hybrid JavaFX + Spring Boot bootstrap:
 * the Spring context initialises before the JavaFX thread starts, then JavaFX pulls the main view from it.
 * This keeps dependency injection and service extensibility while respecting the JavaFX threading model.</p>
 */
@SpringBootApplication
public class PhytoScopeApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Initialise the Spring context during JavaFX init (not on the UI thread)
        context = new SpringApplicationBuilder(PhytoScopeApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        MainView mainView = context.getBean(MainView.class);

        Scene scene = new Scene(mainView, 1400, 860);

        stage.setTitle("PhytoScope \u2014 Plant Genome Structure Browser");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        // Redraw the canvas when the window is resized
        scene.widthProperty().addListener((obs, old, val) -> mainView.onViewportResized());
        scene.heightProperty().addListener((obs, old, val) -> mainView.onViewportResized());

        stage.show();

        // Load data after the window is shown so first paint is not blocked
        mainView.loadInitialData();
        mainView.onViewportResized();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}
