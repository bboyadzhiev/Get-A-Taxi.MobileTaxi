package com.getataxi.mobiletaxi.comm.contracts;

import com.getataxi.mobiletaxi.comm.models.TaxiDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;


import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by bvb on 9.5.2015 г..
 */
public interface TaxiAPI {
    @GET("/api/Taxi")
    void getTaxies(Callback<List<TaxiDetailsDM>> callback);

    @GET("/api/Taxi/{id}")
    void getTaxiDetails(@Path("id") int id, Callback<TaxiDetailsDM> callback);

    @GET("/api/Taxi/{page}")
    void getTaxiesPage(@Path("page") int page, Callback<List<TaxiDetailsDM>> callback);

    @POST("/api/Taxi")
    void assignDriver(@Body TaxiDM taxiDM,  Callback<TaxiDetailsDM> callback);

    @PUT("/api/Taxi")
    void updateTaxi(@Body TaxiDM taxiDM, Callback callback);

    @DELETE("/api/Taxi/{id}")
    void unassignDriver(@Path("id") int id, Callback callback);
}
