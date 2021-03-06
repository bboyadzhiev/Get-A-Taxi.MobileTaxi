package com.getataxi.mobiletaxi.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.Date;

/**
 * Created by bvb on 9.5.2015 г..
 */
@Parcel
public class OrderDetailsDM extends OrderDM {

    @SerializedName("customerID")
    public String customerID;
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("lastName")
    public String lastName;
    @SerializedName("custPhone")
    public String customerPhoneNumber;
    @SerializedName("orderedAt")
    public Date orderedAt;

    // Properties, updated by taxi assignment
    @SerializedName("taxiId")
    public int taxiId;
    @SerializedName("taxiPlate")
    public String taxiPlate;
    @SerializedName("driverPhone")
    public String driverPhone;
    @SerializedName("driverName")
    public String driverName;

    // Properties, updated in progress
    @SerializedName("arrivalTime")
    public int arrivalTime;
    @SerializedName("bill")
    public double bill;
}
