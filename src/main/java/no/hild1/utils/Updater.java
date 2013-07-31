package no.hild1.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static org.apache.commons.io.FileUtils.copyURLToFile;

import javax.swing.*;

public class Updater {
    private int latestBuild = -1;
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

        conf.setProperty("origin", configFileName);
        return conf;
    }
    public static boolean checkConfig(Properties conf, String configFileName) {
        if (!conf.containsKey("jenkinsModule") 
                || !conf.containsKey("fileMatchRegexp") 
                || !conf.containsKey("fileMatchRegexp")
                || !conf.containsKey("finalName")
                || !conf.containsKey("execute")) {
            error("Config parameters missing", "Missing keys in " + configFileName);
        }
        return true;
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

    public String getUrl(String jsonText, String jsonURLString, String regexp) {
        String relativePath = "";
        String url = "";
        try {
            JSONObject json = new JSONObject(jsonText);
            if (json.has("url") && json.has("artifacts") && json.has("number")) {
                url = json.optString("url");
                latestBuild = json.optInt("number", -1);
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
                if(url.isEmpty() || relativePath.isEmpty() || latestBuild == -1) {
                    error("Failed to find download path","Failed to find relativePath matching '" + regexp + "' on " + jsonURLString);
                }

            } else {
                error("Failed to find download path","Element url or artifacts missing from " + jsonURLString);
            }

        } catch (JSONException jsone) {
            error("Syntax error","Syntax error on " + jsonURLString + "\n\n" + jsone.getMessage());
        }
        String jarURL = url + "artifact/" + relativePath;
        return jarURL;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Checking for update");
        JProgressBar pb = new JProgressBar(0,5);
        pb.setIndeterminate(true);
        frame.add(pb, BorderLayout.CENTER);
        frame.setSize(300, 100);
        final Toolkit tk = Toolkit.getDefaultToolkit();
        final Dimension d = tk.getScreenSize();
        frame.setLocation((d.width - frame.getWidth())/2, (d.height - frame.getHeight())/2);
        frame.setVisible(true);
        pb.setValue(1);
        String configFileName = "updater.conf";
        Properties conf = loadConfig(configFileName);
        checkConfig(conf, configFileName);
        
        String jenkinsModule = conf.getProperty("jenkinsModule");
        String fileMatchRegexp = conf.getProperty("fileMatchRegexp");
        String finalName = conf.getProperty("finalName");
        boolean execute = conf.getProperty("execute").equals("true");
        String jsonURLString = jenkinsModule + "/api/json";
    
        String jsonText = loadJSONText(jsonURLString);
        pb.setValue(2);
        Updater u = new Updater();
        String downloadUrlString = u.getUrl(jsonText, jsonURLString, fileMatchRegexp);

        pb.setValue(3);
        URL downloadURL;
        File output = new File(".");
        try {
            output = new File(finalName);
            downloadURL = new URL(downloadUrlString);
            pb.setIndeterminate(true);
            copyURLToFile(downloadURL, output, 2000, 2000);
        } catch (MalformedURLException ex) {
            error("Failed to download",ex.toString() + "\n" + downloadUrlString);
        } catch (IllegalArgumentException ex) {
            error("Failed to download",ex.toString() + "\n" + downloadUrlString);
        } catch (IOException ex) {
            error("Failed to download",ex.toString() + "\n" + downloadUrlString);
        }
        
        if(execute) {
            try {
                URL url = output.toURI().toURL();//new URL("file:foo.jar");
                URLClassLoader loader = new URLClassLoader (new URL[] {url});
                //loader.getResourceAsStream(finalName)
                //.getResourceAsStream("com/foo/bar/theta.properties");
                Class cl = Class.forName ("no.hild1.bank.TelepayGUI", true, loader);
                Runnable foo = (Runnable) cl.newInstance();
                //foo.run();
                loader.close();
                frame.dispose();
            } catch (Exception ex) {
                error("Failed to launch", ex.toString());
            }
        }
    } 

} 
