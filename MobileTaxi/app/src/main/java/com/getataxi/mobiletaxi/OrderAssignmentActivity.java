package com.getataxi.mobiletaxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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

    private TextView orderAddressTxt;
    private TextView orderDestinationTxt;
    private TextView clientCommentTxt;
    private int selectedOrderId;

    private View mProgressView;
    private TextView mNoOrdersTxt;

    ClientOrdersListAdapter ordersListAdapter;
    Context context = this;

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
                orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(orderMap);
            }
        });

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(UserPreferencesManager.hasAssignedOrder(context)){
            // If still active order, goes directly to order map
            checkForActiveOrder();
        } else {
            getDistrictOrders();
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        // Register for Order Assignment Hub broadcasts
        filter.addAction(Constants.HUB_ORDERS_UPDATED_BC);
        filter.addAction(Constants.HUB_ADDED_ORDER_BC);
        filter.addAction(Constants.HUB_ASSIGNED_ORDER_BC);
        filter.addAction(Constants.HUB_CANCELLED_ORDER_BC);
        filter.addAction(Constants.HUB_UPDATED_ORDER_BC);
        // And peer location change
        registerReceiver(broadcastsReceiver, filter);
        initiateOrdersTracking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop tracking service
        Intent ordersTrackingIntent = new Intent(OrderAssignmentActivity.this, SignalROrdersService.class);
        stopService(ordersTrackingIntent);

        unregisterReceiver(broadcastsReceiver);
    }

    private final BroadcastReceiver broadcastsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();


            if (action.equals(Constants.HUB_ORDERS_UPDATED_BC)) {
                String ordersString = intent.getStringExtra(Constants.HUB_UPDATE_ORDERS_LIST);
                Gson gson = new GsonBuilder()
                        .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS")
                        .create();
                Type listOfOrders = new TypeToken<List<OrderDetailsDM>>(){}.getType();
                List<OrderDetailsDM> ordersData = gson.fromJson(ordersString, listOfOrders);
                orders.clear();
                orders.addAll(ordersData);
                populateOrdersListView();
            }
            if(action.equals(Constants.HUB_ADDED_ORDER_BC)){
                String orderString = intent.getStringExtra(Constants.HUB_ADDED_ORDER);
                Gson gson = new GsonBuilder()
                        .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS")
                        .create();
                Type type = new TypeToken<OrderDetailsDM>(){}.getType();
                OrderDetailsDM order = gson.fromJson(orderString, type);
                OrderDM orderDM = fromOrderDetailsDM(order);
                orders.add(orderDM);
                populateOrdersListView();


            }
            if(action.equals(Constants.HUB_CANCELLED_ORDER_BC)){
                int cancelledOrderId = intent.getIntExtra(Constants.HUB_CANCELLED_ORDER, -1);
                if(cancelledOrderId != -1  && !orders.isEmpty()){
                    Iterator<OrderDM> itr = orders.iterator();
                    while (itr.hasNext()){
                        OrderDM element = itr.next();
                        if(element.orderId == cancelledOrderId) {
                            itr.remove();
                        }
                    }
                    populateOrdersListView();

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
//                        populateOrdersListView();
//                    }
                }
            }
            if(action.equals(Constants.HUB_UPDATED_ORDER_BC)){
                String orderString = intent.getStringExtra(Constants.HUB_UPDATED_ORDER);
                Gson gson = new GsonBuilder()
                        .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS")
                        .create();
                Type type = new TypeToken<OrderDetailsDM>(){}.getType();
                OrderDetailsDM order = gson.fromJson(orderString, type);

                OrderDM orderDM = fromOrderDetailsDM(order);

                for(OrderDM ord: orders){
                    if(ord.orderId == order.orderId){
                        updateOrderDM(ord, orderDM);
                    }
                }
                populateOrdersListView();

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
                        populateOrdersListView();
                    }
                }
            }
        }
    };

    // ORDERS TRACKING SERVICE
    protected void initiateOrdersTracking(){
        Intent ordersTrackingIntent = new Intent(OrderAssignmentActivity.this, SignalROrdersService.class);
        ordersTrackingIntent.putExtra(Constants.BASE_URL_STORAGE, UserPreferencesManager.getBaseUrl(context));
        ordersTrackingIntent.putExtra(Constants.DISTRICT_ID, UserPreferencesManager.getDistrictId(context));
        startService(ordersTrackingIntent);
    }

    private void populateOrdersListView() {
        if(ordersListAdapter != null){
            ordersListAdapter.notifyDataSetChanged();
        } else {
            if(!orders.isEmpty()) {
                Collections.sort(orders, new DistanceComparator());
                ordersListAdapter = new ClientOrdersListAdapter(context,
                        R.layout.fragment_order_list_item, orders);

                ordersListView.setAdapter(ordersListAdapter);
                ordersListView.setOnItemClickListener(this);
            }
        }

    }

    public class DistanceComparator implements Comparator<OrderDM> {
        @Override
        public int compare(OrderDM o1, OrderDM o2) {
            double val1 = ((o1.orderLatitude - assignedTaxi.latitude) + (o1.orderLongitude - assignedTaxi.longitude));
            double val2 = ((o1.orderLatitude - assignedTaxi.latitude) + (o1.orderLongitude - assignedTaxi.longitude));
            boolean result = val2 < val1;
            return result ? 1 : -1;
        }
    }

    private void getDistrictOrders() {

        showProgress(true);
        RestClientManager.getDistrictOrders(context, new Callback<List<OrderDM>>() {
            @Override
            public void success(List<OrderDM> orderDMs, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    if (orderDMs.size() > 0) {
                        mNoOrdersTxt.setVisibility(View.INVISIBLE);
                        assignButton.setEnabled(false);
                    } else {
                        mNoOrdersTxt.setVisibility(View.VISIBLE);
                        assignButton.setEnabled(false);
                    }

                    orders.clear();
                    orders.addAll(orderDMs);

                    populateOrdersListView();
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
                if (orderDetailsDM.taxiId == assignedTaxi.taxiId && !orderDetailsDM.isFinished) {
                    // Still active order for this taxi
                    Toast.makeText(context, R.string.in_order_assignment, Toast.LENGTH_LONG).show();
                    Intent orderMap = new Intent(context, OrderMap.class);
                    orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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
        showProgress(true);
        RestClientManager.assignOrder(selectedOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM orderDetailsDM, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    UserPreferencesManager.storeOrderId(orderDetailsDM.orderId, context);

                    Intent orderMap = new Intent(context, OrderMap.class);
                    orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_order_unassign_taxi) {
            showProgress(true);
            if (UserPreferencesManager.hasAssignedOrder(context)) {
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
