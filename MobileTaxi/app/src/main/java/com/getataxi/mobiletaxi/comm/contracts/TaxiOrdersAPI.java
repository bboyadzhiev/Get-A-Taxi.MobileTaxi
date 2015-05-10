package com.getataxi.mobiletaxi.comm.contracts;

import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.comm.models.OrderDM;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by bvb on 31.3.2015 г..
 */
public interface TaxiOrdersAPI {
    @GET("/api/TaxiOrders")
    void getOrders(Callback<List<OrderDM>> callback);

    @GET("/api/TaxiOrders/{page}")
    void getOrdersPage(@Path("page") int page, Callback<List<OrderDM>> callback);

    @GET("/api/TaxiOrders/{id}")
    void getOrder(@Path("id") int id, Callback<OrderDetailsDM> callback);

    @POST("/api/TaxiOrders")
    void addOrder(@Body OrderDetailsDM orderDM,  Callback<OrderDetailsDM> callback);

    @PUT("/api/TaxiOrders")
    void assignOrder(@Path("id") int orderId, Callback<OrderDetailsDM> callback);

    @PUT("/api/TaxiOrders")
    void updateOrder(@Body OrderDetailsDM orderDM, Callback<OrderDetailsDM> callback);

    @DELETE("/api/TaxiOrders/{id}")
    void cancelOrder(@Path("id") int id, Callback<OrderDM> callback);
}
