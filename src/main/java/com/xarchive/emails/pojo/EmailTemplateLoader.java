package com.xarchive.emails.pojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmailTemplateLoader {

    public static String loadEmailTemplate(String filePath) {
        try {
            // Read the file content as a single string
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}