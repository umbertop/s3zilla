package com.umbertopalazzini.s3zilla.concurrency;

public enum TransferTaskType {
    DOWNLOAD("Download"),
    UPLOAD("Upload"),
    UNDEFINED("Undefined");

    private String description;

    TransferTaskType(String description) {
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public static TransferTaskType getTransferTaskType(String description){
        for(TransferTaskType type : values()){
            if (type.getDescription().equals(description)){
                return type;
            }
        }

        return UNDEFINED;
    }
}
