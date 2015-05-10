package com.getataxi.mobiletaxi.comm.models;

/**
 * Created by bvb on 29.3.2015 г..
 */
import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;

@Parcel
public class RegisterUserDM {

    @SerializedName("email")
    public String email;

    @SerializedName("username")
    public String userName;

    @SerializedName("firstname")
    public String firstName;

    @SerializedName("middlename")
    public String middleName;

    @SerializedName("lastname")
    public String lastName;

    @SerializedName("phonenumber")
    public String phoneNumber;

    @SerializedName("password")
    public String password;

    @SerializedName("confirmpassword")
    public String confirmPassword;
}
