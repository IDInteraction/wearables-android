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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;

import org.opencv.android.BaseLoaderCallback;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;


public class MainActivityCamera extends AppCompatActivity implements BleScannerFragment.ScannerCommunicationBus,ServiceConnection,CameraBridgeViewBase.CvCameraViewListener2 {
    private MetaWearBleService.LocalBinder serviceBinder;

    private MetaWearBoard mwBoardGeneric;

    //   public enum CaptureMode {STREAM,LOG};

    private Constants.CaptureMode captureMode =  Constants.CaptureMode.STREAM;

    //Device physically marked as L
    private MetaWearBoard mwBoardLeft;
    private final String MW_LEFT_MAC_ADDRESS= Constants.MW_LEFT_MAC_ADDRESS;
    private Accelerometer accelerometerLeft;
    private String accLeftFilename = "accLeft"+System.currentTimeMillis()+".csv";
    private Gyroscope gyroLeft;
    private String gyroLeftFilename = "gyroLeft"+System.currentTimeMillis()+".csv";

    //Device physically marked as R
    private MetaWearBoard mwBoardRight;
    private final String MW_RIGHT_MAC_ADDRESS= Constants.MW_RIGHT_MAC_ADDRESS;
    private Accelerometer accelerometerRight;
    private String accRightFilename = "accRight"+System.currentTimeMillis()+".csv";
    private Gyroscope gyroRight;
    private String gyroRightFilename = "gyroRight"+System.currentTimeMillis()+".csv";


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

    TextView captureModeStream;
    TextView captureModeLog;
    Button resetButton;
    Button disconnectAllButton;

    //Face detection
    Button startFaceDetection;

    //sensors will have access to a textview, in order to report individual status and problems
    TextView leftGyroStatus;
    TextView leftAccStatus;

    TextView rightGyroStatus;
    TextView rightAccStatus;

    private Logging logModule;

