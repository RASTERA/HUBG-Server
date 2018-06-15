// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// Communicator.java | Link to central auth server

package com.rastera.Networking;

import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.HashMap;

import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;

class Communicator {

    private static String token;
    public static final boolean developmentMode = true;

    // Establishes API key
    public Communicator() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("token")));
            token = reader.readLine().trim();
        } catch (Exception e) {
            System.out.println("Unable to read API token");
            System.exit(0);
        }

        System.out.println("Token loaded successfully");
    }

    // Get URL based on development mode
    private static final HashMap<RequestDestination, String> baseProductionHashMap = new HashMap<RequestDestination, String>() {
        {
            put(RequestDestination.URL, "https://rastera.xyz/");
            put(RequestDestination.API, "https://api.rastera.xyz/");
            put(RequestDestination.AUTH, "https://authentication.rastera.xyz/");
        }
    };

    private static final HashMap<RequestDestination, String> baseDevelopmentHashMap = new HashMap<RequestDestination, String>() {
        {
            put(RequestDestination.URL, "http://localhost:3005/");
            put(RequestDestination.API, "http://localhost:3005/api/");
            put(RequestDestination.AUTH, "http://localhost:3005/auth/");
        }
    };

    public enum RequestType {POST, GET}
    public enum RequestDestination {URL, API, AUTH}

    public static String getURL(RequestDestination destination) {
        if (Communicator.developmentMode) {
            return baseDevelopmentHashMap.get(destination);
        } else {
            return baseProductionHashMap.get(destination);
        }
    }

    // General request
    // GET or POST
    public static JSONObject request(RequestType type, JSONObject data, String destination) {
        try {
            // Init connection
            URLConnection socket;
            if (Communicator.developmentMode) {
                socket = (HttpURLConnection) new URL(destination).openConnection();
                ((HttpURLConnection) socket).setRequestMethod(type.toString());
            } else {
                socket = (HttpsURLConnection) new URL(destination).openConnection();
                ((HttpsURLConnection) socket).setRequestMethod(type.toString());
            }

            // Configure header
            socket.setConnectTimeout(5000);
            socket.setRequestProperty("User-Agent", "Mozilla/5.0");
            socket.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            socket.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            // Flush JSON if POST request
            if (type == RequestType.POST) {
                socket.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());

                writer.write(data.toString());
                writer.flush();
                writer.close();
            }

            // Response data
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder rawData = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                rawData.append(line);
            }

            return new JSONObject(rawData.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Update player kills
    public static void updateKills(String killer, String opponent, String weapon) {
        Thread socketThread = new Thread(() -> {
            Long date = Instant.now().toEpochMilli();

            try {

                // Update attacker kills
                JSONObject changesKiller = new JSONObject() {
                    {
                        put("kills", 1);
                        put("actions", new JSONObject() {
                                {
                                    put("caption", String.format("You killed %s with %s", opponent, weapon));
                                    put("date", date);
                                    put("type", "KILL");
                                }
                            }
                        );
                    }
                };

                // Update victim deaths
                JSONObject changesOpponent = new JSONObject() {
                    {
                        put("deaths", 1);
                        put("actions", new JSONObject() {
                                {
                                    put("caption", String.format("%s killed you with %s", killer, weapon));
                                    put("date", date);
                                    put("type", "KILLED");
                                }
                            }
                        );
                    }
                };

                // Issues changes to auth server
                Communicator.updateUser(changesKiller, killer);
                Communicator.updateUser(changesOpponent, opponent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socketThread.setDaemon(true);
        socketThread.start();
    }

    // Update number of matches
    public static void updateMatches(String username) {
        Thread socketThread = new Thread(() -> {
            try {
                JSONObject changes = new JSONObject() {
                    {
                        put("matches", 1);
                    }
                };

                Communicator.updateUser(changes, username);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socketThread.setDaemon(true);
        socketThread.start();
    }

    // General user update given changes
    private static void updateUser(JSONObject changes, String username) {
        // kills, deaths, matches
        try {
            // Label desired changes and requests
            JSONObject out = new JSONObject() {
                {
                    put("changes", changes);
                    put("token", token);
                    put("username", username);
                }
            };
            Communicator.request(RequestType.POST, out, Communicator.getURL(RequestDestination.API) + "update");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}