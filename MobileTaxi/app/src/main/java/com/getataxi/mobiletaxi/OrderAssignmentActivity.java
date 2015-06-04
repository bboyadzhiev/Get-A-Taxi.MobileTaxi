package com.getataxi.mobiletaxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getataxi.mobiletaxi.comm.RestClientManager;
import com.getataxi.mobiletaxi.comm.SignalROrdersService;
import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.fragments.OrderDetailsFragment;
import com.getataxi.mobiletaxi.utils.ClientOrdersListAdapter;
import com.getataxi.mobiletaxi.utils.Constants;
import com.getataxi.mobiletaxi.utils.LocationService;
import com.getataxi.mobiletaxi.utils.OrdersNotificationReceiver;
import com.getataxi.mobiletaxi.utils.UserPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpStatus;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class OrderAssignmentActivity extends ActionBarActivity implements
        AdapterView.OnItemClickListener {

    private ArrayList<OrderDM> orders;
    private OrderDetailsFragment orderDetailsFragment;
    private ListView ordersListView;
    private Button assignButton;
    private Button skipAssignmentButton;

    private TaxiDetailsDM assignedTaxi;
    private Location taxiDriverLocation;
    private Location lastLocation;

    private TextView orderAddressTxt;
    private TextView orderDestinationTxt;
    private TextView clientCommentTxt;
    private int selectedOrderId;

    private View mProgressView;
    private TextView mNoOrdersTxt;

    ClientOrdersListAdapter ordersListAdapter;
    Context context = this;
    Gson gson;
    DistanceComparator distanceComparator;
    private AlarmManager mgr=null;
    private PendingIntent pi=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_assignment);

        orderDetailsFragment = (OrderDetailsFragment)getFragmentManager()
                .findFragmentById(R.id.orderDetailsFragment);

        getFragmentManager().beginTransaction().hide(orderDetailsFragment).commit();



        ordersListView = (ListView) this.findViewById(R.id.orders_list_view);

        assignedTaxi = UserPreferencesManager.getAssignedTaxi(context);

        orders = new ArrayList<>();

        mProgressView = findViewById(R.id.get_orders_progress);
        mNoOrdersTxt = (TextView)findViewById(R.id.noOrdersLabel);


        this.assignButton = (Button)this.findViewById(R.id.assignOrderButton);
        assignButton.setEnabled(false);
        assignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assignSelectedOrder();
            }
        });

        skipAssignmentButton = (Button)this.findViewById(R.id.skipOrderAssignmentButton);
        skipAssignmentButton.setEnabled(true);
        skipAssignmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent stopOrdersService = new Intent(Constants.STOP_ORDERS_HUB_BC);
                sendBroadcast(stopOrdersService);

                Intent orderMap = new Intent(context, OrderMap.class);
                //orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(orderMap);
            }
        });

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void orderNotificationReceiver() {
//        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
//                .cancelAll();
//
//        mgr=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
//
//        Intent i=new Intent(this, SignalROrdersService.class);
//
//        pi=PendingIntent.getService(this, 0, i, 0);
//
//        cancelAlarm(null);
//
//        mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime()+1000,
//                5000,
//                pi);
    }

    public void cancelAlarm(View v) {
        mgr.cancel(pi);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        gson = new GsonBuilder()
                .setDateFormat(Constants.GSON_DATE_FORMAT)
                .create();

        // Cancel all notifications
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                .cancelAll();

        distanceComparator = new DistanceComparator();

        // Start location service
        Intent locationService = new Intent(OrderAssignmentActivity.this, LocationService.class);
        context.startService(locationService);

        IntentFilter filter = new IntentFilter();
        // Register for Orders Service Hub broadcasts
        filter.addAction(Constants.HUB_ORDERS_UPDATED_BC);
        filter.addAction(Constants.HUB_ADDED_ORDER_BC);
        filter.addAction(Constants.HUB_ASSIGNED_ORDER_BC);
        filter.addAction(Constants.HUB_CANCELLED_ORDER_BC);
        filter.addAction(Constants.HUB_UPDATED_ORDER_BC);
        filter.addAction(Constants.LOCATION_UPDATED);

        // And broadcasts receiver
        registerReceiver(broadcastsReceiver, filter);

        if(UserPreferencesManager.hasAssignedOrder(context)){
            // If still active order, goes directly to order map
            checkForActiveOrder();
        } else {
            getDistrictOrders();
        }

        initiateOrdersTracking();

    }

    @Override
    protected void onPause(){
        super.onPause();


        ///UserPreferencesManager.setAssignedTaxi(assignedTaxi, context);
        unregisterReceiver(broadcastsReceiver);
    }

    @Override
    protected void onStop(){
        super.onStop();



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Stop location service
        Intent locationService = new Intent(OrderAssignmentActivity.this, LocationService.class);
        stopService(locationService);

        disableOrdersTracking();

    }

    // ORDERS TRACKING SERVICE
    protected void initiateOrdersTracking(){
        Intent ordersTrackingIntent = new Intent(OrderAssignmentActivity.this, SignalROrdersService.class);
        ordersTrackingIntent.putExtra(Constants.BASE_URL_STORAGE, UserPreferencesManager.getBaseUrl(context));
        ordersTrackingIntent.putExtra(Constants.DISTRICT_ID, UserPreferencesManager.getDistrictId(context));
        startService(ordersTrackingIntent);
    }

    private void disableOrdersTracking(){
        // Stop orders tracking service
        Intent stopOrdersService = new Intent(OrderAssignmentActivity.this, SignalROrdersService.class);
        stopService(stopOrdersService);
    }

    // BROADCAST RECEIVER
    private final BroadcastReceiver broadcastsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // LOCATION
            if (action.equals(Constants.LOCATION_UPDATED)) {
                // Client location change
                Bundle data = intent.getExtras();


                taxiDriverLocation = data.getParcelable(Constants.LOCATION);

                // Update taxi location too
                assignedTaxi.latitude = taxiDriverLocation.getLatitude();
                assignedTaxi.longitude = taxiDriverLocation.getLongitude();
                updateOrdersListView();
                if(lastLocation == null) {
                    lastLocation = taxiDriverLocation;
                    updateTaxiDetails();
                } else if(lastLocation.distanceTo(taxiDriverLocation) > Constants.LOCATION_REST_REPORT_THRESHOLD){
                    lastLocation = taxiDriverLocation;
                    updateTaxiDetails();
                }
            }

            // ORDERS HUB
            if (action.equals(Constants.HUB_ORDERS_UPDATED_BC)) {
                if(true) return;
//                String ordersString = intent.getStringExtra(Constants.HUB_UPDATE_ORDERS_LIST);
//
//                Type listOfOrders = new TypeToken<List<OrderDetailsDM>>(){}.getType();
//                List<OrderDetailsDM> ordersData = gson.fromJson(ordersString, listOfOrders);
//                orders.clear();
//                orders.addAll(ordersData);
//                updateOrdersListView();
            }

            if(action.equals(Constants.HUB_ADDED_ORDER_BC)){
                String orderString = intent.getStringExtra(Constants.HUB_ADDED_ORDER);

                Type type = new TypeToken<OrderDetailsDM>(){}.getType();
                OrderDetailsDM order = gson.fromJson(orderString, type);
                OrderDM orderDM = fromOrderDetailsDM(order);
                orders.add(orderDM);
                updateOrdersListView();


            }
            if(action.equals(Constants.HUB_CANCELLED_ORDER_BC)){
                int cancelledOrderId = intent.getIntExtra(Constants.HUB_CANCELLED_ORDER, -1);
                if(cancelledOrderId != -1  && !orders.isEmpty()){
                    Iterator<OrderDM> itr = orders.iterator();
                    while (itr.hasNext()){
                        OrderDM element = itr.next();
                        if(element.orderId == cancelledOrderId) {
                            if(selectedOrderId == cancelledOrderId) {
                                invalidateSelectedOrder();
                            }
                            itr.remove();
                        }
                    }
                    updateOrdersListView();

//                    OrderDM orderToCancel = new OrderDM();
//                    boolean found = false;
//                    for(OrderDM orderDM : orders){
//                        if(orderDM.orderId == cancelledOrderId){
//                            orderToCancel = orderDM;
//                            found = true;
//                        }
//                    }
//                    if(found){
//                        orders.remove(orderToCancel);
//                        updateOrdersListView();
//                    }
                }
            }
            if(action.equals(Constants.HUB_UPDATED_ORDER_BC)){
                String orderString = intent.getStringExtra(Constants.HUB_UPDATED_ORDER);

                Type type = new TypeToken<OrderDetailsDM>(){}.getType();
                OrderDetailsDM order = gson.fromJson(orderString, type);
                for(OrderDM ord: orders){
                    if(ord.orderId == order.orderId){
                        OrderDM orderDM = fromOrderDetailsDM(order);
                        updateOrderDM(ord, orderDM);
                    }
                }
                updateOrdersListView();

            }
            if(action.equals(Constants.HUB_ASSIGNED_ORDER_BC)){
                int assignedOrderId = intent.getIntExtra(Constants.ORDER_ID, -1);
                int taxiId = intent.getIntExtra(Constants.ASSIGNED_TAXI_ID, -1);
                if(taxiId == assignedTaxi.taxiId){
                   return;
                } else {
                    if(assignedOrderId != -1  && !orders.isEmpty()){
                        Iterator<OrderDM> itr = orders.iterator();
                        while (itr.hasNext()){
                            OrderDM element = itr.next();
                            if(element.orderId == assignedOrderId) {
                                itr.remove();
                            }
                        }
                        updateOrdersListView();
                    }
                }
            }

           // abortBroadcast();
        }
    };

    private void updateTaxiDetails() {
        if(!((assignedTaxi.status == Constants.TaxiStatus.Available.getValue()) || (assignedTaxi.status == Constants.TaxiStatus.Busy.getValue()))){
            return;
        }

        RestClientManager.updateTaxi(assignedTaxi, context, new Callback<Integer>() {
            @Override
            public void success(Integer o, Response response) {
                Log.d(getClass().getName(), "Taxi location reported!");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(getClass().getName(), "Taxi location report ERROR!");
            }
        });
    }

    private void invalidateSelectedOrder(){
        selectedOrderId = -1;
        assignButton.setEnabled(false);
        getFragmentManager().beginTransaction().hide(orderDetailsFragment).commit();
    }

    private void updateOrdersListView() {
        if(ordersListAdapter != null){
            Collections.sort(orders, distanceComparator);
            ordersListAdapter.notifyDataSetChanged();
        } else {
            if(!orders.isEmpty()) {
                mNoOrdersTxt.setVisibility(View.INVISIBLE);
                Collections.sort(orders, distanceComparator);
                ordersListAdapter = new ClientOrdersListAdapter(context,
                        R.layout.fragment_order_list_item, orders);
                ordersListView.setVisibility(View.VISIBLE);
                ordersListView.setAdapter(ordersListAdapter);
                ordersListView.setOnItemClickListener(this);
            } else {
                mNoOrdersTxt.setVisibility(View.VISIBLE);
                ordersListView.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void disableAssignButton(){
        assignButton.setEnabled(false);
    }

    public class DistanceComparator implements Comparator<OrderDM> {
        @Override
        public int compare(OrderDM o1, OrderDM o2) {
            double val1 = (Math.abs(o1.orderLatitude - assignedTaxi.latitude) + Math.abs(o1.orderLongitude - assignedTaxi.longitude));
            double val2 = (Math.abs(o2.orderLatitude - assignedTaxi.latitude) + Math.abs(o2.orderLongitude - assignedTaxi.longitude));
            boolean result = val1 < val2;
            return result ? 1 : -1;
        }
    }

    private void getDistrictOrders() {
       // if(true) return;
        showProgress(true);
        RestClientManager.getDistrictOrders(context, new Callback<List<OrderDM>>() {
            @Override
            public void success(List<OrderDM> orderDMs, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    assignButton.setEnabled(false);
                    orders.clear();
                    orders.addAll(orderDMs);
                    updateOrdersListView();
                }

                if (status == HttpStatus.SC_BAD_REQUEST) {
                    mNoOrdersTxt.setVisibility(View.INVISIBLE);
                    Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                }

                showProgress(false);
            }

            @Override
            public void failure(RetrofitError error) {
                showProgress(false);
                showToastError(error);
                assignButton.setEnabled(false);
            }
        });
    }

    private void checkForActiveOrder(){
        showProgress(true);
       int assignedOrderId =  UserPreferencesManager.getLastOrderId(context);
        RestClientManager.getOrder(assignedOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM orderDetailsDM, Response response) {
                showProgress(false);
                if (orderDetailsDM.taxiId == assignedTaxi.taxiId && !(orderDetailsDM.status == Constants.OrderStatus.Finished.getValue())) {
                    // Still active order for this taxi
                    Toast.makeText(context, R.string.in_order_assignment, Toast.LENGTH_LONG).show();
                    Intent orderMap = new Intent(context, OrderMap.class);
                    //orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(orderMap);
                } else {
                    UserPreferencesManager.clearOrderAssignment(context);
                    getDistrictOrders();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                showProgress(false);
            }
        });
    }

    private void assignSelectedOrder(){
        if(selectedOrderId == -1) return;

        showProgress(true);
        RestClientManager.assignOrder(selectedOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM orderDetailsDM, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    UserPreferencesManager.storeOrderId(orderDetailsDM.orderId, context);

                    Intent assignment = new Intent(Constants.ASSIGNED_ORDER_BC);
                    assignment.putExtra(Constants.ASSIGNED_TAXI_ID, assignedTaxi.taxiId);
                    assignment.putExtra(Constants.ASSIGNED_TAXI_PLATE, assignedTaxi.plate);
                    sendBroadcast(assignment);

                    Intent orderMap = new Intent(context, OrderMap.class);
                    //orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    showProgress(false);
                    context.startActivity(orderMap);
                }

                if (status == HttpStatus.SC_BAD_REQUEST) {
                    Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                }

                if (status == HttpStatus.SC_NOT_FOUND) {
                    Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                }

                showProgress(false);
            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                showProgress(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order_assignment, menu);
        if((assignedTaxi.status == Constants.TaxiStatus.Available.getValue()) || (assignedTaxi.status == Constants.TaxiStatus.Busy.getValue())){
            menu.findItem(R.id.action_switch_duty).setChecked(true);
        } else {
            menu.findItem(R.id.action_switch_duty).setChecked(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_switch_duty) {

            if (UserPreferencesManager.hasAssignedOrder(context)) {
                Toast.makeText(context, R.string.in_order_assignment, Toast.LENGTH_LONG).show();
                return true;
            }


//            if(assignedTaxi.onDuty != assignedTaxi.isAvailable){
//                if(UserPreferencesManager.hasAssignedOrder(context)) {
//                    Toast.makeText(context, R.string.bad_state, Toast.LENGTH_LONG).show();
//                    return true;
//                }
//            }

            final int previousState = assignedTaxi.status;

            // invert taxi status in model
            if(previousState == Constants.TaxiStatus.Available.getValue() || previousState == Constants.TaxiStatus.Busy.getValue()){
                assignedTaxi.status = Constants.TaxiStatus.OffDuty.getValue();
                item.setChecked(false);
            } else {
                assignedTaxi.status = Constants.TaxiStatus.Available.getValue();
                item.setChecked(true);
            }

            showProgress(true);

//            item.setChecked(!item.isChecked());


            // TODO FINISH !!!!!!!!
            // send the new status
            RestClientManager.updateTaxi(assignedTaxi, context, new Callback<Integer>() {
                @Override
                public void success(Integer newStatus, Response response) {
                    int status = response.getStatus();
                    if (status == HttpStatus.SC_OK) {
                        //if all went fine update
                        if(newStatus == Constants.TaxiStatus.Available.getValue()) {
                            Toast.makeText(context, R.string.now_on_duty_available, Toast.LENGTH_LONG).show();
                            taxiStatusChnaged(newStatus);
                        } else if(newStatus == Constants.TaxiStatus.Busy.getValue()){
                            Toast.makeText(context, R.string.now_on_duty_busy, Toast.LENGTH_LONG).show();
                            taxiStatusChnaged(newStatus);
                        } else if (newStatus == Constants.TaxiStatus.OffDuty.getValue()){
                            Toast.makeText(context, R.string.now_off_duty, Toast.LENGTH_LONG).show();
                            taxiStatusChnaged(newStatus);
                        } else {
                            Toast.makeText(context, R.string.taxi_bad_state, Toast.LENGTH_LONG).show();
                        }


                    }
                    if (status == HttpStatus.SC_BAD_REQUEST) {
                        Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                    }

                    if (status == HttpStatus.SC_NOT_FOUND) {
                        Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                    }

                    showProgress(false);
                }

                @Override
                public void failure(RetrofitError error) {
                    showToastError(error);
                    showProgress(false);
                }
            });

            return true;
        }

        if (id == R.id.action_order_unassign_taxi) {
            showProgress(true);

            if (UserPreferencesManager.hasAssignedOrder(context)) {
                Toast.makeText(context, R.string.in_order_assignment, Toast.LENGTH_LONG).show();
                return true;
            }

            RestClientManager.unassignTaxi(assignedTaxi.taxiId, context, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    int status = response.getStatus();
                    if (status == HttpStatus.SC_OK) {
                        UserPreferencesManager.clearAssignedTaxi(context);

                        Intent taxiAssignmentActivity = new Intent(context, TaxiAssignmentActivity.class);
                        taxiAssignmentActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        showProgress(false);
                        startActivity(taxiAssignmentActivity);
                    }
                    if (status == HttpStatus.SC_BAD_REQUEST) {
                        Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                    }

                    if (status == HttpStatus.SC_NOT_FOUND) {
                        Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                    }

                    showProgress(false);
                }

                @Override
                public void failure(RetrofitError error) {
                    showToastError(error);
                    showProgress(false);
                }
            });

            return true;
        }

        if (id == R.id.action_order_assignment_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void taxiStatusChnaged(int status) {
        UserPreferencesManager.setAssignedTaxi(assignedTaxi, context);
        if(status == Constants.TaxiStatus.Available.getValue() || status == Constants.TaxiStatus.Busy.getValue()){
            initiateOrdersTracking();
        } else {
            disableOrdersTracking();
        }
    }


    // ORDERS LIST
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        getFragmentManager().beginTransaction().show(orderDetailsFragment).commit();

        orderAddressTxt = (TextView)findViewById(R.id.orderAddressDetail);
        orderAddressTxt.setText(orders.get(position).orderAddress);
        orderDestinationTxt = (TextView)findViewById(R.id.orderDestinationDetail);
        orderDestinationTxt.setText(orders.get(position).destinationAddress);
        clientCommentTxt = (TextView)findViewById(R.id.clientCommentDetail);
        clientCommentTxt.setText(orders.get(position).userComment);
        selectedOrderId = orders.get(position).orderId;

        assignButton.setEnabled(true);
    }

    private void showToastError(RetrofitError error) {
        if(error.getResponse() != null) {
            if (error.getResponse().getBody() != null) {
                String json =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
                if(!json.isEmpty()){
                    Toast.makeText(context, json, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private OrderDM fromOrderDetailsDM(OrderDetailsDM orderDetails){
        OrderDM order = new OrderDM();
        order.orderId = orderDetails.orderId;
        order.orderAddress = orderDetails.orderAddress;
        order.orderLatitude = orderDetails.orderLatitude;
        order.orderLongitude = orderDetails.orderLongitude;
        order.destinationAddress = orderDetails.destinationAddress;
        order.destinationLatitude = orderDetails.destinationLatitude;
        order.destinationLongitude = orderDetails.destinationLongitude;
        order.userComment = orderDetails.userComment;
        order.pickupTime = orderDetails.pickupTime;
        return order;
    }

    private void updateOrderDM(OrderDM order, OrderDM newOrderDM){
        order.orderId = newOrderDM.orderId;
        order.orderAddress = newOrderDM.orderAddress;
        order.orderLatitude = newOrderDM.orderLatitude;
        order.orderLongitude = newOrderDM.orderLongitude;
        order.destinationAddress = newOrderDM.destinationAddress;
        order.destinationLatitude = newOrderDM.destinationLatitude;
        order.destinationLongitude = newOrderDM.destinationLongitude;
        order.userComment = newOrderDM.userComment;
        order.pickupTime = newOrderDM.pickupTime;
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
