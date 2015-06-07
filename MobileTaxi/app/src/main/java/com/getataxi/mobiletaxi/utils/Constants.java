package com.getataxi.mobiletaxi.utils;

/**
 * Created by bvb on 21.4.2015 г..
 */
public final class Constants {

    // APP NAME
    public static final String PACKAGE_NAME = Constants.class.getPackage().getName();
    public static final String GSON_DATE_FORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS";
    public static final int MAP_ANIMATION_ZOOM = 15;


    // COMMUNICATIONS
    //public static final String DEFAULT_URL = "http://get-a-taxi.apphb.com";
    //public static final String DEFAULT_URL = "http://192.168.50.112:14938";
    //public static final String DEFAULT_URL = "http://192.168.43.245:14938";
    public static final String DEFAULT_URL = "http://172.16.250.145:14938";
    public static final String BASE_URL_STORAGE = PACKAGE_NAME + ".BASE_URL";


    // HUBS
    // endpoints, DO NOT CHANGE
    public static final String HUB_ENDPOINT = "/signalr";
    // proxy commands, DO NOT CHANGE
    public static final String HUB_CONNECT = "Open";
    public static final String HUB_DISCONNECT = "Close";


    // TrackingHub
    public static final String TRACKING_HUB_PROXY = "trackingHub";
    public static final String HUB_PEER_LOCATION_CHANGED = "updatePeerLocation";
    public static final String HUB_MY_LOCATION_CHANGED = "locationChanged";
    public static final String HUB_TAXI_ASSIGNED_TO_ORDER = "taxiAssignedToOrder";
    public static final String HUB_ORDER_STATUS_CHANGED = "orderStatusChanged";
    public static final String LOCATION_REPORT_ENABLED = PACKAGE_NAME + ".LOCATION_REPORT_ENABLED";
    // Hub broadcasts
    public static final String HUB_PEER_LOCATION_CHANGED_BC = PACKAGE_NAME +  HUB_PEER_LOCATION_CHANGED;
    // Broadcasts reaction
    public static final String ORDER_STATUS_CHANGED_BC = PACKAGE_NAME + HUB_ORDER_STATUS_CHANGED;

    // OrdersHub
    public static final String ORDERS_HUB_PROXY = "ordersHub";
    public static final String HUB_UPDATE_ORDERS_LIST =  "updateOrders";
    public static final String HUB_ADDED_ORDER =  "addedOrder";
    public static final String HUB_CANCELLED_ORDER =  "cancelledOrder";
    public static final String HUB_ASSIGNED_ORDER =  "assignedOrder";
    public static final String HUB_UPDATED_ORDER =  "updatedOrder";
    // Hub broadcasts
    public static final String HUB_ORDERS_UPDATED_BC = PACKAGE_NAME + HUB_UPDATE_ORDERS_LIST;
    public static final String HUB_ADDED_ORDER_BC = PACKAGE_NAME +  HUB_ADDED_ORDER;
    public static final String HUB_CANCELLED_ORDER_BC = PACKAGE_NAME +  HUB_CANCELLED_ORDER;
    public static final String HUB_ASSIGNED_ORDER_BC = PACKAGE_NAME +  HUB_ASSIGNED_ORDER;
    public static final String HUB_UPDATED_ORDER_BC = PACKAGE_NAME +  HUB_UPDATED_ORDER;
    // Broadcasts reaction
    public static final String STOP_ORDERS_HUB_BC = PACKAGE_NAME + ".STOP_ORDERS_HUB_BC";

    public static final String ORDER_ID = PACKAGE_NAME +  ".ORDER_ID";
    public static final String DISTRICT_ID =  PACKAGE_NAME + ".DISTRICT_ID";

    // Connection timeouts in milliseconds
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECT_TIMEOUT = 15000;
    public static final int WRITE_TIMEOUT = 5000;



    //LOCATION SERVICE
    public static final String LOCATION_UPDATED = PACKAGE_NAME + ".LOCATION_UPDATED";
    public static final String LOCATION = PACKAGE_NAME + ".LOCATION";
    public static final String LOCATION_ACCURACY = PACKAGE_NAME + ".LOCATION_ACCURACY";
    public static final float LOCATION_ACCURACY_THRESHOLD = 20; // meters
    public static final float LOCATION_PICKUP_DISTANCE_THRESHOLD = 100; // meters

    // Report location on change threshold
    public static final float LOCATION_REST_REPORT_THRESHOLD = 200;

    // Times
    public static final int LOCATION_UPDATE_INTERVAL = 10000; // milliseconds
    public static final int LOCATION_UPDATE_DISTANCE = 1; // meters
    public static final int LOCATION_TIMEOUT = 1000 * 60 * 5; // five minutes;

    public static final String USER_LOCATIONS =  PACKAGE_NAME + ".USER_LOCATIONS";

    // USER PREFERENCES
    public static final String TOKEN_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    public static final String USER_DATA = "UserData";
    public static final String LOGIN_DATA = "LoginData";
    public static final String IS_LOGGED = "isLogged";
    public static final String ORDER_DATA = "OrderData";
    public static final String IS_IN_ORDER = "isInOrder";
    public static final String LAST_ORDER_ID = PACKAGE_NAME + ".lastOrderId";
    public static final String TRACKING_ENABLED = PACKAGE_NAME + "trackingEnabled";

    public static final String ASSIGNED_TAXI =  PACKAGE_NAME + ".ASSIGNED_TAXI";
    public static final String ASSIGNED_TAXI_ID =  PACKAGE_NAME + ".ASSIGNED_TAXI_ID";
    public static final String ASSIGNED_TAXI_PLATE = PACKAGE_NAME + ".TAXI_PLATE";

    public static final String ASSIGNED_ORDER_BC =  PACKAGE_NAME + ".ASSIGNED_TAXI";


    // UI - no UI strings should be here!

    // DEBUGGING STRINGS


    public enum TaxiStatus {
        Available(0), Busy(1), OffDuty(2), Unassigned(3), Decommissioned(4);
        private final int value;
        private TaxiStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum OrderStatus {
        Unassigned(0), Waiting(1), InProgress(2), Finished(3), Cancelled(4);
        private final int value;
        private OrderStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}