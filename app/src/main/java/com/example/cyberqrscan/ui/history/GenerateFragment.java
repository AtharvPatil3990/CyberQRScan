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

public class GenerateFragment extends Fragment {

    RecyclerView recyclerView;
    Button clear;
    ArrayList<QRHistoryData> list;
    QRDatabase database;
    HistoryRecyclerViewAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewGenerate);
        clear = view.findViewById(R.id.btnGenerateClear);

        database = new QRDatabase(requireContext());

        // initialize adapter with data
        list = database.getAllGenerate();
        adapter = new HistoryRecyclerViewAdapter(requireContext(), list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        for (QRHistoryData data: list) {
            Log.d("Binding","GenerateFrag Type: " + data.getType() + " Data: " + data.getData() + " Time: " + data.getCreationTime());
        }
        // clear button
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.deleteAll(QRDatabase.generateTable);
                int size = list.size();
                list.clear();
                adapter.notifyItemRangeRemoved(0, size);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        list.clear();
        // 2. Get the fresh, updated list from the database
        list.addAll(database.getAllGenerate());
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