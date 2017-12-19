package com.umbertopalazzini.s3zilla.model;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.*;
import com.umbertopalazzini.s3zilla.utility.Consts;

import java.io.File;
import java.util.ArrayList;
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

    public void download(S3ObjectSummary... s3ObjectSummaries) {
        for(S3ObjectSummary s3ObjectSummary : s3ObjectSummaries){
            Download download = null;
            File downloadFile = null;
            String filename = s3ObjectSummary.getKey();

            // If the s3Object is contained in a directory, extract only the name.
            filename = !filename.contains("/")
                    ? filename
                    : filename.substring(filename.lastIndexOf("/") + 1, filename.length());


            downloadFile = new File(Consts.DOWNLOAD_PATH + filename);
            download = transferManager.download(s3ObjectSummary.getBucketName(), s3ObjectSummary.getKey(), downloadFile);


            // TODO: manage thread for each download.
            while(!download.isDone()){
                transferProgress = download.getProgress();
            }
        }
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
