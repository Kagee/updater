package no.hild1.utils;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

public class Updater {
    public String foo;
    public Updater(String f) {
        this.foo = f;
    }
    public static void main(String[] args) throws Exception {
        URL oracle = new URL("http://hjelp-meg.nu.hild1.no:8080/job/telepie/lastSuccessfulBuild/no.hild1.bank$telepie/api/json");
        BufferedReader in = new BufferedReader(
        new InputStreamReader(oracle.openStream()));
        String jsonText = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            jsonText += inputLine; //System.out.println(inputLine);
        in.close();
    
        //Updater upd = new Updater("Hello World");
        //System.out.println(upd.foo);
        JSONObject json = JSONObject(jsonText);
    } 

} 
