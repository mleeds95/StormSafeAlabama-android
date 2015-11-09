package mwleeds.stormsafealabama;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class downloads refuge area locations from Google Spreadsheets
 * and converts the CSV into GeoJSON format.
 * params[0]: (String) the URL for the spreadsheet
 * params[1]: (Context) the application context
 * params[2]: (ProgressBar) the View for the progress bar
 */
public class DownloadFileTask extends AsyncTask<Object, Void, Integer> {

    Context appContext;
    ProgressBar progressBar;

    @Override
    protected Integer doInBackground(Object... params) {
        String stringURL = (String) params[0];
        appContext = (Context) params[1];
        progressBar = (ProgressBar) params[2];
        try {
            int count;
            URL url = new URL(stringURL);
            URLConnection connection = url.openConnection();
            connection.connect();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            // Output stream
            String outFile = appContext.getString(R.string.csv_filename);
            FileOutputStream output = appContext.openFileOutput(outFile, Context.MODE_PRIVATE);

            byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            return -1;
        }
        //TODO convert CSV -> GeoJSON
        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != 0) {
            Toast.makeText(appContext, R.string.download_error, Toast.LENGTH_LONG).show();
        }
        progressBar.setVisibility(View.INVISIBLE);
    }
}
