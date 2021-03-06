package chimehack.beheard;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//-----------------------------------------------------------------
import android.app.Dialog; // to use Dialog
import android.support.v4.app.FragmentActivity;
// To open menu and right-click menu items on top of screen
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
//-----------------------------------------------------------------
// Google Maps core classes
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
// Lattitude and Longitude for google maps
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
// Classes to be able to update camera for google maps
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
// To give location to a given text address
import android.location.Address;
import android.location.Geocoder;
// To work with user location
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import android.os.Bundle;

// need FragmentActivity instead of Activity since running on fragment in activity_maps.xml
// Need implement ConnectionCallbacks and OnConnectionFailedListener to get current location
public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final float initialZoom = 14;
    private static final double TWITTER_LAT = 37.776853, TWITTER_LNG = -122.416836;

    // To initialize how often to detect location changes
    private LocationRequest mLocationRequest;
    // Get pointers to point to newly created markers
    List<Marker> markers = new ArrayList<Marker>();

    // Nav Button
    Button mFeedButton;

    // Marker marker;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setUpMapIfNeeded();

        // Set up the GoogleApiClient to give back location data
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Button
        mFeedButton = (Button) findViewById(R.id.feed_button);
        mFeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(i);
            }
        });

        /*
        // Add in Parse
        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        String ApplicationIdParse= "ZpIt6iPESaAZmCacQ8gwPzRPiWpzSd1ojgpUMsvm";
        String ClientIdParse="6HVSSvx7ekbf4DyR6NA64jQuHBDg8EaOfyE9L6ex";
        Parse.initialize(this, ApplicationIdParse, ClientIdParse);
        // Enable Key
        ParseInstallation.getCurrentInstallation().saveInBackground();

        // Store an example test object
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();

        ParseCredentials pc = new ParseCredentials();
        //Parse.enableLocalDatastore(this);
        Parse.initialize(this, pc.getAPI_KEY(), pc.getCLIENT_KEY());
             */
        // parse
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect(); // connect the GoogleApiClient object

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        // Map State Saver
        MapStateManager mgr = new MapStateManager(this);
        // Try getting any saved position, if any
        CameraPosition position = mgr.getSavedCameraPosition();
        // If something was indeed saved
        if (position != null) {
           //  Update initial state of map programmatically
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
            mMap.moveCamera(update);
        }

        // Call geoLocate method
        geoLocate();

        // Get data for Post
        Post nearbyPost = new Post();
        // TODO: CHANGE TO CURRENT USER GIVEN LOCATION
        ParseGeoPoint location = new ParseGeoPoint(TWITTER_LAT,TWITTER_LNG);

        //ParseQuery<ParseObject> query = nearbyPost.getNearbyPosts(new ParseGeoPoint(0, 0));
        //Log.d("SOOOON", "ONE");


        ParseQuery<ParseObject> query = ParseQuery.getQuery("Post");
        query.whereNear("location", location);
        query.whereWithinMiles("location", location, 30);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> postList, ParseException e) {
                if (e == null) {
                    //Log.d("score", "Retrieved " + scoreList.size() + " scores");
                    // Iterate in reverse so latest overwrites.
                    for (int i = 0; i < postList.size(); i++) {
                            //Log.d("LALALA", postList.get(i).getString("message").toString());
                            ParseObject curr = postList.get(i);
                            String description = curr.getString("message");
                            ParseGeoPoint location = curr.getParseGeoPoint("location");
                            int severity = (int) curr.getNumber("severity");
                            setMarker(description, location.getLatitude(), location.getLongitude(), severity);
                            //Log.d("BABABA", temp);
                    }
                } else {
                    //Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
        //Log.d("SOOOON", "TWO");

    }

    @Override
    protected void onStop() {
        // Disconnect the location services
        mGoogleApiClient.disconnect();
        super.onStop(); // BIG NOTE: YOU MUST ALWAYS INCLUDE SUPER.ONSTOP() IN ANY OF THE OVERRIDE METHODS FOR START, STOP ETC!!!!!

        // State Saver
        // Create MapStateManager object to save the state of the map
        MapStateManager mgr = new MapStateManager(this);
        // Give the reference to the mMap object
        // and call the saveMapState method defiend in MapStateManager.java
        mgr.saveMapState(mMap);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the referecen of the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                // Add the My Location not_button to the map
                // which moves camera position to show user's current location
                // Method of finding the location is hidden from developer by Google
                // You will notice a small not_button on the right corner of the map,
                // Touch that and it will bring you to your location on the map.
                // Note: Will use more battery when this is pressed
                // cause will keep using power to use GPS
                // Can make sure don't use GPS by commenting out
                // uses:permission FINE_LOCATION on AndroidManifest.xml
                // but this function WILL USE GPS and won't work if don't enable GPS
                mMap.setMyLocationEnabled(true);
                // add the layout for customizedInfoWindow in Google Maps
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    // Returning null here will automatically call getInfoContents
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    // Get the information to be displayed on the current window
                    @Override
                    public View getInfoContents(Marker marker) {
                        // Get the layout file for InfoContent
                        View v = getLayoutInflater().inflate(R.layout.info_window, null); // null means dont attach to any parent window
                        TextView tvDescription = (TextView) v.findViewById(R.id.tv_description);
                        TextView tvSnippet = (TextView) v.findViewById(R.id.tv_snippet);

                        // Get position of marker
                        LatLng markerPos = marker.getPosition();
                        tvSnippet.setText(marker.getSnippet());
                        // Problem: How to pass the string into this argument?
                        // Solution 1: Call a function that this function can access and that function
                        //          will dynamically access the marker's data in a different database.
                        // Of course, you can use the marker's latitude and longitude as keys to that database
                        // as it will always be unique.
                        // Solution 2: Pass everything inside Snippet, comma separated and get the values there =)
                        //tvDescription.setText("This place is horrible, all the guys hit on me. I am never coming back to this party again");
                        tvDescription.setText(marker.getTitle());
                        // Return the view created
                        return v;
                    }
                });

                // This function is called each time a position on map is clicked
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    // It will pass in the latLng object of location that was clicked
                    @Override
                    public void onMapClick(LatLng latLng) {
                        //setMarker("Hey", latLng.latitude, latLng.longitude);
                        ParseGeoPoint location = new ParseGeoPoint(latLng.latitude,latLng.longitude);
                        //ParseQuery<ParseObject> query = nearbyPost.getNearbyPosts(new ParseGeoPoint(0, 0));
                        //Log.d("SOOOON", "ONE");

                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Post");
                        query.whereNear("location", location);
                        query.whereWithinMiles("location", location, 10);
                        query.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> postList, ParseException e) {
                                if (e == null) {
                                    //Log.d("score", "Retrieved " + scoreList.size() + " scores");
                                    for (int i = 0; i < postList.size(); i++) {
                                        //Log.d("LALALA", postList.get(i).getString("message").toString());
                                        ParseObject curr = postList.get(i);
                                        String description = curr.getString("message");
                                        ParseGeoPoint location = curr.getParseGeoPoint("location");
                                        int severity = (int) curr.getNumber("severity");
                                        setMarker(description, location.getLatitude(), location.getLongitude(), severity);                                        //Log.d("BABABA", temp);
                                    }
                                } else {
                                    //Log.d("score", "Error: " + e.getMessage());
                                }
                            }
                        });

                    }
                });


                mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng latLng) {
                        resetMarker();
                        ParseGeoPoint location = new ParseGeoPoint(latLng.latitude,latLng.longitude);

                        //ParseQuery<ParseObject> query = nearbyPost.getNearbyPosts(new ParseGeoPoint(0, 0));
                        //Log.d("SOOOON", "ONE");
                        gotoLocation(latLng.latitude, latLng.longitude, initialZoom);
                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Post");
                        query.whereNear("location", location);
                        query.whereWithinMiles("location", location, 10);
                        query.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> postList, ParseException e) {
                                if (e == null) {
                                    //Log.d("score", "Retrieved " + scoreList.size() + " scores");
                                    for (int i = 0; i < postList.size(); i++) {
                                        //Log.d("LALALA", postList.get(i).getString("message").toString());
                                        ParseObject curr = postList.get(i);
                                        String description = curr.getString("message");
                                        ParseGeoPoint location = curr.getParseGeoPoint("location");
                                        int severity = (int) curr.getNumber("severity");
                                        setMarker(description, location.getLatitude(), location.getLongitude(), severity);                                        //Log.d("BABABA", temp);
                                    }
                                } else {
                                    //Log.d("score", "Error: " + e.getMessage());
                                }
                            }
                        });

                    }
                });
                //setUpMap();

            }
        }
    }
    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    // Set initial location with zoom of mMap programmatically
    // uses newLatLngZoom()
    private void gotoLocation(double latitude, double longitude, float zoomValue) {
        // Create an object to represent location to be displayed
        LatLng locationObject = new LatLng(latitude, longitude);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(locationObject, zoomValue);
        mMap.moveCamera(update);
        // set marker to new object
        //setMarker("Other Marker", latitude, longitude);
    }

    // To locate object that are near a given text using Google's GeoCoder API
    public void geoLocate()  {
        // Hide the keyboard that was given to user to type
        //hideSoftKeyboard(v);

        // Get the input text that was typed
       // EditText et = (EditText) findViewById(R.id.editText1);
        //String location = et.getText().toString();

        // TODO: REMOVE THIS
        //String location = "Toronto";

        // Initialize Geocoder and search
        //Geocoder gc = new Geocoder(this);
        // Get only 1 address from the function
        // note: May throw IO Exception
        try {
            /*List<Address> list = gc.getFromLocationName(location, 1);
            Address addr = list.get(0); // Get the first element in the List
            String locality = addr.getLocality();
            // Output to user what was returned from what was typed
            //Toast.makeText(this, locality, Toast.LENGTH_LONG).show();

            // Get latitude and longitude values
            double latitude = addr.getLatitude();
            double longitude = addr.getLongitude();

            // Move map to that location
            gotoLocation(latitude, longitude, initialZoom); */
            gotoLocation(TWITTER_LAT, TWITTER_LNG, initialZoom);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    // This removes all current markers on the map
    private void resetMarker() {
        int numberOfMarkers = markers.size();
        // Remove any previous markers
        for(Marker item: markers){
            System.out.println("retrieved element: " + item);
            item.remove();
        }
    }

    // This allows you to set a marker to a map object
    private void setMarker(String description, double latitude, double longitude, int severity) {
        // Make a new GeoCoder object
        Geocoder gc = new Geocoder(this);
        try {
            // Get the location name suggested from Google Maps
            List<Address> list = gc.getFromLocation(latitude, longitude, 1);
            Address addr = list.get(0); // Get the first element in the List
            String address = addr.getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addr.getLocality();
            String state = addr.getAdminArea();
            String country = addr.getCountryName();
            String postalCode = addr.getPostalCode();
            String knownName = addr.getFeatureName();
            String snippetTitle = address;

            MarkerOptions options;
            if (severity >= 4) {
                // For customize graphics for marker, use this
                options = new MarkerOptions().title(description)
                        .position(new LatLng(latitude, longitude))
                        .snippet(snippetTitle)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.red)); // color the marker to orange
            }

            else if (severity >=2) {
                options = new MarkerOptions().title(description)
                        .position(new LatLng(latitude, longitude))
                        .snippet(snippetTitle)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.orange)); // color the marker to orange
            }
            else {
                options = new MarkerOptions().title(description)
                        .position(new LatLng(latitude, longitude))
                        .snippet(snippetTitle)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow)); // color the marker to orange
            }
            Marker marker = mMap.addMarker(options);
            markers.add(marker);
            //snippetTitle = knownName+ ", " + city + ", " + postalCode+", " + address;
            snippetTitle = address;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // For default graphics for marker, use this
        //MarkerOptions options = new MarkerOptions().title(markerTitle)
        //        .position(new LatLng(latitude, longitude))
        //        .snippet("haha")
        //        .icon(BitmapDescriptorFactory.fromResource(R.drawable.pictureName); // color the marker to orange
        // Use this tol to generate custom made markers if you like https://romannurik.github.io/AndroidAssetStudio/

        // Add Marker to the mMap current location
        // Note: Will not remove existing markers
        // Thus, assigned it to created marker reference above to remove later
        // Assign to marker to be removed in future

    }

    // Hide the softkeyboard from the screen
    private void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    // When connection successful, this method is executed
    @Override
    public void onConnected(Bundle connectionHint) {
        // Create a request for location updates
        mLocationRequest = LocationRequest.create();
        // Set to suck more battery but high accuracy of location
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000000); // Update location every 1000 seconds

        // Start requesting for location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        //Toast.makeText(this, "Soon: Success connect to Location Service", Toast.LENGTH_SHORT).show();
        return;
    }

    // When connection is unsuccessful, this method is executed
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        //Toast.makeText(this, "Soon: Failure connect to Location Service", Toast.LENGTH_SHORT).show();
        return;
    }

    // When disconnect occurs, this method is executed
    @Override
    public void onConnectionSuspended(int cause) {
        //Toast.makeText(this, "Soon: Disconnect from Location Service", Toast.LENGTH_SHORT).show();
        return;
    }

    // This method is called every time interval that is set above
    // in the setInterval() method
    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(this, "Soon: Latest Location is: " + location.toString(), Toast.LENGTH_SHORT).show();
        return;
    }


    // This function is where you do REST API or whatever to get the data needed to put into your markers
    private String getDescriptionFromMarkerSoon(double latitude, double longitude) {

        // Access database or data structure here using arguments as key
        if (longitude > 0) {
            return "I am a boy";
        } else {
            return "I am a girl";
        }
    }

}
