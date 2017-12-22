package com.umbertopalazzini.s3zilla.concurrency;

import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.Upload;
import com.umbertopalazzini.s3zilla.utility.Consts;
import com.umbertopalazzini.s3zilla.view.LogItem;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;

import java.io.File;

public class TransferTask extends Task {
    private final Transfer transfer;
    private final File file;

    private final TableView<LogItem> logTable;
    private final ProgressBar progressBar;
    private final Label status;

    public TransferTask(Transfer transfer, File file,
                        TableView<LogItem> logTable, ProgressBar progressBar, Label status) {
        this.transfer = transfer;
        this.file = file;

        this.logTable = logTable;
        this.progressBar = progressBar;
        this.status = status;

        progressBar.progressProperty().bind(progressProperty());
        status.textProperty().bind(messageProperty());
    }

    /**
     * Performs the transfer whenever called.
     *
     * @return
     * @throws Exception
     */
    @Override
    protected Object call() throws Exception {
        LogItem logItem = null;
        long transferred = 0;

        // If the transfer is a download cast it to Download.
        if (transfer instanceof Download) {
            Download download = (Download) transfer;

            // If it's been downloaded from a folder extract its name.
            String fileName = !download.getKey().contains("/")
                    ? download.getKey()
                    : download.getKey().substring(download.getKey().lastIndexOf('/') + 1,
                    download.getKey().length());

            logItem = new LogItem(fileName, progressBar, download, status);
        }
        // Otherwise cast it to Upload.
        else {
            Upload upload = (Upload) transfer;

            logItem = new LogItem(file.getName(), progressBar, upload, status);
        }

        logTable.getItems().add(logItem);

        // While the transfer isn't complete, calc its download speed.
        while (!transfer.isDone()) {
            long startTime = System.currentTimeMillis();
            Thread.sleep(1000);
            long endTime = System.currentTimeMillis();

            long transferredNow = transfer.getProgress().getBytesTransferred() - transferred;
            // Speed in kB/s.
            float speed = transferredNow / ((endTime - startTime) / 1000) / Consts.KB;

            updateProgress(transfer.getProgress().getBytesTransferred(),
                    transfer.getProgress().getTotalBytesToTransfer());

            updateMessage(String.valueOf(speed) + " kB/s");

            transferred += transferredNow;
        }

        return null;
    }


    /**
     * If the task is successful, it will execute this code.
     */
    @Override
    protected void succeeded() {
        super.succeeded();

        progressBar.progressProperty().unbind();

        status.textProperty().unbind();

        if (transfer instanceof Upload) {
            status.setText("Downloaded");
        } else {
            status.setText("Uploaded");
        }
    }

    /**
     * If the task has been cancelled, it will execute this code.
     */
    @Override
    protected void cancelled() {
        // TODO: implement this feature.
    }

    /**
     * If the task is failed, it will execute this code.
     */
    @Override
    protected void failed() {
        super.failed();

        progressBar.progressProperty().unbind();

        status.textProperty().unbind();
        status.setText("Failed");
    }
}
