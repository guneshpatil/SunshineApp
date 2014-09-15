package com.example.gunesh.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gunesh on 9/12/2014.
 */
public class ForecastFragment extends Fragment {

    public ArrayAdapter<String> forecastAdapter;
    private String pincode_preference, unit_preference;
    private SharedPreferences preferences;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        List<String> weatherForecasts = new ArrayList<String>();

        forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast,
                R.id.list_item_textview, weatherForecasts);

        ListView lv = (ListView) rootView.findViewById(R.id.listView_forecast);
        lv.setAdapter(forecastAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String clickedText = forecastAdapter.getItem(position).toString();
                Toast.makeText(getActivity(), clickedText, Toast.LENGTH_SHORT).show();
                Intent detailIntent = new Intent(getActivity(), DetailForecastActivity.class)
                        .putExtra("DetailData", clickedText);
                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchWetherFromPreference();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.view_in_map)
        {
            viewLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void viewLocationInMap()
    {
        pincode_preference = preferences.getString("Location", "411046");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="+pincode_preference));
        startActivity(mapIntent);
    }

    public void fetchWetherFromPreference()
    {
        fetchWeatherData atSample = new fetchWeatherData();
        pincode_preference = preferences.getString("Location", "411046");
        unit_preference = preferences.getString("units_list", "metric");
        atSample.execute(pincode_preference);
    }

    public class fetchWeatherData extends AsyncTask<String, Void, String[]>
    {
        private final String LOG_TAG = fetchWeatherData.class.getSimpleName();
        private final WeatherDataParser weatherDataParser = new WeatherDataParser();
        private final int numOfDays = 7;

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String[] weatherJSONData = null;

// Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String MODE_PARAM = "json";
                final String UNIT_PARAM = "units";
                final String COUNT_PARAM = "cnt";

                Uri builtURL = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(MODE_PARAM, "json")
                        .appendQueryParameter(UNIT_PARAM, unit_preference)
                        .appendQueryParameter(COUNT_PARAM, "7")
                        .build();
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
                URL url = new URL(builtURL.toString());
                Log.v(LOG_TAG, url.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                weatherJSONData = weatherDataParser.getWeatherDataFromJson(forecastJsonStr, numOfDays);
                Log.i(LOG_TAG, weatherJSONData[0]);

                Log.i(LOG_TAG, "The Service Response: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return weatherJSONData;
           }

        @Override
        protected void onPostExecute(String[] strings) {
            if(strings != null)
            {
                forecastAdapter.clear();
                for(String forecast : strings)
                {
                    forecastAdapter.add(forecast);
                }
            }
        }
    }
}
