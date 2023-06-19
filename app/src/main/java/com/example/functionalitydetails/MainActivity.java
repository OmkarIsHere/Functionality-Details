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
import android.content.pm.PackageManager;
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
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    TextView txtDateTime, txtCaptureCount, txtConnectivity, txtBatteryCharging, txtBatteryCharge, txtFrequency, txtLocation;
    Button btnRefresh;
    ImageView imgCapture;

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


        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                    locationPermission = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                }
            }
        });
        isPermissionGranted();
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
      
    }
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void isPermissionGranted(){
        locationPermission = ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED;
        ArrayList<String> permissions = new ArrayList<String>();
        if(!locationPermission){
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(!permissions.isEmpty()){
            mPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = level * 100 / (int)scale;
            String batPct = String.valueOf(batteryPct) + "%";
            txtBatteryCharge.setText(batPct);
        }
    };

}