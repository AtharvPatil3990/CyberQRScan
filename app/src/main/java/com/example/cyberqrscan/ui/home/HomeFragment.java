package com.example.cyberqrscan.ui.home;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.Log;
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
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cyberqrscan.QRDatabase;
import com.example.cyberqrscan.R;

import com.example.cyberqrscan.ui.ImageAdapter;
import com.example.cyberqrscan.SafeBrowsingAPI;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
        // ViewPager Setup
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        // Add your images (from drawable)
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.app);
        imageList.add(R.drawable.files);
        imageList.add(R.drawable.mail);

        ImageAdapter adapter = new ImageAdapter(requireContext(), imageList);
        viewPager.setAdapter(adapter);

        // Auto-slide every 3 seconds
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = viewPager.getCurrentItem();
                int nextItem = (currentItem + 1) % imageList.size();
                viewPager.setCurrentItem(nextItem, true);
                handler.postDelayed(this, 3000); // 3 sec delay
            }
        };
        handler.postDelayed(runnable, 3000);


        btnGenerateQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext() , GenerateQR.class) ;
                startActivity(intent);
            }
        });

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
                        scanBarcodeFromImage(selectedImageUri);
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
                    selectType(barcode , QRDatabase.scanTable);
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(requireContext(), "Scan canceled.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Scan failed: Please try again", Toast.LENGTH_SHORT).show();
                });
    }

    public void selectType(Barcode barcode , String table){
        String scannedValue = barcode.getRawValue();
        Intent intent ;
        QRDatabase database = new QRDatabase(requireContext());
        AlertDialog.Builder builder ;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (preferences.getBoolean("prefBeepSound", false)) {
            // Beep
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
        }

        if (preferences.getBoolean("prefVibrationOnScan", false)) {
            // Vibrate
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                VibrationEffect effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            }
        }

        switch(barcode.getValueType()){
            case Barcode.TYPE_URL :
                assert scannedValue != null;
                openUrl(scannedValue);
                database.insertData("URL" , barcode.getRawValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_TEXT :
                copyData(scannedValue);
                builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Text Detected");
                builder.setMessage(barcode.getDisplayValue());
                builder.setPositiveButton("OK", null);
                builder.show();
                database.insertData("Text" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_EMAIL:
                intent = new Intent (Intent.ACTION_SENDTO , Uri.parse("mailto:")) ;
                String [] details = parseEmailFromQR(scannedValue) ;
                intent.putExtra(Intent.EXTRA_EMAIL, details[0]);
                intent.putExtra(Intent.EXTRA_SUBJECT, details[1]);
                intent.putExtra(Intent.EXTRA_TEXT, details[2]);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                database.insertData("Email" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_PHONE:
                intent = new Intent (Intent.ACTION_DIAL , Uri.parse("tel:+91"+scannedValue)) ;
                startActivity(intent) ;
                database.insertData("Phone Number" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_SMS:
                builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("SMS Detected");
                builder.setMessage("Phone Number: " + barcode.getPhone() + "\nMessage: " + barcode.getSms().getMessage());
                builder.setPositiveButton("OK", null);
                builder.show();
                database.insertData("SMS" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_WIFI:
                connectWifi(barcode);
                database.insertData("WiFi Details" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_GEO:
                loadMap(barcode);
                database.insertData("Geographical Co-ordinates" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_CALENDAR_EVENT:
                loadCalender(barcode);
                database.insertData("Calender" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_CONTACT_INFO:
                loadContacts(barcode);
                database.insertData("Contact Info" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_ISBN:
                searchISBN(barcode);
                database.insertData("ISBN" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_DRIVER_LICENSE:
                ViewDrivingLicense(barcode);
                database.insertData("Driving License" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            case Barcode.TYPE_PRODUCT:
                searchProduct(barcode);
                database.insertData("Product Info" , barcode.getDisplayValue() , System.currentTimeMillis() , table) ;
                break ;

            default:
                Toast.makeText(requireContext(), "Scan failed: Please try again", Toast.LENGTH_SHORT).show();
        }


    }

    private void showURLAlertBox(@NonNull String scannedValue, Context context){
        new AlertDialog.Builder(context)
                .setTitle("QR Code Result")
                .setMessage("Contains a " + scannedValue)
                .setPositiveButton("Open", (dialog, which) -> {
                    // Open URL
                    redirectUrl(scannedValue);
                })
                .setNegativeButton("Copy", (dialog, which) -> {
                    // Copy to clipboard
                    copyData(scannedValue);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void redirectUrl(String scannedValue){
        if(!scannedValue.startsWith("https://"))
            startActivity(new Intent(Intent.ACTION_VIEW , Uri.parse("https://"+scannedValue)));
        else if(!scannedValue.startsWith("http://"))
            startActivity(new Intent(Intent.ACTION_VIEW , Uri.parse("http://"+scannedValue)));
        else
            startActivity(new Intent(Intent.ACTION_VIEW , Uri.parse(scannedValue)));
    }

    private void openUrl(String scannedValue){
        SafeBrowsingAPI.checkURLSafety(scannedValue, new SafeBrowsingAPI.SafeCheckCallback() {
            @Override
            public void onResult(boolean isSafe) {
                if (isSafe) {
                    Toast.makeText(requireContext(), "Opening URL", Toast.LENGTH_SHORT).show();
                    redirectUrl(scannedValue);
                } else {
                    showURLAlertBox(scannedValue, requireContext());
                }
            }
        });

    }git

    private void scanBarcodeFromImage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            InputImage image = InputImage.fromBitmap(bitmap, 0);

            BarcodeScannerOptions options =
                    new BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build();

            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() > 0) {
                            for (Barcode barcode : barcodes) {
                                selectType(barcode , QRDatabase.generateTable); // Create this method to handle various types
                            }
                        } else {
                            Toast.makeText(requireContext(), "No barcode found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Failed to scan image", Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    public void searchProduct(Barcode barcode){
        String productCode = barcode.getDisplayValue();
        new AlertDialog.Builder(requireContext())
                .setTitle("Product Code Detected")
                .setMessage("Code: " + productCode + "\nSearch this product online?")
                .setPositiveButton("Search", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/search?q=" + productCode));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();

    }
    public void ViewDrivingLicense(Barcode barcode){
        Barcode.DriverLicense license = barcode.getDriverLicense();

        String name = license.getFirstName() + " " + license.getLastName();
        String gender = license.getGender();
        String licenseNumber = license.getLicenseNumber();
        String dob = license.getBirthDate();
        String issueDate = license.getIssueDate();
        String expiryDate = license.getExpiryDate();
        String address = license.getAddressStreet() + ", " +
                license.getAddressCity() + ", " +
                license.getAddressState() + ", " +
                license.getAddressZip();

        Log.d("DRIVER_LICENSE", "Name: " + name);
        Log.d("DRIVER_LICENSE", "Number: " + licenseNumber);
        Log.d("DRIVER_LICENSE", "DOB: " + dob);
        Log.d("DRIVER_LICENSE", "Address: " + address);
        Log.d("DRIVER_LICENSE", "Issue: " + issueDate + ", Expiry: " + expiryDate);

        // Optional: Show it in a dialog
        showLicenseDialog(name, gender, licenseNumber, dob, issueDate, expiryDate, address);
    }
    private void showLicenseDialog(String name, String gender, String number, String dob,
                                   String issue, String expiry, String address) {
        String message = "Name: " + name +
                "\nGender: " + gender +
                "\nLicense No: " + number +
                "\nDOB: " + dob +
                "\nIssued: " + issue +
                "\nExpires: " + expiry +
                "\nAddress: " + address;

        new AlertDialog.Builder(requireContext())
                .setTitle("Driver License Info")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public void searchISBN(Barcode barcode){
        String isbn = barcode.getDisplayValue();
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=ISBN+" + isbn));

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent); // works in Fragment
        } else {
            Toast.makeText(requireContext(), "No browser app found!", Toast.LENGTH_SHORT).show();
        }

    }
    public void loadContacts(Barcode barcode){
        Barcode.ContactInfo contact = barcode.getContactInfo();

        String name = contact.getName() != null ? contact.getName().getFormattedName() : "";
        String phone = (contact.getPhones() != null && !contact.getPhones().isEmpty())
                ? contact.getPhones().get(0).getNumber() : "";
        String email = (contact.getEmails() != null && !contact.getEmails().isEmpty())
                ? contact.getEmails().get(0).getAddress() : "";
        String organization = contact.getOrganization();
        String jobTitle = contact.getTitle();

        // Create the insert intent
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
        intent.putExtra(ContactsContract.Intents.Insert.COMPANY, organization);
        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, jobTitle);

        // Start the Contacts app
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No contacts app found!", Toast.LENGTH_SHORT).show();
        }
    }
    public void loadCalender(Barcode barcode) {
        Barcode.CalendarEvent event = barcode.getCalendarEvent();

        String title = event.getSummary();
        String description = event.getDescription();
        String location = event.getLocation();

        // Convert start and end times to milliseconds
        long beginMillis = getTimeInMillis(event.getStart());
        long endMillis = event.getEnd() != null
                ? getTimeInMillis(event.getEnd())
                : beginMillis + 60 * 60 * 1000; // default to 1 hour event

        // Create intent to insert event
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No calendar app found!", Toast.LENGTH_SHORT).show();
        }
    }
    private long getTimeInMillis(Barcode.CalendarDateTime cdt) {
        Calendar calendar = Calendar.getInstance(
                cdt.isUtc() ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault());

        calendar.clear(); // Clear to avoid garbage values

        calendar.set(Calendar.YEAR, cdt.getYear());
        calendar.set(Calendar.MONTH, cdt.getMonth() - 1); // Months are 0-based
        calendar.set(Calendar.DAY_OF_MONTH, cdt.getDay());

        // Some fields may be optional in QR code
        calendar.set(Calendar.HOUR_OF_DAY, cdt.getHours() != -1 ? cdt.getHours() : 0);
        calendar.set(Calendar.MINUTE, cdt.getMinutes() != -1 ? cdt.getMinutes() : 0);
        calendar.set(Calendar.SECOND, cdt.getSeconds() != -1 ? cdt.getSeconds() : 0);

        return calendar.getTimeInMillis();
    }

    public void loadMap(Barcode barcode){
        Barcode.GeoPoint geoPoint = barcode.getGeoPoint();

        double latitude = geoPoint.getLat();
        double longitude = geoPoint.getLng();

        // Create a geo URI
        String geoUri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Scanned Location)";

        // Create intent to open map
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        mapIntent.setPackage("com.google.android.apps.maps"); // open specifically in Google Maps

        // Start the map activity if available
        if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(getContext(), "Google Maps app not found!", Toast.LENGTH_SHORT).show();
        }
    }
    public void connectWifi(Barcode barcode) {
        Barcode.WiFi wifi = barcode.getWifi();

        String ssid = wifi.getSsid();
        String password = wifi.getPassword();
        int encryption = wifi.getEncryptionType();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder().setSsid(ssid);

            if (encryption == Barcode.WiFi.TYPE_WPA || encryption == Barcode.WiFi.TYPE_WPA) {
                builder.setWpa2Passphrase(password);
            } else if (encryption == Barcode.WiFi.TYPE_WEP) {
                Toast.makeText(requireContext(), "WEP not supported on Android Q+", Toast.LENGTH_SHORT).show();
                return;
            } // Open network → no password

            WifiNetworkSpecifier specifier = builder.build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        connectivityManager.bindProcessToNetwork(network);
                    }
                    Toast.makeText(requireContext(), "WiFi Connected to " + ssid, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUnavailable() {
                    Toast.makeText(requireContext(), "Failed to connect Wi-Fi", Toast.LENGTH_SHORT).show();
                }
            };

            connectivityManager.requestNetwork(request, networkCallback);
        } else {
            Toast.makeText(requireContext(), "Android version too low for WifiNetworkSpecifier", Toast.LENGTH_SHORT).show();
        }
    }

    public void copyData(String scannedValue){
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QR Code", scannedValue);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    private String[] parseEmailFromQR(String qrValue) {
        String email = "";
        String subject = "";
        String body = "";

        try {
            if (qrValue.startsWith("mailto:")) {
                Uri uri = Uri.parse(qrValue);

                // Get email (after mailto:)
                email = uri.getSchemeSpecificPart().split("\\?")[0];

                // Get query params
                String query = uri.getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2) {
                            String key = pair[0];
                            String value = Uri.decode(pair[1]); // Decode %20, etc.
                            if (key.equalsIgnoreCase("subject")) {
                                subject = value;
                            } else if (key.equalsIgnoreCase("body")) {
                                body = value;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[]{email, subject, body};
    }

}
