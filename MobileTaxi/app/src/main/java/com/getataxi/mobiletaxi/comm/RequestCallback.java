package com.getataxi.mobiletaxi.comm;

import com.getataxi.mobiletaxi.comm.contracts.RequestListener;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by bvb on 11.4.2015 г..
 */
public class RequestCallback<T> implements Callback<T> {

    protected RequestListener<T> listener;

    public RequestCallback(RequestListener<T> listener){
        this.listener = listener;
    }

    @Override
    public void failure(RetrofitError arg0){
        this.listener.onFailure(arg0);
    }

    @Override
    public void success(T arg0, Response arg1){
        this.listener.onSuccess(arg0);
    }

}
