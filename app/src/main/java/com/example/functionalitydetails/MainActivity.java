package com.example.functionalitydetails;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    TextView txtDateTime, txtCaptureCount, txtConnectivity, txtBatteryCharging, txtBatteryCharge, txtFrequency, txtLocation;
    Button btnRefresh;
    ImageView imgCapture;
    int i =1;
    FusedLocationProviderClient fusedLocationProviderClient;
    List<Address> addresses = null;
    ActivityResultLauncher<String[]> mPermissionLauncher;
    private boolean locationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtCaptureCount = findViewById(R.id.txtCaptureCount);
        txtConnectivity = findViewById(R.id.txtConnectivity);
        txtBatteryCharge = findViewById(R.id.txtBatteryCharge);
        txtBatteryCharging = findViewById(R.id.txtBatteryCharging);
        txtFrequency = findViewById(R.id.txtFrequency);
        txtLocation = findViewById(R.id.txtLocation);
        txtDateTime = findViewById(R.id.txtDateTime);
        imgCapture = findViewById(R.id.imgCaptureImage);
        btnRefresh = findViewById(R.id.btnRefresh);
//==============================================================================
        SharedPreferences pref = getSharedPreferences("Mypref", MODE_PRIVATE);
        int i = pref.getInt("count", 1);
        txtFrequency.setText(String.valueOf(i));

        SharedPreferences preferences = getSharedPreferences("Mypref", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("count" , i++);
        editor.apply();



//==============================================================================
        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                locationPermission = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
            }
        });
        isPermissionGranted();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();
//====================================================================================

        LocalDateTime localDateTime = LocalDateTime.now();
        String str = localDateTime.toString().replace("T", " ");
        txtDateTime.setText(str);
//====================================================================================
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(this.mBatInfoReceiver, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        if (isCharging)
            txtBatteryCharging.setText(R.string.on);
        else
            txtBatteryCharging.setText(R.string.off);

//====================================================================================
        if (isNetworkConnected())
            txtConnectivity.setText(R.string.on);
        else
            txtConnectivity.setText(R.string.off);
//====================================================================================
        String f = txtFrequency.getText().toString().trim();
        f = f + "00000";
        long freq = Long.parseLong(f);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(MainActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        }, freq);

//=============================================================================
        btnRefresh.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
//=====================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = getSharedPreferences("Mypref", MODE_PRIVATE);
        preferences.edit().remove("count").apply();
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();

        if(activeNetworkInfo!= null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            } else
                return activeNetworkInfo.isConnected();
        }else return false;
    }

    private void isPermissionGranted() {
        locationPermission = ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        ArrayList<String> permissions = new ArrayList<String>();
        if (!locationPermission) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissions.isEmpty()) {
            mPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    private void getLocation() {
        Log.d(TAG, "getLocation: Inside");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if(location != null){
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        String loc = " "+ addresses.get(0).getLatitude();// + ", " + addresses.get(0).getLongitude();
                        Log.d(TAG, "Location: "+ loc);
                        txtLocation.setText(loc);
                    } catch (IOException e) {
                        Log.d(TAG, "Exception: "+e);
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = level * 100 / (int)scale;
            String batPct = batteryPct + "%";
            txtBatteryCharge.setText(batPct);
        }
    };

    private  void captureImage(){
        ImagePicker.with(this)
                .cameraOnly()
                .start();
    }

}