package com.example.eviogimble;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Gimbal;
import com.github.clans.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import sample.android.gimbal.com.pleasepermit.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    protected static final String TAG_GIMBAL = "Gimbal";
    private BeaconEventListener beaconSightingListener;
    private BeaconManager beaconManagerGimbal;
    private ExpandableListView mExpandableListBeacons;
    private ExpandableListAdapterBeacons mExpandableListAdapterBeacons;
    private ArrayList<BeaconObject> groupItem = new ArrayList<>();
    private ArrayList<Object> childItem = new ArrayList<>();
    private TextView mTextViewScanningMode;
    private TextView mTextViewBeaconsFound;
    private Boolean scanning = false;
    private LocationPermissions permissions;
    private MqttConnectOptions mOptions;
    private MqttAndroidClient mClient;
    private FloatingActionButton fab;
    private String clientId = MqttClient.generateClientId();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((LinearLayout)findViewById(R.id.list_summary)).setBackgroundColor(Color.RED);

        Gimbal.setApiKey(this.getApplication(), SettingsConstants.GIMBAL_API_KEY);


        if(permissions == null) {
            permissions = new LocationPermissions(this);
        }
        permissions.checkAndRequestPermission();
        mOptions = new MqttConnectOptions();
        mOptions.setCleanSession(true);
        mClient = new MqttAndroidClient(this, "tcp://eviothings.in:1883", clientId);
        configureGUIelements();
        setUpGimbal();
        startGimbal();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        this.permissions.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopGimbal();
        disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        connect();
    }
    @Override
    public void onPause() {
        super.onPause();
        connect();
    }

    private void configureGUIelements(){
        mExpandableListBeacons = (ExpandableListView) findViewById(R.id.exp_list);
        mTextViewScanningMode  = (TextView) findViewById(R.id.beaconMode);
        mTextViewBeaconsFound  = (TextView) findViewById(R.id.beaconNum);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        configureFab();
        updateHeader();
    }

    private void setUpGimbal() {
            beaconSightingListener = new BeaconEventListener() {
                @Override
                public void onBeaconSighting(BeaconSighting sighting) {
                    BeaconObject beaconObject = new BeaconObject(sighting);
                    Log.i(TAG_GIMBAL, beaconObject.toString());
                    updateScanResults(beaconObject);
                }
            };
            beaconManagerGimbal = new BeaconManager();
    }

    private void startGimbal(){
        beaconManagerGimbal.addListener(beaconSightingListener);
        beaconManagerGimbal.startListening();
    }

    private void stopGimbal(){
        beaconManagerGimbal.stopListening();
        beaconManagerGimbal.removeListener(beaconSightingListener);
    }

    private void updateHeader(){
        if (!scanning) {
            String scan_mode = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()).getString(
                            SettingsConstants.SCAN_MODE, SettingsConstants.MODE_ALL);
            String previous_scan_mode = mTextViewScanningMode.getText().toString();
            if (!scan_mode.equals(previous_scan_mode)) {
                groupItem.clear();
                childItem.clear();
                redrawList();
            }

            mTextViewScanningMode.setText(scan_mode);
        }

        int nBeacons = groupItem.size();
        mTextViewBeaconsFound.setText(String.valueOf(nBeacons));
    }

    private void configureFab(){
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("FAB", "Click " + scanning);
                if (!scanning) {
                    String scan_mode = PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext()).getString(
                                    SettingsConstants.SCAN_MODE, SettingsConstants.MODE_ALL);
                    updateHeader();
                    if (scan_mode.equals(SettingsConstants.MODE_ALL)) {
                        startGimbal();
                    } else if (scan_mode.equals(SettingsConstants.MODE_GIMBAL)) {
                        startGimbal();
                    }
                } else {
                    String scan_mode = mTextViewScanningMode.getText().toString();
                    if (scan_mode.equals(SettingsConstants.MODE_ALL)) {
                        stopGimbal();
                    } else if (scan_mode.equals(SettingsConstants.MODE_GIMBAL)) {
                        stopGimbal();
                    }
                }
                fab.setImageResource(
                        scanning ? R.drawable.ic_action_play : R.drawable.ic_action_stop);
                scanning = !scanning;
            }
        });

    }


    private void updateScanResults(BeaconObject beaconObject){
        publish(beaconObject.toString());
        String beaconId = beaconObject.getId();
        childItem.clear();
        Boolean found  = false;
        ArrayList<BeaconObject> tempList = new ArrayList<>();
        for (BeaconObject bs : groupItem){
            ArrayList<BeaconObject> child = new ArrayList<>();
            if (bs.getId().equals(beaconId)){
                tempList.add(beaconObject);
                child.add(beaconObject);
                found = true;
            } else {
                tempList.add(bs);
                child.add(bs);
            }
            childItem.add(child);
        }
        if (!found){
            tempList.add(beaconObject);
            ArrayList<BeaconObject> child = new ArrayList<>();
            child.add(beaconObject);
            childItem.add(child);
        }
        groupItem = new ArrayList<>(tempList);

        sortList();
        redrawList();
        updateHeader();
    }


    private void redrawList(){
        ArrayList<Boolean> expanded = new ArrayList<>();
        for ( int i = 0; i < groupItem.size(); i++ ) {
            try {
                expanded.add(mExpandableListBeacons.isGroupExpanded(i));
            } catch (Exception e) {
                expanded.add(false);
            }
        }

        int index = mExpandableListBeacons.getFirstVisiblePosition();
        View v = mExpandableListBeacons.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        if (mExpandableListBeacons.getAdapter() == null) {
            mExpandableListAdapterBeacons = new ExpandableListAdapterBeacons(this, groupItem, childItem);
            mExpandableListBeacons.setAdapter(mExpandableListAdapterBeacons);
            mExpandableListBeacons.setOnGroupClickListener(new ExpDrawerGroupClickListener());
        } else {
            mExpandableListAdapterBeacons.groupItem = groupItem;
            mExpandableListAdapterBeacons.Childtem = childItem;
            mExpandableListAdapterBeacons.notifyDataSetChanged();
        }
        for ( int i = 0; i < groupItem.size(); i++ ) {
            if (expanded.get(i)) {
                mExpandableListBeacons.expandGroup(i);
            }
        }
        mExpandableListBeacons.setSelectionFromTop(index, top);
    }


    private void sortList(){
        String sort_preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
                SettingsConstants.SORT_PREFERENCE, SettingsConstants.SORT_RSSI);
        if (sort_preference.equals(SettingsConstants.SORT_ALPHABETICALLY)) {
            sortByName();
        } else if (sort_preference.equals(SettingsConstants.SORT_RSSI)){
            sortByRSSI();
        }
    }

    private void sortByRSSI(){
        Collections.sort(groupItem, new Comparator<BeaconObject>() {
            @Override
            public int compare(BeaconObject item1, BeaconObject item2) {

                return item2.getRSSI().compareTo(item1.getRSSI());
            }
        });

        Collections.sort(childItem, new Comparator<Object>() {
            @Override
            public int compare(Object item1, Object item2) {
                BeaconObject i1 = ((ArrayList<BeaconObject>)item1).get(0);
                BeaconObject i2 = ((ArrayList<BeaconObject>)item2).get(0);

                return i2.getRSSI().compareTo(i1.getRSSI());
            }
        });
    }

    private void sortByName(){
        Collections.sort(groupItem, new Comparator<BeaconObject>() {
            @Override
            public int compare(BeaconObject item1, BeaconObject item2) {

                return item1.getName().compareTo(item2.getName());
            }
        });

        Collections.sort(childItem, new Comparator<Object>() {
            @Override
            public int compare(Object item1, Object item2) {
                BeaconObject i1 = ((ArrayList<BeaconObject>)item1).get(0);
                BeaconObject i2 = ((ArrayList<BeaconObject>)item2).get(0);

                return i1.getName().compareTo(i2.getName());
            }
        });
    }


    private class ExpDrawerGroupClickListener implements ExpandableListView.OnGroupClickListener {
        @Override
        public boolean onGroupClick(ExpandableListView parent, View v,
                                    int groupPosition, long id) {

            if (parent.isGroupExpanded(groupPosition)){
                parent.collapseGroup(groupPosition);
            }else {
                parent.expandGroup(groupPosition, true);
            }
            return true;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_sort:
                showSortingPicker();
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_clear_list:
                groupItem.clear();
                childItem.clear();
                redrawList();
                updateHeader();
                return true;
            case R.id.action_scan_mode:
                showScanningPicker();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void showSortingPicker(){
        final View addView = getLayoutInflater().inflate(R.layout.sort_picker, null);
        final RadioGroup rg = (RadioGroup) addView.findViewById(R.id.myRadioGroup);
        final String sort_preference = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).getString(
                        SettingsConstants.SORT_PREFERENCE,
                        SettingsConstants.SORT_RSSI);

        new AlertDialog.Builder(this).setTitle("Sort Beacons").setView(addView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int checkedIndex = rg.getCheckedRadioButtonId();

                        RadioButton b = (RadioButton) addView.findViewById(checkedIndex);
                        String sort_mode = b.getText().toString();

                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                .edit().putString(
                                SettingsConstants.SORT_PREFERENCE, sort_mode).apply();
                        Log.i("SortPicker", "CheckedIndex:" + sort_mode + " SortPreference Previous:" + sort_preference);
                        sortList();
                        redrawList();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showScanningPicker(){
        final View addView = getLayoutInflater().inflate(R.layout.scan_mode_picker, null);
        final RadioGroup rg = (RadioGroup) addView.findViewById(R.id.myRadioGroup);
        final String scan_preference = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).getString(
                        SettingsConstants.SCAN_MODE,
                        SettingsConstants.MODE_ALL);

        new AlertDialog.Builder(this).setTitle("Scan mode").setView(addView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int checkedIndex = rg.getCheckedRadioButtonId();

                        RadioButton b = (RadioButton) addView.findViewById(checkedIndex);
                        String scan_mode = b.getText().toString();

                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                .edit().putString(
                                SettingsConstants.SCAN_MODE, scan_mode).apply();
                        Log.i("SortPicker", "CheckedIndex:" + scan_mode + " SortPreference Previous:" + scan_preference);

                    }
                }).setNegativeButton("Cancel", null).show();
    }


    private IMqttActionListener mConnectCallback = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken token) {
            Log.d("Mqtt","mConnectCallback onSuccess");
            subscribe();
        }

        @Override
        public void onFailure(IMqttToken token, Throwable ex) {
            Log.d("Mqtt","mConnectCallback onFailure " + ex);
        }
    };

    private IMqttActionListener mSubscribeCallback = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken token) {
            Log.d("Mqtt","mSubscribeCallback onSuccess");
        }

        @Override
        public void onFailure(IMqttToken token, Throwable ex) {
            Log.d("Mqtt","mSubscribeCallback onFailure " + ex);
        }
    };

    private IMqttActionListener mPublishCallback = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken token) {
            Log.d("Mqtt","mPublishCallback onSuccess");
        }

        @Override
        public void onFailure(IMqttToken token, Throwable ex) {
            Log.d("Mqtt","mPublishCallback onFailure " + ex);
            disconnect();
            connect();
        }
    };

    private IMqttMessageListener mMessageListener = new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d("Mqtt","mMessageListener onSuccess topic=" + topic + ", message=" + message);
        }
    };

    private void connect() {
        try {
            mClient.connect(mOptions, null, mConnectCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            mClient.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mClient.unregisterResources();
    }

    private void subscribe() {
        try {
            IMqttToken subToken = mClient.subscribe("Evio", 1, null, mSubscribeCallback, mMessageListener);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void publish(String str) {
        try {
        JSONObject jsonObj = new JSONObject(str);
        String id = jsonObj.getString("id");

            mClient.publish(id, new MqttMessage(str.getBytes()), null, mPublishCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}