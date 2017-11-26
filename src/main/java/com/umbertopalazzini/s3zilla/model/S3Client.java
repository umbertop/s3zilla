package com.umbertopalazzini.s3zilla.model;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import java.util.ArrayList;
import java.util.List;

public class S3Client {
    private static S3Client instance = null;

    private AmazonS3 amazonS3Client;
    private TransferManager transferManager;

    public List<Bucket> listBuckets() {
        return amazonS3Client.listBuckets();
    }

    public List<String> listFolders(Bucket bucket) {
        ListObjectsV2Request listRequest;
        ListObjectsV2Result listResult;

        listRequest = new ListObjectsV2Request().withBucketName(bucket.getName()).withDelimiter("/");
        listResult = amazonS3Client.listObjectsV2(listRequest);

        return listResult.getCommonPrefixes();
    }

    public List<S3ObjectSummary> listFiles(Bucket bucket, String folder){
        ListObjectsV2Request listRequest;
        ListObjectsV2Result listResult;

        if(folder == null || folder.equals("")){
            listRequest = new ListObjectsV2Request().withBucketName(bucket.getName()).withDelimiter("/");
            listResult = amazonS3Client.listObjectsV2(listRequest);
        } else {
            listRequest = new ListObjectsV2Request().withBucketName(bucket.getName()).withPrefix(folder);
            listResult = amazonS3Client.listObjectsV2(listRequest);
        }

        return listResult.getObjectSummaries();
    }

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
