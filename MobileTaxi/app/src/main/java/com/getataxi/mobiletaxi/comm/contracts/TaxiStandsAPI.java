package com.getataxi.mobiletaxi.comm.contracts;

import com.getataxi.mobiletaxi.comm.models.TaxiStandDM;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by bvb on 29.3.2015 г..
 */
public interface TaxiStandsAPI {
    @GET("/api/TaxiStands")
    void  getTaxiStands(Callback<List<TaxiStandDM>> callback);

    @GET("/api/TaxiStands/{id}")
    void getTaxiStand(@Path("id") int id, Callback<TaxiStandDM> callback);
}
