package com.umbertopalazzini.s3zilla;

import com.umbertopalazzini.s3zilla.utility.Consts;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {
    private Stage primaryStage;
    private ResourceBundle bundle;

    @Override
    public void start(Stage primaryStage){
        try {
            FXMLLoader fxmlLoader;
            AnchorPane root;
            Scene scene;

            // Retrieves the fxml file containing the layout of the GUI.
            fxmlLoader = new FXMLLoader(Main.class.getResource("/layout_main.fxml"));
            // Retrieves the resource bundle, the default language is english.
            bundle = ResourceBundle.getBundle(Consts.ENGLISH, Locale.ENGLISH);
            // Loads the bundle.
            fxmlLoader.setResources(bundle);

            root = fxmlLoader.load();
            // Loads the fxml.
            scene = new Scene(root);

            // Initializes the stage and show it.
            this.primaryStage = primaryStage;
            this.primaryStage.setScene(scene);
            this.primaryStage.setTitle("S3Zilla");
            this.primaryStage.show();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}