package com.umbertopalazzini.s3zilla.view;

import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * View purpose class in order to well organize data in the logTable.
 * Fields needed:
 * - local file name
 * - remote file name (I retrieve it from the transfer)
 * - progress (it's simply a progress bar)
 * - size (I retrieve it from the transfer)
 * - status
 */
public class LogItem {
    private ProgressBar progress;
    private Transfer transfer;
    private String localFile;
    private Label status;

    public LogItem(String localFile, ProgressBar progressBar, Transfer transfer, Label status) {
        this.localFile = localFile;
        this.progress = progressBar;
        this.transfer = transfer;
        this.status = status;
    }

    public String getLocalFile() {
        return this.localFile;
    }

    /**
     * If the transfer is a Download retrieve its full path (on S3).
     * Otherwise return the localFile.
     *
     * @return
     */
    public String getRemoteFile() {
        if (transfer instanceof Download) {
            return ((Download) this.transfer).getKey();
        } else {
            return localFile;
        }
    }

    public ProgressBar getProgress() {
        return this.progress;
    }

    /**
     * Returns the transfer total upload/download size.
     *
     * @return
     */
    public long getSize(){
        return transfer.getProgress().getTotalBytesToTransfer();
    }

    public Label getStatus(){
        return this.status;
    }
}
