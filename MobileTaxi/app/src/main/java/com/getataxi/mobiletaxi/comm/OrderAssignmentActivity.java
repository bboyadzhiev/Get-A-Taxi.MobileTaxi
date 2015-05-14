package com.getataxi.mobiletaxi.comm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
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

import com.getataxi.mobiletaxi.R;
import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.utils.ClientOrdersListAdapter;

import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrderAssignmentActivity extends ActionBarActivity implements
        AdapterView.OnItemClickListener {

    private ArrayList<OrderDM> orders;
    private ListView ordersList;
    private Button assignButton;

    private TextView orderAddressTxt;
    private TextView orderDestinationTxt;
    private TextView clientCommentTxt;

    private View mProgressView;

    ClientOrdersListAdapter ordersListAdapter;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_assignment);
        orders = new ArrayList<>();
        mProgressView = findViewById(R.id.get_orders_progress);
        getDistrictOrders();
        this.assignButton = (Button)this.findViewById(R.id.assignButton);
        assignButton.setEnabled(false);
        assignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Send PUT request to server!
            }
        });

        ordersList = (ListView) this.findViewById(R.id.orders_list_view);

        ordersListAdapter = new ClientOrdersListAdapter(context,
                R.layout.fragment_order_list_item, orders);

        ordersList.setAdapter(ordersListAdapter);
        ordersList.setOnItemClickListener(this);

        getDistrictOrders();
    }

    private void getDistrictOrders() {
        showProgress(true);
        RestClientManager.getDistrictOrders(context, new Callback<List<OrderDM>>() {
            @Override
            public void success(List<OrderDM> orderDMs, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    orders.clear();
                    orders.addAll(orderDMs);
                }

                if (status == HttpStatus.SC_BAD_REQUEST) {
                    // Cancelled or finished already
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
        if (id == R.id.action_settings) {
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
