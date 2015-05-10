package com.getataxi.mobiletaxi.comm;

import com.getataxi.mobiletaxi.comm.contracts.AccountAPI;
import com.getataxi.mobiletaxi.comm.contracts.TaxiOrdersAPI;
import com.getataxi.mobiletaxi.comm.contracts.TaxiStandsAPI;

import com.getataxi.mobiletaxi.comm.contracts.TaxiAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.List;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by bvb on 31.3.2015 г..
 */
public class RestClient{

    private String baseUrl;
    private AccountAPI accountService;
    private TaxiOrdersAPI ordersService;
    private TaxiStandsAPI taxiStandsService;
    private TaxiAPI taxiService;
    private List<NameValuePair> headers;


    public RestClient(String baseUrl)
    {
        this.headers = new ArrayList<NameValuePair>();
        this.baseUrl = baseUrl;
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                .create();

        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Content-Type", "application/json");
                if(!headers.isEmpty()){
                    for (NameValuePair header : headers) {
                        request.addHeader(header.getName(), header.getValue());
                    }
                }
            }
        };

        final RetrofitHttpClient client = new RetrofitHttpClient();


        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(this.baseUrl)
                .setConverter(new GsonConverter(gson))
                //.setClient(new OkClient(okHttpClient))
                .setClient(client)
                .setRequestInterceptor(requestInterceptor)
                .build();

        accountService = restAdapter.create(AccountAPI.class);
        ordersService = restAdapter.create(TaxiOrdersAPI.class);
        taxiStandsService = restAdapter.create(TaxiStandsAPI.class);
        taxiService = restAdapter.create(TaxiAPI.class);
    }

    public AccountAPI getAccountService(List<NameValuePair> headers){
        this.headers.clear();

        if (headers != null) {
            this.headers = headers;
        }
        return accountService;
    }

    public TaxiOrdersAPI getOrdersService(List<NameValuePair> headers){
        this.headers.clear();
        if (headers != null) {
            this.headers = headers;
        }
        return ordersService;
    }

    public TaxiStandsAPI getTaxiStandsService(List<NameValuePair> headersPassed){
      //  this.headers.clear();
        if (headersPassed != null || !headersPassed.isEmpty()) {
            this.headers = headersPassed;
        }
        return taxiStandsService;
    }

    public TaxiAPI getTaxiService(List<NameValuePair> headers){
        this.headers.clear();
        if (headers != null) {
            this.headers = headers;
        }
        return taxiService;
    }
}
