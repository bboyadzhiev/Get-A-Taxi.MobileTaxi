package com.getataxi.mobiletaxi.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.getataxi.mobiletaxi.OrderAssignmentActivity;
import com.getataxi.mobiletaxi.R;


public class OrderDetailsFragment extends Fragment {

    OrderDetailsFragment f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_order_details, container, false);
        f = this;

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                OrderDetailsFragment  orderDetailsFragment = (OrderDetailsFragment)getFragmentManager()
//                        .findFragmentById(R.id.orderDetailsFragment);
                getFragmentManager()
                        .beginTransaction()
                        .hide(f)
                        .commit();
                Activity act = getActivity();
                if (act instanceof OrderAssignmentActivity) {
                    ((OrderAssignmentActivity) act).disableAssignButton();
                }
            }
        });
        return v;
    }

}
