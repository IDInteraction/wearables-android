package uom.idinteractionmetawear;

import android.util.Log;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Logging.DownloadHandler;

/**
 * Created by Aitor on 03/05/2016.
 */
public class LoggingModule {

    private Logging logModule;

    public LoggingModule(MetaWearBoard mwBoard){
        try{
        logModule= mwBoard.getModule(Logging.class);
        } catch (UnsupportedModuleException e) {
            logModule = null;
        }

    }
    // start logging, if log is full, no new data will be added
    //logModule.startLogging();

    // start logging, if log is full, overrite existing data
    //logModule.startLogging(true);
}
