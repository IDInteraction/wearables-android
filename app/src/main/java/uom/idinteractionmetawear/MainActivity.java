package uom.idinteractionmetawear;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;

import java.io.File;
import java.util.Locale;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements BleScannerFragment.ScannerCommunicationBus,ServiceConnection {
    private MetaWearBleService.LocalBinder serviceBinder;

    private MetaWearBoard mwBoardGeneric;

    //Device physically marked as L
    private MetaWearBoard mwBoardLeft;
    private final String MW_LEFT_MAC_ADDRESS= "E6:0D:8E:C7:1D:45";
    private Accelerometer accelerometerLeft;
    private String accLeftFilename = "accLeft.csv";
    private Gyroscope gyroLeft;
    private String gyroLeftFilename = "gyroLeft.csv";

    //Device physically marked as R
    private MetaWearBoard mwBoardRight;
    private final String MW_RIGHT_MAC_ADDRESS= "C3:2D:1A:0E:30:C5";
    private Accelerometer accelerometerRight;
    private String accRightFilename = "accRight.csv";
    private Gyroscope gyroRight;
    private String gyroRightFilename = "gyroRight.csv";


    //Interface elements
    Activity interfaceAccess;
    TextView leftDeviceStatus;
    TextView leftDeviceAddress;
    TextView rightDeviceStatus;
    TextView rightDeviceAddress;

    Button startAccLeftButton;
    Button startGyroLeftButton;

    Button startAccRightButton;
    Button startGyroRightButton;

    Button startAllButton;

    private Logging logModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedIFinally,instanceState);//Typo in documentation?
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialiseInterface();

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    private void initialiseInterface(){
        interfaceAccess = this;
        leftDeviceStatus = (TextView) findViewById(R.id.leftMvBoardStatus);
        leftDeviceAddress = (TextView) findViewById(R.id.leftMvBoardAddress);
        leftDeviceStatus.setText(R.string.leftDeviceDisconnected);
        leftDeviceStatus.setTextColor(Color.RED);
        leftDeviceAddress.setText(MW_LEFT_MAC_ADDRESS);

        rightDeviceStatus = (TextView) findViewById(R.id.rightMvBoardStatus);
        rightDeviceAddress = (TextView) findViewById(R.id.rightMvBoardAddress);
        rightDeviceStatus.setText(R.string.rightDeviceDisconnected);
        rightDeviceStatus.setTextColor(Color.RED);
        rightDeviceAddress.setText(MW_RIGHT_MAC_ADDRESS);

        startAccLeftButton = (Button) findViewById(R.id.startAccLeft);
        startAccLeftButton.setTextColor(Color.RED);
        startAccLeftButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mwBoardLeft==null || !mwBoardLeft.isConnected()){
                    Toast.makeText(getApplicationContext(), R.string.leftDeviceDisconnected,
                            Toast.LENGTH_LONG).show();
                }
                //If the sensor is not recording, start
                else if (startAccLeftButton.getCurrentTextColor() == Color.RED){
                    startAccLeftButton.setTextColor(Color.GREEN);
                    Toast.makeText(getApplicationContext(), "Starting Left accelerometer",
                            Toast.LENGTH_LONG).show();
                    //If accelerometer object is not created yet, initialise it
                    if (accelerometerLeft == null)
                        accelerometerLeft =  new Accelerometer(mwBoardLeft,getApplicationContext());
                    //start logging
                    accelerometerLeft.activateAccelerometer(false,true,accLeftFilename);
                }
                //If the sensor is recording, stop
                else if (startAccLeftButton.getCurrentTextColor() == Color.GREEN) {
                    startAccLeftButton.setTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Stopping Left accelerometer",
                            Toast.LENGTH_LONG).show();
                    accelerometerLeft.stopAccelerometer();
                }
            }
        });
        startGyroLeftButton = (Button) findViewById(R.id.startGyroLeft);
        startGyroLeftButton.setTextColor(Color.RED);
        startGyroLeftButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mwBoardLeft==null || !mwBoardLeft.isConnected()){
                    Toast.makeText(getApplicationContext(), R.string.leftDeviceDisconnected,
                            Toast.LENGTH_LONG).show();
                }
                //If the sensor is not recording, start
                else if (startGyroLeftButton.getCurrentTextColor() == Color.RED){
                    startGyroLeftButton.setTextColor(Color.GREEN);
                    Toast.makeText(getApplicationContext(), "Starting Left gyroscope",
                            Toast.LENGTH_LONG).show();
                    //If accelerometer object is not created yet, initialise it
                    if (gyroLeft == null)
                        gyroLeft =  new Gyroscope(mwBoardLeft,getApplicationContext());

                    //start logging
                    gyroLeft.activateGyroscope(false,true,gyroLeftFilename);
                }
                //If the sensor is recording, stop
                else if (startGyroLeftButton.getCurrentTextColor() == Color.GREEN) {
                    startGyroLeftButton.setTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Stopping Left gyroscope",
                            Toast.LENGTH_LONG).show();
                    gyroLeft.stopGyroscope();
                }
            }
        });

        startAccRightButton = (Button) findViewById(R.id.startAccRight);
        startAccRightButton.setTextColor(Color.RED);
        startAccRightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mwBoardRight==null || !mwBoardRight.isConnected()){
                    Toast.makeText(getApplicationContext(), R.string.rightDeviceDisconnected,
                            Toast.LENGTH_LONG).show();
                }
                //If the sensor is not recording, start
                else if (startAccRightButton.getCurrentTextColor() == Color.RED){
                    startAccRightButton.setTextColor(Color.GREEN);
                    Toast.makeText(getApplicationContext(), "Starting Right accelerometer",
                            Toast.LENGTH_LONG).show();
                    //If accelerometer object is not created yet, initialise it
                    if (accelerometerRight == null)
                        accelerometerRight =  new Accelerometer(mwBoardRight,getApplicationContext());
                    //start logging
                    accelerometerRight.activateAccelerometer(false,true,accRightFilename);
                }
                //If the sensor is recording, stop
                else if (startAccRightButton.getCurrentTextColor() == Color.GREEN) {
                    startAccRightButton.setTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Stopping Right accelerometer",
                            Toast.LENGTH_LONG).show();
                    accelerometerRight.stopAccelerometer();
                }
            }
        });

        startGyroRightButton = (Button) findViewById(R.id.startGyroRight);
        startGyroRightButton.setTextColor(Color.RED);
        startGyroRightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mwBoardRight==null || !mwBoardRight.isConnected()){
                    Toast.makeText(getApplicationContext(), R.string.rightDeviceDisconnected,
                            Toast.LENGTH_LONG).show();
                }
                //If the sensor is not recording, start
                else if (startGyroRightButton.getCurrentTextColor() == Color.RED){
                    startGyroRightButton.setTextColor(Color.GREEN);
                    Toast.makeText(getApplicationContext(), "Starting Right gyroscope",
                            Toast.LENGTH_LONG).show();
                    //If accelerometer object is not created yet, initialise it
                    if (gyroRight == null)
                        gyroRight =  new Gyroscope(mwBoardRight,getApplicationContext());

                    //start logging
                    gyroRight.activateGyroscope(false,true,gyroRightFilename);
                }
                //If the sensor is recording, stop
                else if (startGyroRightButton.getCurrentTextColor() == Color.GREEN) {
                    startGyroRightButton.setTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Stopping Right gyroscope",
                            Toast.LENGTH_LONG).show();
                    gyroRight.stopGyroscope();
                }
            }
        });

        startAllButton = (Button) findViewById(R.id.startAll);
        startAllButton.setTextColor(Color.RED);
        startAllButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (mwBoardLeft==null || !mwBoardLeft.isConnected()){
                    Toast.makeText(getApplicationContext(), R.string.leftDeviceDisconnected,
                            Toast.LENGTH_LONG).show();
                }
                else if (mwBoardRight==null || !mwBoardRight.isConnected()){
                    Toast.makeText(getApplicationContext(), R.string.rightDeviceDisconnected,
                            Toast.LENGTH_LONG).show();
                }
                //If the sensor is not recording, start
                else if (startAllButton.getCurrentTextColor() == Color.RED){
                    startAllButton.setTextColor(Color.GREEN);
                    Toast.makeText(getApplicationContext(), "Starting All sensors",
                            Toast.LENGTH_LONG).show();
                    //If accelerometer object is not created yet, initialise it
                    if (gyroLeft == null)
                        gyroLeft =  new Gyroscope(mwBoardLeft,getApplicationContext());
                    //If accelerometer object is not created yet, initialise it
                    if (gyroRight == null)
                        gyroRight =  new Gyroscope(mwBoardRight,getApplicationContext());
                    //If accelerometer object is not created yet, initialise it
                    if (accelerometerLeft == null)
                        accelerometerLeft =  new Accelerometer(mwBoardLeft,getApplicationContext());
                    //If accelerometer object is not created yet, initialise it
                    if (accelerometerRight == null)
                        accelerometerRight =  new Accelerometer(mwBoardRight,getApplicationContext());

                    //start logging
                    gyroLeft.activateGyroscope(false,true,gyroLeftFilename);
                    gyroRight.activateGyroscope(false,true,gyroRightFilename);
                    accelerometerLeft.activateAccelerometer(false,true,accLeftFilename);
                    accelerometerRight.activateAccelerometer(false,true,accRightFilename);
                }
                //If the sensor is recording, stop
                else if (startAllButton.getCurrentTextColor() == Color.GREEN) {
                    startAllButton.setTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Stopping All sensors",
                            Toast.LENGTH_LONG).show();
                    gyroLeft.stopGyroscope();
                    gyroRight.stopGyroscope();
                    accelerometerLeft.stopAccelerometer();
                    accelerometerRight.stopAccelerometer();
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (MetaWearBleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }

    @Override
    public UUID[] getFilterServiceUuids() {
        return new UUID[] {MetaWearBoard.METAWEAR_SERVICE_UUID};
    }

    @Override
    public long getScanDuration() {
        return 10000L;
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device) {
        mwBoardGeneric = serviceBinder.getMetaWearBoard(device);

        final ProgressDialog connectDialog = new ProgressDialog(this);
        connectDialog.setTitle(getString(R.string.title_connecting));
        connectDialog.setMessage(getString(R.string.message_wait));
        connectDialog.setCancelable(false);
        connectDialog.setCanceledOnTouchOutside(false);
        connectDialog.setIndeterminate(true);
        connectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mwBoardGeneric.disconnect();
            }
        });
        connectDialog.show();

        mwBoardGeneric.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                connectDialog.dismiss();
                Log.i("MainActivity", mwBoardGeneric.getMacAddress() + "Connected");
                switch (mwBoardGeneric.getMacAddress()) {
                    case MW_LEFT_MAC_ADDRESS:
                        Log.i("MainActivity", "Left mvBoard Connected");
                        mwBoardLeft = mwBoardGeneric;
                        //hierarchy rules requires the execution of an "interface" thread to modify interface
                        interfaceAccess.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                leftDeviceStatus.setTextColor(Color.GREEN);
                                leftDeviceStatus.setText(R.string.leftDeviceConnected);

                                //Request battery level, and display it when received
                                mwBoardLeft.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
                                    @Override
                                    public void success(final Byte result) {
                                        //hierarchy rules requires the execution of an "interface" thread to modify interface
                                        interfaceAccess.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                leftDeviceStatus.setText(leftDeviceStatus.getText() + " Battery:" + String.format(Locale.UK, "%d", result));
                                            }
                                        });
                                    }
                                    @Override
                                    public void failure(Throwable error) {
                                        Log.e("test", "Error reading battery level", error);
                                    }
                                });
                            }
                        });
                        break;
                    case MW_RIGHT_MAC_ADDRESS:
                        Log.i("MainActivity", "Right mvBoard Connected");
                        mwBoardRight = mwBoardGeneric;
                        interfaceAccess.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rightDeviceStatus.setTextColor(Color.GREEN);
                                rightDeviceStatus.setText(R.string.rightDeviceConnected);

                                //Request battery level, and display it when received
                                mwBoardRight.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
                                    @Override
                                    public void success(final Byte result) {
                                        //hierarchy rules requires the execution of an "interface" thread to modify interface
                                        interfaceAccess.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                rightDeviceStatus.setText(rightDeviceStatus.getText() + " Battery:" + String.format(Locale.UK, "%d", result));
                                            }
                                        });
                                    }
                                    @Override
                                    public void failure(Throwable error) {
                                        Log.e("test", "Error reading battery level", error);
                                    }
                                });

                            }
                        });
                        break;
                    default:
                        Log.i("MainActivity", mwBoardGeneric.getMacAddress() + " unregistered mvBoard Connected");
                }

            }

            @Override
            public void disconnected() {
                switch (mwBoardGeneric.getMacAddress()) {
                    case MW_LEFT_MAC_ADDRESS:
                        Log.i("MainActivity", "Left mvBoard Disconnected");
                        mwBoardLeft = mwBoardGeneric;
                        interfaceAccess.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                leftDeviceStatus.setTextColor(Color.YELLOW);
                                leftDeviceStatus.setText(R.string.leftDeviceInterrupted);
                            }
                        });
                        break;
                    case MW_RIGHT_MAC_ADDRESS:
                        Log.i("MainActivity", "Right mvBoard Disconnected");
                        mwBoardRight = mwBoardGeneric;
                        interfaceAccess.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rightDeviceStatus.setTextColor(Color.YELLOW);
                                rightDeviceStatus.setText(R.string.rightDeviceInterrupted);
                            }
                        });
                        break;
                }
                mwBoardGeneric.connect();
            }

            @Override
            public void failure(int status, Throwable error) {
                switch (mwBoardGeneric.getMacAddress()) {
                    case MW_LEFT_MAC_ADDRESS:
                        Log.i("MainActivity", "Left mvBoard connection failed");
                        mwBoardLeft = mwBoardGeneric;
                        interfaceAccess.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                leftDeviceStatus.setTextColor(Color.YELLOW);
                                leftDeviceStatus.setText(R.string.leftDeviceInterrupted);
                            }
                        });
                        break;
                    case MW_RIGHT_MAC_ADDRESS:
                        Log.i("MainActivity", "Right mvBoard connection failed");
                        mwBoardRight = mwBoardGeneric;
                        interfaceAccess.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rightDeviceStatus.setTextColor(Color.YELLOW);
                                rightDeviceStatus.setText(R.string.rightDeviceInterrupted);
                            }
                        });
                        break;
                }
                mwBoardGeneric.connect();
            }
        });
        mwBoardGeneric.connect();
    }

    public void turnOnLed(MetaWearBoard mwBoard) {
        try {
            // Do not need to type cast result to Led class
            //plenty of typos in https://mbientlab.com/androiddocs/latest/metawearboard.html
            //using Led documentation from same page
            Led ledCtrllr= mwBoard.getModule(Led.class);
            ledCtrllr.configureColorChannel(Led.ColorChannel.BLUE)
                    .setRiseTime((short) 0).setPulseDuration((short) 1000)
                    .setRepeatCount((byte) -1).setHighTime((short) 500)
                    .setHighIntensity((byte) 16).setLowIntensity((byte) 16)
                    .commit();
            ledCtrllr.play(false);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, "Led module not supported on this board / firmware",
                    Toast.LENGTH_LONG).show();
            Log.e("MainActivity", "No Led on the board", e);
        }
    }

    public void startDeviceLogging(MetaWearBoard mwBoard){
        try {
            logModule= mwBoard.getModule(Logging.class);
            logModule.startLogging();
            // start logging, if log is full, overrite existing data
            //logModule.startLogging(true);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, "Logging module not supported on this board / firmware",
                    Toast.LENGTH_LONG).show();
            Log.e("MainActivity", "No logging module on the board", e);
        }
    }

    // Start downloading the log
    // All MessageHandlers passed to setLogMessageHandler will be called
    public void downloadDeviceData() {
       //TODO Look into the documentation and finish implementing the data downloader module.
    }

}
