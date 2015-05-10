package com.getataxi.mobiletaxi.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by bvb on 9.5.2015 г..
 */
@Parcel
public class TaxiDetailsDM extends TaxiDM {

    @SerializedName("plate")
    public String plate;

    @SerializedName("driverName")
    public String driverName;

    @SerializedName("phone")
    public String phone;

    @SerializedName("districtId")
    public int districtId;

    @SerializedName("taxiStandId")
    public int taxiStandId;

    @SerializedName("taxiStandAlias")
    public String taxiStandAlias;

    @SerializedName("address")
    public String address;
}
