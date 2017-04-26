package in.nisb.helmetmaps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.google.android.gms.location.LocationListener;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationServices;

public class MapsActivity extends AppCompatActivity {

    private GoogleMap mMap;
    ArrayList<LatLng> MarkerPoints;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    Toast t;

//    private BluetoothAdapter mBluetoothAdapter = null;
    String ip = "192.168.43.2";
    String address = "98:D3:31:B4:00:58";
    /**
     * Member object for the chat services
     */
//    private BluetoothChatService mChatService = null;

    int prec = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        // Initializing
        MarkerPoints = new ArrayList<>();


        Button btnip = (Button) findViewById(R.id.btn_ip);
        btnip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText e = (EditText) findViewById(R.id.ip_field);
                ip = "http://" + e.getText().toString();
                ToastIt("IP : " + ip);
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                //Initialize Google Play Services
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else {
                    mMap.setMyLocationEnabled(true);
                }

                // Setting onclick event listener for the map
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                    @Override
                    public void onMapClick(LatLng point) {

                        // Already two locations
                        if (MarkerPoints.size() > 1) {
                            MarkerPoints.clear();
                            mMap.clear();
                        }

                        // Adding new item to the ArrayList
                        MarkerPoints.add(point);

                        // Creating MarkerOptions
                        MarkerOptions options = new MarkerOptions();

                        // Setting the position of the marker
                        options.position(point);

                        /**
                         * For the start location, the color of marker is GREEN and
                         * for the end location, the color of marker is RED.
                         */
                        if (MarkerPoints.size() == 1) {
                            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        } else if (MarkerPoints.size() == 2) {
                            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        }


                        // Add new marker to the Google Map Android API V2
                        mMap.addMarker(options);

                        // Checks, whether start and end locations are captured
                        if (MarkerPoints.size() >= 2) {
                            LatLng origin = MarkerPoints.get(0);
                            LatLng dest = MarkerPoints.get(1);

                            // Getting URL to the Google Directions API
                            String url = getUrl(origin, dest);
                            Log.d("onMapClick", url.toString());
                            FetchUrl FetchUrl = new FetchUrl();

                            // Start downloading json data from Google Directions API
                            FetchUrl.execute(url);
                            //move map camera
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                        }

                    }
                });


            }
        });

        Button b = (Button) findViewById(R.id.setp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText e = (EditText) findViewById(R.id.precision);
                prec = Integer.parseInt(e.getText().toString());
            }
        });

        LocationManager lm = (LocationManager) getSystemService(this.LOCATION_SERVICE);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                ((TextView) findViewById(R.id.cur_pos)).setText(String.valueOf(location.getLatitude()+"000000000").substring(0,10) + " : " + String.valueOf(location.getLongitude()+"000000000").substring(0,10));

                if (DataParser.jStepsM!=null) {
                    for (int i = 0; i < DataParser.jStepsM.length(); i++) {
                        try {

                            if (String.valueOf(location.getLatitude()).substring(0, prec).equals(DataParser
                                    .jStepsM.getJSONObject(i)
                                    .getJSONObject("start_location")
                                    .getString("lat").substring(0, prec)) && String.valueOf(location.getLongitude()).substring(0, prec)
                                    .equals(DataParser
                                            .jStepsM.getJSONObject(i)
                                            .getJSONObject("start_location")
                                            .get("lng").toString().substring(0, prec))) {
                                String dir="";
                                if (DataParser.jStepsM.getJSONObject(i).has("maneuver")) {
                                    dir = DataParser.jStepsM.getJSONObject(i).getString("maneuver");
                                    //Toast.makeText(getApplicationContext(), dir, Toast.LENGTH_SHORT).show();
                                }


                                if (dir.equals("turn-left")
                                        || dir.equals("turn-slight-left")
                                        || dir.equals("ramp-left")
                                        || dir.equals("turn-sharp-left")
                                        || dir.equals("roundabout-left")
                                        || dir.equals("fork-left")) {
                                    //bluetooth left indicator
                                    sendData("L");

                                } else if (dir.equals("turn-right")
                                        || dir.equals("turn-slight-right")
                                        || dir.equals("ramp-right")
                                        || dir.equals("turn-sharp-right")
                                        || dir.equals("roundabout-right")
                                        || dir.equals("fork-right")) {
                                    //bluetooth right indicator
                                    sendData("R");
                                } else if (dir.equals("uturn-left")) {
                                    //bluetooth uturn left indicator
                                    sendData("UL");
                                } else if (dir.equals("uturn-right")) {
                                    //uturn right
                                    sendData("UR");

                                }


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }


            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
//        mChatService = new BluetoothChatService(getApplicationContext(), null);
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        ToastIt(device.getName());
//        // Attempt to connect to the device
//        mChatService.connect(device, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void ToastIt(String s){
        t = Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT);
        t.show();
    }

//    public void sendData(String data){
//
//                byte[] send = data.getBytes();
//                mChatService.write(send);
//
//    }


    public void sendData(final String data){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ip,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.length()>0)
                            Toast.makeText(getApplicationContext(),response,Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(),error.toString(),Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("turn",data);
                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }


    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }
}