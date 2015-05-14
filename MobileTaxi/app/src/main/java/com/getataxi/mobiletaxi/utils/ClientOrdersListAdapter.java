package com.getataxi.mobiletaxi.utils;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.getataxi.mobiletaxi.R;
import com.getataxi.mobiletaxi.comm.models.OrderDM;

import java.util.List;

/**
 * Created by bvb on 13.5.2015 ã..
 */
public class ClientOrdersListAdapter extends ArrayAdapter<OrderDM> {

    List<OrderDM> orders;
    Context context;
    int currentPosition;

    public ClientOrdersListAdapter(Context context, int resource, List<OrderDM> objects) {
        super(context, resource, objects);
        this.orders = objects;
        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolderItem holder;

        if (convertView == null) {
            holder = new ViewHolderItem();

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.fragment_order_list_item, parent,
                    false);

            holder.orderAddress = (TextView) convertView
                    .findViewById(R.id.orderAddress);
            holder.orderDestination = (TextView) convertView
                    .findViewById(R.id.orderDestination);
            holder.clientComment = (TextView) convertView
                    .findViewById(R.id.clientComment);
            convertView.setTag(holder);
            this.currentPosition = position;

        } else {
            holder = (ViewHolderItem) convertView.getTag();
        }

        holder.orderAddress.setText(this.orders.get(position).orderAddress);
        holder.orderDestination.setText(Html.fromHtml("<small><small>"
                + this.orders.get(position).destinationAddress + "</small></small>"));
        holder.clientComment.setText(Html.fromHtml("<small><small>"
                + this.orders.get(position).userComment + "</small></small>"));

        return convertView;
    }

    static class ViewHolderItem {
        TextView orderAddress;
        TextView orderDestination;
        TextView clientComment;
    }


}
