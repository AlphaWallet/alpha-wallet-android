package com.alphawallet.app.util;

import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.Wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

public class SendDisplayJsonProcesser {

    private static String senderAddress;
    private static String recipientAddress;


        public static String[] getJsonResponse() {
            String[] messsages = new String[5];
            try {
                // Realizar la solicitud GET y obtener el JSON
                String jsonResponse = getJsonResponseFromServer();
                System.out.println(jsonResponse);
                // Procesar el JSON
                JSONArray jsonArray = new JSONArray(jsonResponse);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                messsages[0] = jsonObject.getString("message_row_1");
                messsages[1] = jsonObject.getString("message_row_2");
                messsages[2] = jsonObject.getString("message_row_3");
                messsages[3] = jsonObject.getString("message_row_4");
                messsages[4] = jsonObject.getString("message_row_5");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return messsages;
        }

        public static String getJsonResponseFromServer() throws Exception {
            // Realizar la solicitud GET y obtener el JSON
            String urlDirection = "http://demo.thinkhawa.com:8080/rest_api/message_to_user_wallet/?amount=1&format=json&from_wallet_address=" + senderAddress
                    + "&to_wallet_address=" + recipientAddress;
            System.out.println(senderAddress);
            System.out.println(recipientAddress);
            URL url = new URL(urlDirection);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            Scanner scanner = new Scanner(conn.getInputStream());
            String jsonResponse = scanner.useDelimiter("\\A").next();
            scanner.close();
            return jsonResponse;
        }

        public static void setSenderWalletAddress(MutableLiveData<Wallet> wallet){
            senderAddress = Objects.requireNonNull(wallet.getValue()).address;
        }

        public static void setRecipientWalletAddress(String destAddress){
            recipientAddress = destAddress;
        }
}
