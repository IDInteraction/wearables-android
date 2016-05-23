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

import java.util.Date;


/**
 * Created by Aitor on 26/04/2016.
 *
 * Class to provide access to the gyroscope sensor.
 * Implemented based on documentation found in https://mbientlab.com/androiddocs/latest/bmi160_gyro.html.
 */
public class Gyroscope {

    private Bmi160Gyro bmi160GyroModule;
    private CsvDAO csvDAO;

    /**
     * Constructor. Returns null if the accelerometer module cannot be found in this device.
     *
     * @param mwBoard
     */
    public Gyroscope(MetaWearBoard mwBoard, Context context){
        try {
            bmi160GyroModule = mwBoard.getModule(Bmi160Gyro.class);
            csvDAO = new CsvDAO(context,context.getExternalFilesDir(null));;

        } catch (UnsupportedModuleException e) {
            bmi160GyroModule = null;
        }
    }

    /**
     *     Start Gyroscope sampling
     *
     * @param reportToConsole if true, it will report the read values to the console
     * @param storeToFile if true, it will store the read values to a file
     * @param filename the name of the file where the data will be stored (if storeToFile is true)
     */
    public void activateGyroscope(final Boolean reportToConsole, final Boolean storeToFile, final String filename) {
        // Set the measurement range to +/-2000 degrees/s
        // Set output data rate to 100Hz
        bmi160GyroModule.configure()
                .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_2000)
                .setOutputDataRate(OutputDataRate.ODR_100_HZ)
                .commit();

        //Stream rotation data around the XYZ axes from the gyro sensor
        bmi160GyroModule.routeData().fromAxes().stream("gyro_stream").commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("gyro_stream", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                final CartesianFloat spinData = msg.getData(CartesianFloat.class);
                                if (reportToConsole)
                                    Log.i("Gyroscope", spinData.toString());
                                if(storeToFile) {
                                    //prepare the csv line to store
                                    csvDAO.writeToFile(filename, System.currentTimeMillis() + ","
                                            + spinData.toString().replaceAll("[()]",""));
                                }
                            }
                        });
                    }
                });
        bmi160GyroModule.start();

    }

    /**
     *     Stop Gyroscope sampling
     *
     */
    public void stopGyroscope(){
        bmi160GyroModule.stop();
    }
}
