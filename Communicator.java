class Communicator {
    private static final boolean developmentMode = true;

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

            // Header stuff
            socket.setConnectTimeout(5000);
            socket.setRequestProperty("User-Agent", "Mozilla/5.0");
            socket.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            socket.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            if (type == RequestType.POST) {
                socket.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());

                writer.write(data.toString());
                writer.flush();
                writer.close();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String rawData = "";
            String line;

            while ((line = reader.readLine()) != null) {
                rawData += line;
            }

            return new JSONObject(rawData);

        } catch (Exception e) {
            e.printStackTrace();
            Main.errorQuit("Unable to process request. Please try again later.");
            return null;
        }
    }

}