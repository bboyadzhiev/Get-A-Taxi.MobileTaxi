package com.getataxi.mobiletaxi.comm;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.mobiletaxi.R;
import com.getataxi.mobiletaxi.comm.models.LoginUserDM;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.utils.Constants;
import com.getataxi.mobiletaxi.utils.UserPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;

/**
 * Created by Bobi on 5/26/2015.
 */
public class SignalROrdersService extends Service {
    private HubConnection connection;
    private Intent broadcastIntent;
    private HubProxy proxy;
    private int districtId;

    @Override
    public void onCreate() {
        super.onCreate();
        districtId = -1;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Toast.makeText(this, getString(R.string.district_orders_monitoring_service), Toast.LENGTH_LONG).show();
        Logger l  = new Logger() {
            @Override
            public void log(String s, LogLevel logLevel) {

            }
        };
        Log.d("SignalROrdersService", "onStartCommand");
        l.log("onStartCommand", LogLevel.Verbose);

        districtId = intent.getIntExtra(Constants.DISTRICT_ID, -1);

        String baseUsrl = intent.getStringExtra(Constants.BASE_URL_STORAGE);

        if(districtId == -1){
            return -1;
        }

        String server =  baseUsrl + Constants.HUB_ENDPOINT;

        //String accessToken = UserPreferencesManager.getToken(getApplicationContext());
        // Getting user details
        LoginUserDM loginData = UserPreferencesManager.getLoginData(getApplicationContext());

        connection = new HubConnection(server);
        proxy = connection.createHubProxy(Constants.ORDERS_HUB_PROXY);
        connection.setCredentials(new TokenAuthenticationCredentials(loginData.accessToken));

        Log.d("SignalROrdersService", "Awaiting connection");
        l.log("Awaiting connection", LogLevel.Verbose);
        SignalRFuture<Void> awaitConnection = connection.start();
        try {
            awaitConnection.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Log.d("SignalROrdersService", "Invoking orders hub");
        l.log("Invoking orders hub", LogLevel.Verbose);
        proxy.invoke(Constants.HUB_CONNECT, districtId);

        Log.d("SignalROrdersService", "Registering callback");
        l.log("Registering callback", LogLevel.Verbose);

//        proxy.subscribe(Constants.HUB_ORDERS_UPDATED).addReceivedHandler(new Action<JSONArray>() {
//            @Override
//            public void run(JSONArray jsonElements) throws Exception {
//
//                Log.i("SignalR", "Message From Server: " + jsonElements.toString());
//            }
//        });



        proxy.on(Constants.HUB_ORDERS_UPDATED, new SubscriptionHandler1<JSONArray>() {
            @Override
            public void run(JSONArray ordersJASONArray) {
                Log.d("SignalROrdersService", ordersJASONArray.toString());
                broadcastIntent = new Intent(Constants.HUB_ORDERS_UPDATED_BC);
                broadcastIntent.putExtra(Constants.HUB_UPDATED_ORDERS_LIST, ordersJASONArray.toString());;
                sendBroadcast(broadcastIntent);
            }
        }, JSONArray.class);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch(Exception e) {
                    Log.d("Error", e.toString());
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
}
