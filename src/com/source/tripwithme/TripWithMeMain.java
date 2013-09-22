package com.source.tripwithme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseFacebookUtils;
import com.parse.ParseGeoPoint;
import com.source.tripwithme.R.id;
import com.source.tripwithme.R.string;
import com.source.tripwithme.components.Country;
import com.source.tripwithme.components.CountryFactory;
import com.source.tripwithme.components.LimitedListWithListeners;
import com.source.tripwithme.components.ListWithListeners;
import com.source.tripwithme.components.ListenerOnCollection;
import com.source.tripwithme.components.PointWithDistance;
import com.source.tripwithme.components.PointWithID;
import com.source.tripwithme.components.TreeSetWithListeners;
import com.source.tripwithme.databases.InterestsDatabase;
import com.source.tripwithme.databases.InterestsDatabase.DoneLoadingCallBack;
import com.source.tripwithme.databases.OneFoundCallback;
import com.source.tripwithme.databases.ParseUtil;
import com.source.tripwithme.databases.ParseUtil.DetailsFoundCallback;
import com.source.tripwithme.main_ui.CountriesDialog;
import com.source.tripwithme.main_ui.MapAndOnIt;
import com.source.tripwithme.main_ui.UsersManagerUI;
import com.source.tripwithme.managers.LocationManager;
import com.source.tripwithme.managers.MessagesHelper;
import com.source.tripwithme.people_menu.PeopleMenu;
import com.source.tripwithme.people_menu.UserItemAdapter;
import com.source.tripwithme.visible_data.BaseVisibleData;
import com.source.tripwithme.visible_data.PersonVisibleData;
import com.source.tripwithme.visible_data.RemoverCallback;
import com.source.tripwithme.visible_data.TappedCallback;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class TripWithMeMain extends Activity implements ListenerOnCollection<PointWithID>, LocationListener {

    // distance consts and limits
    private static final double INITIAL_INTEREST_DISTANCE = 50;
    public static final double NEW_COUNTRY_INTEREST_DISTANCE = 80;
    private static final int INTERESTS_LIMIT = 5;

    private static final long ONE_MINUTE_MILLIS = 60000L;

    // formating consts
    public static final DecimalFormat DECIMAL_FORMAT_TWO_POINTS = new DecimalFormat("#.00");
    public static final DecimalFormat DECIMAL_FORMAT_ONE_POINT = new DecimalFormat("#.#");

    // activities returned
    private static final int RETURNED_PIC_CODE = 1000;
    public static final int REQUEST_CODE_FACEBOOK = 2000;
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 3000;

    // states
    private static final String CHECKED_IN = "Checked in State";
    private static final String CHECKED_OUT = "Checked out State";

    // handler
    public static final int REQUEST_PHOTO_HANDLER = 1500;
    public static final int SHOW_SAVE_LOCATION_ERROR_HANDLER = 2500;
    public static final int ADAPTER_NEW_PERSON_HANDLER = 3500;
    public static final int RESOLVE_TO_BITMAP_HANDLER = 4500;


    private static final String UPDATE_TAG = "UpdateState";
    private static final String GOOGLE_SERVICES_TAG = "GoogleSevices";


    // init right away - should be final
    public static String APP_NAME;

    private Handler guiHandler;

    // peple list and adapter
    private TreeSetWithListeners<PersonVisibleData> people;
    private UserItemAdapter adapter;

    // core components
    private MapAndOnIt mapAndOnIt;
    private ListRemoverCallback remover;
    private PersonTapperToMapCallback tapper;
    private CountryFactory countryFactory;

    // interesets
    private InterestsDatabase db;
    private ListWithListeners<PointWithID> interests;
    private boolean suppressAddingInterests;
    private Button btnAroundYou;

    // parse util + me
    private ParseUtil parseUtil;
    private PersonVisibleData me;

    // record state for sleep and all...
    private String lastState;

    // messages
    private ParseGeoPoint lastInterestGeoForMsgs;

    // location
    private boolean refershLocation;

    private double lastDistance;

    private boolean readyForNextLocationChange;

    private boolean appIsOn;

    private LocationManager loationManager;

    private long lastSucceededChange;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screenone);

        appIsOn = false;

        initCoreFields();

        startLists();

        startLocalDB();

        startLocationDealing();

        startParse(new DetailsFoundCallback() {
            @Override
            public void details(PersonVisibleData personVisibleData) {
                me = personVisibleData;

                startMassages();

                startGUIComponents();

                appIsOn = true;

                boolean updatedLocationFirstTime = updateByLocationIfCan();
                if (!updatedLocationFirstTime) {
                    enableButtons(); // allow user to get locations in other way.
                }
            }
        });
    }

    private boolean updateByLocationIfCan() {
        Location lastKnown = loationManager.getCurrentLocation();
        return currentLocationAsInterestPoint(lastKnown, System.currentTimeMillis());
    }

    private void startMassages() {
        MessagesHelper messagesHelper = new MessagesHelper(this, people);
        messagesHelper.start();
    }


    private void startGUIComponents() {
        /* Start Gui Components - at end */
        GoogleMap mMap = ((MapFragment)getFragmentManager().findFragmentById(id.map)).getMap();
        try {
            mapAndOnIt = new MapAndOnIt(mMap, this, interests, countryFactory, parseUtil, me);
        } catch (Exception e) {      // problems with play services!!!
            if (!servicesConnected()) {
                Toast.makeText(this, "Fix Google Play Services and restart application", Toast.LENGTH_LONG).show();
            } else {
                Log.e(GOOGLE_SERVICES_TAG, "Failure in map", e);
                showErrorInMapAndExit();
            }
        }
        people.addListListener(mapAndOnIt);    // map contains parralel list with it own use
        addPeopleMenuAdapter();
    }

    private void addPeopleMenuAdapter() {
        adapter = new UserItemAdapter(this, new ArrayList<PersonVisibleData>());
        people.addListListener(adapter.getNewListener(guiHandler));
        new PeopleMenu(adapter, getString(string.selectPopoleStr)); // statically add adapater...
    }

    private void showErrorInMapAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Error in Google Map or Play Services had accoured.").setTitle(
            "You've got serious error with Google Maps !!!")
            .setCancelable(true).setPositiveButton("Exit now :(", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(1);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startLocationDealing() {
        // every time user start with its own location
        refershLocation = true;
        // boolean to not interupt ongoing location change
        readyForNextLocationChange = true;

        loationManager = LocationManager.instance(this);

        btnAroundYou = (Button)this.findViewById(id.aroundyou);
        btnAroundYou.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refershLocation = true;
                boolean tryUpdate = updateByLocationIfCan();
                if (!tryUpdate) {
                    Toast.makeText(TripWithMeMain.this, "Can't update location - please wait and try again!",
                                   Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    private void startParse(DetailsFoundCallback callback) {
        // init parse only w/o user
        parseUtil = ParseUtil.instance(this, countryFactory);

        parseUtil.getInitialDetailBackground(callback);

    }

    private void startLocalDB() {
        suppressAddingInterests = true;
        db = InterestsDatabase.init(this, interests, new DoneLoadingCallBack() {
            @Override
            public void doneLoading() {
                suppressAddingInterests = false;
            }
        });
    }

    private void startLists() {
        /* start people list */
        people = new TreeSetWithListeners<PersonVisibleData>();

        /* start interests list */
        interests = new LimitedListWithListeners<PointWithID>(INTERESTS_LIMIT);
        interests.addListListener(this);
    }

    private void initCoreFields() {
        APP_NAME = getString(string.app_name);
        remover = new ListRemoverCallback();
        tapper = new PersonTapperToMapCallback();
        countryFactory = CountryFactory.getLanguageFactorySigelton(this);
        lastDistance = INITIAL_INTEREST_DISTANCE;
        guiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == REQUEST_PHOTO_HANDLER) {
                    Intent getImage = new Intent(Intent.ACTION_GET_CONTENT);
                    getImage.setType("image/*");
                    startActivityForResult(getImage, RETURNED_PIC_CODE);
                } else if (msg.what == SHOW_SAVE_LOCATION_ERROR_HANDLER) {
                    Toast.makeText(TripWithMeMain.this,
                                   "Failure to update your location to server, please check network",
                                   Toast.LENGTH_LONG).show();
                } else if (msg.what == ADAPTER_NEW_PERSON_HANDLER) {
                    PersonVisibleData person = (PersonVisibleData)msg.obj;
                    // Search if user is relevant or resolving took too long and user no longer needed
                    if (person.isMatchToCurrentRequest(parseUtil.getRequestNumber())) {
                        adapter.add(person);
                        adapter.updateFriendsCount();
                    } else {
                        Log.d("AdapterAddPerson", "OLD Person isn't added to friends list: " + person.name());
                    }
                    adapter.publishEnd();
                } else if (msg.what == RESOLVE_TO_BITMAP_HANDLER) {
                    Bitmap bitmap = (Bitmap)msg.obj;
                    UsersManagerUI.bitmapReceived(TripWithMeMain.this, bitmap);
                }
            }
        };
    }

    //private void addOneForEachCountryForDebug() {
    //    List<Country> countriesList = countryFactory.getAllCountries();
    //    for (Country c : countriesList) {
    //        parseUtil.addNewPerson(c.getFullName() + " Person", c.getGeoPoint(), c);
    //    }
    //}

    private boolean currentLocationAsInterestPoint(Location currentLocation, long timeOfRequestMillis) {
        if (currentLocation != null) {
            if (readyForNextLocationChange && appIsOn) {
                readyForNextLocationChange = false; // will be ready only after end of people fetching from DB
                lastSucceededChange = timeOfRequestMillis;
                Log.d(LocationManager.LOCATION_HANDELING_TAG,
                      "Conditions FILLED in setting current location as interest point!");
                double lat = currentLocation.getLatitude();
                double longi = currentLocation.getLongitude();
                // show in map on ui thread
                mapAndOnIt.showNewLocationOnMap(new LatLng(lat, longi));
                // try using current place somehow
                final ParseGeoPoint finalGeoPoint = new ParseGeoPoint(lat, longi);
                new AsyncTask<Void, Void, PointWithDistance>() {
                    @Override
                    protected void onPostExecute(PointWithDistance interestPoint) {
                        // only after we have uploaded the user well to the cloud
                        iterestPointHasChanged(interestPoint);
                    }

                    @Override
                    protected PointWithDistance doInBackground(Void... params) {
                        Country country = countryFactory.getByNetSearchBlock(finalGeoPoint);
                        parseUtil.updateUserLocationAndMeBlock(finalGeoPoint, country, me, guiHandler);
                        return new PointWithDistance(finalGeoPoint, lastDistance, country.getFullName());
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return true;
            } else {
                return false;
            }
        } else {
            Toast.makeText(this, "Can't update location - please enable in device!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RETURNED_PIC_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            UsersManagerUI.resultImageInUri(this, uri);
        } else if (requestCode == REQUEST_CODE_FACEBOOK) {
            try {
                ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
            } catch (Exception e) {
                Log.e(ParseUtil.FACEBOOK_TAG, "Problem with Facebook login", e);
                Toast.makeText(this, "Problem with Facebook login", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
            switch (resultCode) {
                // If Google Play services resolved the problem
                case Activity.RESULT_OK:
                    // Log the result
                    Log.d(LocationManager.LOCATION_HANDELING_TAG, "Was resolved");
                    break;
                // If any other result was returned by Google Play services
                default:
                    // Log the result
                    Log.d(LocationManager.LOCATION_HANDELING_TAG, "Not Resolved !!!");
                    break;
            }
        }
    }

    // click on signup button
    public void showSignupUpdateDialog(boolean asMust) {
        UsersManagerUI.showUpdateSignupDialog(this, parseUtil, guiHandler, me, asMust);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case id.updateuseritem:
                showSignupUpdateDialog(false);
                break;
            case id.showallcountries: {
                showCountriesDialog();
                break;
            }
            case id.aroundme: {
                showRefreshMeDialog();
                break;
            }
            case id.savedInterests: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Old Locations - Click to choose");
                int pointSize = interests.size();
                String[] items = new String[pointSize];
                for (int i = 0; i < pointSize; i++) {
                    PointWithDistance pointSaved = interests.get(i).getPoint();
                    items[i] = getPlaceString(pointSaved) + "  (" +
                               DECIMAL_FORMAT_ONE_POINT.format(pointSaved.getDistance()) + " Kms Radius)";
                }
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        PointWithDistance saved = interests.get(item).getPoint();
                        refershLocation = false;
                        iterestPointHasChanged(saved);
                    }
                }).show();
            }
        }
        return true;
    }

    private void showRefreshMeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.refreshmedialog);
        Button aroundMeButton = (Button)dialog.findViewById(id.aroundmenowbutton);
        aroundMeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAroundYou.performClick();
            }
        });
        EditText radiusEdit = (EditText)dialog.findViewById(id.radiusedit);
        radiusEdit.setText(DECIMAL_FORMAT_TWO_POINTS.format(lastDistance));
        radiusEdit.addTextChangedListener(new TextWatcherAfterOnly() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double typed = Double.parseDouble(s.toString().trim());
                    if (typed > 0) {
                        lastDistance = typed;
                    }
                } catch (Exception ignored) {
                }
            }
        });
        EditText intervalEdit = (EditText)dialog.findViewById(id.changeintervaledit);
        intervalEdit.setText(Long.toString(loationManager.getMillisIntervalBetweenRefresh() / ONE_MINUTE_MILLIS));
        intervalEdit.addTextChangedListener(new TextWatcherAfterOnly() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    long typedToMilis = Integer.parseInt(s.toString().trim()) * ONE_MINUTE_MILLIS;
                    if (typedToMilis > 0) {  // in big numbers there can be overflaw
                        loationManager.setMillisIntervalBetweenRefresh(typedToMilis);
                    }
                } catch (Exception ignored) {
                }
            }
        });
        Button returnToMapButton = (Button)dialog.findViewById(id.returntomap);
        returnToMapButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setTitle("Around Me");
        dialog.setCancelable(true);
        dialog.show();
    }

    private void showCountriesDialog() {
        CountriesDialog.getNewDialog(this, countryFactory, interests).show();

    }

    private String getPlaceString(PointWithDistance pointSaved) {
        String ret = pointSaved.getSpecialName();
        if (ret != null) {
            return ret;
        }
        LatLng latLng = pointSaved.getLatLng();
        return DECIMAL_FORMAT_TWO_POINTS.format(latLng.latitude) + " , " +
               DECIMAL_FORMAT_TWO_POINTS.format(latLng.longitude);
    }


    private void updateServerOfInterstChange(PointWithDistance point) {
        new UpdateServerTask(point).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // method for person dialog before IM
    public boolean signUpIsMissing() {
        return !parseUtil.isSignedUp();
    }

    // method for messagesHelper
    public String getUserUniqueID() {
        if (me != null) {
            return me.getUniqueChatID();
        } else {
            return null;
        }
    }

    // method for messagesHelper
    public PersonVisibleData getPersonFromServerBlock(String personID) {
        return parseUtil.getPersonFromDBlock(personID, lastInterestGeoForMsgs, remover, tapper);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (refershLocation) {  // minimal check unique for listener
            // check time because updates can come faster by Android doc, and to sychronize with one time requests.
            long now = System.currentTimeMillis();
            if (now - lastSucceededChange >= loationManager.getMillisIntervalBetweenRefresh()) {
                currentLocationAsInterestPoint(location, now);
            }
        }
    }


//public boolean onKeyDown(int keyCode, KeyEvent event) {
//    if (_firstScreen == null) {
//        return false;
//    }
//    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//        _firstScreen.dpadUp();
//        return true;
//    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
//        _firstScreen.dpadDown();
//        return true;
//    }
//    return (super.onKeyDown(keyCode, event));
//}


    private class UpdateServerTask extends AsyncTask<Void, PersonVisibleData, Void> {

        private final PointWithDistance point;
        private final ProgressBar progressBar;

        public UpdateServerTask(PointWithDistance point) {
            this.point = point;
            progressBar = (ProgressBar)findViewById(id.progressBar);
        }

        @Override
        protected void onPreExecute() {
            disableButtons();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... unused) {
            parseUtil.startGettingPeopleBlock(point.getParseGeoPosition(), point.getDistance(),
                                              new OneFoundCallback<PersonVisibleData>() {
                                                  @Override
                                                  public void found(PersonVisibleData person) {
                                                      publishProgress(person);
                                                  }
                                              }, remover, tapper);
            return null;
        }

        @Override
        protected void onProgressUpdate(PersonVisibleData... newPeople) {
            for (PersonVisibleData person : newPeople) {
                people.add(person);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            enableButtons();
            progressBar.setVisibility(View.INVISIBLE);
            readyForNextLocationChange = true;
        }
    }

    private void disableButtons() {
        if (btnAroundYou != null) {
            btnAroundYou.setEnabled(false);
        }
        mapAndOnIt.disableLocationButtons();
    }

    private void enableButtons() {
        if (btnAroundYou != null) {
            btnAroundYou.setEnabled(true);
        }
        mapAndOnIt.enableLocationsButtons();
    }


    // add target from outside with distance
    @Override
    public void itemWasAdded(PointWithID point) {
        if (!suppressAddingInterests) {
            refershLocation = false;
            iterestPointHasChanged(point.getPoint());
        }
    }

    private void iterestPointHasChanged(PointWithDistance point) {
        // Disable location change - sometimes it came even before (i.e. needed to update user first)
        // freed after server has finished
        readyForNextLocationChange = false;
        // store the point in case we add some user from messages
        lastInterestGeoForMsgs = point.getParseGeoPosition();
        // update map
        lastDistance = point.getDistance();
        mapAndOnIt.drawCirclePaintAnimate(point.getLatLng(), lastDistance);
        // change title
        String specialName = point.getSpecialName();
        if (specialName != null) {
            TripWithMeMain.this.setTitle(APP_NAME + " - " + specialName);
        } else {
            TripWithMeMain.this.setTitle(APP_NAME);
        }
        // remove all old points
        people.removeAll();
        // get people from server
        updateServerOfInterstChange(point);
    }

    @Override
    public void itemWasRemoved(PointWithID point) {
        // no need to implement - DB will do it itself
    }

    private class ListRemoverCallback implements RemoverCallback {

        @Override
        public void removeMe(BaseVisibleData person) {
            people.remove((PersonVisibleData)person);
        }
    }

    private class PersonTapperToMapCallback implements TappedCallback {

        @Override
        public void onTap(BaseVisibleData person) {
            PersonVisibleData asPerson = (PersonVisibleData)person;
            mapAndOnIt.animatePersonAndShowDialog(asPerson);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
        // logout & delete user if anonymus
        parseUtil.deleteAnonymusAndLogOut();
    }

    // put user to last recorded state on start - TODO need to fill ready conditions...
    // first time lastState == null so no fear we are updating all new one
    @Override
    protected void onStart() {
        super.onStart();
        // State
        if (lastState != null) {
            boolean lastStateBool = getStateBoolAfterNullCheck();
            if (me != null) {
                Log.d(UPDATE_TAG, "Updating state as last remembered: " + lastStateBool);
                parseUtil.updateOnlineStateUserAndMe(lastStateBool, me);
            }
        }

        // Location
        loationManager.connect();
    }

    // put user to offline on Stop + stop updates
    @Override
    protected void onStop() {
        // State
        lastState = getStateString();
        if (lastState != null) {
            Log.d(UPDATE_TAG, "Updating online state to false, remembered: " + lastState);
            parseUtil.updateOnlineStateUserAndMe(false, me);
        }

        // Location
        loationManager.stopAndDisconnect();
        super.onStop();
    }

    private String getStateString() {
        if (me != null) {
            if (me.isCheckedIn()) {
                return CHECKED_IN;
            } else {
                return CHECKED_OUT;
            }
        } else {
            return null;
        }
    }

    private boolean getStateBoolAfterNullCheck() {
        return lastState.equals(CHECKED_IN);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    public boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(GOOGLE_SERVICES_TAG, "Google play services available");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                dialog.getWindow().setGravity(Gravity.CENTER);
                dialog.show();
            }
            return false;
        }
    }

    private abstract static class TextWatcherAfterOnly implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public abstract void afterTextChanged(Editable s);

    }

}