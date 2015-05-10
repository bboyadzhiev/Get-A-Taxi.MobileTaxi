package com.getataxi.mobiletaxi.comm.contracts;

import retrofit.RetrofitError;

/**
 * Created by bvb on 11.4.2015 г..
 */
public abstract interface RequestListener<T> {

    void onSuccess(T response);

    void onFailure(RetrofitError error);
}