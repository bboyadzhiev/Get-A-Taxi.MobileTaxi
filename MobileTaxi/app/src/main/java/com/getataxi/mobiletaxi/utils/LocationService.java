package com.getataxi.mobiletaxi.utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.mobiletaxi.comm.RestClientManager;
import com.getataxi.mobiletaxi.comm.models.TaxiDM;
import com.getataxi.mobiletaxi.comm.models.TaxiStandDM;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by bvb on 5.4.2015 г..
 */
public class LocationService extends Service
{
    public  final String TAG = "utils.LocationService";
    public LocationManager locationManager;
    public ClientLocationListener listener;
    public Location previousBestLocation = null;

    private String reportLocationTitle;

    Intent broadcastIntent;

    @Override
    public void onCreate()
    {
        super.onCreate();
        broadcastIntent = new Intent(Constants.LOCATION_UPDATED);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new ClientLocationListener();
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                Constants.LOCATION_UPDATE_INTERVAL,
                Constants.LOCATION_UPDATE_DISTANCE,
                listener);
    }

    @Override
    public void onStart(Intent intent, int startId)
    {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Bundle b = intent.getExtras();
        reportLocationTitle = b.getString(Constants.LOCATION_REPORT_TITLE, "Unknown");

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > Constants.LOCATION_TIMEOUT;
        boolean isSignificantlyOlder = timeDelta < -Constants.LOCATION_TIMEOUT;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        locationManager.removeUpdates(listener);
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

    public class ClientLocationListener implements LocationListener
    {
        public void onLocationChanged(final Location loc)
        {
            if(isBetterLocation(loc, previousBestLocation)) {

                broadcastIntent.putExtra(Constants.LOCATION, loc);
                broadcastIntent.putExtra(Constants.LOCATION_ACCURACY, loc.getAccuracy());
                // Notify all interested parties
                sendBroadcast(broadcastIntent);
            }
        }


        public void onProviderDisabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "GPS disabled!", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "GPS enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }
}
