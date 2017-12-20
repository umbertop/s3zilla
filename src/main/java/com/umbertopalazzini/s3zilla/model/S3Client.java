package com.umbertopalazzini.s3zilla.model;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.*;

import com.umbertopalazzini.s3zilla.utility.Consts;

import java.io.File;
import java.util.List;

public class S3Client {
    private static S3Client instance = null;

    private AmazonS3 amazonS3Client;
    private TransferManager transferManager;

    /**
     * Returns the current transfer (upload/download) manager.
     *
     * @return
     */
    public TransferManager getTransferManager() {
        return this.transferManager;
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

    /**
     * Downloads a single file and returns it in order to be accessed by JavaFX for tracking its progress.
     *
     * @param s3ObjectSummary
     * @return
     */
    public Download download(S3ObjectSummary s3ObjectSummary) throws AmazonServiceException {
        Download download;
        File downloadFile;
        String filename = s3ObjectSummary.getKey();

        // If the s3Object is in a directory, extract only the name.
        filename = !filename.contains("/")
                ? filename
                : filename.substring(filename.lastIndexOf("/") + 1, filename.length());

        downloadFile = new File(Consts.DOWNLOAD_PATH + filename);

        return transferManager.download(s3ObjectSummary.getBucketName(), s3ObjectSummary.getKey(), downloadFile);
    }

    /**
     * Uploads a single file in a bucket and in a "optional" specified folder.
     * @param bucket
     * @param key
     * @param uploadfile
     * @return
     * @throws AmazonServiceException
     */
    public Upload upload(Bucket bucket, String key, File uploadfile) throws AmazonServiceException {
        String uploadfileName = uploadfile.getName();

        // It extracts the file name from the full path.
        String fullkey = key == null
                ? ""
                : key + uploadfileName.substring(uploadfileName.lastIndexOf(File.separator) + 1, uploadfileName.length());


        return transferManager.upload(bucket.getName(), fullkey, uploadfile);
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