package org.example;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.net.Socket;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;


public class Main {
    public static void main(String[] args) {
        String pathToCsv = "service-names-port-numbers.csv"; // replace with your CSV file path
        String line;
        Map<Integer, String> portDescriptions = new TreeMap<>(); //A sorted map to hold Ports and Descriptions in numerical order

        try (BufferedReader br = new BufferedReader(new FileReader(pathToCsv))) {

            while ((line = br.readLine()) != null) {
                // Define comma as line delimiter
                String[] columns = line.split(",");

                try {
                    if (Character.isDigit(columns[1].charAt(0))){
                        int portIntoInt = Integer.parseInt(columns[1]);
                        portDescriptions.put(portIntoInt, columns[3]);
                    }

                }catch (Exception e){
                    // don't care :)
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid: Cannot Parse Letters to Integer");
        }

        //Scan host system for ports and enter into TreeMap
        Map<Integer, Integer> localPorts = new TreeMap<>(); // holds local ports

        String targetHost = "localhost"; // Can be changed to target a system other than local
        int minPort = 1;
        int maxPort = 65535;

        System.out.println("Scanning ports on " + targetHost + "...");

        for (int port = minPort; port <= maxPort; port++) {
            try {
                Socket socket = new Socket(targetHost, port);
                localPorts.put(port, port);
                socket.close();
            } catch (IOException e) {
                //Error handling for closed ports
            }
        }

        System.out.println("Port scanning finished.");

        //Compare CSV and Port scan to match descriptions to ports
        Map<Integer, String> localPortDescriptions = new TreeMap<>();
        for (Map.Entry<Integer, String> entry : portDescriptions.entrySet()){
            if (localPorts.containsKey(entry.getKey())) {
                localPortDescriptions.put(entry.getKey(), entry.getValue());
            }
        }

        System.out.println();
                for(Map.Entry<Integer, String> entry : localPortDescriptions.entrySet()){
            int localHostKey = entry.getKey();
            String localHostDescription = entry.getValue();
            }

        //Send ports to the Redis Database and Print
        Jedis jedis = null;
        try{
            jedis = new Jedis("localhost", 6379);
            System.out.println("Local System Open Ports: ");
            for (Map.Entry<Integer, String> entry : localPortDescriptions.entrySet()) {
                int localKey = entry.getKey();
                String localDescription = entry.getValue();
                String localKeyAsString = String.valueOf(localKey);
                //Set entry in database
                jedis.set(localKeyAsString, localDescription);
            }
            for (Map.Entry<Integer, String> entry : localPortDescriptions.entrySet()) {
                String value = jedis.get(String.valueOf(entry.getKey()));
                System.out.println("Port: " + entry.getKey() + " Description: " + value);
            }
        }catch (JedisConnectionException e){
            System.out.println("Could not connect to Redis:" + e.getMessage());
        }finally {
            jedis.close();
        }



    }
}