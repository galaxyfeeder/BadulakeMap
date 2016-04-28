package com.tomorrowdev.badulakemap;

import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class MainActivity extends ActionBarActivity {

    private GoogleMap map;
    private MapView mapView;

    private boolean centeredMyLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.map);
        AdView mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder().addTestDevice("AC571141375B0EC2EB7FA688B5B58002").build();
        mAdView.loadAd(adRequest);

        mapView.onCreate(savedInstanceState);

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setScrollGesturesEnabled(true);
        map.setMyLocationEnabled(true);
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (!centeredMyLocation) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16);
                    map.animateCamera(cameraUpdate);
                    centeredMyLocation = true;
                }
            }
        });

        MapsInitializer.initialize(this);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(41.4, 2.17), 14);
        map.animateCamera(cameraUpdate);

        //getting data from db
        Realm realm = Realm.getInstance(this);
        List<Badulake> badulakes = realm.where(Badulake.class).findAll();
        addBadulakesToMap(badulakes);

        //updating data
        new GetBadulakesRequest().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    private boolean isNight(){
        int from = 2300;
        int to = 800;
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int t = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
        return to > from && t >= from && t <= to || to < from && (t >= from || t <= to);
    }

    private class GetBadulakesWithFilterRequest extends AsyncTask<String, Void, BadulakeResponse> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("I/O", "Request started");
        }

        @Override
        protected BadulakeResponse doInBackground(String... strings) {
            HashMap<String, String> params = new HashMap<>();
            params.put("latitude", strings[0]);
            params.put("longitude", strings[1]);
            Log.d("I/O", params.toString());

            BadulakeResponse response = new BadulakeResponse();
            try{
                URL url = new URL("http://badulakemap.herokuapp.com/badulake/filter?"+getQuery(params));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "");
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                connection.connect();

                String plain = readHttpInputStreamToString(connection);

                Log.d("I/O", plain);

                response.setStatus(connection.getResponseCode());
                response.setResponse(plain);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(BadulakeResponse response) {
            super.onPostExecute(response);
            Log.d("I/O", "Request finished with status " + response.getStatus());
            switch (response.getStatus()){
                case 200:
                    addBadulakesToMap(updateRealmWithPlainText(response.getResponse()));
                    break;
                default:
                    break;
            }
        }
    }

    private class GetBadulakesRequest extends AsyncTask<Void, Void, BadulakeResponse> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("I/O", "Request started");
        }

        @Override
        protected BadulakeResponse doInBackground(Void... strings) {
            BadulakeResponse response = new BadulakeResponse();
            try{
                URL url = new URL("http://badulakemap.herokuapp.com/badulake");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "");
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                connection.connect();

                String plain = readHttpInputStreamToString(connection);

                Log.d("I/O", plain);

                response.setStatus(connection.getResponseCode());
                response.setResponse(plain);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(BadulakeResponse response) {
            super.onPostExecute(response);
            Log.d("I/O", "Request finished with status " + response.getStatus());
            switch (response.getStatus()){
                case HttpURLConnection.HTTP_OK:
                    //updating the badulakes saved on the db
                    addBadulakesToMap(updateRealmWithPlainText(response.getResponse()));
                    break;
                default:
                    break;
            }
        }
    }

    private void addBadulakesToMap(List<Badulake> badulakes){
        map.clear();
        for(Badulake badulake : badulakes){
            if (badulake.isAlwaysopened()) {
                map.addMarker(new MarkerOptions()
                        .position(new LatLng(badulake.getLatitude(), badulake.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.badu24))
                        .title(badulake.getName())
                        .snippet(getString(R.string.always_opened))
                        .anchor(0.5f, 0.5f));
            } else {
                if(isNight()){
                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(badulake.getLatitude(), badulake.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.badu))
                            .title(badulake.getName())
                            .snippet(getString(R.string.probably_closed))
                            .alpha(0.5f)
                            .anchor(0.5f, 0.5f));
                }else{
                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(badulake.getLatitude(), badulake.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.badu))
                            .title(badulake.getName())
                            .snippet(getString(R.string.probably_opened))
                            .anchor(0.5f, 0.5f));
                }

            }
        }
    }

    private List<Badulake> updateRealmWithPlainText(String plain){
        Realm realm = Realm.getInstance(MainActivity.this);

        //removing all the badus from db
        realm.beginTransaction();
        List<Badulake> realmBadulakes = realm.where(Badulake.class).findAll();
        for(int i = 0; i < realmBadulakes.size(); i++)
            realmBadulakes.get(i).removeFromRealm();
        realm.commitTransaction();

        //adding the badus received from server
        realm.beginTransaction();
        List<Badulake> serverBadulakes = convertFromPlainToList(plain);
        for(Badulake badulake: serverBadulakes)
            realm.copyToRealmOrUpdate(badulake);
        realm.commitTransaction();
        return serverBadulakes;
    }

    private List<Badulake> convertFromPlainToList(String plain){
        List<Badulake> badulakes = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(plain);
            for(int i = 0; i < array.length(); i++){
                Badulake badulake = new Badulake();
                JSONObject jsonObject = array.getJSONObject(i);
                badulake.setAlwaysopened(jsonObject.getBoolean("alwaysopened"));
                badulake.setName(jsonObject.getString("name"));
                badulake.setLatitude(jsonObject.getDouble("latitude"));
                badulake.setLongitude(jsonObject.getDouble("longitude"));
                badulake.setId(jsonObject.getLong("id"));
                badulakes.add(badulake);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return badulakes;
    }

    private String getQuery(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode((String)entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode((String)entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private String readHttpInputStreamToString(HttpURLConnection connection) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        InputStream is = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        }
        catch (Exception e) {
            Log.i("I/O", "Error reading InputStream");
            result = null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    Log.i("I/O", "Error closing InputStream");
                }
            }
        }

        return result;
    }
}
