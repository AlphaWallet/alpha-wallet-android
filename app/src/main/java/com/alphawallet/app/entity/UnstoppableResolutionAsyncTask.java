package com.alphawallet.app.entity;

import android.os.AsyncTask;

import com.unstoppabledomains.exceptions.NamingServiceException;
import com.unstoppabledomains.resolution.Resolution;

public class UnstoppableResolutionAsyncTask extends AsyncTask<String, Void, String> {
    private Resolution resolution = new Resolution("https://mainnet.infura.io/v3/213fff28936343858ca9c5115eff1419");

    @Override
    protected String doInBackground(String... domain) {
        try {
            return this.resolution.addr(domain[0], "eth");
        } catch (NamingServiceException e) {
            return e.getMessage();
        }
    }
}
