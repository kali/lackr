package com.fotonauts.lackr.interpolr.utils;

import java.util.Arrays;

public class BoyerMooreScanner {

    private final int[] firstTable;
    private final int[] secondTable;
    private final byte[] needle;

    private static int unsign(byte b) {
        return b >= 0 ? b : b + 256;
    }

    public static int[] computeFirstTable(byte[] needle) {
        int[] result = new int[256];
        for (int i = 0; i < 256; i++)
            result[i] = needle.length;
        for (int i = 1; i < needle.length; i++) {
            int b = unsign(needle[needle.length - 1 - i]);
            if (result[b] == needle.length)
                result[b] = i;
        }
        return result;
    }

    public static int[] computeSecondTable(byte[] needle) {
        int[] result = new int[needle.length];
        Arrays.fill(result, -1);
        for (int prefixLength = 0; prefixLength < needle.length; prefixLength++) {

            for (int offsetTried = 0; result[prefixLength] == -1 && offsetTried <= needle.length; offsetTried++) {
                boolean match = true;
                // try to match end of pattern at that offset
                for (int i = 0; match && i < prefixLength && i + offsetTried < needle.length; i++)
                    if (needle[needle.length - 1 - i] != needle[needle.length - 1 - offsetTried - i])
                        match = false;
                // the previous character must not match
                if (prefixLength + offsetTried < needle.length)
                    match = match
                            && (needle[needle.length - 1 - prefixLength] != needle[needle.length - 1 - offsetTried - prefixLength]);
                // System.err.format("prefixLength:%d offsetTried:%d\n",
                // prefixLength, offsetTried);
                if (match) {
                    result[prefixLength] = offsetTried;
                }
            }

        }
        return result;
    }

    public BoyerMooreScanner(byte[] needle) {
        this.needle = needle;
        this.firstTable = computeFirstTable(needle);
        this.secondTable = computeSecondTable(needle);
    }

    public int searchNext(byte[] buffer, int start, int stop) {
        while (start + needle.length <= stop) {
            int cursor = start + needle.length - 1;
            while (cursor > start && buffer[cursor] == needle[cursor - start])
                cursor--;
            if (cursor == start && buffer[start] == needle[0]) {
                return start;
            }
            if (cursor == start + needle.length - 1) {
                start += firstTable[unsign(buffer[cursor])];
            } else
                start += secondTable[start + needle.length - 1 - cursor];
        }
        return -1;
    }

    public int length() {
        return needle.length;
    }
}
