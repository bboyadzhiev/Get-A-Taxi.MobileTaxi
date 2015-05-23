package com.getataxi.mobiletaxi.comm;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.mobiletaxi.comm.contracts.TaxiOrdersAPI;
import com.getataxi.mobiletaxi.comm.contracts.TaxiAPI;
import com.getataxi.mobiletaxi.comm.models.OrderDetailsDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDM;
import com.getataxi.mobiletaxi.comm.models.TaxiStandDM;
import com.getataxi.mobiletaxi.comm.models.LoginUserDM;
import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.comm.models.RegisterUserDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.utils.Constants;
import com.getataxi.mobiletaxi.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

/**
 * Created by bvb on 1.4.2015 г..
 */
public class RestClientManager {
    private static final String User_Info_File = "GetATaxiDriver";

    public static String base_url = Constants.DEFAULT_URL;
    public static RestClient client = new RestClient(base_url);
    private static List<NameValuePair> headers = new ArrayList<NameValuePair>();

    public RestClientManager(){
    }

    private static void init(){

    }

    private static List<NameValuePair> getAuthorisationHeaders(Context context){
        LoginUserDM loginData = UserPreferencesManager.getLoginData(context);
       // List<NameValuePair> heads = new ArrayList<NameValuePair>();
      //  heads.add(new BasicNameValuePair("Authorization", "Bearer " + loginData.accessToken));

       //headers.clear();

        if ( headers.isEmpty()){
            headers.add(new BasicNameValuePair("Authorization", "Bearer " + loginData.accessToken));
        }
        if (UserPreferencesManager.tokenHasExpired(loginData.expires)){
            updateToken(loginData, "password", context);
        }
        Log.d("RESTMANAGER: ", "HEADERS ADDED:");
        for (NameValuePair header : headers) {
            Log.d(header.getName()+" : ", header.getValue());
        }
        return headers;
    }


    public static void updateToken(LoginUserDM loginUserDM, String grantType, final Context context) {
        client.getAccountService(null).login(loginUserDM.email, loginUserDM.password, grantType,new Callback<LoginUserDM>(){
            @Override
            public void success(LoginUserDM loginUserDM, Response response) {
                int status  = response.getStatus();
                if (status == HttpStatus.SC_OK){
                    try {
                        headers.clear();
                        headers.add(new BasicNameValuePair("Authorization", "Bearer " + loginUserDM.accessToken));
                        UserPreferencesManager.saveLoginData(loginUserDM, context);
                        Log.d("RESTMANAGER: ", "Token UPDATED!");
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                String errorJson =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
                Log.d("RESTMANAGER: ", "Token update FAILED!");
                Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void login(LoginUserDM loginUserDM, String grantType, Callback<LoginUserDM> callback){
        client.getAccountService(null).login(loginUserDM.email, loginUserDM.password, grantType,callback);
    }

    public static void register(final RegisterUserDM registerUserDM, Callback<String> callback){
        client.getAccountService(null).register(registerUserDM, callback);
    }


    // TaxiStands
    public static void getTaxiStandsByLocation(double lat, double lon, Context context, Callback<List<TaxiStandDM>> callback){
        List<NameValuePair> heads = getAuthorisationHeaders(context);
        client.getTaxiStandsService(heads).getTaxiStandsByLocation(lat, lon, callback);
    }

    public static void getTaxiStand(int taxiStandId, Context context, Callback<TaxiStandDM> callback){
        client.getTaxiStandsService(getAuthorisationHeaders(context)).getTaxiStand(taxiStandId, callback);
    }

    public static void getTaxiStandsPage(int page, Context context, Callback<List<TaxiStandDM>> callback){
        List<NameValuePair> heads = getAuthorisationHeaders(context);
        client.getTaxiStandsService(heads).getTaxiStandsPage(page, callback);
    }

//    public static void updateTaxiLocation(final LocationDM locationDM, Context context, Callback callback){
//        List<NameValuePair> heads = getAuthorisationHeaders(context);
//        LocationsAPI locationsApi = client.getLocationsService(heads);
//        locationsApi.updateLocation(locationDM, callback);
//    }

    // Orders
    public static void addOrder(OrderDetailsDM order, Context context, Callback<OrderDetailsDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.addOrder(order, callback);
    }



    public static void getDistrictOrders(Context context, Callback<List<OrderDM>> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.getOrders(callback);
    }

    public static void getOrder(int id, Context context, Callback<OrderDetailsDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.getOrder(id, callback);
    }
    public static void assignOrder(int id, Context context, Callback<OrderDetailsDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.assignOrder(id, callback);
    }

    public static void updateOrder(OrderDetailsDM orderDM, Context context, Callback<OrderDetailsDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.updateOrder(orderDM, callback);
    }

    public static void cancelOrder(int id, Context context, Callback<OrderDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.cancelOrder(id, callback);
    }

    // Taxi
    public static void getTaxies(Context context, Callback<List<TaxiDetailsDM>> callback) {
        TaxiAPI taxiApi = getTaxiAPI(context);
        taxiApi.getTaxies(callback);
    }

    private static TaxiAPI getTaxiAPI(Context context) {
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        return client.getTaxiService(heads);
    }

    public static void getTaxiDetails(int id, Context context, Callback<TaxiDetailsDM> callback){
        TaxiAPI taxiApi = getTaxiAPI(context);
        taxiApi.getTaxiDetails(id, callback);
    }

    public static void getTaxiesPage(int page, Context context, Callback<List<TaxiDetailsDM>> callback){
        TaxiAPI taxiApi = getTaxiAPI(context);
        taxiApi.getTaxiesPage(page, callback);
    }

    public static void assignTaxi(TaxiDM taxi, Context context, Callback<TaxiDetailsDM> callback){
        TaxiAPI taxiApi = getTaxiAPI(context);
        taxiApi.assignTaxi(taxi, callback);
    }

    public static void updateTaxi(TaxiDM taxi, Context context, Callback<Object> callback){
        TaxiAPI taxiApi = getTaxiAPI(context);
        taxiApi.updateTaxi(taxi, callback);
    }

    public static void unassignTaxi(int id, Context context, Callback<Object> callback){
        TaxiAPI taxiApi = getTaxiAPI(context);
        taxiApi.unassignTaxi(id, callback);
    }
}
