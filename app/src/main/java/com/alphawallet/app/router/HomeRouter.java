package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.ui.HomeActivity;

public class HomeRouter {
    public void open(Context context, boolean isClearStack) {
        Intent intent = new Intent(context, HomeActivity.class);
        if (isClearStack) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        context.startActivity(intent);
    }

    public void openWithIntent(Context context, String importData) {
        Intent intent = new Intent(context, HomeActivity.class);
        if (importData != null) intent.putExtra("startIntent", importData);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
