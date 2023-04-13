package com.alphawallet.app.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SendDisplayJsonProcesser {

        public static String[] getJsonResponse() {
            String[] messsages = new String[5];
            try {
                // Realizar la solicitud GET y obtener el JSON
                String jsonResponse = getJsonResponseFromServer();
                System.out.println(jsonResponse);
                // Procesar el JSON
                JSONArray jsonArray = new JSONArray(jsonResponse);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                System.out.println(jsonObject.getString("message_row_1"));
                messsages[0] = jsonObject.getString("message_row_1");
                System.out.println(jsonObject.getString("message_row_2"));
                messsages[1] = jsonObject.getString("message_row_2");
                System.out.println(jsonObject.getString("message_row_3"));
                messsages[2] = jsonObject.getString("message_row_3");
                System.out.println(jsonObject.getString("message_row_4"));
                messsages[3] = jsonObject.getString("message_row_4");
                System.out.println(jsonObject.getString("message_row_5"));
                messsages[4] = jsonObject.getString("message_row_5");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return messsages;
        }

        public static String getJsonResponseFromServer() throws Exception {
            // Realizar la solicitud GET y obtener el JSON
            URL url = new URL("http://demo.thinkhawa.com:8080/rest_api/message_to_user_wallet/?amount=100&format=json&from_wallet_address=0x261176b8b4373d98f616bF9b88E0b0B15a03Dc4d&to_phone_number=+5491163769380&to_phone_number=+5491163769380");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            Scanner scanner = new Scanner(conn.getInputStream());
            String jsonResponse = scanner.useDelimiter("\\A").next();
            scanner.close();
            return jsonResponse;
        }
}
