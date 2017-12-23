package com.umbertopalazzini.s3zilla.controller;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.umbertopalazzini.s3zilla.Main;
import com.umbertopalazzini.s3zilla.concurrency.TransferTask;
import com.umbertopalazzini.s3zilla.model.S3Client;
import com.umbertopalazzini.s3zilla.utility.SizeConverter;
import com.umbertopalazzini.s3zilla.view.LogItem;
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
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
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
    private TableView<LogItem> logTable;
    @FXML
    private TableColumn<LogItem, String> logTable_localFile;
    @FXML
    private TableColumn<LogItem, String> logTable_remoteFile;
    @FXML
    private TableColumn<LogItem, ProgressBar> logTable_progress;
    @FXML
    private TableColumn<LogItem, String> logTable_size;
    @FXML
    private TableColumn<LogItem, Label> logTable_status;
    @FXML
    private TableColumn<LogItem, HBox> logTable_actions;

    @FXML
    private ListView<String> foldersListView;
    @FXML
    private ComboBox bucketComboBox;
    @FXML
    private Button downloadButton;
    @FXML
    private Button uploadButton;

    // Main class reference.
    private Main main;

    private ObservableList<Bucket> buckets;
    private ObservableList<String> folders;
    private ObservableList<S3ObjectSummary> files;

    // Amazon S3 Client.
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

        // Initializes the list and loads all the folders to the list view.
        Platform.runLater(() -> {
            folders = FXCollections.observableArrayList(s3Client.listFolders(buckets.get(6)));
            foldersListView.getItems().addAll(folders);
        });

        // Initializes the table and load all the files to the table view.
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

        // Converts a Bucket to a String and a String to a Bucket to be properly read.
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

        // As the bucket changes in the combo box, the folders in the list view change too.
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

    // As the folder changes in the list view, the files gets loaded too.
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

        // Sets the cell factory for the size column of the filesTable.
        filesTable_lastModified.setCellValueFactory(column ->
                new SimpleObjectProperty<Date>(column.getValue().getLastModified())
        );

        // Sets the cell factory for the last modified column of the filesTable.
        filesTable_size.setCellValueFactory(column ->
                new SimpleObjectProperty<>(SizeConverter.format(column.getValue().getSize()))
        );

        // Enables the download button if a row is clicked in order to deny the user to download a null Object.
        filesTable.setRowFactory(table -> {
            TableRow<S3ObjectSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    downloadButton.setDisable(false);
                }
            });
            return row;
        });

        // Sets the cell factory for the localFile column of the logTable.
        logTable_localFile.setCellValueFactory(column ->
                new SimpleStringProperty(column.getValue().getLocalFile())
        );

        // Sets the cell factory for the remoteFile column of the logTable.
        logTable_remoteFile.setCellValueFactory(column ->
                new SimpleStringProperty(column.getValue().getRemoteFile())
        );

        // Sets the cell factory for the progress column of the logTable.
        logTable_progress.setCellValueFactory(column ->
                new SimpleObjectProperty<ProgressBar>(column.getValue().getProgress())
        );

        // Sets the cell factory for the size column of the logTable.
        logTable_size.setCellValueFactory(column ->
                new SimpleStringProperty(SizeConverter.format(column.getValue().getSize()))
        );

        // Sets the cell factory for the status column of the logTable.
        logTable_status.setCellValueFactory(column ->
                new SimpleObjectProperty<Label>(column.getValue().getStatus())
        );

        // Sets the cell factory for the actions column of the logTable.
        logTable_actions.setCellValueFactory(column ->
                new SimpleObjectProperty<HBox>(column.getValue().getActions())
        );
    }

    @FXML
    private void download() {
        S3ObjectSummary selectedObject = filesTable.getSelectionModel().getSelectedItem();
        ProgressBar progressBar = new ProgressBar(0.0f);
        Label status = new Label();
        HBox actions = new HBox();

        TransferManager transferManager = s3Client.getTransferManager();
        Download download = s3Client.download(selectedObject);
        File downloadFile = s3Client.getFile();

        TransferTask downloadTask = new TransferTask(download, downloadFile, transferManager,
                logTable, progressBar, status, actions);

        new Thread(downloadTask).start();
    }

    @FXML
    private void upload() {
        Bucket selectdBucket = (Bucket) bucketComboBox.getSelectionModel().getSelectedItem();
        String selectedFolder = foldersListView.getSelectionModel().getSelectedItem();
        ProgressBar progressBar = new ProgressBar(0.0f);
        Label status = new Label();
        HBox actions = new HBox();

        // Opens up a popup to choose a single file to upload.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to upload");
        s3Client.setFile(fileChooser.showOpenDialog(main.getPrimaryStage()));
        File uploadFile = s3Client.getFile();

        TransferManager transferManager = s3Client.getTransferManager();
        Upload upload = s3Client.upload(selectdBucket, selectedFolder, uploadFile);

        // TODO: fix upload to folder.
        TransferTask uploadTask = new TransferTask(upload, uploadFile, transferManager, logTable, progressBar, status, actions);

        new Thread(uploadTask).start();
    }

    public void setMain(Main main) {
        this.main = main;
    }
}