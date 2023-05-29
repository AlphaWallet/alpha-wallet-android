package com.alphawallet.app.widget;

import static java.lang.System.exit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import android.widget.TextView;
import com.alphawallet.app.R;
import com.alphawallet.app.util.SendDisplayJsonProcesser;

import javax.annotation.Nullable;

public class JsonViewWidget extends LinearLayout {

    public TextView line1;
    public TextView line2;
    public TextView line3;
    public TextView line4;
    public TextView line5;

    public String[] jsonResponse;


    public JsonViewWidget(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        inflate(context, R.layout.item_json_display, this);
        line1 = findViewById(R.id.jsonLine1);
        line2 = findViewById(R.id.jsonLine2);
        line3 = findViewById(R.id.jsonLine3);
        line4 = findViewById(R.id.jsonLine4);
        line5 = findViewById(R.id.jsonLine5);
    }

    public void getJsonData(){
        try {
            GetJsonInBackground getJsonInBackground = new GetJsonInBackground();
            getJsonInBackground.start();
            getJsonInBackground.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        jsonResponse[0] = "Destinatario: " + jsonResponse[0];
        line1.setText(jsonResponse[0]);
        line2.setText(jsonResponse[1]);
        line3.setText(jsonResponse[2]);
        line4.setText(jsonResponse[3]);
        line5.setText(jsonResponse[4]);

        line1.setVisibility(View.VISIBLE);
        line2.setVisibility(View.VISIBLE);
        line3.setVisibility(View.VISIBLE);
        line4.setVisibility(View.VISIBLE);
        line5.setVisibility(View.VISIBLE);
    }

    public class GetJsonInBackground extends Thread{
        @Override
        public void run(){
            jsonResponse = SendDisplayJsonProcesser.getJsonResponse();
        }
    }
}
