package com.example.cyberqrscan.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.R;

import java.util.ArrayList;

public class GenerateQRResult extends Fragment {
    LinearLayout layout ;
    LinearLayout inputLayout ;
    Button submit ;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_generateqrresult, container, false);

        layout = view.findViewById(R.id.dynamicLayout) ;
        submit = view.findViewById(R.id.submit) ;
        inputLayout = view.findViewById(R.id.inputLayout) ;



        return view;
    }
    public void addInputs(ArrayList <String> array){
        layout.removeAllViews() ;
        for(int i = 1 ; i <= array.size ; i++){

        }
    }
}
