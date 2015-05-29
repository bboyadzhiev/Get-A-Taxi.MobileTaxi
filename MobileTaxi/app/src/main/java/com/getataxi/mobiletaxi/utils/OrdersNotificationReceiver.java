package com.getataxi.mobiletaxi.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.getataxi.mobiletaxi.OrderAssignmentActivity;
import com.getataxi.mobiletaxi.R;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * Created by Bobi on 5/29/2015.
 */
public class OrdersNotificationReceiver extends BroadcastReceiver {
    private static final int NOTIFY_ME_ID=1337;

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean busy = UserPreferencesManager.hasAssignedOrder(context);

        if(!busy) {
            String orderString = intent.getStringExtra(Constants.HUB_ADDED_ORDER);
            Gson gson = new GsonBuilder()
                    .setDateFormat(Constants.GSON_DATE_FORMAT)
                    .create();
            Type type = new TypeToken<OrderDetailsDM>() {
            }.getType();
            OrderDetailsDM order = gson.fromJson(orderString, type);

            NotificationManager mgr =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification note = new Notification(R.mipmap.person,
                    "New order available!",
                    System.currentTimeMillis());
            PendingIntent i = PendingIntent.getActivity(context, 0,
                    new Intent(context, OrderAssignmentActivity.class),
                    0);

            note.setLatestEventInfo(context, order.orderAddress,
                    order.destinationAddress,
                    i);

            mgr.notify(NOTIFY_ME_ID, note);
        }
    }
}
