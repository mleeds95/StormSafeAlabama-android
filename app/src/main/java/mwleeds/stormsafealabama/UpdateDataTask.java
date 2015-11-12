package mwleeds.stormsafealabama;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class downloads refuge area locations from Google Spreadsheets
 * and converts the CSV into GeoJSON format (if it has changed since last update).
 * params[0]: (String) the URL for the spreadsheet
 * params[1]: (Context) the application context
 * params[2]: (ProgressBar) the View for the progress bar
 */
public class UpdateDataTask extends AsyncTask<Object, Void, Integer> {

    public static final Integer PROCESSING_FAILED = -1;
    public static final Integer NO_UPDATE_AVAILABLE = 1;
    public static final Integer UPDATE_SUCCEEDED = 0;

    private Context appContext;
    private ProgressBar progressBar;
    private SharedPreferences preferences;

    @Override
    protected Integer doInBackground(Object... params) {
        String stringURL = (String) params[0];
        this.appContext = (Context) params[1];
        this.progressBar = (ProgressBar) params[2];
        this.preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        try {
            // download data from the specified URL into a CSV file
            int count;
            URL url = new URL(stringURL);
            URLConnection connection = url.openConnection();
            connection.connect();

            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            String outFile = appContext.getString(R.string.csv_filename);
            FileOutputStream output = appContext.openFileOutput(outFile, Context.MODE_PRIVATE);

            byte data[] = new byte[1024];
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            // check if the data is new by checking the MD5 sum
            FileInputStream checksumInputStream = appContext.openFileInput(appContext.getString(R.string.csv_filename));
            String newMD5 = calculateMD5(checksumInputStream);
            checksumInputStream.close();
            String oldMD5 = preferences.getString(appContext.getString(R.string.BARA_data_MD5), "");

            if (newMD5.equals(oldMD5)) {
                return NO_UPDATE_AVAILABLE;
            } else {
                // convert the CSV into GeoJSON format

                String inputFilename = appContext.getString(R.string.csv_filename);
                FileInputStream inputStream = appContext.openFileInput(inputFilename);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String outputFilename = appContext.getString(R.string.geojson_filename);
                FileOutputStream outputStream = appContext.openFileOutput(outputFilename, Context.MODE_PRIVATE);
                PrintWriter printWriter = new PrintWriter(outputStream);

                convertCSVToGeoJSON(reader, printWriter);

                reader.close();
                printWriter.close();

                // update the MD5 sum so we don't redo the work next time
                preferences.edit()
                           .putString(appContext.getString(R.string.BARA_data_MD5), newMD5)
                           .commit();

                return UPDATE_SUCCEEDED;
            }
        } catch (JSONException | IOException | NoSuchAlgorithmException e) {
            Log.e("Error: ", e.getMessage());
            return PROCESSING_FAILED;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        // inform the user of the result with a Toast
        if (result.equals(NO_UPDATE_AVAILABLE)) {
            Toast.makeText(appContext, R.string.no_update_avail, Toast.LENGTH_LONG).show();
        } else if (result.equals(PROCESSING_FAILED)) {
            Toast.makeText(appContext, R.string.processing_error, Toast.LENGTH_LONG).show();
        } else if (result.equals(UPDATE_SUCCEEDED)) {
            Toast.makeText(appContext, R.string.update_data_success, Toast.LENGTH_LONG).show();
        }

        // disappear the loading graphic
        progressBar.setVisibility(View.INVISIBLE);

        // start the Maps Activity
        Intent mapsIntent = new Intent(appContext, MapsActivity.class);
        mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(mapsIntent);
     }

    public static String calculateMD5(FileInputStream is)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest;
        digest = MessageDigest.getInstance("MD5");

        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        byte[] md5sum = digest.digest();
        BigInteger bigInt = new BigInteger(1, md5sum);
        String output = bigInt.toString(16);
        // Fill to 32 chars
        output = String.format("%32s", output).replace(' ', '0');
        return output;
    }

    private void convertCSVToGeoJSON(BufferedReader reader, PrintWriter printWriter)
            throws IOException, JSONException {
        CSVReader csvReader = new CSVReader(reader);
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");
        JSONArray featureList = new JSONArray();
        String[] line;
        reader.readLine(); // ignore column names
        // iterate over the rows, converting them to Points
        while ((line = csvReader.readNext()) != null) {
            if (line[4].length() < 3 || !line[4].contains(","))
                continue;

            JSONObject feature = new JSONObject();
            feature.put("type", "Feature");

            JSONObject point = new JSONObject();
            point.put("type", "Point");
            String transposedCoords = line[4].split(",")[1] + ", " + line[4].split(",")[0];
            JSONArray coord = new JSONArray("[" + transposedCoords + "]");
            point.put("coordinates", coord);
            feature.put("geometry", point);

            JSONObject properties = new JSONObject();
            properties.put("Building", line[0]);
            properties.put("BARA?", line[1]);
            properties.put("Floor", line[2]);
            properties.put("Description", line[3]);
            properties.put("Address", line[5]);
            feature.put("properties", properties);

            featureList.put(feature);
        }
        featureCollection.put("features", featureList);
        printWriter.write(featureCollection.toString());
    }
}
