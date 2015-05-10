package com.getataxi.mobiletaxi.utils;

/**
 * Created by bvb on 28.3.2015 г..
 */
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

public class DeviceState {

//    private Context _context;
//
//    public DeviceState(Context context) {
//        this._context = context;
//    }

    /**
     * Checking for all possible internet providers
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED ||
                            info[i].getState() == NetworkInfo.State.CONNECTING) {
                        return true;
                    }
        }
        return false;
    }

    public static boolean isPositioningAvailable(Context context){
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isGPSEnabled || isNetworkProviderEnabled) {
            return true;
        }

        return false;
    }

    public static void showSettingsAlert(int title, int message, final String intentString, final Context context) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        alertDialog.setTitle(title);

        alertDialog.setMessage(message);

        alertDialog.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(intentString.compareTo(Settings.ACTION_WIRELESS_SETTINGS) == 0){
                            //
                            //ComponentName cName = new ComponentName("com.android.phone","com.android.phone.Settings");
                            //intent.setComponent(cName);
                            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                            context.startActivity(intent);
                        } else {
                            Intent intent = new Intent(intentString);
                            context.startActivity(intent);
                        }
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

}