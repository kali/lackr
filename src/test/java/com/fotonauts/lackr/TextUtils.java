package com.fotonauts.lackr;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextUtils {

    // From:
    // http://blog.efftinge.de/2008/10/multi-line-string-literals-in-java.html
    // Takes a comment (/**/) and turns everything inside the comment to a
    // string that is returned from S()
    public static String S() throws FileNotFoundException {
        StackTraceElement element = new RuntimeException().getStackTrace()[1];
        String name = "src/test/java/" + element.getClassName().replace('.', '/') + ".java";
        InputStream in = new FileInputStream(name);
        String s = convertStreamToString(in, element.getLineNumber());
        return s.substring(s.indexOf("/*") + 2, s.indexOf("*/"));
    }

    // From http://www.kodejava.org/examples/266.html
    private static String convertStreamToString(InputStream is, int lineNum) {
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
    
        String line = null;
        int i = 1;
        try {
            while ((line = reader.readLine()) != null) {
                if (i++ >= lineNum) {
                    sb.append(line + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        return sb.toString();
    }

    
}
