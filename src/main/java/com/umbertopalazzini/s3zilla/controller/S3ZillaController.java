package com.umbertopalazzini.s3zilla.controller;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.umbertopalazzini.s3zilla.model.S3Client;
import com.umbertopalazzini.s3zilla.utility.SizeConverter;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

public class S3ZillaController implements Initializable {
    @FXML
    private TableView<S3ObjectSummary> filesTable;
    @FXML
    private TableColumn<S3ObjectSummary, String> filesTable_name;
    @FXML
    private TableColumn<S3ObjectSummary, Date> filesTable_lastModified;
    @FXML
    private TableColumn<S3ObjectSummary, String> filesTable_size;
    @FXML
    private TableView logTable;
    @FXML
    private ListView<String> foldersListView;
    @FXML
    private ComboBox bucketComboBox;
    @FXML
    private Button downloadButton;
    @FXML
    private Button uploadButton;

    private ObservableList<Bucket> buckets;
    private ObservableList<String> folders;
    private ObservableList<S3ObjectSummary> files;

    private S3Client s3Client;

    public void initialize(URL location, ResourceBundle resources) {
        // Initializes the S3Client.
        s3Client = S3Client.getInstance();

        // Initializes the lists, loads it to the ComboBox and selects the first element.
        Platform.runLater(() -> {
            buckets = FXCollections.observableArrayList(s3Client.listBuckets());
            bucketComboBox.getItems().addAll(buckets);
            bucketComboBox.getSelectionModel().select(6);// TODO: remove this line.
        });

        // Initializes the list and loads all the folders to the list view
        Platform.runLater(() -> {
            folders = FXCollections.observableArrayList(s3Client.listFolders(buckets.get(6)));
            foldersListView.getItems().addAll(folders);
        });

        Platform.runLater(() -> {
            files = FXCollections.observableArrayList(s3Client.listFiles(buckets.get(6), null));
            filesTable.getItems().addAll(files);
        });

        initComboCellFactory();
        initListCellFactory();
        initTableCellFactory();
    }

    private void initComboCellFactory() {
        // Sets the cell factory for the bucketComboBox.
        bucketComboBox.setCellFactory(cell ->
                new ListCell<Bucket>() {
                    public void updateItem(Bucket bucket, boolean empty) {
                        if (!empty)
                            setText(bucket.getName());
                        super.updateItem(bucket, empty);
                    }
                }
        );

        // Converts the Bucket to a String to be properly read.
        bucketComboBox.setConverter(
                new StringConverter<Bucket>() {
                    public String toString(Bucket bucket) {
                        return bucket.getName();
                    }

                    public Bucket fromString(String name) {
                        return new Bucket(name);
                    }
                }
        );

        // As the bucket is changed in the combo box, it changes the folders in the list view.
        bucketComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Bucket>() {
            @Override
            public void changed(ObservableValue<? extends Bucket> observable, Bucket oldValue, Bucket newValue) {
                Platform.runLater(() -> {
                    folders.clear();
                    folders = FXCollections.observableArrayList(s3Client.listFolders(newValue));
                    foldersListView.setItems(folders);

                    files.clear();
                    files = FXCollections.observableArrayList(s3Client.listFiles(
                            (Bucket) bucketComboBox.getSelectionModel().getSelectedItem(),
                            null));
                    filesTable.setItems(files);
                });
            }
        });
    }

    private void initListCellFactory() {
        foldersListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                Platform.runLater(() -> {
                    files.clear();
                    files = FXCollections.observableArrayList(s3Client.listFiles(
                            (Bucket) bucketComboBox.getSelectionModel().getSelectedItem(),
                            newValue));
                    filesTable.setItems(files);
                });
            }
        });
    }

    private void initTableCellFactory() {
        // Sets the cell factory for the name column of the filesTable.
        filesTable_name.setCellValueFactory(column ->
                new SimpleStringProperty(column.getValue().getKey())
        );

        // Sets the cell factory for the last modified column of the filesTable.
        filesTable_lastModified.setCellValueFactory(column ->
                new SimpleObjectProperty<Date>(column.getValue().getLastModified())
        );

        // Sets the cell factory for the last modified column of the filesTable.
        filesTable_size.setCellValueFactory(column ->
                new SimpleObjectProperty<>(SizeConverter.format(column.getValue().getSize()))
        );

        // Enables the download button if a row is pressed.
        filesTable.setRowFactory(table -> {
            TableRow<S3ObjectSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    downloadButton.setDisable(false);
                }
            });
            return row;
        });
    }

    @FXML
    private void download() {
        Thread thread = new Thread(() -> {
            S3ObjectSummary downloadObject = filesTable.getSelectionModel().getSelectedItem();
            Download download = s3Client.download(downloadObject);
            TransferProgress progress = download.getProgress();

            while (!download.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double progressD = progress.getPercentTransferred();
                long transferred = progress.getBytesTransferred();
                long to = progress.getTotalBytesToTransfer();
                System.out.printf("\rDownloading: %.2f%%\t%d of %d", progressD, transferred, to);
            }
        });

        thread.start();
    }
}