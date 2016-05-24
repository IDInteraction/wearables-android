package uom.idinteractionmetawear;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Accelerometer;

import com.mbientlab.metawear.MetaWearBoard;

import com.mbientlab.metawear.module.Bmi160Accelerometer.AccRange;
import com.mbientlab.metawear.module.Bmi160Accelerometer.OutputDataRate;

import java.util.ArrayList;


/**
 * Created by Aitor on 26/04/2016.
 *
 * Class to provide access to the accelerometer sensor.
 * Implemented based on documentation found in https://mbientlab.com/androiddocs/latest/bmi160_accelerometer.html.
 */
public class Accelerometer {

    private Bmi160Accelerometer bmi160AccModule;

    private String filename;
    private ArrayList<String> capturedData;
    private int bufferSize = 100;
    private CsvDAO csvDAO;

    //Parameters for the gyroscope sensors
    private final AccRange scaleRange = AccRange.AR_16G;
    private final OutputDataRate outputDataRate = OutputDataRate.ODR_25_HZ;

    /**
     * Constructor. Returns null if the accelerometer module cannot be found in this device.
     * @param mwBoard
     */
    public Accelerometer(MetaWearBoard mwBoard, Context context, String outputFilename){
        try {
            bmi160AccModule= mwBoard.getModule(Bmi160Accelerometer.class);
            csvDAO = new CsvDAO(context,context.getExternalFilesDir(null));
            capturedData = new ArrayList<String>();
            filename = outputFilename;
        } catch (UnsupportedModuleException e) {
            bmi160AccModule = null;
        }
    }

    public Bmi160Accelerometer getAccModule(){
        return bmi160AccModule;
    }

    /**
     *     Start Accelerometer sampling
     *
     * @param reportToConsole if true, it will report the read values to the console
     * @param storeToFile if true, it will store the read values to a file
     */
    public void configureAccelerometer(final Boolean reportToConsole, final Boolean storeToFile) {
        // Set measurement range to +/- 16G
        // Set output data rate to 100Hz
        bmi160AccModule.configureAxisSampling()
                .setFullScaleRange(scaleRange)
                .setOutputDataRate(outputDataRate)
                .commit();
        // enable axis sampling
        bmi160AccModule.enableAxisSampling();

        // Switch the accelerometer to active mode
        bmi160AccModule.routeData().fromAxes().stream("accel_stream").commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("accel_stream", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message message) {
                                CartesianFloat cartesian = message.getData(CartesianFloat.class);
                                if (reportToConsole)
                                    Log.i("Accelerometer", cartesian.toString());
                                if(storeToFile) {
                                    capturedData.add(System.currentTimeMillis() + "," + message.getTimestamp().getTimeInMillis() + ","
                                            + cartesian.toString().replaceAll("[()]",""));
                                    if (capturedData.size() >= bufferSize){
                                        flushDataBuffer();
                                    }

                                }
                            }
                        });
                    }
                });
    }

    /**
     *     Stop Accelerometer sampling
     *
     */
    public void startAccelerometer(){
        Log.i("Accelerometer", "Starting at:"+System.currentTimeMillis());
        capturedData.add(System.currentTimeMillis() + ",-1,-1,-1");
        bmi160AccModule.start();
    }

    private void flushDataBuffer(){
        Log.i("Accelerometer", "Storing " + capturedData.size() +" to " + filename);
        for (int i=0; i<capturedData.size();i++) {
            csvDAO.writeToFile(filename, capturedData.get(i));
        }
        capturedData = new ArrayList<String>();
    }

    /**
     *     Stop Accelerometer sampling
     *
     */
    public void stopAccelerometer(){
        flushDataBuffer();
        Log.i("Accelerometer", "Final flush to " +filename);
        bmi160AccModule.stop();
    }
}
