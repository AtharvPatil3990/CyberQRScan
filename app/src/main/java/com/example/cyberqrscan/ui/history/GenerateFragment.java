package com.example.cyberqrscan.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;

import java.util.List;

public class GenerateFragment extends Fragment {
    ListView listView ;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listView = view.findViewById(R.id.listView) ;
        QRDatabase database = new QRDatabase(getContext());
        List list = database.getAllScan() ;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);

        return view ;
    }
}
