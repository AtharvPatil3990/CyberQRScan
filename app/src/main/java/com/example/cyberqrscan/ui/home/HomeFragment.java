package com.example.cyberqrscan.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class HomeFragment extends Fragment {

    private MaterialButton btnScan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnScan = view.findViewById(R.id.btnScan);

        btnScan.setOnClickListener(v -> {
            // Request camera permission
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        });

        return view;
    }

    // Launcher for requesting camera permission
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, start scanning
                    startBarcodeScanner();
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    private void startBarcodeScanner() {
        // Use requireActivity() to pass context instead of "this"
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(requireActivity());

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String scannedValue = barcode.getRawValue();
                    if (barcode.getValueType() == Barcode.TYPE_URL && scannedValue != null) {
                        Intent loadPage = new Intent(Intent.ACTION_VIEW, Uri.parse(scannedValue));
                        startActivity(loadPage);
                    } else {
                        Toast.makeText(requireContext(), "Scanned: " + scannedValue, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(requireContext(), "Scan canceled.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
