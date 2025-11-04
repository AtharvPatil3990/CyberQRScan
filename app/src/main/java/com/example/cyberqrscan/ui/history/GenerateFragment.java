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

public class GenerateFragment extends Fragment {

    ListView listView;
    Button clear;
    List<String> list;
    QRDatabase database;
    ArrayAdapter<String> adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        listView = view.findViewById(R.id.listViewGenerate);
        clear = view.findViewById(R.id.btnGenerateClear);

        database = new QRDatabase(requireContext());

        // initialize adapter with data
        // initialize adapter with empty list first
        list = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);

// load data from DB
        refreshData();

        System.out.println(list) ;
        // clear button
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.deleteAll(QRDatabase.generateTable);
                list.clear(); // also clear local list
                adapter.notifyDataSetChanged();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData(); // reload latest data when tab is opened
    }
    private void refreshData() {
        list.clear();
        list.addAll(database.getAllGenerate()); // fetch latest DB entries
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