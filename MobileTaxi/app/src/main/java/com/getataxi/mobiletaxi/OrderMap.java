package com.getataxi.mobiletaxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.getataxi.mobiletaxi.comm.RestClientManager;
import com.getataxi.mobiletaxi.comm.SignalRTrackingService;
import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.utils.Constants;
import com.getataxi.mobiletaxi.utils.LocationService;
import com.getataxi.mobiletaxi.utils.UserPreferencesManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpStatus;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class OrderMap extends FragmentActivity {
    public static final String TAG = "ORDER_MAP";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private String destinationDialogTag;
    private String startDialogTag;

    private Context context;
    private  String phoneNumber;

    // Addresses inputs
    private AddressesInputsFragment locationsInputs;

    private View mProgressView;

    private Location taxiDriverLocation;
    private Location clientLocation;

    // Initialized by the broadcast receiver
    //private LocationDM currentReverseGeocodedLocation = null;

    private Marker currentLocationMarker;
    private Marker destinationLocationMarker;
    private Marker taxiLocationMarker;
    private boolean trackingEnabled;
    private Button orderButton;


    // Order details
    private OrderDetailsDM orderDM;


    // TRACKING SERVICES
    protected void initiateTracking(int orderId){
        Intent trackingIntent = new Intent(OrderMap.this, SignalRTrackingService.class);
        trackingIntent.putExtra(Constants.LOCATION_REPORT_ENABLED, trackingEnabled);
        trackingIntent.putExtra(Constants.ORDER_ID, orderId);
        startService(trackingIntent);
    }

    /**
     * The receiver for the Location Service - location update broadcasts
     * and the SignalR Notification Service - peer location change broadcasts
     */
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.LOCATION_UPDATED)) {
                // Client location change
                Bundle data = intent.getExtras();

                taxiDriverLocation = data.getParcelable(Constants.LOCATION);


                double clientLat = taxiDriverLocation.getLatitude();
                double clientLon = taxiDriverLocation.getLongitude();
                String markerTitle = "Taxi";
                LatLng latLng =  new LatLng(clientLat, clientLon);



                currentLocationMarker = updateMarker(
                        currentLocationMarker,
                        latLng,
                        markerTitle
                );
                currentLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi));


            } else if(action.equals(Constants.HUB_PEER_LOCATION_CHANGED)){
                // Client location change

                // Checking if we have any order data
                if(orderDM == null){
                    orderDM = new OrderDetailsDM();
                    return;
                }

                if(orderDM.orderId == -1){
                    orderDM.firstName = getResources().getString(R.string.getting_details_txt);
                    // Try to get the assigned order details
                    getAssignedOrder();
                }

                Bundle data = intent.getExtras();
                clientLocation = data.getParcelable(Constants.LOCATION);

                String markerTitle = orderDM.taxiPlate;
                LatLng latLng =  new LatLng(clientLocation.getLatitude(), clientLocation.getLongitude());
                taxiLocationMarker = updateMarker(
                        taxiLocationMarker,
                        latLng,
                        markerTitle
                );
                taxiLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.person));
            }
        }
    };


    /**
     * onCreate
     * @param savedInstanceState - the bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_map);
        context = this;
        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tMgr.getLine1Number();

        mProgressView = findViewById(R.id.order_map_progress);

        initInputs();

        setUpMapIfNeeded();

        getAssignedOrder();

    }


    private void getAssignedOrder(){
        int lastOrderId = UserPreferencesManager.getLastOrderId(context);
        if(lastOrderId == -1){
            // No stored order id!
            return;
        }
        // Driver has taken an order in the district
        RestClientManager.getOrder(lastOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM assignedOrderDM, Response response) {
                orderButton.setText(getResources().getString(R.string.cancel_order_txt));
                orderButton.setEnabled(true);
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    try {
                        orderDM = assignedOrderDM;
                        currentLocationMarker = updateMarker(
                                currentLocationMarker,
                                new LatLng(assignedOrderDM.orderLatitude, assignedOrderDM.orderLongitude),
                                assignedOrderDM.orderAddress);
                        if (!assignedOrderDM.destinationAddress.isEmpty()) {
                            destinationLocationMarker = updateMarker(destinationLocationMarker,
                                    new LatLng(assignedOrderDM.destinationLatitude, assignedOrderDM.destinationLongitude),
                                    assignedOrderDM.destinationAddress);
                        }

                        if (orderDM.taxiId == -1) {
                            // No taxi assigned yet

                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                if (status == HttpStatus.SC_NOT_FOUND) {
                    // Clear stored order id
                    clearStoredOrder();
                }

            }

            @Override
            public void failure(RetrofitError error) {
                // Clear stored order id
                clearStoredOrder();
            }
        });
    }

    private void clearStoredOrder() {
        UserPreferencesManager.storeOrderId(-1, context);
        orderDM = null;
        orderButton.setText(getResources().getString(R.string.confirm_order_txt));
        orderButton.setEnabled(true);
    }


    @Override
    protected void onStart(){
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        //trackingEnabled = UserPreferencesManager.getTrackingState(context);

        // Start location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);

        locationService.putExtra(Constants.LOCATION_REPORT_TITLE, phoneNumber);
        context.startService(locationService);

        IntentFilter filter = new IntentFilter();
        // Register for Location Service broadcasts
        filter.addAction(Constants.LOCATION_UPDATED);
        // And peer location change
        filter.addAction(Constants.HUB_PEER_LOCATION_CHANGED);
        registerReceiver(locationReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Stop location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);
        stopService(locationService);

        // Stop tracking service
        Intent trackingService = new Intent(OrderMap.this, SignalRTrackingService.class);
        stopService(trackingService);

        unregisterReceiver(locationReceiver);

    }


    @Override
    public void onStop(){
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(locationReceiver);
    }

    private void initInputs() {


        orderButton = (Button)findViewById(R.id.btn_cancel_order);
        orderButton.setEnabled(false);

        // Place Order and get it with the order's id
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderButton.setEnabled(false);
                if (!trackingEnabled) {
                    // Stop location service
                    Intent stopLocationServiceIntent = new Intent(OrderMap.this, LocationService.class);
                    context.stopService(stopLocationServiceIntent);
                }

                showProgress(true);

                int currentOrderId = UserPreferencesManager.getLastOrderId(context);
                if (currentOrderId != -1) {
                    // Order in progress, try to cancel it
                    RestClientManager.cancelOrder(currentOrderId, context, new Callback<OrderDM>() {
                        @Override
                        public void success(OrderDM clientOrderDM, Response response) {
                            showProgress(false);
                            int status = response.getStatus();
                            clearStoredOrder();
                            if (status == HttpStatus.SC_OK) {
                                // Cancelled successfully
                                Toast.makeText(context, getResources().getString(R.string.order_cancelled_toast), Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (status == HttpStatus.SC_BAD_REQUEST) {
                                // Cancelled or finished already
                                Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                                return;
                            }

                            Toast.makeText(context, getResources().getString(R.string.last_order_not_found_toast), Toast.LENGTH_LONG).show();

                        }

                        @Override
                        public void failure(RetrofitError error) {
                            showProgress(false);
                            orderButton.setText(getResources().getString(R.string.cancel_order_txt));
                            orderButton.setEnabled(true);
                        }
                    });
                }

            }
        });
    }

    private OrderDetailsDM prepareDriverOrderDM() {
        OrderDetailsDM driverOrder = new OrderDetailsDM();
        driverOrder.orderLatitude = taxiDriverLocation.getLatitude();
        driverOrder.orderLongitude = taxiDriverLocation.getLongitude();
        driverOrder.isWaiting = false;
        driverOrder.isFinished = false;
        TaxiDetailsDM taxi = UserPreferencesManager.getAssignedTaxi(context);
        driverOrder.taxiId = taxi.taxiId;
        driverOrder.taxiPlate = taxi.plate;
        driverOrder.driverPhone = phoneNumber;
        return driverOrder;
    }




//    private AssignedOrderDM fromClientOrderDM(OrderDetailsDM clientOrder) {
//        AssignedOrderDM order = new AssignedOrderDM();
//        order.orderId = clientOrder.orderId;
//        order.orderAddress = clientOrder.orderAddress;
//        order.orderLatitude = clientOrder.orderLatitude;
//        order.orderLongitude = clientOrder.orderLongitude;
//        order.destinationAddress = clientOrder.destinationAddress;
//        order.destinationLatitude = clientOrder.destinationLatitude;
//        order.destinationLongitude = clientOrder.destinationLongitude;
//        order.userComment = clientOrder.userComment;
//        order.taxiId = -1;
//        return  order;
//    }



    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private Marker updateMarker(Marker marker, LatLng location, String title ){
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                        //   .bearing(90)   // Sets the orientation of the camera to east
                        //   .tilt(40)       // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (marker == null){

            MarkerOptions markerOpts = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi))
                    .title(title);

            marker = mMap.addMarker(markerOpts);
        } else {
            marker.setTitle(title);
            animateMarker(marker, location, false);
        }
        marker.showInfoWindow();
        return marker;
    }



    /**
     * Shows the ordering progress UI
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

}
