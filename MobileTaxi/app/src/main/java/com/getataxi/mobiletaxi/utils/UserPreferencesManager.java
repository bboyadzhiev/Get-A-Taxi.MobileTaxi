package com.getataxi.mobiletaxi.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.getataxi.mobiletaxi.comm.models.OrderDM;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;
import com.getataxi.mobiletaxi.comm.models.TaxiStandDM;
import com.getataxi.mobiletaxi.comm.models.LoginUserDM;
import com.getataxi.mobiletaxi.comm.models.RegisterUserDM;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by bvb on 31.3.2015 г..
 */
public class UserPreferencesManager {
    private static final String USER_LOGIN_INFO = "Driver";


    public static DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
    //".issued":"Thu, 09 Apr 2015 20:48:26 GMT"
    public static DateFormat tokenDateFormat = new SimpleDateFormat(Constants.TOKEN_DATE_FORMAT, Locale.ENGLISH);

    public static Date GetDate(String dateString) throws ParseException {
        Date date = new Date();

        if (dateString != null) {
            date = tokenDateFormat.parse(dateString);
        }

        return date;
    }


    /**
     * Checks if token has expired.
     * @return true if token has expired
     */
    public static boolean tokenHasExpired(String expires) {
        if(expires == null || expires.isEmpty()){
            return true;
        }
        Date tokenExpirationDate = null;
        Date now =  new Date();
        try {
            tokenExpirationDate = GetDate(expires);
            //now = tokenDateFormat.parse(tokenDateFormat.format(GetDate(null)));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (tokenExpirationDate != null){
            int expired = tokenExpirationDate.compareTo(now);
            if (expired <= 0 ){
                // Token has expired!
                return true;
            }
        }
        return false;
    }


    // Driver account data
    public static void saveUserData(RegisterUserDM userDM, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String registerData = gson.toJson(userDM);
        editor.putString(Constants.USER_DATA, registerData);
        editor.putBoolean(Constants.IS_LOGGED, false);
        editor.commit();
    }

    public static void saveLoginData(LoginUserDM userDM, Context context)
            throws IllegalStateException, IOException {
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String loginData = gson.toJson(userDM);
        editor.putString(Constants.LOGIN_DATA, loginData);
        editor.putBoolean(Constants.IS_LOGGED, true);
        editor.commit();
    }

    public static void clearLoginData(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit()
                .remove(Constants.LOGIN_DATA)
                .putBoolean(Constants.IS_LOGGED, false);
        editor.commit();
    }

    public static boolean checkForLoginCredentials(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String userData = userPrefs.getString(Constants.LOGIN_DATA, "");
        if(userData.length() > 0){
            return true;
        }
        return false;
    }

    public static boolean isLoggedIn(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getBoolean(Constants.IS_LOGGED, false);
    }

    public static void setDistrictId(int distritId, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt(Constants.DISTRICT_ID, distritId);
        editor.commit();
    }

    public static int getDistrictId(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getInt(Constants.DISTRICT_ID, -1);
    }


    public static boolean checkForRegistration(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);

       // boolean email = userPref.contains("email");
       // boolean password = userPref.contains("password");
       // boolean isRegistered  =  email && password;
        return userPref.contains(Constants.USER_DATA);
    }


    public static String getToken(Context context){

        LoginUserDM userInfo = getLoginData(context);
        return userInfo.token;
    }

    public static String getEmail(Context context){
        LoginUserDM userInfo = getLoginData(context);
        return userInfo.email;
    }

    public static String getPassword(Context context){
        LoginUserDM userInfo = getLoginData(context);
        return userInfo.password;
    }


    public static LoginUserDM getLoginData(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String userData = userPrefs.getString(Constants.LOGIN_DATA, "");
       // Log.d("USER_DATA", userData);
        Gson gson = new Gson();
        LoginUserDM userInfo = gson.fromJson(userData, LoginUserDM.class);
        return userInfo;
    }

    public static void logoutUser(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putBoolean(Constants.IS_LOGGED, false);
        editor.commit();
    }


    // Taxi Stands data
    public static void storeTaxiStands(List<TaxiStandDM> locationDMList, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        Type listOfLocation = new TypeToken<List<TaxiStandDM>>(){}.getType();
        String locationsData = gson.toJson(locationDMList, listOfLocation);
        editor.putString(Constants.USER_LOCATIONS, locationsData);
        editor.commit();
    }

    public static List<TaxiStandDM> loadTaxiStands(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String locations = userPref.getString(Constants.USER_LOCATIONS, "");
        Gson gson = new Gson();
        Type listOfLocation = new TypeToken<List<TaxiStandDM>>(){}.getType();
        List<TaxiStandDM> locationsData = gson.fromJson(locations, listOfLocation);
        return locationsData;
    }

    // Current order
    public static void storeOrderId(int orderId, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt(Constants.LAST_ORDER_ID, orderId);
        editor.commit();
    }

    public static int getLastOrderId(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getInt(Constants.LAST_ORDER_ID, -1);
    }

    public static void clearOrderAssignment(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt(Constants.LAST_ORDER_ID, -1);
        editor.commit();
    }

    public static boolean hasAssignedOrder(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getInt(Constants.LAST_ORDER_ID, -1) != -1;
    }

    // Assigned Taxi details
    public static void setAssignedTaxi(TaxiDetailsDM taxi, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        Type taxiType = new TypeToken<TaxiDetailsDM>(){}.getType();
        String taxiInfo = gson.toJson(taxi,taxiType);
        editor.putString(Constants.ASSIGNED_TAXI, taxiInfo);
        editor.putInt(Constants.ASSIGNED_TAXI_ID, taxi.taxiId);
        editor.commit();
    }

    public static TaxiDetailsDM getAssignedTaxi(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String taxiInfo = userPrefs.getString(Constants.ASSIGNED_TAXI, "");
        Gson gson = new Gson();
        Type taxiType = new TypeToken<TaxiDetailsDM>(){}.getType();
        TaxiDetailsDM taxi = gson.fromJson(taxiInfo, taxiType);
        return taxi;
    }

    public static void clearAssignedTaxi(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt(Constants.ASSIGNED_TAXI_ID, -1);
        editor.commit();
    }

    public static boolean hasAssignedTaxi(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        int assignedId = userPrefs.getInt(Constants.ASSIGNED_TAXI_ID, -1);
        if(assignedId != -1 ){
            return true;
        }
        return false;
    }

}
