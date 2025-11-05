package com.example.cyberqrscan.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;

import java.util.ArrayList;
import java.util.List;

public class ScanFragement extends Fragment {

    ListView listView;
    Button clear;
    List<String> list;
    QRDatabase database;
    ArrayAdapter<String> adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        listView = view.findViewById(R.id.listViewScan);
        clear = view.findViewById(R.id.btnScanClear);

        database = new QRDatabase(requireContext());

        // Initialize adapter with empty list first
        list = new ArrayList<>();
        list = database.getAllScan() ;
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        refreshData(); // load actual data once
        System.out.println(list);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.deleteAll(QRDatabase.scanTable);
                list.clear(); // clear local list
                adapter.notifyDataSetChanged();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();  // reload data when coming back
    }
    private void refreshData() {
        list.clear();
        list.addAll(database.getAllScan());
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
