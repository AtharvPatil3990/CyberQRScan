package com.example.cyberqrscan.ui.home;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cyberqrscan.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;  // <--- This line seems wrong or incomplete


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

        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();

    }
}
