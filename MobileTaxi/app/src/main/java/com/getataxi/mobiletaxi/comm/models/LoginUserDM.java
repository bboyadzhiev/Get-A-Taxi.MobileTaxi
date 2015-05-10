package com.getataxi.mobiletaxi.comm.models;

import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;
/**
 * Created by bvb on 29.3.2015 г..
 */
@Parcel
public class LoginUserDM {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("token_type")
    public String password;

    @SerializedName("expires_in")
    public String expires_in;

    @SerializedName("username")
    public String userName;

    @SerializedName("email")
    public String email;

    @SerializedName(".issued")
    public String issued;

    @SerializedName(".expires")
    public String expires;

    @SerializedName("token")
    public String token;
}
