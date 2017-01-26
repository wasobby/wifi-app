package com.ringrose.noah.wifiprotector.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ringrose.noah.wifiprotector.R;
import com.ringrose.noah.wifiprotector.util.ChannelUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_ACCESS = 22;

    private WifiInfo mCurrentConnection;
    private ListView mAccessPointsList;
    private ScanResultsAdapter mScanResultsAdapter;
    private List<ScanResult> mScanResults;
    private ArrayList<Integer[]> mChannelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mChannelList = new ArrayList<>();
        mScanResults = Collections.emptyList();
        mScanResultsAdapter = new ScanResultsAdapter();
        mAccessPointsList = (ListView)findViewById(R.id.access_point_list_view);
        mAccessPointsList.setAdapter(mScanResultsAdapter);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                mScanResults = wifiManager.getScanResults();
                mScanResultsAdapter.notifyDataSetChanged();
                mChannelList = createChannelList(mScanResults);
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForLocationPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_LOCATION_ACCESS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readyUi();
                }
            default:
                //- intentionally left blank
        }
    }

    public void handleInspectClicked(View view) {
        Intent intent = new Intent(this, WifiInspectorActivity.class);

        Bundle args = new Bundle();
        intent.putExtra(WifiInspectorActivity.ARG_WIFI, mCurrentConnection);
        intent.putExtra(WifiInspectorActivity.ARG_CHANNELS, mChannelList);

        startActivity(intent);
    }

    private void readyUi() {
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mCurrentConnection = wifiManager.getConnectionInfo();
        displayCurrentConnectionInfo();
        wifiManager.startScan();
    }

    private void checkForLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            readyUi();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_ACCESS);
            }
        }
    }

    private void displayCurrentConnectionInfo() {
        TextView textView = (TextView)findViewById(R.id.ssid);
        textView.setText(mCurrentConnection.getSSID());

        textView = (TextView)findViewById(R.id.mac_address);
        textView.setText(mCurrentConnection.getBSSID());

        int channel = ChannelUtil.convertFrequencyToChannel(mCurrentConnection.getFrequency());
        textView = (TextView)findViewById(R.id.channel);
        textView.setText(getString(R.string.wifi_channel, channel));

        textView = (TextView)findViewById(R.id.rssi);
        textView.setText(getString(R.string.wifi_rssi, mCurrentConnection.getRssi()));

        textView = (TextView)findViewById(R.id.link_speed);
        textView.setText(getString(R.string.configured_wifi_link_speed, mCurrentConnection.getLinkSpeed(), WifiInfo.LINK_SPEED_UNITS));
    }

    private ArrayList<Integer[]> createChannelList(List<ScanResult> scanResults) {
        Map<Integer, Integer[]> channelMap = new HashMap<>();

        for (ScanResult s : scanResults) {
            int channel = ChannelUtil.convertFrequencyToChannel(s.frequency);
            Integer[] pair = channelMap.get(channel);

            if (pair == null) {
                pair = new Integer[] { channel, 0 };
            }

            pair[1] = pair[1] + 1;

            channelMap.put(channel, pair);
        }

        ArrayList<Integer[]> channelList = new ArrayList<>(channelMap.values());
        Collections.sort(channelList, new ComparatorChain<Integer[]>() {
            @Override
            public int compare(Integer[] o1, Integer[] o2) throws UnsupportedOperationException {
                return (o1[1] - o2[1]);
            }
        });

        return channelList;
    }

    private class ScanResultsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mScanResults.size();
        }

        @Override
        public Object getItem(int i) {
            return mScanResults.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = getLayoutInflater();

            if (view == null) {
                view = inflater.inflate(R.layout.access_point_list_item, null);
            }

            ScanResult scanResult = (ScanResult)getItem(i);

            TextView textView = (TextView)view.findViewById(R.id.wifi_ssid);

            if (StringUtils.isEmpty(scanResult.SSID)) {
                textView.setText(R.string.wifi_no_ssid);
            } else {
                textView.setText(scanResult.SSID);
            }

            textView = (TextView)view.findViewById(R.id.wifi_mac);
            textView.setText(scanResult.BSSID);

            textView = (TextView)view.findViewById(R.id.wifi_security);
            textView.setText(scanResult.capabilities);

            textView = (TextView)view.findViewById(R.id.wifi_channel);
            textView.setText(getString(R.string.wifi_channel, ChannelUtil.convertFrequencyToChannel(scanResult.frequency)));

            int signalStrength = scanResult.level;

            textView = (TextView)view.findViewById(R.id.wifi_signal_strength);
            textView.setText(getString(R.string.wifi_rssi, scanResult.level));

            if (signalStrength <= -85) {
                textView.setTextColor(getResources().getColor(R.color.wifi_really_bad));
            } else if (signalStrength <= -65) {
                textView.setTextColor(getResources().getColor(R.color.wifi_kind_of_bad));
            } else {
                textView.setTextColor(getResources().getColor(R.color.textColorSecondary));
            }

            return view;
        }
    }
}
