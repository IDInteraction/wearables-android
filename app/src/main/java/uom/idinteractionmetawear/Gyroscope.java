package uom.idinteractionmetawear;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmi160Gyro.OutputDataRate;
import com.mbientlab.metawear.module.Logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


/**
 * Created by Aitor on 26/04/2016.
 *
 * Class to provide access to the gyroscope sensor.
 * Implemented based on documentation found in https://mbientlab.com/androiddocs/latest/bmi160_gyro.html.
 */
public class Gyroscope {


    private String gyroStreamKEY = "gyro_stream";
    private String gyroLogKEY = "gyro_log";

    private static final String TAG = "Gyroscope";

    private Bmi160Gyro bmi160GyroModule;

    private String filename;
    private ArrayList<String> capturedData;
    private int bufferSize = 100;
    private CsvDAO csvDAO;
    private Logging logModule;
    private MainActivity.CaptureMode captureMode;
    private Handler logDownloadHandler;
    private static final int logDownloadFrequency = 2000;

    private Activity interfaceAccess;
    private TextView gyroStatus;

    //Parameters for the gyroscope sensors
    private final Bmi160Gyro.FullScaleRange scaleRange = Bmi160Gyro.FullScaleRange.FSR_2000;
    private final OutputDataRate outputDataRate = OutputDataRate.ODR_25_HZ;

    /**
     * Constructor. Returns null if the accelerometer module cannot be found in this device.
     *
     * @param mwBoard
     */
    public Gyroscope(MetaWearBoard mwBoard, Activity parentInterfaceAccess, TextView statusTextview, String outputFilename){
        try {
            interfaceAccess = parentInterfaceAccess;
            gyroStatus = statusTextview;
            bmi160GyroModule = mwBoard.getModule(Bmi160Gyro.class);
            gyroStreamKEY+= SystemClock.currentThreadTimeMillis();
            gyroLogKEY+= SystemClock.currentThreadTimeMillis();

            csvDAO = new CsvDAO(parentInterfaceAccess.getApplicationContext(),
                    parentInterfaceAccess.getApplicationContext().getExternalFilesDir(null));
            capturedData = new ArrayList<String>();
            filename = outputFilename;
            logModule = mwBoard.getModule(Logging.class);
            setStatus("Interface Initialised",Color.RED);
        } catch (UnsupportedModuleException e) {
            bmi160GyroModule = null;
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
                gyroStatus.setTextColor(color);
                gyroStatus.setText(statusMessage);
            }
        });
    }


    /**
     *     Start Gyroscope sampling via Stream
     *
     * @param reportToConsole if true, it will report the read values to the console
     * @param storeToFile if true, it will store the read values to a file
     */
    public void configureGyroscope(MainActivity.CaptureMode mode, final Boolean reportToConsole, final Boolean storeToFile) {
        // Set the measurement range to +/-2000 degrees/s
        // Set output data rate to 100Hz
        bmi160GyroModule.configure()
                .setFullScaleRange(scaleRange)
                .setOutputDataRate(outputDataRate)
                .commit();

        captureMode = mode;

        switch (captureMode){
            case STREAM:
                //Stream rotation data around the XYZ axes from the gyro sensor
                bmi160GyroModule.routeData().fromAxes().stream(gyroStreamKEY).commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                setStatus("Stream working correctly",Color.GREEN);
                                result.subscribe(gyroStreamKEY, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        setStatus("Stream working correctly" + System.currentTimeMillis(),Color.GREEN);
                                        final CartesianFloat spinData = message.getData(CartesianFloat.class);
                                        if (reportToConsole)
                                            Log.i(TAG, spinData.toString());
                                        if(storeToFile) {

                                            capturedData.add(System.currentTimeMillis() + "," + message.getTimestamp().getTimeInMillis() + ","
                                                    + spinData.toString().replaceAll("[()]",""));
                                            if (capturedData.size() >= bufferSize){
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
                //Stream rotation data around the XYZ axes from the gyro sensor
                bmi160GyroModule.routeData().fromAxes().log(gyroLogKEY).commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                Log.i(TAG, "log data source correctly configured");
                                setStatus("Log working correctly",Color.GREEN);

                                result.setLogMessageHandler(gyroLogKEY, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        final CartesianFloat spinData = message.getData(CartesianFloat.class);
                                        Log.i(TAG, "Processing:" + String.format("Log: %s", spinData.toString()));
                                        if (reportToConsole)
                                            Log.i(TAG, spinData.toString());
                                        if(storeToFile) {
                                            capturedData.add(System.currentTimeMillis() + "," + message.getTimestamp().getTimeInMillis() + ","
                                                    + spinData.toString().replaceAll("[()]",""));
                                            if (capturedData.size() >= bufferSize){
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
     *     Stop Gyroscope sampling
     *
     */
    public void startGyroscope(){
        Log.i(TAG, "Starting at:"+System.currentTimeMillis());
        capturedData.add(captureMode +"," + System.currentTimeMillis() + ",-1,-1,-1");
        if(captureMode== MainActivity.CaptureMode.LOG) {
            logModule.startLogging();
            startPeriodicFlush();
        }
        bmi160GyroModule.start();
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

    // Start downloading the log
    // All MessageHandlers passed to setLogMessageHandler will be called
    //The first argument of downloadLog indicates the frequency of progress updates
    //expressed as a fraction between [0, 1] where 0= no updates, 0.1= 10 updates, 0.25= 4 updates, etc.
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
     *     Stop Gyroscope sampling
     *
     */
    public void stopGyroscope(){
        Log.i(TAG, "Final flush to " +filename);
        flushDataBuffer();
        if(captureMode== MainActivity.CaptureMode.LOG) {
            Log.i(TAG, "Stopping log");
            logModule.stopLogging();
            logDownloadHandler.removeCallbacks(periodicLogDownload);
        }
        bmi160GyroModule.stop();
    }


}
