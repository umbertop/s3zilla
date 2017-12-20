package com.umbertopalazzini.s3zilla.utility;

import java.io.File;

public class Consts {
    public static final String DOWNLOAD_PATH = System.getProperty("user.home") + File.separator + "S3Zilla" + File.separator;

    public static final long BYTE = 1024L;
    public static final long MB = 1024L * 1024L;
    public static final long GB = 1024L * 1024L * 1024L;

    public static final String ITALIAN = "locales/locale_it_IT";
    public static final String ENGLISH = "locales/locale_en_GB";
}
