package Server;

import Communication.GetProperties;
import com.sun.istack.internal.NotNull;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Core {

    public static boolean done = false;

    public static void main(String[] args) {

        ConcurrentHashMap<String, User> myDatabase;
        String filePath = "";
        try {
            filePath = (GetProperties.getPropertiesFile().getProperty("server.databasePath"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        File database = new File(filePath);
        if(!database.exists()) {
            // create database from scratch
            myDatabase = new ConcurrentHashMap<String, User>();
        }
        else{
            // deserialize old database
            myDatabase = Deserializer.deserialize(filePath);
        }
        // default port picked at random
        int port = 61543;
        ServerSocket connector = null;
        ExecutorService clientHandlers = Executors.newCachedThreadPool();
        // load the port on which to run the server from the ./server.properties file
        try{
            port = Integer.parseInt(GetProperties.getPropertiesFile().getProperty("server.port"));
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }

        // run a loop to accept connections
        // start a thread for each one and restart the loop
        // listening for incoming connections is a blocking operation
        // requires a ServerSocket
        try {
            connector = new ServerSocket(port);
        } catch (BindException e) {
            System.out.println("Port " + port + " busy, couldn't bind it. Please choose a different one in the server.properties file.");
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // deamon with the task of saving the user database every 2 seconds
        SavestateDeamon databaseDeamon = new SavestateDeamon(myDatabase);
        Thread databaseDeamonThread = new Thread(databaseDeamon);
        databaseDeamonThread.start();

        // main loop, in here we will accept the clients and hand them off to a handler thread
        while(!done){
            System.out.println("Accepting a new connection on port "+port+".");
            Socket sock = null;
            try {
                sock = connector.accept();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if(sock != null) {
                Runnable clientInstance = new ClientInstance(myDatabase, sock, port);
                clientHandlers.execute(clientInstance);
            }
        }
        //TODO: thread dedicated to saving any changes to the User database
        //TODO: intercept sigterm
        clientHandlers.shutdown();
        while (!clientHandlers.isTerminated()) {
        }
        //TODO: save user database
        System.out.println("Server shutdown complete.");
    }
}
