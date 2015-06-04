package com.getataxi.mobiletaxi.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.Date;

/**
 * Created by bvb on 31.3.2015 г..
 */

@Parcel
public class OrderDM {
    @SerializedName("orderId")
    public int orderId;

    @SerializedName("startLat")
    public double orderLatitude;

    @SerializedName("startLng")
    public double orderLongitude;

    @SerializedName("start")
    public String orderAddress;

    @SerializedName("endLat")
    public double destinationLatitude;

    @SerializedName("endLng")
    public double destinationLongitude;

    @SerializedName("end")
    public String destinationAddress;

    @SerializedName("custComment")
    public String userComment;

    @SerializedName("status")
    public int status;

    @SerializedName("pickupTime")
    public int pickupTime;
}
