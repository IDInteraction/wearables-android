package uom.idinteractionmetawear;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Accelerometer;

import com.mbientlab.metawear.MetaWearBoard;

import com.mbientlab.metawear.module.Bmi160Accelerometer.AccRange;
import com.mbientlab.metawear.module.Bmi160Accelerometer.OutputDataRate;
import com.mbientlab.metawear.module.Logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


/**
 * Created by Aitor on 26/04/2016.
 *
 * Class to provide access to the accelerometer sensor.
 * Implemented based on documentation found in https://mbientlab.com/androiddocs/latest/bmi160_accelerometer.html.
 */
public class Accelerometer {

    private String accStreamKEY = "acc_stream";
    private String accLogKEY = "acc_log";

    private static final String TAG = "Accelerometer";

    private Bmi160Accelerometer bmi160AccModule;

    private String filename;
    private ArrayList<String> capturedData;
    private static final int bufferSize = 100;
    private CsvDAO csvDAO;
    private Logging logModule;
    private MainActivity.CaptureMode captureMode;
    private Handler logDownloadHandler;
    private static final int logDownloadFrequency = 2000;

    private Activity interfaceAccess;
    private TextView accStatus;

    //Parameters for the gyroscope sensors
    private final AccRange scaleRange = AccRange.AR_16G;
    private final OutputDataRate outputDataRate = OutputDataRate.ODR_25_HZ;

    /**
     * Constructor. Returns null if the accelerometer module cannot be found in this device.
     * @param mwBoard
     */
    public Accelerometer(MetaWearBoard mwBoard, Activity parentInterfaceAccess, TextView statusTextview, String outputFilename){
        try {
            interfaceAccess = parentInterfaceAccess;
            accStatus = statusTextview;
            bmi160AccModule= mwBoard.getModule(Bmi160Accelerometer.class);
            csvDAO = new CsvDAO(parentInterfaceAccess.getApplicationContext(),
                    parentInterfaceAccess.getApplicationContext().getExternalFilesDir(null));
            capturedData = new ArrayList<String>();
            filename = outputFilename;
            logModule = mwBoard.getModule(Logging.class);
            setStatus("Interface Initialised",Color.RED);

        } catch (UnsupportedModuleException e) {
            bmi160AccModule = null;
        }
    }