    TextView faceDetectStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedIFinally,instanceState);//Typo in documentation?
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialiseInterface();

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

        ////////*****************
        // ///FACE DETECTION ONCREATE CODE
        Log.i(faceDetectLogTag, "called onCreate");

        //super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        /*
        mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
        mValue = (TextView) findViewById(R.id.method);

        mMethodSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                method = progress;
                switch (method) {
                    case 0:
                        mValue.setText("TM_SQDIFF");
                        break;
                    case 1:
                        mValue.setText("TM_SQDIFF_NORMED");
                        break;
                    case 2:
                        mValue.setText("TM_CCOEFF");
                        break;
                    case 3:
                        mValue.setText("TM_CCOEFF_NORMED");
                        break;
                    case 4:
                        mValue.setText("TM_CCORR");
                        break;
                    case 5:
                        mValue.setText("TM_CCORR_NORMED");
                        break;
                }


            }
        });*/
    }

    private void initialiseInterface(){
        try {
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

            leftGyroStatus = (TextView) findViewById(R.id.leftDeviceGyroStatus);
            leftAccStatus = (TextView) findViewById(R.id.leftDeviceAccStatus);

            rightGyroStatus = (TextView) findViewById(R.id.rightDeviceGyroStatus);
            rightAccStatus = (TextView) findViewById(R.id.rightDeviceAccStatus);

            faceDetectStatus = (TextView) findViewById(R.id.faceDetectStatus);

            //captureModeStream and capturemodeLog are textviews that will switch the
            //capture mode when pressed, alternating between red and green to show the current state.
            captureModeStream = (TextView) findViewById(R.id.captureStream);
            captureModeStream.setTextColor(Color.GREEN);
            captureModeStream.setTypeface(null, Typeface.BOLD_ITALIC);
            captureModeStream.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardLeft!=null||mwBoardRight!=null) {
                        Toast.makeText(getApplicationContext(), "Capture Mode cannot be changed while any device is connected, disconnect and try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                    else if (captureModeStream.getCurrentTextColor() == Color.RED) {
                        captureMode = Constants.CaptureMode.STREAM;
                        captureModeStream.setTextColor(Color.GREEN);
                        captureModeStream.setTypeface(null, Typeface.BOLD_ITALIC);
                        captureModeLog.setTextColor(Color.RED);
                        captureModeLog.setTypeface(null, Typeface.NORMAL);
                        Toast.makeText(getApplicationContext(), "Current capture mode is STREAMING",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            captureModeLog = (TextView) findViewById(R.id.captureLog);
            captureModeLog.setTextColor(Color.RED);
            captureModeLog.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardLeft!=null||mwBoardRight!=null) {
                        Toast.makeText(getApplicationContext(), "Capture Mode cannot be changed while any device is connected, disconnect and try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                    else if (captureModeLog.getCurrentTextColor() == Color.RED) {
                        captureMode = Constants.CaptureMode.LOG;
                        captureModeLog.setTextColor(Color.GREEN);
                        captureModeLog.setTypeface(null, Typeface.BOLD_ITALIC);
                        captureModeStream.setTextColor(Color.RED);
                        captureModeStream.setTypeface(null, Typeface.NORMAL);
                        Toast.makeText(getApplicationContext(), "Current capture mode is LOGGING",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            resetButton = (Button) findViewById(R.id.resetButton);
            resetButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        Toast.makeText(getApplicationContext(), "Resetting all connected devices",
                                Toast.LENGTH_SHORT).show();
                        if (mwBoardLeft != null) mwBoardLeft.getModule(Debug.class).resetDevice();
                        if (mwBoardRight != null) mwBoardRight.getModule(Debug.class).resetDevice();
                    }catch(UnsupportedModuleException e){
                        e.printStackTrace();
                    }
                }
            });
            disconnectAllButton = (Button) findViewById(R.id.disconnectButton);
            disconnectAllButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardLeft != null) {
                        mwBoardLeft.setConnectionStateHandler(null);
                        mwBoardLeft.removeRoutes();
                        mwBoardLeft.disconnect();
                        mwBoardLeft=null;

                        if(gyroLeft!=null){
                            gyroLeft.stopGyroscope();
                            gyroLeft=null;
                        }
                        if(accelerometerLeft!=null){
                            accelerometerLeft.stopAccelerometer();
                            accelerometerLeft=null;
                        }
                    }

                    if (mwBoardRight != null){
                        mwBoardRight.setConnectionStateHandler(null);
                        mwBoardRight.removeRoutes();
                        mwBoardRight.disconnect();
                        mwBoardRight=null;
                    }
                    if(gyroRight!=null){
                        gyroRight.stopGyroscope();
                        gyroRight=null;
                    }
                    if(accelerometerRight!=null){
                        accelerometerRight.stopAccelerometer();
                        accelerometerRight=null;
                    }

                    interfaceAccess.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leftDeviceStatus.setTextColor(Color.RED);
                            leftDeviceStatus.setText(R.string.leftDeviceDisconnected);
                            rightDeviceStatus.setTextColor(Color.RED);
                            rightDeviceStatus.setText(R.string.leftDeviceDisconnected);

                            leftGyroStatus.setTextColor(Color.GRAY);
                            leftGyroStatus.setText("Gyroscope Status");
                            leftAccStatus.setTextColor(Color.GRAY);
                            leftAccStatus.setText("Accelerometer Status");

                            rightGyroStatus.setTextColor(Color.GRAY);
                            rightGyroStatus.setText("Gyroscope Status");
                            rightAccStatus.setTextColor(Color.GRAY);
                            rightAccStatus.setText("Accelerometer Status");
                        }
                    });
                }
            });


            startAccLeftButton = (Button) findViewById(R.id.startAccLeft);
            startAccLeftButton.setTextColor(Color.RED);
            startAccLeftButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardLeft == null || !mwBoardLeft.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.leftDeviceDisconnected,
                                Toast.LENGTH_SHORT).show();
                    }
                    //If the sensor is not recording, start
                    else if (startAccLeftButton.getCurrentTextColor() == Color.RED) {
                        startAccLeftButton.setTextColor(Color.GREEN);
                        Toast.makeText(getApplicationContext(), "Starting Left accelerometer",
                                Toast.LENGTH_SHORT).show();

                        //start logging
                        accelerometerLeft.startAccelerometer();
                    }
                    //If the sensor is recording, stop
                    else if (startAccLeftButton.getCurrentTextColor() == Color.GREEN) {
                        startAccLeftButton.setTextColor(Color.RED);
                        Toast.makeText(getApplicationContext(), "Stopping Left accelerometer",
                                Toast.LENGTH_SHORT).show();
                        accelerometerLeft.stopAccelerometer();
                    }
                }
            });
            startGyroLeftButton = (Button) findViewById(R.id.startGyroLeft);
            startGyroLeftButton.setTextColor(Color.RED);
            startGyroLeftButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardLeft == null || !mwBoardLeft.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.leftDeviceDisconnected,
                                Toast.LENGTH_SHORT).show();
                    }
                    //If the sensor is not recording, start
                    else if (startGyroLeftButton.getCurrentTextColor() == Color.RED) {
                        startGyroLeftButton.setTextColor(Color.GREEN);
                        Toast.makeText(getApplicationContext(), "Starting Left gyroscope",
                                Toast.LENGTH_SHORT).show();

                        //start logging
                        gyroLeft.startGyroscope();
                    }
                    //If the sensor is recording, stop
                    else if (startGyroLeftButton.getCurrentTextColor() == Color.GREEN) {
                        startGyroLeftButton.setTextColor(Color.RED);
                        Toast.makeText(getApplicationContext(), "Stopping Left gyroscope",
                                Toast.LENGTH_SHORT).show();
                        gyroLeft.stopGyroscope();
                    }
                }
            });

            startAccRightButton = (Button) findViewById(R.id.startAccRight);
            startAccRightButton.setTextColor(Color.RED);
            startAccRightButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardRight == null || !mwBoardRight.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.rightDeviceDisconnected,
                                Toast.LENGTH_SHORT).show();
                    }
                    //If the sensor is not recording, start
                    else if (startAccRightButton.getCurrentTextColor() == Color.RED) {
                        startAccRightButton.setTextColor(Color.GREEN);
                        Toast.makeText(getApplicationContext(), "Starting Right accelerometer",
                                Toast.LENGTH_SHORT).show();

                        //start logging
                        accelerometerRight.startAccelerometer();
                    }
                    //If the sensor is recording, stop
                    else if (startAccRightButton.getCurrentTextColor() == Color.GREEN) {
                        startAccRightButton.setTextColor(Color.RED);
                        Toast.makeText(getApplicationContext(), "Stopping Right accelerometer",
                                Toast.LENGTH_SHORT).show();
                        accelerometerRight.stopAccelerometer();
                    }
                }
            });

            startGyroRightButton = (Button) findViewById(R.id.startGyroRight);
            startGyroRightButton.setTextColor(Color.RED);
            startGyroRightButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mwBoardRight == null || !mwBoardRight.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.rightDeviceDisconnected,
                                Toast.LENGTH_SHORT).show();
                    }
                    //If the sensor is not recording, start
                    else if (startGyroRightButton.getCurrentTextColor() == Color.RED) {
                        startGyroRightButton.setTextColor(Color.GREEN);
                        Toast.makeText(getApplicationContext(), "Starting Right gyroscope",
                                Toast.LENGTH_SHORT).show();

                        //start logging
                        gyroRight.startGyroscope();
                    }
                    //If the sensor is recording, stop
                    else if (startGyroRightButton.getCurrentTextColor() == Color.GREEN) {
                        startGyroRightButton.setTextColor(Color.RED);
                        Toast.makeText(getApplicationContext(), "Stopping Right gyroscope",
                                Toast.LENGTH_SHORT).show();
                        gyroRight.stopGyroscope();
                    }
                }
            });

            startAllButton = (Button) findViewById(R.id.startAll);
            startAllButton.setTextColor(Color.RED);
            startAllButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (mwBoardLeft == null || !mwBoardLeft.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.leftDeviceDisconnected,
                                Toast.LENGTH_SHORT).show();
                    } else if (mwBoardRight == null || !mwBoardRight.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.rightDeviceDisconnected,
                                Toast.LENGTH_SHORT).show();
                    }
                    //If all devices are ready and the sensor is not recording, start
                    else if (startAllButton.getCurrentTextColor() == Color.RED) {
                        startAllButton.setTextColor(Color.GREEN);

                        //Turn on the rest of buttons, indicating they are all working.
                        //They will also be disabled.
                        // Individual sensor interaction is disabled while all sensors are activated
                        startAccLeftButton.setEnabled(false);
                        startAccLeftButton.setTextColor(Color.GREEN);
                        startGyroLeftButton.setEnabled(false);
                        startGyroLeftButton.setTextColor(Color.GREEN);
                        startAccRightButton.setEnabled(false);
                        startAccRightButton.setTextColor(Color.GREEN);
                        startGyroRightButton.setEnabled(false);
                        startGyroRightButton.setTextColor(Color.GREEN);

                        //start logging
                        Log.i("MainActivity", "Starting all sensors at: " + System.currentTimeMillis());
                        gyroLeft.startGyroscope();
                        Log.i("MainActivity", "Starting gyroLeft at: " + System.currentTimeMillis());
                        gyroRight.startGyroscope();
                        Log.i("MainActivity", "Starting gyroRight at: " + System.currentTimeMillis());
                        accelerometerLeft.startAccelerometer();
                        Log.i("MainActivity", "Starting accelerometerLeft at: " + System.currentTimeMillis());
                        accelerometerRight.startAccelerometer();
                        Log.i("MainActivity", "Starting accelerometerRight at: " + System.currentTimeMillis());
                    }
                    //If the sensor is recording, stop
                    else if (startAllButton.getCurrentTextColor() == Color.GREEN) {
                        startAllButton.setTextColor(Color.RED);

                        startAccLeftButton.setEnabled(true);
                        startAccLeftButton.setTextColor(Color.RED);
                        startGyroLeftButton.setEnabled(true);
                        startGyroLeftButton.setTextColor(Color.RED);
                        startAccRightButton.setEnabled(true);
                        startAccRightButton.setTextColor(Color.RED);
                        startGyroRightButton.setEnabled(true);
                        startGyroRightButton.setTextColor(Color.RED);

                        Toast.makeText(getApplicationContext(), "Stopping All sensors",
                                Toast.LENGTH_SHORT).show();
                        gyroLeft.stopGyroscope();
                        gyroRight.stopGyroscope();
                        accelerometerLeft.stopAccelerometer();
                        accelerometerRight.stopAccelerometer();
                    }
                }
            });
            startFaceDetection = (Button) findViewById(R.id.faceDetectButton);
            startFaceDetection.setTextColor(Color.RED);
            startFaceDetection.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (startFaceDetection.getCurrentTextColor() == Color.RED) {
                        startAllButton.setTextColor(Color.GREEN);
                        startFaceDetection();
                    }
                    if (startFaceDetection.getCurrentTextColor() == Color.GREEN) {
                        startAllButton.setTextColor(Color.RED);
                        stopFaceDetection();
                    }
                }
            });
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);

        //Disable the FaceDetection View
        mOpenCvCameraView.disableView();

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
                                configureSensorsLeft();

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
                                configureSensorsRight();

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

    /**
     * This function is executed as soon as the board is selected.
     * This way any possible delay caused by the configuration of the sensors is avoided
     */
    private void configureSensorsLeft() {
        Log.i("MainActivity", "Configuring Left sensors");
        mwBoardLeft.removeRoutes();
        //If accelerometer object is not created yet, initialise it
        if (gyroLeft != null) gyroLeft.stopGyroscope();
        gyroLeft = new Gyroscope(mwBoardLeft, this, leftGyroStatus, captureMode, gyroLeftFilename,"Left");

        //If accelerometer object is not created yet, initialise it
        if (accelerometerLeft != null) accelerometerLeft.stopAccelerometer();
        accelerometerLeft = new Accelerometer(mwBoardLeft, this, leftAccStatus, captureMode, accLeftFilename,"Left");

        //start configuration
        gyroLeft.configureGyroscope(false, true);
        accelerometerLeft.configureAccelerometer(false, true);
    }

    private void configureSensorsRight() {
        Log.i("MainActivity", "Configuring Right sensors");
        mwBoardRight.removeRoutes();
        //If accelerometer object is not created yet, initialise it
        if (gyroRight != null) gyroRight.stopGyroscope();
        gyroRight = new Gyroscope(mwBoardRight, this, rightGyroStatus, captureMode, gyroRightFilename,"Right");
        //If accelerometer object is not created yet, initialise it
        if (accelerometerRight != null) accelerometerRight.stopAccelerometer();
        accelerometerRight = new Accelerometer(mwBoardRight, this, rightAccStatus, captureMode, accRightFilename,"Right");

        //start configuration
        gyroRight.configureGyroscope(false, true);
        accelerometerRight.configureAccelerometer(false, true);
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
                    Toast.LENGTH_SHORT).show();
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
                    Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "No logging module on the board", e);
        }
    }


    /***VARIABLES FOR FACE DETECTION*****/

    private TextView faceStatus;
    private boolean isFaceDetectionOn=false;
    private String faceDetectFilename = "faceDetect"+System.currentTimeMillis()+".csv";//filename
    private String faceDetectLogTag = "FaceDetection";//logTag
    private ArrayList<String> faceDetectCapturedData;//capturedData
    private CsvDAO faceDetectCsvDAO;//csvDAO
    private static final int faceDetectBufferSize = 100;//bufferSize

    //Local variables made global so they can be reported back
    private ArrayList<Rect> eyesArrayGlobal = new ArrayList<>();
    private ArrayList<Rect> facesArrayGlobal = new ArrayList<>();

    //Variables for the facedetection algorithm


    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;


    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    int method = 0;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    // matrix for zooming
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;


    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;

    private SeekBar mMethodSeekbar;
    private TextView mValue;

    double xCenter = -1;
    double yCenter = -1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(faceDetectLogTag, "OpenCV loaded successfully");


                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(
                                R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir,
                                "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // --------------------------------- load left eye
                        // classificator -----------------------------------
                        InputStream iser = getResources().openRawResource(
                                R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirER = getDir("cascadeER",
                                Context.MODE_PRIVATE);
                        File cascadeFileER = new File(cascadeDirER,
                                "haarcascade_eye_right.xml");
                        FileOutputStream oser = new FileOutputStream(cascadeFileER);

                        byte[] bufferER = new byte[4096];
                        int bytesReadER;
                        while ((bytesReadER = iser.read(bufferER)) != -1) {
                            oser.write(bufferER, 0, bytesReadER);
                        }
                        iser.close();
                        oser.close();

                        mJavaDetector = new CascadeClassifier(
                                mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(faceDetectLogTag, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(faceDetectLogTag, "Loaded cascade classifier from "
                                    + mCascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(
                                cascadeFileER.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(faceDetectLogTag, "Failed to load cascade classifier");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(faceDetectLogTag, "Loaded cascade classifier from "
                                    + mCascadeFile.getAbsolutePath());



                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(faceDetectLogTag, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public void FaceDetection() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(faceDetectLogTag, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    /**DEPRECATED**/
    /**
     * This code has been modified and added to the original activity's onCreate

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(faceDetectLogTag, "called onCreate");

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
        mValue = (TextView) findViewById(R.id.method);

        mMethodSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                method = progress;
                switch (method) {
                    case 0:
                        mValue.setText("TM_SQDIFF");
                        break;
                    case 1:
                        mValue.setText("TM_SQDIFF_NORMED");
                        break;
                    case 2:
                        mValue.setText("TM_CCOEFF");
                        break;
                    case 3:
                        mValue.setText("TM_CCOEFF_NORMED");
                        break;
                    case 4:
                        mValue.setText("TM_CCORR");
                        break;
                    case 5:
                        mValue.setText("TM_CCORR_NORMED");
                        break;
                }


            }
        });
    }
     */

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
                mLoaderCallback);
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mZoomWindow.release();
        mZoomWindow2.release();
    }

    //http://answers.opencv.org/question/7313/rotating-android-camera-to-portrait/
    public Mat transposeInput(Mat input){
        Mat mRgbaT = input.t();
        Core.flip(input.t(), mRgbaT, -1);
        Size sz = new Size(100,100);
        Imgproc.resize(mRgbaT, mRgbaT, sz);
        return (mRgbaT);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        //We need to transpose the inputs
       // mRgba = transposeInput(mRgba);
        //mGray = transposeInput(mGray);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mZoomWindow == null || mZoomWindow2 == null)
            CreateAuxiliaryMats();

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
                    2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
                    new Size());

        Rect[] facesArray = faces.toArray();

        for (int i = 0; i < facesArray.length; i++) {

            if(isFaceDetectionOn) facesArrayGlobal.add(facesArray[i]);

            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
                    FACE_RECT_COLOR, 3);
            xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
            yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
            Point center = new Point(xCenter, yCenter);

            Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

            Core.putText(mRgba, "[" + center.x + "," + center.y + "]",
                    new Point(center.x + 20, center.y + 20),
                    Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                            255));

            Rect r = facesArray[i];
            // compute the eye area
            Rect eyearea = new Rect(r.x + r.width / 8,
                    (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
                    (int) (r.height / 3.0));
            // split it
            Rect eyearea_right = new Rect(r.x + r.width / 16,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            Rect eyearea_left = new Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            // draw the area - mGray is working grayscale mat, if you want to
            // see area in rgb preview, change mGray to mRgba
            Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
                    new Scalar(255, 0, 0, 255), 2);
            Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
                    new Scalar(255, 0, 0, 255), 2);

            if (learn_frames < 5) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            } else {
                // Learning finished, use the new templates for template
                // matching
                match_eye(eyearea_right, teplateR, method);
                match_eye(eyearea_left, teplateL, method);

            }


            // cut eye areas and put them to zoom windows
            Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2,
                    mZoomWindow2.size());
            Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow,
                    mZoomWindow.size());


        }

        if(isFaceDetectionOn)
            addToFaceDetectionDataBuffer();
        try {
            setFaceDetectStatus(Integer.toString(facesArray.length),Color.GREEN);

        }catch(android.content.res.Resources.NotFoundException e){
            e.printStackTrace();
        }

        //This way we don't show anything in the screen
        return null;
        //return mRgba;

    }

    private void setFaceDetectStatus(final String statusMessage,final int color){
        interfaceAccess.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceDetectStatus.setTextColor(color);
                faceDetectStatus.setText(statusMessage);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(faceDetectLogTag, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(faceDetectLogTag, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }


    private void CreateAuxiliaryMats() {
        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
                    + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
                    + cols / 10, cols);
        }

    }

    private void match_eye(Rect area, Mat mTemplate, int type) {
        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return ;
        }
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);

        Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                255));
        Rect rec = new Rect(matchLoc_tx,matchLoc_ty);


    }

    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template = new Rect();
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();

        for (int i = 0; i < eyesArray.length;) {

            if(isFaceDetectionOn) eyesArrayGlobal.add(eyesArray[i]);

            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRgba.submat(eye_only_rectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    public void onRecreateClick(View v)
    {
        learn_frames = 0;
    }


    public void startFaceDetection(){
        Log.i("MainActivity", "Starting Face Detection");
        isFaceDetectionOn = true;
        faceDetectCapturedData = new ArrayList<>();
        faceDetectCapturedData.add("FaceDetection," + System.currentTimeMillis() + ",-1,-1,-1");
    }

    public void stopFaceDetection(){
        isFaceDetectionOn = false;
        flushFaceDetectionDataBuffer();
    }
    private void addToFaceDetectionDataBuffer(){
        String info="NoFaceDetected";
        if (facesArrayGlobal.size()>0) {

            //For each face there are 2 eyes, or none
            for (int i = 0; i < facesArrayGlobal.size(); i++) {
                info = "Face" + i + facesArrayGlobal.get(i).toString();
                if (eyesArrayGlobal.size()>0)
                    info += "Eye,"+eyesArrayGlobal.remove(0).toString();
                if (eyesArrayGlobal.size()>0)
                    info += "Eye,"+eyesArrayGlobal.remove(0).toString();

                faceDetectCapturedData.add(System.currentTimeMillis() + "," + info);
            }
        }
        else
            faceDetectCapturedData.add(System.currentTimeMillis() + "," + info);

        if (faceDetectCapturedData.size() >= faceDetectBufferSize) {
            flushFaceDetectionDataBuffer();
        }
        facesArrayGlobal = new ArrayList<>();
        eyesArrayGlobal = new ArrayList<>();
    }
    private void flushFaceDetectionDataBuffer(){
        Log.i(faceDetectLogTag, "Storing " + faceDetectCapturedData.size() +" to " + faceDetectFilename);

        if (faceDetectCsvDAO==null)
            faceDetectCsvDAO = new CsvDAO(getApplicationContext(),
                    getApplicationContext().getExternalFilesDir(null));
        for (int i=0; i<faceDetectCapturedData.size();i++) {
            faceDetectCsvDAO.writeToFile(faceDetectFilename, faceDetectCapturedData.get(i));
        }
        faceDetectCapturedData = new ArrayList<String>();
    }
}
