package com.getataxi.mobiletaxi.comm.models;

import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;
/**
 * Created by bvb on 30.3.2015 г..
 */
@Parcel
public class TaxiStandDM {

    @SerializedName("taxiStandId")
    public int taxiStandId;

    @SerializedName("lat")
    public double latitude;

    @SerializedName("lng")
    public double longitude;

    @SerializedName("address")
    public String address;

    @SerializedName("alias")
    public String alias;
}
