package com.bluetooth.transfer;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class BluetoothDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_detail);
        BluetoothDevice device = getIntent().getParcelableExtra(BluetoothFragment.SELECTED_DEVICE);
        ImageView btIconView = (ImageView)findViewById(R.id.bt_icon);

        int btIcon = device.getBluetoothClass().getDeviceClass();
        switch(btIcon){
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                btIcon = R.drawable.ic_laptop_black_24dp;
                break;
            case BluetoothClass.Device.PHONE_SMART:
                btIcon = R.drawable.ic_smartphone_black_24dp;
                break;
            default:
                btIcon = R.drawable.ic_bluetooth_black_24dp;
                break;

        }
        btIconView.setImageResource(btIcon);
        TextView textViewItemName = (TextView)
                findViewById(R.id.text_view_item_name);
        TextView textViewItemDescription = (TextView)
                findViewById(R.id.text_view_item_description);

        textViewItemName.setText(device.getName());
        textViewItemDescription.setText(device.getAddress());
    }
}
