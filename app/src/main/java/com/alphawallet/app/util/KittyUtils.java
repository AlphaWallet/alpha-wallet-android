package com.alphawallet.app.util;

public class KittyUtils {
    public static String parseCooldownIndex(String index) {
        switch (Integer.parseInt(index)) {
            case 0:
                return "Fast";
            case 1:
            case 2:
                return "Swift";
            case 3:
            case 4:
                return "Snappy";
            case 5:
            case 6:
                return "Brisk";
            case 7:
            case 8:
                return "Plodding";
            case 9:
            case 10:
                return "Slow";
            case 11:
            case 12:
                return "Sluggish";
            default:
                return "Catatonic";
        }
    }
}
