package com.umbertopalazzini.s3zilla.utility;

import java.text.DecimalFormat;

public class SizeConverter {
    private static final String[] units = {"B", "kB", "MB", "GB", "TB"};

    /**
     * Converts a given size (integer number) to a formatted string with the size suffix.
     *
     * @param size
     * @return
     */
    public static String format(long size){
        int digits = (int) (Math.log10(size) / Math.log10(1024));

        return size <= 0
                ? "Nan"
                : new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digits)) + " " + units[digits];
    }
}