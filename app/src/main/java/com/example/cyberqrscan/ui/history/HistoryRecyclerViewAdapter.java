package com.example.cyberqrscan.ui.history;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cyberqrscan.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class HistoryRecyclerViewAdapter extends RecyclerView.Adapter<HistoryRecyclerViewAdapter.HistoryViewHolder> {
    ArrayList<QRHistoryData> dataList;
    Context context;

    public HistoryRecyclerViewAdapter(Context context, ArrayList<QRHistoryData> dataList) {
        this.context = context;
        this.dataList = dataList;
        for (QRHistoryData data: dataList) {
            Log.d("Binding","RV Type: " + data.getType() + " Data: " + data.getData() + " Time: " + data.getCreationTime());
        }
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HistoryViewHolder(LayoutInflater.from(context).inflate(R.layout.qr_history_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.tvType.setText("Type: "+dataList.get(position).getType());
        holder.tvData.setText("Data: "+dataList.get(position).getData());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm aa");
        String time = "Time: "+sdf.format(dataList.get(position).getCreationTime());
        holder.tvTime.setText(time);
        Log.d("Binding","Binding Type: " + dataList.get(position).getType() + " Data: " + dataList.get(position).getData() + " Time: " + dataList.get(position).getCreationTime());

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvData, tvTime;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvQRType);
            tvData = itemView.findViewById(R.id.tvQRData);
            tvTime = itemView.findViewById(R.id.tvQRTime);
        }
    }

}
