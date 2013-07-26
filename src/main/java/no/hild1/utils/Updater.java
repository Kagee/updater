package no.hild1.utils;

import java.net.*;
import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.management.InvalidApplicationException;
import javax.swing.*;

public class Updater {
    public String foo;
    public Updater(String f) {
        this.foo = f;
    }
    private static void error(String header, String msg) {
        JOptionPane.showMessageDialog(null, msg, header, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static Properties loadConfig(String configFileName) {
        Properties conf = new Properties();
        boolean internal = true;
        try {
            InputStream confIS = Updater.class.getResourceAsStream(configFileName);
            if (confIS == null) {
                throw new IOException();
            }
            conf.load(confIS);
        } catch (IOException ioe) {
            try {
                internal = false;
                conf.load(new FileInputStream(configFileName));
            } catch (FileNotFoundException fnfe) {
                error("updater.conf missing", "Found neither a internal nor external updater.conf");
            } catch (IOException ioe2) {
                error("Failed to read updater.conf", "Failed while reading external config: " + ioe2.getMessage());
            }
        }
        configFileName = ((internal) ? "internal ":"external ") + " file "+ configFileName;
        if (!conf.containsKey("jenkinsModule") || !conf.containsKey("fileMatchRegexp") || !conf.containsKey("fileMatchRegexp")) {
            error("Config parameters missing", "Missing keys in " + configFileName);
        }
        conf.setProperty("origin", configFileName);
        return conf;
    }

    public static String loadJSONText(String jsonURLString) {
        String jsonText = "";
        try {
            URL jsonURL = new URL(jsonURLString);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(jsonURL.openStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null)
                jsonText += inputLine;
            in.close();
            return jsonText;
        } catch (MalformedURLException mue) {
            error("Error in config", "jenkinsModule is not a valid URL in "  + "\nUrl ends up as " + jsonURLString);
        } catch (IOException ioe) {
            error("Fail to donwload", "Failed to download " + jsonURLString + "\n\n" + ioe.getMessage());
        }
        return jsonText;
    }

    public static String getUrl(String jsonText, String jsonURLString, String regexp) {
        String relativePath = "";
        String url = "";
        try {
            JSONObject json = new JSONObject(jsonText);
            if (json.has("url") && json.has("artifacts")) {
                url = json.optString("url");
                JSONArray artifacts = json.optJSONArray("artifacts");
                assert artifacts != null;
                for(int i = 0; i < artifacts.length(); i++) {
                    JSONObject jsob = artifacts.getJSONObject(i);

                    if (jsob.has("relativePath")) {
                        String name2test = jsob.optString("relativePath");
                        System.out.println(name2test);
                        if (name2test != null && name2test.matches(regexp)) {
                            relativePath = name2test;
                        }
                    }
                }
                if(url.isEmpty() || relativePath.isEmpty()) {
                    error("Failed to find download path","Failed to find relativePath matching '" + regexp + "' on " + jsonURLString);
                }

            } else {
                error("Failed to find download path","Element url or artifacts missing from " + jsonURLString);
            }

        } catch (JSONException jsone) {
            error("Syntax error","Syntax error on " + jsonURLString + "\n\n" + jsone.getMessage());
        }
        String jarURL = url + "/artifact/" + relativePath;
        return jarURL.replace("//", "/");
    }

    public static void main(String[] args) {
        String configFileName = "updater.conf";
        Properties conf = loadConfig(configFileName);

        String jenkinsModule = conf.getProperty("jenkinsModule");
        String fileMatchRegexp = conf.getProperty("fileMatchRegexp");
        String jsonURLString = jenkinsModule + "/api/json";

        String jsonText = loadJSONText(jsonURLString);

        String downloadUrl = getUrl(jsonText, jsonURLString, fileMatchRegexp);

        System.out.println(downloadUrl);


    } 

} 
