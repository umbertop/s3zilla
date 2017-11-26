package com.umbertopalazzini.s3zilla.model;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.TransferProgress;

import java.io.File;
import java.util.List;

public class S3Client {
    private static S3Client instance = null;

    private AmazonS3 amazonS3Client;
    private TransferManager transferManager;
    private TransferProgress transferProgress;

    /**
     * Returns the current transfer (upload/download) progress.
     * @return
     */
    public TransferProgress getTransferProgress(){
        return this.transferProgress;
    }

    /**
     * Lists the buckets.
     *
     * @return
     */
    public List<Bucket> listBuckets() {
        return amazonS3Client.listBuckets();
    }

    /**
     * List the folders in a bucket.
     *
     * @param bucket
     * @return
     */
    public List<String> listFolders(Bucket bucket) {
        ListObjectsV2Request listRequest;
        ListObjectsV2Result listResult;

        listRequest = new ListObjectsV2Request().withBucketName(bucket.getName()).withDelimiter("/");
        listResult = amazonS3Client.listObjectsV2(listRequest);

        return listResult.getCommonPrefixes();
    }

    /**
     * Lists the files (keys) in a bucket and in an optional speficied folder.
     *
     * @param bucket
     * @param folder
     * @return
     */
    public List<S3ObjectSummary> listFiles(Bucket bucket, String folder) {
        ListObjectsV2Request listRequest;
        ListObjectsV2Result listResult;

        if (folder == null || folder.equals("")) {
            listRequest = new ListObjectsV2Request().withBucketName(bucket.getName()).withDelimiter("/");
            listResult = amazonS3Client.listObjectsV2(listRequest);
        } else {
            listRequest = new ListObjectsV2Request().withBucketName(bucket.getName()).withPrefix(folder);
            listResult = amazonS3Client.listObjectsV2(listRequest);
        }

        return listResult.getObjectSummaries();
    }

    public void download(S3ObjectSummary s3Object) {
        Download download;
        File downloadFile;

        downloadFile = new File("nome file");
        // String filename = s3Object.getKey();

        /*
        filename = !filename.contains("/")
                ? filename
                : filename.substring(filename.lastIndexOf("/") + 1);
        */

        download = transferManager.download(s3Object.getBucketName(), s3Object.getKey(), downloadFile);

        // TODO: manage thread
        do{
            try{
                Thread.sleep(100);
            } catch (InterruptedException e){
                return;
            }

            transferProgress = download.getProgress();
        } while(!download.isDone());
    }

    /**
     * Initializes the s3 client and the transfer manager in order to track downloads/uploads progress.
     */
    private void initialize() {
        amazonS3Client = AmazonS3ClientBuilder.defaultClient();
        transferManager = TransferManagerBuilder.defaultTransferManager();
    }

    public static S3Client getInstance() {
        if (instance == null) {
            instance = new S3Client();
            instance.initialize();
        }

        return instance;
    }
}
