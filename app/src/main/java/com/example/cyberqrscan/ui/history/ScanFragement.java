package com.example.cyberqrscan.ui.history;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;

import java.util.ArrayList;

public class ScanFragement extends Fragment {

    RecyclerView recyclerView;
    Button clear;
    ArrayList<QRHistoryData> list;
    QRDatabase database;
    HistoryRecyclerViewAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        recyclerView = view.findViewById(R.id.listViewScan);
        clear = view.findViewById(R.id.btnScanClear);

        database = new QRDatabase(requireContext());

        // Initialize adapter with empty list first
        list = new ArrayList<>();
        list = database.getAllScan();

        adapter = new HistoryRecyclerViewAdapter(requireContext(), list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        for (QRHistoryData data: list) {
            Log.d("Binding","ScanFrag Scanned Type " + data.getType() + " Data: " + data.getData() + " Time: " + data.getCreationTime());
        }
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.deleteAll(QRDatabase.scanTable);
                int size = list.size();
                list.clear();
                adapter.notifyItemRangeRemoved(0, size);
            }
        });
        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        list.clear();
        // 2. Get the fresh, updated list from the database
        list.addAll(database.getAllScan());
        // 3. Notify the adapter that the data has completely changed
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }
}
