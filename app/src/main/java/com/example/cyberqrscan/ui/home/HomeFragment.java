package com.example.cyberqrscan.ui.home;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.R;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.Calendar;

public class HomeFragment extends Fragment {
    private ActivityResultLauncher<Intent> galleryLauncher;

    private MaterialButton btnScan , btnUploadQR, btnGenerateQR;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnScan = view.findViewById(R.id.btnScan);
        btnUploadQR = view.findViewById(R.id.btnUploadQR);
        btnGenerateQR = view.findViewById(R.id.btnGenerate);

        btnScan.setOnClickListener(v -> {
            // Request camera permission
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        });

        btnUploadQR.setOnClickListener(v -> {
            openGallery();
        });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();

                        // Implement the code for Local database Storage of code and show the appropriate data
                    }
                }
        );
        return view;
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // Launcher for requesting camera permission
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, start scanning
                    startBarcodeScanner();
                }
                else {
                    Toast.makeText(requireContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    private void startBarcodeScanner() {
        // Use requireActivity() to pass context instead of "this"
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(requireActivity());

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String scannedValue = barcode.getRawValue();
                    Intent intent ;
                    switch(barcode.getValueType()){
                        case Barcode.TYPE_URL :
                            startActivity(new Intent(Intent.ACTION_VIEW , Uri.parse(scannedValue)));
                            break ;
                        case Barcode.TYPE_TEXT :
                            copyData(scannedValue);
                            intent = new Intent(requireContext(), QRData.class) ;
                            intent.putExtra("type" , "Text : ") ;
                            intent.putExtra("data",scannedValue);
                            startActivity(intent);
                            break ;
                        case Barcode.TYPE_EMAIL:
                            intent = new Intent (Intent.ACTION_SENDTO , Uri.parse("mailto:")) ;
                            startActivity(intent);
                            break ;
                        case Barcode.TYPE_PHONE:
                            intent = new Intent (Intent.ACTION_DIAL , Uri.parse("tel:+91"+scannedValue)) ;
                            startActivity(intent) ;
                            break ;
                        case Barcode.TYPE_SMS:
                            intent = new Intent (requireContext() , QRData.class) ;
                            intent.putExtra("type" , "SMS") ;
                            intent.putExtra("data" , scannedValue) ;
                            startActivity(intent);
                            break ;
                        case Barcode.TYPE_WIFI:
                           connectWifi(barcode);
                           break ;
                        case Barcode.TYPE_GEO :
                            loadMap(barcode);
                            break ;
                        case Barcode.TYPE_CALENDAR_EVENT :
                            loadCalender(barcode);
                            break ;
                        case Barcode.TYPE_ISBN :
                            String url = "https://www.google.com/search?q=ISBN+" + scannedValue;
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                            break ;
                        case Barcode.TYPE_DRIVER_LICENSE :
                            loadDrivingLiscenceInfo(barcode);
                            break ;
                    }
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(requireContext(), "Scan canceled.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Scan failed: Please try again", Toast.LENGTH_SHORT).show();
                });
    }
    public void connectWifi(Barcode barcode){
        Barcode.WiFi wifi = barcode.getWifi();

        String ssid = wifi.getSsid();
        String password = wifi.getPassword();
        int encryption = wifi.getEncryptionType();

        WifiNetworkSpecifier specifier = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();
        }

        NetworkRequest request = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Toast.makeText(requireContext(), "WiFi Connected !", Toast.LENGTH_SHORT).show();
            }
            public void onUnavailable() {
                Toast.makeText(requireContext(), "Failed to connect Wi-Fi", Toast.LENGTH_SHORT).show();
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);
    }
    public void copyData(String scannedValue){
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QR Code", scannedValue);
        clipboard.setPrimaryClip(clip);
    }
    public void loadMap(Barcode barcode){
        if (barcode.getValueType() == Barcode.TYPE_GEO) {
            Barcode.GeoPoint geoPoint = barcode.getGeoPoint();
            double lat = geoPoint.getLat();
            double lng = geoPoint.getLng();

            // Build URI to launch Google Maps
            String uri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");

            // Check if Google Maps is installed
            if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(getContext(), "Google Maps not found", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void loadCalender(Barcode barcode){
        if (barcode.getValueType() == Barcode.TYPE_CALENDAR_EVENT) {
            Barcode.CalendarEvent event = barcode.getCalendarEvent();

            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.Events.TITLE, event.getSummary());
            intent.putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription());
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation());

            Calendar startCal = getCalendarFromEventDate(event.getStart());
            Calendar endCal = getCalendarFromEventDate(event.getEnd());

            if (startCal != null) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.getTimeInMillis());
            }
            if (endCal != null) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.getTimeInMillis());
            }

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "No calendar app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Calendar getCalendarFromEventDate(Barcode.CalendarDateTime eventDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear(); // Clear all fields

        if (eventDate != null) {
            calendar.set(eventDate.getYear(),
                    eventDate.getMonth() - 1,  // Month is 0-based in Calendar
                    eventDate.getDay(),
                    eventDate.getHours(),
                    eventDate.getMinutes(),
                    eventDate.getSeconds());
        }
        return calendar;
    }

    public void loadDrivingLiscenceInfo(Barcode barcode){
        if (barcode.getValueType() == Barcode.TYPE_DRIVER_LICENSE) {
            Barcode.DriverLicense license = barcode.getDriverLicense();

            // Extract information
            String firstName = license.getFirstName();
            String middleName = license.getMiddleName();
            String lastName = license.getLastName();
            String gender = license.getGender();
            String birthDate = license.getBirthDate();
            String licenseNumber = license.getLicenseNumber();
            String issueDate = license.getIssueDate();
            String expiryDate = license.getExpiryDate();
            String addressCity = license.getAddressCity();
            String addressState = license.getAddressState();
            String addressStreet = license.getAddressStreet();
            String addressZip = license.getAddressZip();

            // For example, show in a Toast or pass to another Activity
            String info = "Name: " + firstName + " " + middleName + " " + lastName + "\n" +
                    "License Number: " + licenseNumber + "\n" +
                    "DOB: " + birthDate + "\n" +
                    "Expires: " + expiryDate;

            Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();

            // Or start an activity to display details, or save data, etc.
        }
    }
    public void OnAutoCopyDialogue(String title , String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("title")
                .setMessage("message")
                .setPositiveButton("Open App", (dialog, which) -> {
                    // Handle Yes click
                    Toast.makeText(getContext(), "Clicked Yes", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", (dialog, which) -> {
                    // Handle No click
                    Toast.makeText(getContext(), "Clicked No", Toast.LENGTH_SHORT).show();
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }
    public void OffAutoCopyDialogue(String title , String message ){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Open App", (dialog, which) -> {
                    // Handle Yes click
                    Toast.makeText(getContext(), "Clicked Yes", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", (dialog, which) -> {
                    // Handle No click
                    Toast.makeText(getContext(), "Clicked No", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Copy", (dialog, which) -> {
                    // Handle Maybe click
                    Toast.makeText(getContext(), "Clicked Maybe", Toast.LENGTH_SHORT).show();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void checkAutoCopy(){

    }
}
