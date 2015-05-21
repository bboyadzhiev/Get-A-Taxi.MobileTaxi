package com.getataxi.mobiletaxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
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
import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.utils.ClientOrdersListAdapter;
import com.getataxi.mobiletaxi.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrderAssignmentActivity extends ActionBarActivity implements
        AdapterView.OnItemClickListener {

    private ArrayList<OrderDM> orders;
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
        ordersListView = (ListView) this.findViewById(R.id.orders_list_view);
        assignedTaxi = UserPreferencesManager.getAssignedTaxi(context);

        if(UserPreferencesManager.hasAssignedOrder(context)){
            // If still active order, goes directly to order map
            checkForActiveOrder();
        }

        // no order assigned
        orders = new ArrayList<>();
        mProgressView = findViewById(R.id.get_orders_progress);
        mNoOrdersTxt = (TextView)findViewById(R.id.noOrdersLabel);

        getDistrictOrders();
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
                //TODO REVIEW!
                Intent orderMap = new Intent(context, OrderMap.class);
                orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(orderMap);
            }
        });

        getDistrictOrders();
    }

    private void populateOrdersListView() {
        if(!orders.isEmpty()) {
            ordersListView = (ListView) this.findViewById(R.id.orders_list_view);

            ordersListAdapter = new ClientOrdersListAdapter(context,
                    R.layout.fragment_order_list_item, orders);

            ordersListView.setAdapter(ordersListAdapter);
            ordersListView.setOnItemClickListener(this);
        }
    }

    private void getDistrictOrders() {
        showProgress(true);
        RestClientManager.getDistrictOrders(context, new Callback<List<OrderDM>>() {
            @Override
            public void success(List<OrderDM> orderDMs, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    if(orderDMs.size() > 0){
                        mNoOrdersTxt.setVisibility(View.INVISIBLE);
                    } else  {
                        mNoOrdersTxt.setVisibility(View.VISIBLE);
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
                assignButton.setEnabled(true);
            }
        });
    }

    private void checkForActiveOrder(){
       int assignedOrderId =  UserPreferencesManager.getLastOrderId(context);
        RestClientManager.getOrder(assignedOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM orderDetailsDM, Response response) {
                if (orderDetailsDM.taxiId == assignedTaxi.taxiId && !orderDetailsDM.isFinished) {
                    // Still active order for this taxi
                    Toast.makeText(context, R.string.in_order_assignment, Toast.LENGTH_LONG).show();
                    Intent orderMap = new Intent(context, OrderMap.class);
                    orderMap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(orderMap);
                }
            }

            @Override
            public void failure(RetrofitError error) {

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
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
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

            RestClientManager.unassignTaxi(assignedTaxi.taxiId, context, new Callback() {
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
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
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

        orderAddressTxt = (TextView)findViewById(R.id.orderAddressDetail);
        orderAddressTxt.setText(orders.get(position).orderAddress);
        orderDestinationTxt = (TextView)findViewById(R.id.orderDestinationDetail);
        orderDestinationTxt.setText(orders.get(position).destinationAddress);
        clientCommentTxt = (TextView)findViewById(R.id.clientCommentDetail);
        clientCommentTxt.setText(orders.get(position).userComment);
        selectedOrderId = orders.get(position).orderId;

        assignButton.setEnabled(true);
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
