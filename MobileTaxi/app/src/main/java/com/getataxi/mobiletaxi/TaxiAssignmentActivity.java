package com.getataxi.mobiletaxi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import com.getataxi.mobiletaxi.comm.models.TaxiDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.utils.TaxiesListAdapter;
import com.getataxi.mobiletaxi.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class TaxiAssignmentActivity extends ActionBarActivity  implements
        AdapterView.OnItemClickListener {

    List<TaxiDetailsDM> taxies;
    TaxiDetailsDM selectedTaxi;
    private ListView taxiesListView;
    private Button assignTaxiButton;

    private TextView taxiIdTxt;
    private TextView taxiPlateTxt;

    private View mProgressView;
    private TextView mNoTaxies;

    TaxiesListAdapter taxiesListAdapter;
    Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxi_assignment);
        taxiesListView = (ListView) this.findViewById(R.id.taxies_list_view);

        mProgressView = findViewById(R.id.get_taxies_progress);
        mNoTaxies = (TextView)findViewById(R.id.noTaxiesLabel);

        getDistrictTaxies();
        this.assignTaxiButton = (Button)this.findViewById(R.id.assignTaxiButton);
        assignTaxiButton.setEnabled(false);
        assignTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedTaxi != null) {
                    TaxiDM taxi = new TaxiDM();
                    taxi.taxiId = selectedTaxi.taxiId;
                    taxi.latitude = selectedTaxi.latitude;
                    taxi.longitude = selectedTaxi.longitude;
                    assignTaxi(taxi);
                }
            }
        });



    }

    private void populateTaxiesList() {
        if(!taxies.isEmpty()) {

            taxiesListAdapter = new TaxiesListAdapter(context,
                    R.layout.fragment_taxi_list_item, taxies);

            taxiesListView.setAdapter(taxiesListAdapter);
            taxiesListView.setOnItemClickListener(this);
        }
    }

    private void assignTaxi(TaxiDM taxi) {
        showProgress(true);
        RestClientManager.assignTaxi(taxi, context, new Callback<TaxiDetailsDM>() {
            @Override
            public void success(TaxiDetailsDM taxiDetailsDM, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    selectedTaxi = taxiDetailsDM;
                    UserPreferencesManager.setAssignedTaxi(selectedTaxi, context);
                    UserPreferencesManager.setDistrictId(selectedTaxi.districtId, context);
                    Toast.makeText(context, getResources().getText(R.string.taxi_assigned), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(context, OrderAssignmentActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                if (status == HttpStatus.SC_BAD_REQUEST || status == HttpStatus.SC_NOT_FOUND) {
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

    private void getDistrictTaxies() {
        showProgress(true);
        RestClientManager.getTaxies(context, new Callback<List<TaxiDetailsDM>>() {
            @Override
            public void success(List<TaxiDetailsDM> taxiesDMs, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    if (taxiesDMs.size() > 0) {
                        mNoTaxies.setVisibility(View.INVISIBLE);
                    } else {
                        mNoTaxies.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(context, "Taxies: " + taxiesDMs.size(), Toast.LENGTH_LONG).show();
                    taxies = new ArrayList<>();
                    taxies.clear();
                    taxies.addAll(taxiesDMs);
                    populateTaxiesList();
                }

                if (status == HttpStatus.SC_BAD_REQUEST) {
                    mNoTaxies.setVisibility(View.INVISIBLE);
                    // Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
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

   public void logoutDriver(){
       UserPreferencesManager.clearLoginData(context);
       Intent intent = new Intent(context, LoginActivity.class);
       intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
       startActivity(intent);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_taxi_assignment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_taxi_exit) {
            finish();
            return true;
        }

        if(id == R.id.action_taxi_logout){
            logoutDriver();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        taxiIdTxt = (TextView)findViewById(R.id.taxiIdDetail);
        taxiIdTxt.setText(String.valueOf(taxies.get(position).taxiId));
        taxiPlateTxt = (TextView)findViewById(R.id.taxiPlateDetail);
        taxiPlateTxt.setText(taxies.get(position).plate);
        selectedTaxi = taxies.get(position);
        assignTaxiButton.setEnabled(true);
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
