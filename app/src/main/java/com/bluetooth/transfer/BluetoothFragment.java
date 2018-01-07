package com.bluetooth.transfer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class BluetoothFragment extends Fragment {

    public static final String SELECTED_DEVICE = "device_address";

    private static final int REQUEST_ENABLE_PERMISSION = 3;


    public static final int SCAN_NOT_STARTED = 0;
    public static final int SCAN_STARTED = 1;
    public static final int SCAN_FINISHED = 2;

    private OnFragmentInteractionListener mListener;

    private BluetoothAdapter mBtAdapter;

    private SwitchCompat switchCompat;

    private View pairedDevicesLayout, newDevicesLayout;
    private TextView pairedDevicesEmpty, newDevicesEmpty;
    private ListView pairedDevicesListView, newDevicesListView;
    private Button mScan;
    private ProgressBar scanProgress = null;
    private EditText sendChat = null;
    private Button sendButton = null;
    private View sendLayout = null;

    private boolean scanStarted = false;


    private BluetoothDeviceAdapter mNewDevicesArrayAdapter;

    private BluetoothService mChatService = null;

    public BluetoothFragment() {
        // Required empty public constructor
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device);
                    newDevicesListView.setVisibility(View.VISIBLE);
                    newDevicesEmpty.setVisibility(View.GONE);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                scanProgress.setVisibility(View.GONE);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    newDevicesListView.setVisibility(View.GONE);
                    newDevicesEmpty.setVisibility(View.VISIBLE);
                } else {
                    newDevicesListView.setVisibility(View.VISIBLE);
                    newDevicesEmpty.setVisibility(View.GONE);
                }
            }
        }
    };

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBtAdapter.cancelDiscovery();
            BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
