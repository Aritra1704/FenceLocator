package com.arpaul.fencelocator.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arpaul.fencelocator.R;
import com.arpaul.fencelocator.activity.BaseActivity;
import com.arpaul.fencelocator.dataObject.PrefLocationDO;
import com.arpaul.utilitieslib.CalendarUtils;

import java.util.ArrayList;

/**
 * Created by Aritra on 23-06-2016.
 */
public class GeoLocationsAdapter extends RecyclerView.Adapter<GeoLocationsAdapter.ViewHolder> {

    private Context context;
    private ArrayList<PrefLocationDO> arrTours = new ArrayList<>();

    public GeoLocationsAdapter(Context context, ArrayList<PrefLocationDO> arrTours) {
        this.context = context;
        this.arrTours = arrTours;
    }

    public void refresh(ArrayList<PrefLocationDO> arrTours) {
        this.arrTours = arrTours;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_cell_tours, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final PrefLocationDO objTourDO = arrTours.get(position);

        holder.tvTourName.setText(objTourDO.LocationName);
        holder.tvTourDesc.setText(objTourDO.Address);

        ((BaseActivity)context).applyTypeface(((BaseActivity)context).getParentView(holder.mView),((BaseActivity)context).tfRegular, Typeface.NORMAL);
    }

    @Override
    public int getItemCount() {
        if(arrTours != null)
            return arrTours.size();

        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView tvTourName;
        public final TextView tvTourDesc;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            tvTourName                  = (TextView) view.findViewById(R.id.tvLocationName);
            tvTourDesc                  = (TextView) view.findViewById(R.id.tvLocationAddress);
        }

        @Override
        public String toString() {
            return "";
        }
    }
}
