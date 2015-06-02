package com.getataxi.mobiletaxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.getataxi.mobiletaxi.comm.RestClientManager;
import com.getataxi.mobiletaxi.comm.SignalRTrackingService;
import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.utils.Constants;
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

import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;


public class OrderMap extends ActionBarActivity {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.


    private Context context;
    private  String phoneNumber;


    private View mProgressView;

    private Location taxiDriverLocation;
    private Location clientLocation;
    private Location lastLocation;

    // Initialized by the broadcast receiver
    //private LocationDM currentReverseGeocodedLocation = null;

    private Marker clientLocationMarker;
    private Marker destinationLocationMarker;

    private TaxiDetailsDM taxi;

    private Marker taxiLocationMarker;
    private boolean trackingEnabled;
    private Button cancelOrderButton;
    private Button pickupOrderButton;
    private Button placeDriverOrderButton;
    private Button finishOrderButton;


    // Order details
    private OrderDetailsDM clientOrderDM;
    private boolean hasAssignedOrder = false;
    private int assignedOrderId = -1;

    // TRACKING SERVICES
    protected void initiateTracking(int orderId){
        Intent trackingIntent = new Intent(OrderMap.this, SignalRTrackingService.class);
        trackingIntent.putExtra(Constants.BASE_URL_STORAGE, UserPreferencesManager.getBaseUrl(context));
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
                float threshold = data.getFloat(Constants.LOCATION_ACCURACY, Constants.LOCATION_ACCURACY_THRESHOLD);

                double clientLat = taxiDriverLocation.getLatitude();
                double clientLon = taxiDriverLocation.getLongitude();
                String markerTitle = "Taxi";
                LatLng latLng =  new LatLng(clientLat, clientLon);

                taxiLocationMarker = updateMarker(
                        taxiLocationMarker,
                        latLng,
                        markerTitle, false
                );
                // Update taxi location too sadasd
                taxi.latitude = clientLat;
                taxi.longitude = clientLon;

                if(threshold < Constants.LOCATION_ACCURACY_THRESHOLD) {
                    // Reverse geocode for an address
                    placeDriverOrderButton.setEnabled(isTaxiOnDuty());
                }
                if(lastLocation == null) {
                    lastLocation = taxiDriverLocation;
                    reportTaxiStatus(taxi);
                } else if(lastLocation.distanceTo(taxiDriverLocation) > Constants.LOCATION_REST_REPORT_THRESHOLD){
                    lastLocation = taxiDriverLocation;
                    reportTaxiStatus(taxi);
                }

                checkPickupAvailability();

            } else if(action.equals(Constants.HUB_PEER_LOCATION_CHANGED_BC)){
                // Client location change

                // Checking if we have any order data
                if(clientOrderDM == null){
                    clientOrderDM = new OrderDetailsDM();
                    clientOrderDM.orderId = -1;
                    return;
                }

                if(clientOrderDM.orderId == -1){
                    clientOrderDM.firstName = getResources().getString(R.string.getting_details_txt);
                    // Try to get the assigned order details
                    getAssignedOrder();
                }

                Bundle data = intent.getExtras();
                clientLocation = data.getParcelable(Constants.LOCATION);

                LatLng latLng =  new LatLng(clientLocation.getLatitude(), clientLocation.getLongitude());
                String title = clientOrderDM.orderAddress + "</br><small>" +clientOrderDM.firstName + " " + clientOrderDM.lastName + "</small>";
                clientLocationMarker = updateMarker(
                        clientLocationMarker,
                        latLng,
                        title,
                        true
                );

                checkPickupAvailability();

            }
        }
    };

    private void checkPickupAvailability() {
        if(hasAssignedOrder && clientOrderDM.status == Constants.OrderStatus.Waiting.getValue()){
            if(clientLocation == null && clientOrderDM != null){
                clientLocation = new Location("void");
                clientLocation.setLatitude(clientOrderDM.orderLatitude);
                clientLocation.setLongitude(clientOrderDM.orderLongitude);
            }

            if(taxiDriverLocation.distanceTo(clientLocation) < Constants.LOCATION_PICKUP_DISTANCE_THRESHOLD){
                toggleButton(ButtonType.Pickup);
            }
        }
    }

    private boolean isTaxiOnDuty() {
        if(taxi.status == Constants.TaxiStatus.Available.getValue() || taxi.status == Constants.TaxiStatus.Busy.getValue()){
            return true;
        }
        return false;
    }


    /**
     * ACTIVITY LIFECYCLE
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
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        trackingEnabled = true;
        taxi = UserPreferencesManager.getAssignedTaxi(context);

        taxiLocationMarker = updateMarker(
                taxiLocationMarker,
                new LatLng(taxi.latitude, taxi.longitude),
                taxi.plate, true);
        taxiLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi));

        hasAssignedOrder = UserPreferencesManager.hasAssignedOrder(context);
        if(hasAssignedOrder) {
            assignedOrderId = UserPreferencesManager.getLastOrderId(context);
            getAssignedOrder();
            toggleButton(ButtonType.Cancel);
        } else {
            toggleButton(ButtonType.Place);
            placeDriverOrderButton.setEnabled(isTaxiOnDuty());
        }

        IntentFilter filter = new IntentFilter();
        // Register for Location Service broadcasts
        filter.addAction(Constants.LOCATION_UPDATED);
        // And peer location change
        filter.addAction(Constants.HUB_PEER_LOCATION_CHANGED_BC);
        registerReceiver(locationReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Store taxi position
        UserPreferencesManager.setAssignedTaxi(taxi, context);
    }

    @Override
    public void onStop(){
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop tracking service
        Intent trackingService = new Intent(OrderMap.this, SignalRTrackingService.class);
        stopService(trackingService);

        unregisterReceiver(locationReceiver);
    }


    // INPUTS
    private void initInputs() {
        cancelOrderButton = (Button)findViewById(R.id.btn_cancel_order);
        cancelOrderButton.setEnabled(false);

        pickupOrderButton = (Button)findViewById(R.id.btn_pickup_order);
        pickupOrderButton.setEnabled(false);

        pickupOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelOrderButton.setEnabled(false);
                showProgress(true);

                //set order in progress
                clientOrderDM.status = Constants.OrderStatus.InProgress.getValue();
                RestClientManager.updateOrder(clientOrderDM, context, new Callback<OrderDetailsDM>() {
                    @Override
                    public void success(OrderDetailsDM orderDetailsDM, Response response) {
                        int status = response.getStatus();

                        if (status == HttpStatus.SC_OK) {
                            // Picked up successfully
                            clientOrderDM = orderDetailsDM;
                            UserPreferencesManager.storeOrderId(orderDetailsDM.orderId, context);
                            assignedOrderId = orderDetailsDM.orderId;
                            hasAssignedOrder = true;
                            toggleButton(ButtonType.Finish);
                            taxi.status = Constants.TaxiStatus.Busy.getValue();
                            reportTaxiStatus(taxi);
                        }

                        showProgress(false);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        showToastError(error);
                        if (error.getResponse() != null) {
                            int status = error.getResponse().getStatus();

                            if (status == HttpStatus.SC_NOT_FOUND) {
                                // Clear stored order id
                                clearStoredOrder();
                                toggleButton(ButtonType.Place);
                            }
                            if (status == HttpStatus.SC_BAD_REQUEST) {
                                // Cancelled or finished already
                                clearStoredOrder();
                                toggleButton(ButtonType.Place);

                            }

                        }
                        showProgress(false);
                    }
                });
            }
        });

        placeDriverOrderButton = (Button)findViewById(R.id.btn_place_order);
        placeDriverOrderButton.setEnabled(false);

        finishOrderButton = (Button)findViewById(R.id.btn_finish_order);
        finishOrderButton.setEnabled(false);
        // Cancel order if possible
        cancelOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelOrderButton.setEnabled(false);
                showProgress(true);

                // TODO: finish
                // int currentOrderId = UserPreferencesManager.getLastOrderId(context);
                // Order in progress, try to cancel it
                RestClientManager.cancelOrder(assignedOrderId, context, new Callback<Integer>() {
                    @Override
                    public void success(Integer id, Response response) {

                        int status = response.getStatus();

                        if (status == HttpStatus.SC_OK) {
                            // Cancelled successfully
                            clearStoredOrder();
                            toggleButton(ButtonType.Place);
                            taxi.status = Constants.TaxiStatus.Available.getValue();
                            reportTaxiStatus(taxi);
                            Toast.makeText(context, getResources().getString(R.string.order_cancelled_toast), Toast.LENGTH_LONG).show();
                        }

                        showProgress(false);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        showToastError(error);
                        if(error.getResponse() != null){
                            int status = error.getResponse().getStatus();

                            if(status == HttpStatus.SC_NOT_FOUND){
                                // Clear stored order id
                                clearStoredOrder();
                                toggleButton(ButtonType.Place);
                            }
                            if (status == HttpStatus.SC_BAD_REQUEST) {
                                // Cancelled or finished already
                                clearStoredOrder();
                                toggleButton(ButtonType.Place);

                            }

                        }
                        showProgress(false);
                    }
                });

            }
        });

        placeDriverOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeDriverOrderButton.setEnabled(false);
                showProgress(true);
                if (taxiDriverLocation != null) {

                    OrderDetailsDM driverOrder = new OrderDetailsDM();
                    driverOrder = prepareDriverOrderDetails(driverOrder);
                    driverOrder.orderedAt = Calendar.getInstance().getTime();
                    driverOrder.pickupTime = 0;
                    RestClientManager.addOrder(driverOrder, context, new Callback<OrderDetailsDM>() {
                        @Override
                        public void success(OrderDetailsDM orderDetailsDM, Response response) {
                            int status = response.getStatus();
                            if (status == HttpStatus.SC_OK) {
                                clientOrderDM = orderDetailsDM;
                                UserPreferencesManager.storeOrderId(orderDetailsDM.orderId, context);
                                assignedOrderId = orderDetailsDM.orderId;
                                hasAssignedOrder = true;
                                taxi.status = Constants.TaxiStatus.Busy.getValue();
                                toggleButton(ButtonType.Finish);
                            }
                            showProgress(false);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            showProgress(false);
                            showToastError(error);

                            toggleButton(ButtonType.Place);
                        }
                    });
                }
                showProgress(false);
            }
        });

        finishOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOrderButton.setEnabled(false);
                showProgress(true);

                clientOrderDM = completeOrderDetails(clientOrderDM);

                // TODO: add additional data like bill or time

                RestClientManager.updateOrder(clientOrderDM, context, new Callback<OrderDetailsDM>() {
                    @Override
                    public void success(OrderDetailsDM orderDetailsDM, Response response) {
                        int status = response.getStatus();
                        if (status == HttpStatus.SC_OK) {
                            clearStoredOrder();
                            toggleButton(ButtonType.Place);
                            taxi.status = Constants.TaxiStatus.Available.getValue();
                            reportTaxiStatus(taxi);
                        }
                        showProgress(false);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        showProgress(false);
                        showToastError(error);
                        toggleButton(ButtonType.Finish);
                    }
                });

                showProgress(false);
            }
        });
    }

    private void reportTaxiStatus(TaxiDetailsDM taxiDetailsDM){
        if(taxiDetailsDM == null){
            return;
        }

        RestClientManager.updateTaxi(taxiDetailsDM, context, new Callback<Integer>() {
            @Override
            public void success(Integer o, Response response) {

            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
            }
        });
    }

    private void toggleButton(ButtonType button){
        if(button == ButtonType.Cancel){ // Cancel Order
            cancelOrderButton.setVisibility(View.VISIBLE);
            cancelOrderButton.setEnabled(true);
            pickupOrderButton.setVisibility(View.VISIBLE);
            pickupOrderButton.setEnabled(false);
            placeDriverOrderButton.setVisibility(View.INVISIBLE);
            placeDriverOrderButton.setEnabled(false);
            finishOrderButton.setVisibility(View.INVISIBLE);
            finishOrderButton.setEnabled(false);
        } else if (button == ButtonType.Place) { // Place Driver Order
            cancelOrderButton.setVisibility(View.INVISIBLE);
            cancelOrderButton.setEnabled(false);
            pickupOrderButton.setVisibility(View.INVISIBLE);
            pickupOrderButton.setEnabled(false);
            placeDriverOrderButton.setVisibility(View.VISIBLE);
            if(taxiDriverLocation != null) {
                placeDriverOrderButton.setEnabled(true);
            }
            finishOrderButton.setVisibility(View.INVISIBLE);
            finishOrderButton.setEnabled(false);
        } else if (button == ButtonType.Pickup) { // Pickup client
            cancelOrderButton.setVisibility(View.VISIBLE);
            cancelOrderButton.setEnabled(true);
            pickupOrderButton.setVisibility(View.VISIBLE);
            pickupOrderButton.setEnabled(true);
            placeDriverOrderButton.setVisibility(View.INVISIBLE);
            if(taxiDriverLocation != null) {
                placeDriverOrderButton.setEnabled(true);
            }
            finishOrderButton.setVisibility(View.INVISIBLE);
            finishOrderButton.setEnabled(false);
        } else { // Finish Order
            cancelOrderButton.setVisibility(View.INVISIBLE);
            cancelOrderButton.setEnabled(false);
            pickupOrderButton.setVisibility(View.INVISIBLE);
            pickupOrderButton.setEnabled(false);
            placeDriverOrderButton.setVisibility(View.INVISIBLE);
            placeDriverOrderButton.setEnabled(false);
            finishOrderButton.setVisibility(View.VISIBLE);
            finishOrderButton.setEnabled(true);
        }
    }

    public enum ButtonType {
        Place, Pickup, Cancel, Finish
    }

    // LOGIC
    private void getAssignedOrder(){

        if(!hasAssignedOrder){
            toggleButton(ButtonType.Place);
            return;
        }

        showProgress(true);
        // Driver has taken an order in the district
        RestClientManager.getOrder(assignedOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM assignedOrderDM, Response response) {

                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    showProgress(false);
                    try {
                        if (orderNotActive(assignedOrderDM)) return;

                        clientOrderDM = assignedOrderDM;
                        String title = assignedOrderDM.orderAddress + "</br><small>" + assignedOrderDM.firstName + " " + assignedOrderDM.lastName + "</small>";
                        clientLocationMarker = updateMarker(
                                clientLocationMarker,
                                new LatLng(assignedOrderDM.orderLatitude, assignedOrderDM.orderLongitude),
                                title, true);

                        clientLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.person));
                        if (assignedOrderDM.destinationAddress != null && !assignedOrderDM.destinationAddress.isEmpty()) {
                            destinationLocationMarker = updateMarker(destinationLocationMarker,
                                    new LatLng(assignedOrderDM.destinationLatitude, assignedOrderDM.destinationLongitude),
                                    assignedOrderDM.destinationAddress,
                                    false
                            );

                            destinationLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.destination));
                        }

                        initiateTracking(assignedOrderDM.orderId);

                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    showProgress(false);
                }

            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                showProgress(false);
            }
        });
    }



    private boolean orderNotActive(OrderDetailsDM orderDM) {
        if(orderDM.status == Constants.OrderStatus.Cancelled.getValue() || orderDM.status == Constants.OrderStatus.Finished.getValue()){
            clearStoredOrder();
            toggleButton(ButtonType.Place); // "Place custom order"
            return true;
        }
        if(orderDM.status == Constants.OrderStatus.Waiting.getValue()){
            toggleButton(ButtonType.Cancel); // "Cancel order"
        }
        if(orderDM.status == Constants.OrderStatus.InProgress.getValue()){
            toggleButton(ButtonType.Finish); // "Finish order"
        }
        return false;
    }

    private void clearStoredOrder() {
        UserPreferencesManager.clearOrderAssignment(context);
        hasAssignedOrder = false;
        assignedOrderId = -1;
        if(clientLocationMarker != null){
            clientLocationMarker.remove();
        }

        if(destinationLocationMarker !=null){
            destinationLocationMarker.remove();
        }

        clientOrderDM = null;
    }

    private OrderDetailsDM prepareDriverOrderDetails(OrderDetailsDM sourceOrderDM) {
        sourceOrderDM.orderId = -1; // Will be updated later by the server
        if(taxiDriverLocation != null) {
            sourceOrderDM.orderLatitude = taxiDriverLocation.getLatitude();
            sourceOrderDM.orderLongitude = taxiDriverLocation.getLongitude();
        }
        // In progress
        sourceOrderDM.status = Constants.OrderStatus.InProgress.getValue();

        sourceOrderDM.taxiId = taxi.taxiId;
        sourceOrderDM.taxiPlate = taxi.plate;
        sourceOrderDM.driverPhone = phoneNumber;
        return sourceOrderDM;
    }

    private OrderDetailsDM completeOrderDetails(OrderDetailsDM sourceOrderDM) {
        sourceOrderDM.orderId = assignedOrderId; //UserPreferencesManager.getLastOrderId(context);
       // if(sourceOrderDM.destinationAddress != null && sourceOrderDM.destinationAddress.isEmpty()) {
            sourceOrderDM.destinationLatitude = taxiDriverLocation.getLatitude();
            sourceOrderDM.destinationLongitude = taxiDriverLocation.getLongitude();
      //  }
        // Set to Finished
        sourceOrderDM.status = Constants.OrderStatus.Finished.getValue();
        TaxiDetailsDM taxi = UserPreferencesManager.getAssignedTaxi(context);
        sourceOrderDM.taxiId = taxi.taxiId;
        sourceOrderDM.taxiPlate = taxi.plate;
        sourceOrderDM.driverPhone = phoneNumber;
        return sourceOrderDM;
    }



    // OPTIONS MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order_map, menu);
        menu.findItem(R.id.action_order_assignment).setEnabled(!hasAssignedOrder);
        menu.findItem(R.id.action_release_taxi).setEnabled(!hasAssignedOrder);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        if(id == R.id.action_order_assignment && !hasAssignedOrder){
            Intent intent = new Intent(context, OrderAssignmentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }

        if(id == R.id.action_release_taxi && !hasAssignedOrder){
            showProgress(true);
            int assignedTaxiId = UserPreferencesManager.getAssignedTaxi(context).taxiId;
            RestClientManager.unassignTaxi(assignedTaxiId, context, new Callback<Object>() {

                @Override
                public void success(Object o, Response response) {
                    UserPreferencesManager.clearAssignedTaxi(context);
                    Intent taxiAssignmentActivity = new Intent(context, TaxiAssignmentActivity.class);
                    taxiAssignmentActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    showProgress(false);
                    startActivity(taxiAssignmentActivity);
                }

                @Override
                public void failure(RetrofitError error) {
                    showToastError(error);
                    showProgress(false);
                }
            });

            return true;
        }

        if (id == R.id.action_order_map_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // MAP ROUTINES
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

    private Marker updateMarker(Marker marker, LatLng location, String title, boolean animate ){
        if (marker == null){
            MarkerOptions markerOpts = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi))
                    .title(title);

            marker = mMap.addMarker(markerOpts);
            animateMarker(marker, location, false);
        } else {
            marker.setTitle(title);
            animateMarker(marker, location, false);
        }
        if(animate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(location)      // Sets the center of the map to location user
                    .zoom(Constants.MAP_ANIMATION_ZOOM)                   // Sets the zoom
                            //   .bearing(90)   // Sets the orientation of the camera to east
                            //   .tilt(40)       // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        marker.showInfoWindow();
        return marker;
    }


    private void showToastError(RetrofitError error) {
        if (error.getResponse().getBody() != null) {
            String json =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
            if(!json.isEmpty()){
                Toast.makeText(context, json, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
        }
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
