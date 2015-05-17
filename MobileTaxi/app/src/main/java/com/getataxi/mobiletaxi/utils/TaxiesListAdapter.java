package com.getataxi.mobiletaxi.utils;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.getataxi.mobiletaxi.R;
import com.getataxi.mobiletaxi.comm.models.TaxiDetailsDM;

import java.util.List;

/**
 * Created by bvb on 13.5.2015..
 */
public class TaxiesListAdapter extends ArrayAdapter<TaxiDetailsDM> {

    List<TaxiDetailsDM> taxies;
    Context context;
    int currentPosition;

    public TaxiesListAdapter(Context context, int resource, List<TaxiDetailsDM> objects) {
        super(context, resource, objects);
        this.taxies = objects;
        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolderItem holder;

        if (convertView == null) {
            holder = new ViewHolderItem();

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.fragment_taxi_list_item, parent,
                    false);

            holder.taxiId = (TextView) convertView
                    .findViewById(R.id.taxiId);
            holder.taxiPlate = (TextView) convertView
                    .findViewById(R.id.taxiPlate);
            convertView.setTag(holder);
            this.currentPosition = position;

        } else {
            holder = (ViewHolderItem) convertView.getTag();
        }

        holder.taxiId.setText(this.taxies.get(position).taxiId);
        holder.taxiPlate.setText(this.taxies.get(position).plate);


        return convertView;
    }

    static class ViewHolderItem {
        TextView taxiId;
        TextView taxiPlate;
    }


}
