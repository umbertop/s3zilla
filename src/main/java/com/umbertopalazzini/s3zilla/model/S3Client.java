package com.umbertopalazzini.s3zilla.model;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.*;
import com.umbertopalazzini.s3zilla.utility.Consts;

import java.io.File;
import java.util.List;

public class S3Client {
    // Singleton instance because only a S3Client is needed in the runtime.
    private static S3Client instance = null;

    private AmazonS3 amazonS3Client;
    private TransferManager transferManager;
    private ClientConfiguration clientConfiguration;

    private File file;

    /**
     * Returns the current transfer (upload/download) manager.
     *
     * @return
     */
    public TransferManager getTransferManager() {
        return this.transferManager;
    }

    public File getFile() {
        return this.file;
    }

    public void setFile(File file){
        this.file = file;
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
     * Lists the files (keys) in a bucket and in an optional folder.
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
        String filename = s3ObjectSummary.getKey();

        // If the s3Object is in a directory, extract only the name.
        filename = !filename.contains("/")
                ? filename
                : filename.substring(filename.lastIndexOf("/") + 1, filename.length());

        file = new File(Consts.DOWNLOAD_PATH + filename);

        return transferManager.download(s3ObjectSummary.getBucketName(), s3ObjectSummary.getKey(), file);
    }

    /**
     * Upload a single file and returns it in order to be accessed by JavaFX for tracking its progress.
     *
     * @param bucket
     * @param key
     * @param uploadfile
     * @return
     * @throws AmazonServiceException
     */
    public Upload upload(Bucket bucket, String key, File uploadfile) throws AmazonServiceException {
        String uploadfileName = uploadfile.getName();

        // It extracts the file name from the full file path.
        String fullkey = key == null
                ? uploadfileName.substring(uploadfileName.lastIndexOf(File.separator) + 1, uploadfileName.length())
                : key + uploadfileName.substring(uploadfileName.lastIndexOf(File.separator) + 1, uploadfileName.length());


        return transferManager.upload(bucket.getName(), fullkey, uploadfile);
    }

    /**
     * Initializes the s3 client and the transfer manager in order to track downloads/uploads progress.
     */
    private void initialize() {
        clientConfiguration = new ClientConfiguration();

        clientConfiguration.setMaxErrorRetry(10);
        clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(10));

        amazonS3Client = AmazonS3ClientBuilder
                .standard()
                .withClientConfiguration(clientConfiguration)
                .build();

        transferManager = TransferManagerBuilder
                .standard()
                .withMultipartUploadThreshold(5 * Consts.MB)
                .withMultipartCopyThreshold(25 * Consts.MB)
                .withDisableParallelDownloads(false)
                .withS3Client(amazonS3Client)
                .build();
    }

    public static S3Client getInstance() {
        if (instance == null) {
            instance = new S3Client();
            instance.initialize();
        }

        return instance;
    }
}