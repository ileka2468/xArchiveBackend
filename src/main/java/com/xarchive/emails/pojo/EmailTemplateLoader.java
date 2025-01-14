package com.xarchive.emails.pojo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class EmailTemplateLoader {

    public static String loadEmailTemplate(String filePath) {
        try (InputStream inputStream = EmailTemplateLoader.class.getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found in classpath: " + filePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}