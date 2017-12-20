package com.umbertopalazzini.s3zilla.controller;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import com.umbertopalazzini.s3zilla.Main;
import com.umbertopalazzini.s3zilla.model.S3Client;
import com.umbertopalazzini.s3zilla.utility.SizeConverter;
import com.umbertopalazzini.s3zilla.view.LogItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
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
    private ListView<String> foldersListView;
    @FXML
    private ComboBox bucketComboBox;
    @FXML
    private Button downloadButton;
    @FXML
    private Button uploadButton;

    private Main main;

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

        // Sets the cell factory for the size column of the filesTable.
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
    }

    @FXML
    private void download() {
        S3ObjectSummary transferObject = filesTable.getSelectionModel().getSelectedItem();
        ProgressBar progressBar = new ProgressBar(0.0f);
        Label status = new Label();

        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                Download download = s3Client.download(transferObject);
                long downloaded = 0;
                String filename = !download.getKey().contains("/")
                        ? download.getKey()
                        : download.getKey().substring(download.getKey().lastIndexOf("/") + 1, download.getKey().length());

                LogItem logItem = new LogItem(filename, progressBar, download, status);
                logTable.getItems().add(logItem);

                while (!download.isDone()) {
                    long startTime = System.currentTimeMillis();

                    Thread.sleep(1000);

                    long endTime = System.currentTimeMillis();
                    long transferred = download.getProgress().getBytesTransferred();
                    long transferredNow = transferred - downloaded;
                    // Speed in kB/s
                    float speed = transferredNow / ((endTime - startTime) / 1000) / 1024;
                    updateProgress(download.getProgress().getBytesTransferred(), download.getProgress().getTotalBytesToTransfer());
                    updateMessage(String.valueOf(speed) + " kB/s");

                    downloaded += transferredNow;
                }

                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            status.textProperty().unbind();

            status.setText("Downloaded");
        });

        new Thread(task).start();
    }

    @FXML
    private void upload(){
        Bucket bucket = (Bucket) bucketComboBox.getSelectionModel().getSelectedItem();
        ProgressBar progressBar = new ProgressBar(0.0f);
        Label status = new Label();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to upload");

        File uploadfile = fileChooser.showOpenDialog(main.getPrimaryStage());

        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                File uploadFile = uploadfile;
                String key = foldersListView.getSelectionModel().getSelectedItem();
                Upload upload = s3Client.upload(bucket, key, uploadFile);
                long uploaded = 0;

                LogItem logItem = new LogItem(uploadfile.getName(), progressBar, upload, status);
                logTable.getItems().add(logItem);

                while(!upload.isDone()){
                    long startTime = System.currentTimeMillis();

                    Thread.sleep(1000);

                    long endTime = System.currentTimeMillis();
                    long transferred = upload.getProgress().getBytesTransferred();
                    long transferredNow = transferred - uploaded;
                    // Speed in kB/s
                    float speed = transferredNow / ((endTime - startTime) / 1000) / 1024;
                    updateProgress(upload.getProgress().getBytesTransferred(), upload.getProgress().getTotalBytesToTransfer());
                    updateMessage(String.valueOf(speed) + " kB/s");

                    uploaded += transferredNow;
                }

                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            status.textProperty().unbind();

            status.setText("Uploaded");
        });

        new Thread(task).start();
    }

    public void setMain(Main main){
        this.main = main;
    }
}