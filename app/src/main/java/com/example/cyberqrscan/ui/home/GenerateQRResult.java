package com.example.cyberqrscan.ui.home;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class GenerateQRResult extends AppCompatActivity {
    LinearLayout layout  , saveAndshareLayout;
    LinearLayout inputLayout ;
    Button submit  , save , share ;
    ImageView qr ;
    QRDatabase database ;
    SharedPreferences preferences ;
    List<TextInputEditText> dynamicEditTexts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generateqrresult);
        layout = findViewById(R.id.dynamicLayout) ;
        submit = findViewById(R.id.submit) ;
        inputLayout = findViewById(R.id.inputLayout) ;
        saveAndshareLayout = findViewById(R.id.saveAndshareLayout) ;
        save = findViewById(R.id.save) ;
        share = findViewById(R.id.share) ;
        qr = findViewById(R.id.qr) ;

        saveAndshareLayout.setVisibility(View.INVISIBLE);
        preferences = PreferenceManager.getDefaultSharedPreferences(GenerateQRResult.this);

        Intent intent = getIntent() ;
        String type = intent.getStringExtra("type") ;
        List <String> array = intent.getStringArrayListExtra("array") ;
        List<String> inputValues = new ArrayList<>();

        addInputs(array);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (TextInputEditText editText : dynamicEditTexts) {
                    inputValues.add(editText.getText().toString().trim());
                }
                String data = formatQRString(type , inputValues) ;
                generateQR(data);
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageToGallery();
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareQRCode(qr);
            }
        });
    }
    public void generateQR(String data){
        if (data != null) {
            try {
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
                qr.setImageBitmap(bitmap);
                saveAndshareLayout.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(GenerateQRResult.this , "QR generation failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void addInputs(List <String> array){
        layout.removeAllViews() ;
        dynamicEditTexts.clear();
        LayoutInflater inflater = LayoutInflater.from(GenerateQRResult.this) ;
        for(int i = 0 ; i < array.size() ; i++){
            View inputView = inflater.inflate(R.layout.inputs ,layout, false) ;
            TextInputLayout textInputLayout = inputView.findViewById(R.id.textInputLayout);
            TextInputEditText editText = inputView.findViewById(R.id.editText);

            dynamicEditTexts.add(editText);
            editText.setHint(array.get(i));
            layout.addView(inputView);
        }
    }
    private String formatQRString(String type, List<String> values) {
        database = new QRDatabase(GenerateQRResult.this) ;
        switch (type) {
            case "url":
                database.insertData("URL" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return values.get(0).startsWith("http") ? values.get(0) : "https://" + values.get(0);

            case "phone":
                database.insertData("Phone Number" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return "tel:+91" + values.get(0);

            case "mail":
                database.insertData("Email" , values.get(0) + " " + values.get(1) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return "mailto:" + values.get(0) + "?subject=" + values.get(1) + "&body=" + values.get(2);

            case "plain":
                database.insertData("Text" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return values.get(0);

            case "wifi":
                database.insertData("WiFi Details" , values.get(1) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return "WIFI:T:" + values.get(0) + ";S:" + values.get(1) + ";P:" + values.get(2) + ";;";

            case "sms":
                database.insertData("SMS" , values.get(0) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return "SMSTO:" + values.get(0) + ":" + values.get(1);

            case "location":
                database.insertData("Geographical Co-ordinates" , values.get(0) + " " + values.get(1) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
                return "geo:" + values.get(0) + "," + values.get(1);

            case "contact":
                database.insertData("Contact Info" , values.get(0) + " " + values.get(2) + " " + values.get(4) , System.currentTimeMillis() , QRDatabase.generateTable , checkSaveDatabasePermission(preferences)) ;
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

    private static Boolean checkSaveDatabasePermission(SharedPreferences preferences) {
        if (preferences.getBoolean("prefSaveHistory",false)){
            return false ;
        }
        else {
            return true ;
        }
    }

    private void saveImageToGallery() {
        OutputStream fos;
        qr.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) qr.getDrawable()).getBitmap();
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "qr_code_" + System.currentTimeMillis() + ".png");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CyberQRScan");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                fos = resolver.openOutputStream(imageUri);
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File image = new File(imagesDir, "qr_code_" + System.currentTimeMillis() + ".png");
                fos = new FileOutputStream(image);
            }

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Toast.makeText(this, "QR saved to Gallery!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
    private void shareQRCode(ImageView imageView) {
        try {
            // Get Bitmap from ImageView
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

            // Save to cache directory
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); // create folder if not exists
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get content URI with FileProvider
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider", // must match provider in Manifest
                    file
            );

            if (contentUri != null) {
                // Create share intent
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // permission for apps
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }



}
