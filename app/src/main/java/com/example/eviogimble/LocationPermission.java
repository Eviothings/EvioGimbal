/* So as to work with the beacon we require the location and bluetooth permission, below code is standard java code for android to 
provide the location access.
*/

package com.example.eviogimble;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gimbal.android.BeaconSighting;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

class LocationPermissions implements DialogInterface.OnClickListener {
    private static final String TAG = "LocationPermissions";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    static private Activity activity;
    public LocationPermissions(MainActivity activity) {
        this.activity = activity;
    }
    private PlaceManager placeManager;
    private PlaceEventListener placeEventListener;
    private ArrayAdapter<String> listAdapter;

    public void checkAndRequestPermission() {
        if (isLocationPermissionEnabled()) {
        }
        else {
            requestLocationPermission();
        }
    }

    public boolean isLocationPermissionEnabled() {
        return ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this.activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showMessageOKCancel("Permitting us to access your location will entitle you to receive exclusive offers when you visit our stores. Please allow us to access your location so that we can better help you navigate our event.", this.activity, this, this);
            return;
        }
        activityRequestPermission();
    }

    private static void showMessageOKCancel(String message, Activity activity, DialogInterface.OnClickListener okListener,
                                            DialogInterface.OnClickListener cancelListener) {
        new AlertDialog.Builder(activity).setMessage(message).setPositiveButton("OK", okListener).create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            ActivityCompat.requestPermissions(this.activity, new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, LOCATION_PERMISSION_REQUEST_CODE);
        }
        else if (which == DialogInterface.BUTTON_NEGATIVE) {
            Log.e(TAG, "Application was denied permission!");

        }

    }

    private void activityRequestPermission() {
        ActivityCompat.requestPermissions(this.activity, new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }else{
                Log.e(TAG, "Application was denied permission!");
            }

        }
    }



}