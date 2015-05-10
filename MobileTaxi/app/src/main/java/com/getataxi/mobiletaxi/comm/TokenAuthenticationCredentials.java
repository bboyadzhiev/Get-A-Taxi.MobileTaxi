package com.getataxi.mobiletaxi.comm;

import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.http.Request;

/**
 * Created by feratel on 6.5.2015 ?..
 */
public class TokenAuthenticationCredentials implements Credentials {

    private String token;

    public TokenAuthenticationCredentials(String token) {
        this.initialize(token);
    }

    private void initialize(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void prepareRequest(Request request) {

        request.addHeader("Authorization", "Bearer " + this.token);
    }

}
