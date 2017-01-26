package com.ringrose.noah.wifiprotector.ui;

import android.net.wifi.WifiInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.widget.TextView;

import com.ringrose.noah.wifiprotector.R;
import com.ringrose.noah.wifiprotector.util.ChannelUtil;

import java.util.ArrayList;
import java.util.List;

public class WifiInspectorActivity extends AppCompatActivity {

    public static final String ARG_WIFI = "wifi";
    public static final String ARG_CHANNELS = "channels";

    private WifiInfo mWifiInfo;
    private ArrayList<Integer[]> mChannels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_inspector);

        Bundle args = getIntent().getExtras();
        mWifiInfo = (WifiInfo)args.getParcelable(ARG_WIFI);
        mChannels = (ArrayList<Integer[]>) args.get(ARG_CHANNELS);

        TextView textView = (TextView)findViewById(R.id.inspector_ssid);
        textView.setText(getString(R.string.inspector_report_title, mWifiInfo.getSSID()));

        int signalStrength = mWifiInfo.getRssi();
        textView = (TextView)findViewById(R.id.inspector_rssi);
        textView.setText(getString(R.string.wifi_rssi, signalStrength));

        if (signalStrength <= -85) {
            textView.setTextColor(getResources().getColor(R.color.wifi_really_bad));
        } else if (signalStrength <= -65) {
            textView.setTextColor(getResources().getColor(R.color.wifi_kind_of_bad));
        }

        textView = (TextView)findViewById(R.id.inspector_your_channel);
        textView.setText(getString(R.string.inspector_your_channel, ChannelUtil.convertFrequencyToChannel(mWifiInfo.getFrequency())));

        textView = (TextView)findViewById(R.id.inspector_channels);
        textView.setText(getString(R.string.inspector_best_channels, formatChannels()));
    }

    private String formatChannels() {
        StringBuilder builder = new StringBuilder();

        for (Integer[] channel : mChannels) {
            if (channel[0] < 15) {
                builder.append(channel[0]);
                builder.append(", ");
            }
        }

        if (builder.length() > 0) {
            builder.setLength(builder.length() - 2);
        }

        return builder.toString();
    }
}
