package com.example.cyberqrscan.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;
import android.view.View ;

import java.util.ArrayList;

public class GenerateQR extends AppCompatActivity {
    Button url , phone , mail , plain ,wifi , sms  , location , contact;
    QRDatabase database ;
    ArrayList <String> array ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_generateqr);
        url = findViewById(R.id.btnUrl) ;
        phone = findViewById(R.id.btnphone) ;
        mail = findViewById(R.id.btnemail) ;
        plain = findViewById(R.id.btnplain) ;
        wifi = findViewById(R.id.btnwifi) ;
        sms = findViewById(R.id.btnSMS) ;
        location = findViewById(R.id.btnGeo) ;
        contact = findViewById(R.id.btnvCard) ;

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
            array = new ArrayList<String>() ;
            Intent intent = new Intent(GenerateQR.this , GenerateQRResult.class) ;
           switch(tag){
               case "url" :
                   array.add("Url : ") ;
                   intent.putExtra("type" , "url") ;
                   break ;
               case "phone" :
                   array.add("Phone Number : ") ;
                   intent.putExtra("type" , "phone") ;
                   break ;
               case "mail" :
                   array.add("Mail ID : ");
                   array.add("Subject : ") ;
                   array.add("Message : ") ;
                   intent.putExtra("type" , "mail") ;
                   break ;
               case "plain" :
                   array.add("Text : ") ;
                   intent.putExtra("type" , "plain") ;
                   break ;
               case "wifi" :
                   array.add("WiFi Type : ") ;
                   array.add("WiFi Name : ") ;
                   array.add("WiFi Password : ") ;
                   intent.putExtra("type" , "wifi") ;
                   break ;
               case "sms" :
                   array.add("Mobile Number : ") ;
                   array.add("Message : ") ;
                   intent.putExtra("type" , "sms") ;
                   break ;
               case "location" :
                   array.add("Latitude : ") ;
                   array.add("Longitude : ") ;
                   intent.putExtra("type" , "location") ;
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
                   intent.putExtra("type" , "contact") ;
                   break ;
           }
           intent.putStringArrayListExtra("array" , array) ;
           startActivity(intent);
        } ;
        url.setOnClickListener(listener);
        phone.setOnClickListener(listener);
        mail.setOnClickListener(listener);
        plain.setOnClickListener(listener);
        wifi.setOnClickListener(listener);
        sms.setOnClickListener(listener);
        location.setOnClickListener(listener);
        contact.setOnClickListener(listener);
    }
}
