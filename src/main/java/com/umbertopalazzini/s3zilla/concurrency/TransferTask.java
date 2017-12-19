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

public class TransferTask {
    public static Task getTransferTask(final TransferTaskType transferTaskType, final Transfer transfer, final File file,
                                       final TableView logTable, final ProgressBar progressBar, final Label status) {
        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                LogItem logItem = null;
                long transferred = 0;

                if (transferTaskType == TransferTaskType.DOWNLOAD) {
                    Download download = (Download) transfer;

                    String fileName = !download.getKey().contains("/")
                            ? download.getKey()
                            : download.getKey().substring(download.getKey().lastIndexOf('/') + 1,
                            download.getKey().length());

                    logItem = new LogItem(fileName, progressBar, download, status);
                } else if (transferTaskType == TransferTaskType.UPLOAD) {
                    Upload upload = (Upload) transfer;

                    logItem = new LogItem(file.getName(), progressBar, upload, status);
                }

                logTable.getItems().add(logItem);

                while (!transfer.isDone()) {
                    long startTime = System.currentTimeMillis();
                    Thread.sleep(1000);
                    long endTime = System.currentTimeMillis();

                    long transferredNow = transfer.getProgress().getBytesTransferred() - transferred;
                    // Speed in kB/s.
                    float speed = transferredNow / ((endTime - startTime) / 1000) / Consts.BYTE;

                    updateProgress(transfer.getProgress().getBytesTransferred(),
                            transfer.getProgress().getTotalBytesToTransfer());

                    updateMessage(String.valueOf(speed) + " kB/s");

                    transferred += transferredNow;
                }

                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            progressBar.progressProperty().unbind();

            status.textProperty().unbind();

            if (transferTaskType == TransferTaskType.DOWNLOAD) {
                status.setText("Downloaded");
            } else {
                status.setText("Uploaded");
            }
        });

        task.setOnFailed(event -> {
            progressBar.progressProperty().unbind();

            status.textProperty().unbind();
            status.setText("Failed");
        });


        return task;
    }
}