//            Intent intent = new Intent(getActivity(), BluetoothDetailActivity.class);
//            intent.putExtra(SELECTED_DEVICE, device);
//            getActivity().startActivity(intent);
            connectDevice(device);
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            sendLayout.setVisibility(View.VISIBLE);
//                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            sendLayout.setVisibility(View.GONE);
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                   // mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(activity,readMessage, Toast.LENGTH_SHORT).show();
                   // mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name

                    String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                      Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mChatService = new BluetoothService(mHandler);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                mChatService.start();
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        pairedDevicesLayout = view.findViewById(R.id.paired_layout);
        newDevicesLayout = view.findViewById(R.id.new_devices_layout);

        pairedDevicesEmpty = (TextView) view.findViewById(R.id.paired_devices_empty);
        newDevicesEmpty = (TextView) view.findViewById(R.id.new_devices_empty);

        pairedDevicesListView = (ListView) view.findViewById(R.id.paired_devices);
        newDevicesListView = (ListView) view.findViewById(R.id.new_devices);

        pairedDevicesListView.setOnItemClickListener(mDeviceClickListener);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        mScan = (Button) view.findViewById(R.id.button_scan);
        mScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });

        switchCompat = (SwitchCompat) view.findViewById(R.id.bluetooth_switch);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    mBtAdapter.enable();
                } else {
                    mBtAdapter.disable();
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setUpPairedDevices(isChecked);
                        setUpNewDevices(isChecked);
                        makeDiscoverable(isChecked);
                    }
                }, 300);

            }
        });

        scanProgress = (ProgressBar) view.findViewById(R.id.scan_progress);
        sendChat = (EditText) view.findViewById(R.id.sned_chat);
        sendLayout = view.findViewById(R.id.send_layout);
        sendLayout.setVisibility(View.GONE);
        sendButton = (Button) view.findViewById(R.id.send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = sendChat.getText().toString();
                sendMessage(message);
                sendChat.setText("");
            }
        });


        return view;
    }

    private void startScan() {
        IntentFilter filter = new IntentFilter();

        // Register for broadcasts when a device is discovered
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(mReceiver, filter);

        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        boolean started = mBtAdapter.startDiscovery();
        if (started) {
            newDevicesEmpty.setVisibility(View.GONE);
            scanProgress.setVisibility(View.VISIBLE);
            scanStarted = true;
            setUpNewDevices(mBtAdapter.isEnabled());
        }
    }

    private void checkPermission() {
        if (accessLocationPermission()) {
            startScan();
        }
    }

    private void makeDiscoverable(boolean isEnabled) {
//        if (isEnabled && mBtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(discoverableIntent);
//        }
    }


    private boolean accessLocationPermission() {
        int accessFineLocation = 0;
        int accessCoarseLocation = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            accessFineLocation = getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            accessCoarseLocation = getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        List<String> listRequestPermission = new ArrayList<String>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(strRequestPermission, REQUEST_ENABLE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private void setUpPairedDevices(boolean isEnabled) {
        BluetoothDeviceAdapter pairedDevicesArrayAdapter;
        if (isEnabled) {
            pairedDevicesLayout.setVisibility(View.VISIBLE);
            Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
            if (pairedDevices.size() == 0) {
                pairedDevicesListView.setVisibility(View.GONE);
                pairedDevicesEmpty.setVisibility(View.VISIBLE);
            } else {
                pairedDevicesListView.setVisibility(View.VISIBLE);
                pairedDevicesEmpty.setVisibility(View.GONE);
                List<BluetoothDevice> pairedList = new ArrayList<>();
                for (BluetoothDevice device : pairedDevices) {
                    pairedList.add(device);
                }
                pairedDevicesArrayAdapter = new BluetoothDeviceAdapter(getActivity(), pairedList);
                pairedDevicesListView.setAdapter(pairedDevicesArrayAdapter);
            }
        } else {
            pairedDevicesLayout.setVisibility(View.GONE);
        }
    }

    private void setUpNewDevices(boolean isEnabled) {
        if (isEnabled) {
            List<BluetoothDevice> newDevicesList = new ArrayList<>();
            mNewDevicesArrayAdapter = new BluetoothDeviceAdapter(getActivity(), newDevicesList);
            newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
            newDevicesLayout.setVisibility(scanStarted ? View.VISIBLE : View.GONE);
            mScan.setVisibility(View.VISIBLE);
        } else {
            newDevicesLayout.setVisibility(View.GONE);
            mScan.setVisibility(View.GONE);
            scanStarted = false;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enableDisableBT();
        //makeDiscoverable(mBtAdapter.isEnabled());
    }

    private void enableDisableBT() {
        switchCompat.setChecked(mBtAdapter.isEnabled());
        setUpPairedDevices(mBtAdapter.isEnabled());
        setUpNewDevices(mBtAdapter.isEnabled());
    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private class BluetoothDeviceAdapter extends BaseAdapter {

        private List<BluetoothDevice> devices;
        private Context mContext;

        BluetoothDeviceAdapter(Context context, List<BluetoothDevice> devices) {
            mContext = context;
            this.devices = devices;
        }

        public void add(BluetoothDevice device) {
            if (devices != null) {
                devices.add(device);
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            if (devices != null)
                return devices.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).
                        inflate(R.layout.scan_list_item, parent, false);
            }

            BluetoothDevice device = (BluetoothDevice) getItem(position);
            ImageView btIconView = (ImageView) convertView.findViewById(R.id.bt_icon);
            int btIcon = device.getBluetoothClass().getDeviceClass();
            switch (btIcon) {
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
                    convertView.findViewById(R.id.text_view_item_name);
            TextView textViewItemDescription = (TextView)
                    convertView.findViewById(R.id.text_view_item_description);
            textViewItemName.setText(device.getName());
            textViewItemDescription.setText(device.getAddress());
            return convertView;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_PERMISSION:
                if (grantResults.length > 0) {
                    for (int gr : grantResults) {
                        if (gr != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    startScan();
                }
                break;
            default:
                break;
        }
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }
    private void connectDevice(BluetoothDevice device) {
        // Get the device MAC address
        // Attempt to connect to the device
        mChatService.connect(device);
    }
}
