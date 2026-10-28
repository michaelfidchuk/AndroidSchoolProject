
package com.example.michaelproject2026;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (R.id.share == id) {
            Toast.makeText(this, "You have clicked on share", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (R.id.settings == id) {
            Toast.makeText(this, "You have clicked on settings", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (R.id.exit == id) {
            Toast.makeText(this, "You have clicked on exit", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (R.id.aboutme == id) {
            Toast.makeText(this, "You have clicked on about me", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private TextView tvStatus;
    private TextView tvLocation;

    private Button btnLocation;
    private Button btnNotification;
    private Button btnCheck;

    private FusedLocationProviderClient fusedLocationClient;

    private static final String CHANNEL_ID = "class_notifications";

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvPermissions);
        tvLocation = findViewById(R.id.tvLocation);

        btnLocation = findViewById(R.id.btnLocation);
        btnNotification = findViewById(R.id.btnNotification);
        btnCheck = findViewById(R.id.btnCheck);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // רישום לבקשת הרשאת מיקום
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handleLocationPermissionResult
        );

        // רישום לבקשת הרשאת התראות
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        showNotification();
                    } else {
                        tvStatus.setText("המשתמש לא אישר שליחת התראות");
                    }
                }
        );

        createNotificationChannel();

        btnLocation.setOnClickListener(v -> checkLocationPermission());
        btnNotification.setOnClickListener(v -> checkNotificationPermission());
        btnCheck.setOnClickListener(v -> showPermissionStatus());
    }

    // 1. בדיקת הרשאת מיקום וקבלת קואורדינטות
    private void checkLocationPermission() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            getCurrentLocation();
        } else {
            tvStatus.setText("מבקש הרשאת מיקום...");
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void handleLocationPermissionResult(Map<String, Boolean> result) {
        boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

        if (fineGranted || coarseGranted) {
            tvStatus.setText("הרשאת מיקום אושרה");
            getCurrentLocation();
        } else {
            tvStatus.setText("לא ניתנה הרשאת מיקום");
            tvLocation.setText("לא ניתן לקבל מיקום ללא הרשאה מתאימה");
        }
    }

    private void getCurrentLocation() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            tvStatus.setText("אין הרשאת מיקום");
            return;
        }

        tvStatus.setText("מנסה לקבל את מיקום המכשיר...");
        tvLocation.setText("ממתין לתוצאה...");

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        tvLocation.setText(
                                "Latitude (קו רוחב): " + latitude +
                                        "\nLongitude (קו אורך): " + longitude +
                                        "\nדיוק משוער במטרים: " + location.getAccuracy()
                        );
                        tvStatus.setText("המיקום התקבל בהצלחה");
                    } else {
                        tvStatus.setText("לא התקבל מיקום");
                        tvLocation.setText("בדקו שהמיקום במכשיר מופעל ושיש נתוני מיקום זמינים");
                    }
                })
                .addOnFailureListener(exception -> {
                    tvStatus.setText("אירעה שגיאה בקבלת המיקום");
                    tvLocation.setText("פרטי השגיאה: " + exception.getMessage());
                });
    }

    // 2. בדיקת הרשאת התראות ושליחת התראה
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                showNotification();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            showNotification();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "התראות",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("ערוץ התראות לדוגמה");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            tvStatus.setText("אין הרשאה להצגת התראות");
            return;
        }

        android.app.Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("PermissionLab")
                .setContentText("ההתראה הראשונה שלי באנדרואיד!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(this).notify(1001, notification);
        tvStatus.setText("נשלחה התראה בהצלחה");
    }

    // 3. הצגת מצב ההרשאות
    private void showPermissionStatus() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean notificationGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            notificationGranted = true;
        }

        String locationStatus;
        if (fineGranted) {
            locationStatus = "מיקום מדויק: אושר";
        } else if (coarseGranted) {
            locationStatus = "מיקום משוער: אושר";
        } else {
            locationStatus = "מיקום: לא אושר";
        }

        String notificationStatus = notificationGranted ? "התראות: אושר" : "התראות: לא אושר";

        tvStatus.setText(locationStatus + "\n" + notificationStatus);
    }
}