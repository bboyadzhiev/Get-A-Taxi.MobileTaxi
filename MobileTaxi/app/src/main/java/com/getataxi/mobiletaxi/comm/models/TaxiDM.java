package com.getataxi.mobiletaxi.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by bvb on 10.5.2015 г..
 */
@Parcel
public class TaxiDM {
    @SerializedName("taxiId")
    public int taxiId;

    @SerializedName("lat")
    public double latitude;

    @SerializedName("lon")
    public double longitude;

    @SerializedName("onDuty")
    public boolean onDuty;

    @SerializedName("isAvailable")
    public boolean isAvailable;
}