    /**
     * Function to help return status messages from the gyroscope to the main interface
     * @param statusMessage message to be shown
     * @param color color of the message. generally, it will change from red (error)
     *              to green (working correctly)
     */
    private void setStatus(final String statusMessage,final int color){
        interfaceAccess.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accStatus.setTextColor(color);
                accStatus.setText(statusMessage);
            }
        });
    }

    public Bmi160Accelerometer getAccModule(){
        return bmi160AccModule;
    }

    /**
     *     Start Accelerometer sampling via Stream
     *
     * @param reportToConsole if true, it will report the read values to the console
     * @param storeToFile if true, it will store the read values to a file
     */
    public void configureAccelerometer(MainActivity.CaptureMode mode, final Boolean reportToConsole, final Boolean storeToFile) {
        // Set measurement range to +/- 16G
        // Set output data rate to 100Hz
        bmi160AccModule.configureAxisSampling()
                .setFullScaleRange(scaleRange)
                .setOutputDataRate(outputDataRate)
                .commit();
        // enable axis sampling
        bmi160AccModule.enableAxisSampling();

        captureMode = mode;

        switch (captureMode) {
            case STREAM:
                // Switch the accelerometer to active mode
                bmi160AccModule.routeData().fromAxes().stream(accStreamKEY).commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                setStatus("Stream working correctly" + System.currentTimeMillis(),Color.GREEN);
                                result.subscribe(accStreamKEY, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        CartesianFloat cartesian = message.getData(CartesianFloat.class);
                                        if (reportToConsole)
                                            Log.i(TAG, cartesian.toString());
                                        if (storeToFile) {
                                            capturedData.add(System.currentTimeMillis() + "," + message.getTimestamp().getTimeInMillis() + ","
                                                    + cartesian.toString().replaceAll("[()]", ""));
                                            if (capturedData.size() >= bufferSize) {
                                                flushDataBuffer();
                                            }

                                        }
                                    }
                                });
                            }
                            @Override
                            public void failure(Throwable error) {
                                setStatus("Stream error!",Color.RED);
                                Log.e(TAG, "Error committing route", error);
                            }
                        });

                break;

            case LOG:
                Log.i(TAG, "Starting log mode");
                // Switch the accelerometer to active mode
                bmi160AccModule.routeData().fromAxes().log(accLogKEY).commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                Log.i(TAG, "log data source correctly configured");
                                setStatus("Log working correctly",Color.GREEN);

                                result.setLogMessageHandler(accLogKEY, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        CartesianFloat cartesian = message.getData(CartesianFloat.class);
                                        Log.i(TAG, "Processing:" + String.format("Log: %s", cartesian.toString()));
                                        if (reportToConsole)
                                            Log.i(TAG, cartesian.toString());
                                        if (storeToFile) {
                                            capturedData.add(System.currentTimeMillis() + "," + message.getTimestamp().getTimeInMillis() + ","
                                                    + cartesian.toString().replaceAll("[()]", ""));
                                            if (capturedData.size() >= bufferSize) {
                                                flushDataBuffer();
                                            }
                                        }
                                    }
                                });
                            }
                            @Override
                            public void failure(Throwable error) {
                                Log.e(TAG, "Error committing route", error);
                                setStatus("Log error!",Color.RED);
                            }
                        });
                break;
            default:
                Log.e(TAG,"incorrect logging mode");
                setStatus("Incorrect logging mode",Color.YELLOW);

                break;
        }
    }

    /**
     *     Stop Accelerometer sampling
     *
     */
    public void startAccelerometer(){
        Log.i(TAG, "Starting at:"+System.currentTimeMillis());
        capturedData.add(captureMode +"," + System.currentTimeMillis() + ",-1,-1,-1");
        if(captureMode== MainActivity.CaptureMode.LOG) {
            logModule.startLogging();
            startPeriodicFlush();
        }
        bmi160AccModule.start();
    }

    public void startPeriodicFlush(){
        logDownloadHandler = new Handler();
        // Run the above code block on the main thread after 2 seconds
        logDownloadHandler.postDelayed(periodicLogDownload, logDownloadFrequency);
    }

    // Define the code block to be executed
    private Runnable periodicLogDownload = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.d(TAG, "Executing periodic log flush");
            downloadDataLog();
            logDownloadHandler.postDelayed(periodicLogDownload, logDownloadFrequency);
        }
    };

    private void downloadDataLog(){
        logModule.downloadLog(0.05f,new Logging.DownloadHandler() {
            @Override
            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                Log.i(TAG, String.format("Progress= %d / %d", nEntriesLeft,
                        totalEntries));
            }
            @Override
            public void receivedUnknownLogEntry(byte logId, Calendar timestamp, byte[] data) {
                Log.i(TAG, String.format("Unknown log entry: {id: %d, data: %s}", logId,timestamp.getTimeInMillis(), Arrays.toString(data)));
            }
        });
    }

    private void flushDataBuffer(){
        Log.i(TAG, "Storing " + capturedData.size() +" to " + filename);
        for (int i=0; i<capturedData.size();i++) {
            csvDAO.writeToFile(filename, capturedData.get(i));
        }
        capturedData = new ArrayList<String>();

        if (captureMode==MainActivity.CaptureMode.LOG)
            downloadDataLog();
    }

    /**
     *     Stop Accelerometer sampling
     *
     */
    public void stopAccelerometer(){
        flushDataBuffer();
        Log.i(TAG, "Final flush to " +filename);
        if(captureMode== MainActivity.CaptureMode.LOG) {
            Log.i(TAG, "Stopping log");
            logModule.stopLogging();
            logDownloadHandler.removeCallbacks(periodicLogDownload);
        }
        bmi160AccModule.stop();
    }
}
