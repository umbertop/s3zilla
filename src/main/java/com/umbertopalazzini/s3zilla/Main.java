package com.umbertopalazzini.s3zilla;

import com.umbertopalazzini.s3zilla.controller.S3ZillaController;
import com.umbertopalazzini.s3zilla.utility.Consts;
import javafx.application.Application;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {
    private Stage primaryStage;
    private ResourceBundle bundle;

    @Override
    public void start(Stage primaryStage){
        try {
            S3ZillaController controller;
            FXMLLoader fxmlLoader;
            BorderPane root;
            Scene scene;

            // Retrieves the fxml file containing the layout of the GUI.
            fxmlLoader = new FXMLLoader(Main.class.getResource("/layout_main.fxml"));
            // Retrieves the resource bundle, the default language is english.
            bundle = ResourceBundle.getBundle(Consts.ENGLISH, Locale.ENGLISH);
            // Loads the bundle/language.
            fxmlLoader.setResources(bundle);

            root = fxmlLoader.load();
            // Loads the fxml.
            scene = new Scene(root);


            controller = fxmlLoader.getController();
            controller.setMain(this);

            // Initializes the stage and show it.
            this.primaryStage = primaryStage;
            this.primaryStage.setScene(scene);
            this.primaryStage.setTitle("S3Zilla");
            this.primaryStage.setMinWidth(800);
            this.primaryStage.setMinHeight(614);
            this.primaryStage.show();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage(){
        return this.primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}