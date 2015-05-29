package com.getataxi.mobiletaxi.comm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
        }  catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();

        }

        Log.d("SignalROrdersService", "Invoking orders hub");
        proxy.invoke(Constants.HUB_CONNECT, districtId);

        Log.d("SignalROrdersService", "Registering callbacks");

        // TODO: IGNORE, USE REST CLIENT FOR THIS
        proxy.on(Constants.HUB_UPDATE_ORDERS_LIST, new SubscriptionHandler1<JsonElement[]>() {
            @Override
            @Deprecated
            public void run(JsonElement[] ordersList) {
                Log.d("SignalROrdersService", Constants.HUB_UPDATE_ORDERS_LIST);
                List<String> asd = new ArrayList<String>();
                for(JsonElement e : ordersList){
                    Log.d("ORDER", e.toString());
                    asd.add(e.toString());
                }

//                String ordersString = asd.toString();
//                broadcastIntent = new Intent(Constants.HUB_ORDERS_UPDATED_BC);
//                broadcastIntent.putExtra(Constants.HUB_UPDATE_ORDERS_LIST, ordersString);
//                sendBroadcast(broadcastIntent);
            }
        }, JsonElement[].class);

        proxy.on(Constants.HUB_ADDED_ORDER, new SubscriptionHandler1<JsonElement>() {
            @Override
            public void run(JsonElement order) {
                Log.d("SignalROrdersService", Constants.HUB_ADDED_ORDER);
                broadcastIntent = new Intent(Constants.HUB_ADDED_ORDER_BC);
                broadcastIntent.putExtra(Constants.HUB_ADDED_ORDER, order.toString());
                //sendBroadcast(broadcastIntent);
                sendOrderedBroadcast(broadcastIntent, null);

            }
        }, JsonElement.class);

        proxy.on(Constants.HUB_CANCELLED_ORDER, new SubscriptionHandler1<Integer>() {
            @Override
            public void run(Integer orderId) {
                Log.d("SignalROrdersService", Constants.HUB_CANCELLED_ORDER);
                broadcastIntent = new Intent(Constants.HUB_CANCELLED_ORDER_BC);
                broadcastIntent.putExtra(Constants.HUB_CANCELLED_ORDER, orderId);
                sendBroadcast(broadcastIntent);
            }
        }, Integer.class);

        proxy.on(Constants.HUB_UPDATED_ORDER, new SubscriptionHandler1<JsonElement>() {
            @Override
            public void run(JsonElement order) {
                Log.d("SignalROrdersService", Constants.HUB_UPDATED_ORDER);
                broadcastIntent = new Intent(Constants.HUB_UPDATED_ORDER_BC);
                broadcastIntent.putExtra(Constants.HUB_UPDATED_ORDER, order.toString());
                sendBroadcast(broadcastIntent);
            }
        }, JsonElement.class);

        proxy.on(Constants.HUB_ASSIGNED_ORDER, new SubscriptionHandler2<Integer, Integer>() {
            @Override
            public void run(Integer orderId, Integer taxiId) {
                Log.d("SignalROrdersService", Constants.HUB_ASSIGNED_ORDER);
                broadcastIntent = new Intent(Constants.HUB_ASSIGNED_ORDER_BC);
                broadcastIntent.putExtra(Constants.ORDER_ID, orderId);
                broadcastIntent.putExtra(Constants.ASSIGNED_TAXI_ID, taxiId);
                sendBroadcast(broadcastIntent);
            }
        }, Integer.class, Integer.class);

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
