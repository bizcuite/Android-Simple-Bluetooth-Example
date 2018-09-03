package com.example.bizcuite.btreader;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // GUI Components
    private TextView mBluetoothStatus;
//     private TextView mReadBuffer;
    private TextView mTest;
    private TextView mTestHeader;
    private TextView mTestFullTrame;
    private TextView mTextRPMEgine;
    private TextView mTextTransState;
    private TextView mTextTransGear;
    private TextView mTextOdometer;



    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private Button mSendSequence;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;

    private final String TAG = MainActivity.class.getSimpleName();
    private Handler mHandler; // Our main handler that will receive callback notifications
    //private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    //0C:14:20:B4:DB:84) my phone

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    //private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private final static int Test = 4; // used in bluetooth handler to identify message status
    private final static int TestHeader = 5; // used in bluetooth handler to identify message status
    private final static int TestFullTrame = 6; // used in bluetooth handler to identify message status
    private final static int TextRPMEgine = 7; //
    private final static int TextTransState = 8; //
    private final static int TextTransGear = 9;
    private final static int TextOdometer = 10; //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mTest = (TextView) findViewById(R.id.test);
        mTestFullTrame = (TextView) findViewById(R.id.testFullTrame);
        mTestHeader = (TextView) findViewById(R.id.testHeader);
        mTextRPMEgine = (TextView) findViewById(R.id.TextRPMEgine);
        mTextTransState = (TextView) findViewById(R.id.TextTransState);
        mTextTransGear = (TextView) findViewById(R.id.TextTransGear);
        mTestFullTrame = (TextView) findViewById(R.id.testFullTrame);
        mTextOdometer = (TextView) findViewById(R.id.TextOdometer);

        mScanBtn = (Button)findViewById(R.id.scan);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
        mSendSequence = (Button)findViewById(R.id.sendSequence);


        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
                if (msg.what == Test) {
                    String readStream = null;
                    readStream = new String((String) msg.obj);
                    mTest.setText(readStream);
                }
                if (msg.what == TestHeader) {
                    String readStreamHeader = null;
                    readStreamHeader = new String((String) msg.obj);
                    mTestHeader.setText(readStreamHeader);
                }
                if (msg.what == TestFullTrame) {
                    String readStreamFull = null;
                    readStreamFull = new String((String) msg.obj);
                    mTestFullTrame.setText(readStreamFull);
                }
                if (msg.what == TextRPMEgine) {
                    String readRPMEgine = null;
                    readRPMEgine = new String((String) msg.obj);
                    mTextRPMEgine.setText(readRPMEgine);
                }
                if (msg.what == TextTransState) {
                    String readTransState = null;
                    readTransState = new String((String) msg.obj);
                    mTextTransState.setText(readTransState);
                }
                if (msg.what == TextTransGear) {
                    String readTransGear = null;
                    readTransGear = new String((String) msg.obj);
                    mTextTransGear.setText(readTransGear);
                }
                if (msg.what == TextOdometer) {
                    String readOdometer = null;
                    readOdometer = new String((String) msg.obj);
                    mTextOdometer.setText(readOdometer);
                }

            }
        };



        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mSendSequence.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mConnectedThread != null) { //First check to make sure thread created

                        List<String> list = new ArrayList<String>();
                        list.add("ATZ\n");
                        list.add("ATI\n");
                        list.add("ATE0\n");
                        list.add("ATL0\n");
                        list.add("ATH1\n");
                        list.add("ATSP0\n");
                        list.add("ATMA\n");

                       // System.out.println("#1 normal for loop");
                        for (int i = 0; i < list.size(); i++) {
                            mConnectedThread.write(list.get(i));
                            SystemClock.sleep(300); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        }
                    }
                    else
                        Toast.makeText(getApplicationContext(),"Start sequence doesn't worked",Toast.LENGTH_SHORT).show();
                }
            });


            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });



        }
    }

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            }
            else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        //SystemClock.sleep(1); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                        ChryslerDecoder(buffer,bytes);
                        //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                        //        .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        public void ChryslerDecoder(byte[] buffer, int bytes){

            //DEBUT DU DECODAGE ICI
            //ICI il faut compter les bytes du message et trouver comment l'afficher dans le view
            //il faut que ca renvoie les 2 premiers bytes, voir a afficher un toast avec le contenu du buffer
            String I1ByteHeader = null;     //Header
            assert I1ByteHeader != null;
            String I2Byte = null;
            assert I2Byte != null;
            String I3Byte = null;
            assert I3Byte != null;
            String I4Byte = null;
            assert I4Byte != null;
            String I5Byte = null;
            assert I5Byte != null;
            String I6Byte = null;
            assert I6Byte != null;
            String I7Byte = null;
            assert I7Byte != null;
            String I8Byte = null;
            assert I8Byte != null;
            String I9Byte = null;
            assert I9Byte != null;
            String StringBuffer = null;
            assert StringBuffer != null;
            //byte -> to string
            StringBuffer = new String(buffer, Charset.forName("utf-8"));
            //Cut string 2 first Char to see which module give you informations
            I1ByteHeader = StringBuffer.substring(0,2);

            //100CA7009C2DA7	RPM Moteur	0CA7	DECIMALE/4
            //RPM Transmission	009C	DECIMALE/4
            //MAP	DA	DECIMALE DIRECT
            mHandler.obtainMessage(TestFullTrame,1, -1, StringBuffer)
                    .sendToTarget(); // Send the obtained bytes to the UI activity
            //Here if header (1Byte) = 10 then it's speed trame etc etc
            switch(I1ByteHeader) {
                case "10":
                    //  HEADER 1  23 45 56 67 89 CRC    BYTES
                    //example: 10 00 00 00 00 65 1F
                    //We send header to UI to test
                    mHandler.obtainMessage(TestHeader,1, -1, I1ByteHeader)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                    //New array list to memories our result, and put its in form after and send it to UI
                    List<String> listEngine = new ArrayList<String>();

                    //CACUL RMP ENGINE
                    I2Byte = StringBuffer.substring(2,6);
                    float RPMEngineToDecimal = (Long.parseLong(I2Byte, 16)/4);
                    //RPMEngineToDecimal = RPMEngineToDecimal/4;
                    String RPMEngineString = String.valueOf(RPMEngineToDecimal);
                    listEngine.add(RPMEngineString);
                    //END CACUL RMP ENGINE

                    // CACUL RMP TRANSAXLE
                    I3Byte = StringBuffer.substring(6,10);
                    float RPMTransDecimal = (Long.parseLong(I3Byte, 16)/4);
                    RPMTransDecimal = RPMTransDecimal/4;
                    String RPMTransString = String.valueOf(RPMTransDecimal);
                    listEngine.add(RPMTransString);
                    //END CACUL RMP TRANSAXLE
                    // CACUL MAP
                    I4Byte = StringBuffer.substring(6,10);
                    float MAPDecimal = (Long.parseLong(I4Byte, 16)/4);
                    //MAPDecimal = MAPDecimal/4;
                    String MAPString= String.valueOf(MAPDecimal);
                    listEngine.add(MAPString);
                    //END CACUL MAP

                    //AJOUTER LE CALCUL DE LA VITESSE DES ROUES!
                    //
                    mHandler.obtainMessage(TextRPMEgine,1, -1, "ENGINE: "+listEngine.get(0)+"Tr/M # TRANS: "+listEngine.get(1)+"Tr/M # MAP: "+listEngine.get(2)+"KPa")
                    //mHandler.obtainMessage(Test,1, -1,RPMTrans )
                            .sendToTarget(); // Send the obtained bytes to the UI activity

                    break;
                case "3A":
                    //  HEADER 1  23 CRC    BYTES
                    //example: 3A 21 27
                    //We send header to UI to test
                    mHandler.obtainMessage(TestHeader,1, -1, I1ByteHeader)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                    //New array list to memories our result, and put its in form after and send it to UI
                    List<String> listTransState = new ArrayList<String>();
                    List<String> listTransGear = new ArrayList<String>();
                    //CALCUL OF DIRECTION OF TRANS
                    I2Byte = StringBuffer.substring(2,3);
                    switch(I2Byte) {
                        case "1":
                            listTransState.add("REVERSE");
                            break;
                        case "2":
                            listTransState.add("FORWARD+CONVERT_UNLOCKED");
                            break;
                        case "6":
                            listTransState.add("FORWARD+CONVERT_LOCKED");
                            break;
                        case "A":
                            listTransState.add("KICKDOWN");
                            break;
                        default:
                            listTransState.add("NOT_DECODED");
                    }
                    //END CALCUL OF DIRECTION OF TRANS
                    //CALCUL OF GEAR OF TRANS
                    I3Byte = StringBuffer.substring(3,4);
                    switch(I3Byte) {
                        case "0":
                            listTransGear.add("1st/Reverse/Low");
                            break;
                        case "1":
                            listTransGear.add("2nd");
                            break;
                        case "2":
                            listTransGear.add("3rd");
                            break;
                        case "3":
                            listTransGear.add("4th");
                            break;
                        default:
                            listTransGear.add("NOT_DECODED");
                    }
                    //END CALCUL OF GEAR OF TRANS
                    mHandler.obtainMessage(TextTransState,1, -1, "GEAR: "+listTransGear.get(0)+" ## STATE: "+listTransState.get(0))
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                    break;
                case "37":
                    //  HEADER 1  23 45 CRC    BYTES
                    //example: 37 02 00 CB
                    //We send header to UI to test
                    mHandler.obtainMessage(TestHeader,1, -1, I1ByteHeader)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                    //New array list to memories our result, and put its in form after and send it to UI
                    List<String> listTransGearSelected = new ArrayList<String>();
                    //CALCUL OF  GEAR SELECT OF TRANS
                    I2Byte = StringBuffer.substring(2,4);
                    switch(I2Byte) {
                        case "01":
                            listTransGearSelected.add("PARKING");
                            break;
                        case "02":
                            listTransGearSelected.add("REVERSE");
                            break;
                        case "03":
                            listTransGearSelected.add("DRIVE");
                            break;
                        case "05":
                            listTransGearSelected.add("THREE (3)");
                            break;
                        case "06":
                            listTransGearSelected.add("LOW (L)");
                            break;
                        default:
                            listTransGearSelected.add("NOT_DECODED");
                    }
                    //END CALCUL OF GEAR SELECT OF TRANS
                    mHandler.obtainMessage(TextTransGear,1, -1, "GEAR SELECTED: "+listTransGearSelected.get(0))
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                    break;
                case "72":
                    //CACUL ODOMETER
                    I2Byte = StringBuffer.substring(2,10);
                    double OdometerDecimal = (Long.parseLong(I2Byte, 16)/8000*1.60934);
                    String OdometerString = String.valueOf(OdometerDecimal);
                    mHandler.obtainMessage(TextOdometer,1, -1, "Odo: "+OdometerString)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                    //END CACUL ODOMETER

                    break;
                case "4":

                    break;
                case "5":

                    break;
                case "100":

                    break;
                case "7":

                    break;
                default:
                    mHandler.obtainMessage(TestHeader,1, -1, "NOT_DECODED")
                            .sendToTarget(); // Send the obtained bytes to the UI activity
            }


            // Toast.makeText(getApplicationContext(),"Lecture", Toast.LENGTH_SHORT).show();
        }





        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
