package com.example.cyberqrscan.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.R;
import android.view.View ;

import java.util.ArrayList;

public class GenerateQR extends Fragment {
    Button url , phone , mail , plain ,wifi , sms  , location , contact;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generateqr, container, false);
        url = view.findViewById(R.id.btnUrl) ;
        phone = view.findViewById(R.id.btnphone) ;
        mail = view.findViewById(R.id.btnemail) ;
        plain = view.findViewById(R.id.btnplain) ;
        wifi = view.findViewById(R.id.btnwifi) ;
        sms = view.findViewById(R.id.btnSMS) ;
        location = view.findViewById(R.id.btnGeo) ;
        contact = view.findViewById(R.id.btnvCard) ;

        url.setTag("url");
        phone.setTag("phone");
        mail.setTag("mail");
        plain.setTag("plain");
        wifi.setTag("wifi");
        sms.setTag("sms");
        location.setTag("location");
        contact.setTag("contact");

        View.OnClickListener listener = view1 -> {
           String tag = (String) view1.getTag() ;
            ArrayList <String> array = new ArrayList<String>() ;
           switch(tag){
               case "url" :
                   array.add("Url : ") ;
                   break ;
               case "phone" :
                   array.add("Phone Number : ") ;
                   break ;
               case "mail" :
                   array.add("Mail ID : ");
                   array.add("Subject : ") ;
                   array.add("Message : ") ;
                   break ;
               case "plain" :
                   array.add("Text : ") ;
                   break ;
               case "wifi" :
                   array.add("WiFi Type : ") ;
                   array.add("WiFi Name : ") ;
                   array.add("WiFi Password : ") ;
                   break ;
               case "sms" :
                   array.add("Mobile Number : ") ;
                   array.add("Message : ") ;
                   break ;
               case "location" :
                   array.add("Latitude : ") ;
                   array.add("Longitude : ") ;
                   break ;
               case "contact" :
                   array.add("Name : ") ;
                   array.add("Organization Name : ") ;
                   array.add("Title : ") ;
                   array.add("Office Contact : ") ;
                   array.add("Personal Contact : ") ;
                   array.add("Mail ID : ") ;
                   array.add("Address : ") ;
                   array.add("URL : ") ;
                   break ;
           }
        } ;
        return view ;
    }
}
