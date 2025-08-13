package com.example.cyberqrscan.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.List;

public class GenerateQRResult extends AppCompatActivity {
    LinearLayout layout ;
    LinearLayout inputLayout ;
    Button submit ;
    ImageView qr ;
    QRDatabase database ;
    @Nullable
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generateqrresult);
        layout = findViewById(R.id.dynamicLayout) ;
        submit = findViewById(R.id.submit) ;
        inputLayout = findViewById(R.id.inputLayout) ;
        qr = findViewById(R.id.qr) ;

        Intent intent = getIntent() ;
        List <String> array = intent.getStringArrayListExtra("array") ;
        String type = intent.getStringExtra("type") ;
        List<String> inputValues = new ArrayList<>();

        addInputs(array);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextInputEditText[] dynamicEditTexts = new TextInputEditText[0];
                for (TextInputEditText editText : dynamicEditTexts) {
                    inputValues.add(editText.getText().toString().trim());
                }
                String data = formatQRString(type , inputValues) ;
            }
        });
    }
    public void generateQR(String data){
        if (data != null) {
            try {
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
                qr.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(GenerateQRResult.this , "QR generation failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void addInputs(List <String> array){
        layout.removeAllViews() ;
        LayoutInflater inflater = LayoutInflater.from(GenerateQRResult.this) ;
        for(int i = 0 ; i < array.size() ; i++){
            View inputView = inflater.inflate(R.layout.inputs ,layout, false) ;
            TextInputLayout textInputLayout = inputView.findViewById(R.id.textInputLayout);
            TextInputEditText editText = inputView.findViewById(R.id.editText);

            editText.setHint(array.get(i));
            layout.addView(inputView);
        }
    }
    private String formatQRString(String type, List<String> values) {
        database = new QRDatabase(GenerateQRResult.this) ;
        switch (type) {
            case "url":
                database.insertData("URL" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return values.get(0).startsWith("http") ? values.get(0) : "https://" + values.get(0);

            case "phone":
                database.insertData("Phone Number" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return "tel:" + values.get(0);

            case "mail":
                database.insertData("Email" , values.get(0) + " " + values.get(1) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return "mailto:" + values.get(0) + "?subject=" + values.get(1) + "&body=" + values.get(2);

            case "plain":
                database.insertData("Text" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return values.get(0);

            case "wifi":
                database.insertData("WiFi Details" , values.get(1) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return "WIFI:T:" + values.get(0) + ";S:" + values.get(1) + ";P:" + values.get(2) + ";;";

            case "sms":
                database.insertData("SMS" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return "SMSTO:" + values.get(0) + ":" + values.get(1);

            case "location":
                database.insertData("Geographical Co-ordinates" , values.get(0) + " " + values.get(1) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return "geo:" + values.get(0) + "," + values.get(1);

            case "contact":
                database.insertData("Contact Info" , values.get(0) + " " + values.get(2) + " " + values.get(4) , System.currentTimeMillis() , QRDatabase.generateTable) ;
                return "BEGIN:VCARD\n" +
                        "VERSION:3.0\n" +
                        "N:" + values.get(0) + "\n" +
                        "ORG:" + values.get(1) + "\n" +
                        "TITLE:" + values.get(2) + "\n" +
                        "TEL;TYPE=WORK,VOICE:" + values.get(3) + "\n" +
                        "TEL;TYPE=CELL,VOICE:" + values.get(4) + "\n" +
                        "EMAIL:" + values.get(5) + "\n" +
                        "ADR:" + values.get(6) + "\n" +
                        "URL:" + values.get(7) + "\n" +
                        "END:VCARD";

            default:
                return null;
        }
    }

}
