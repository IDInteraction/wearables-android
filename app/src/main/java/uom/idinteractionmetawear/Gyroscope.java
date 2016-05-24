package uom.idinteractionmetawear;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmi160Gyro.OutputDataRate;

import java.util.ArrayList;
import java.util.Date;


/**
 * Created by Aitor on 26/04/2016.
 *
 * Class to provide access to the gyroscope sensor.
 * Implemented based on documentation found in https://mbientlab.com/androiddocs/latest/bmi160_gyro.html.
 */
public class Gyroscope {

    private Bmi160Gyro bmi160GyroModule;

    private String filename;
    private ArrayList<String> capturedData;
    private int bufferSize = 100;
    private CsvDAO csvDAO;

    //Parameters for the gyroscope sensors
    private final Bmi160Gyro.FullScaleRange scaleRange = Bmi160Gyro.FullScaleRange.FSR_2000;
    private final OutputDataRate outputDataRate = OutputDataRate.ODR_25_HZ;

    /**
     * Constructor. Returns null if the accelerometer module cannot be found in this device.
     *
     * @param mwBoard
     */
    public Gyroscope(MetaWearBoard mwBoard, Context context,String outputFilename){
        try {
            bmi160GyroModule = mwBoard.getModule(Bmi160Gyro.class);
            csvDAO = new CsvDAO(context,context.getExternalFilesDir(null));
            capturedData = new ArrayList<String>();
            filename = outputFilename;

        } catch (UnsupportedModuleException e) {
            bmi160GyroModule = null;
        }
    }

    /**
     *     Start Gyroscope sampling
     *
     * @param reportToConsole if true, it will report the read values to the console
     * @param storeToFile if true, it will store the read values to a file
     */
    public void configureGyroscope(final Boolean reportToConsole, final Boolean storeToFile) {
        // Set the measurement range to +/-2000 degrees/s
        // Set output data rate to 100Hz
        bmi160GyroModule.configure()
                .setFullScaleRange(scaleRange)
                .setOutputDataRate(outputDataRate)
                .commit();

        //Stream rotation data around the XYZ axes from the gyro sensor
        bmi160GyroModule.routeData().fromAxes().stream("gyro_stream").commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("gyro_stream", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message message) {
                                final CartesianFloat spinData = message.getData(CartesianFloat.class);
                                if (reportToConsole)
                                    Log.i("Gyroscope", spinData.toString());
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
                });

    }

    /**
     *     Stop Gyroscope sampling
     *
     */
    public void startGyroscope(){
        Log.i("Gyroscope", "Starting at:"+System.currentTimeMillis());
        capturedData.add(System.currentTimeMillis() + ",-1,-1,-1");
        bmi160GyroModule.start();
    }

    private void flushDataBuffer(){
        Log.i("Gyroscope", "Storing " + capturedData.size() +" to " + filename);
        for (int i=0; i<capturedData.size();i++) {
            csvDAO.writeToFile(filename, capturedData.get(i));
        }
        capturedData = new ArrayList<String>();
    }
    /**
     *     Stop Gyroscope sampling
     *
     */
    public void stopGyroscope(){
        Log.i("Gyroscope", "Final flush to " +filename);
        flushDataBuffer();
        bmi160GyroModule.stop();
    }
}
