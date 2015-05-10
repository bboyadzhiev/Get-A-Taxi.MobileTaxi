package com.getataxi.mobiletaxi.comm;

import com.getataxi.mobiletaxi.utils.Constants;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import retrofit.client.Request;
import retrofit.client.UrlConnectionClient;

/**
 * Created by bvb on 29.4.2015 г..
 */
public class RetrofitHttpClient extends UrlConnectionClient {



    private static OkUrlFactory generateDefaultOkUrlFactory() {
        OkHttpClient client = new com.squareup.okhttp.OkHttpClient();
        client.setConnectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setReadTimeout(Constants.READ_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(Constants.WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
        return new OkUrlFactory(client);
    }

    private final OkUrlFactory factory;

    public RetrofitHttpClient() {
        factory = generateDefaultOkUrlFactory();
    }

    @Override protected HttpURLConnection openConnection(Request request) throws IOException {
        return factory.open(new URL(request.getUrl()));
    }
}
