package uom.idinteractionmetawear;

import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by Aitor on 20/05/2016.
 */
public class CsvDAO {

    public File dir;

    public CsvDAO(Context context, File contextPath){
        if (isExternalStorageWritable()){
            dir = Environment.getExternalStoragePublicDirectory("idinteractionmetawear");
            if (!dir.mkdirs()) {
                Log.e("CsvDAO.java", "creating folder Directory not created");
            }
            if (dir.exists())
                Log.i("CsvDAO.java", "creating folder"+dir.toString()+" folder already exists");
            else
                Log.i("CsvDAO.java", "creating folder"+dir.toString());
        }
        else
            Log.e("CsvDAO.java", "creating folder External storage is not writable");
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

     public void writeToFile(String filename, String content) {
        try {
            File tempFile = new File(dir,filename);
            Log.i("CsvDAO.java", "logging data to" + tempFile.toString());

            //Second parameter makes the function append to the text, rather than erase old content
            FileOutputStream stream = new FileOutputStream(tempFile,true);
            try {
                stream.write(content.getBytes());
                stream.write("\n".getBytes());
            } finally {
                stream.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public String readFromFile(Context context, String filename) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}
