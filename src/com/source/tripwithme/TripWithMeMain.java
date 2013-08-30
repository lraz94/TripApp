package com.source.tripwithme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.LocationCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.source.tripwithme.R.id;
import com.source.tripwithme.R.layout;
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
import com.source.tripwithme.people_menu.PeopleMenu;
import com.source.tripwithme.people_menu.UserItemAdapter;
import com.source.tripwithme.visible_data.BaseVisibleData;
import com.source.tripwithme.visible_data.PersonVisibleData;
import com.source.tripwithme.visible_data.RemoverCallback;
import com.source.tripwithme.visible_data.TappedCallback;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class TripWithMeMain extends Activity implements ListenerOnCollection<PointWithID> {

    // distance consts and limits
    private static final double INITIAL_INTEREST_DISTANCE = 50;
    public static final double NEW_COUNTRY_INTEREST_DISTANCE = 80;
    private static final int INTERESTS_LIMIT = 5;

    // time consts
    private static final long FIND_LOCATION_TIMEOUT_MILLIS = 1000;    // 1 sec

    private static final long DEFAULT_SLEEP_BETWEEN_UPDATES_MILLIS = 1000 * 5 * 60; // 5 min

    // formating consts
    public static final DecimalFormat DECIMAL_FORMAT_TWO_POINTS = new DecimalFormat("#.00");
    public static final DecimalFormat DECIMAL_FORMAT_ONE_POINT = new DecimalFormat("#.#");

    // activities returned
    private static final int RETURNED_PIC_CODE = 100;

    // states
    private static final String CHECKED_IN = "Checked in State";
    private static final String CHECKED_OUT = "Checked out State";

    // handler
    public static final int REQUEST_PHOTO_HANDLER = 150;
    public static final int SHOW_SAVE_LOCATION_ERROR_HANDLER = 250;
    public static final int ADAPTER_NEW_PERSON_HANDLER = 350;

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

    // updating place by location
    private double lastDistance;
    private boolean stopUpdating;
    private boolean byUserLocation;
    private Thread threadToInterupt;

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

    // refresh location+friends
    private long milisIntervalBetweenRefresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screenone);

        initCoreFields();

        startLists();

        startLocalDB();

        startParse(new DetailsFoundCallback() {
            @Override
            public void details(PersonVisibleData personVisibleData) {
                me = personVisibleData;

                startLocationDealing();

                startMassages();

                startGUIComponents();
            }
        });
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
            showErrorInMapAndExit();
        }
        people.addListListener(mapAndOnIt);    // map contains parralel list with it own use
        addPeopleMenuAdapter();
    }

    private void addPeopleMenuAdapter() {
        adapter = new UserItemAdapter(this, new ArrayList<PersonVisibleData>());
        people.addListListener(adapter.getNewListener(guiHandler));
        new PeopleMenu(adapter); // statically add adapater...
    }

    private void showErrorInMapAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Error in Google Map or Play Services had accoured.").setTitle(
            "You've got error with Google Maps !!!")
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
        /* Get Location as interest point */
        byUserLocation = true;
        currentLocationAsInterestPoints(INITIAL_INTEREST_DISTANCE, true);
        stopUpdating = false;
        startListeningToLocationChange();
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
        milisIntervalBetweenRefresh = DEFAULT_SLEEP_BETWEEN_UPDATES_MILLIS;
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
                    adapter.add(person);
                }
            }
        };
    }


    private void startListeningToLocationChange() {
        btnAroundYou = (Button)this.findViewById(id.aroundyou);
        btnAroundYou.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                byUserLocation = true;
                if (threadToInterupt != null) {
                    threadToInterupt.interrupt();
                }
            }
        });

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                boolean nextTimeNoSleep = false;
                threadToInterupt = Thread.currentThread();
                do {
                    if (nextTimeNoSleep) {
                        nextTimeNoSleep = false;
                    } else {
                        try {
                            Thread.sleep(milisIntervalBetweenRefresh);
                        } catch (InterruptedException youCanAwakeMe) {
                            System.out.println("Geo Location thread was awaken - first");
                        }
                    }
                    if (byUserLocation) {
                        publishProgress();
                    }
                    // we let the progress to work in case user refresh to fast
                    // this is half of the minimal time... if interupted - next time we won't sleep
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException youCanAwakeMe) {
                        nextTimeNoSleep = true;
                        System.out.println("Geo Location thread was awaken - second. next time won't sleep.");
                    }
                } while (!stopUpdating);
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                System.out.println("try to update location");
                currentLocationAsInterestPoints(lastDistance, false);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //private void addOneForEachCountryForDebug() {
    //    List<Country> countriesList = countryFactory.getAllCountries();
    //    for (Country c : countriesList) {
    //        parseUtil.addNewPerson(c.getFullName() + " Person", c.getGeoPoint(), c);
    //    }
    //}

    private void currentLocationAsInterestPoints(final double distance, final boolean firstTime) {
        ParseGeoPoint.getCurrentLocationInBackground(FIND_LOCATION_TIMEOUT_MILLIS, new LocationCallback() {
            @Override
            public void done(ParseGeoPoint geoPoint, ParseException e) {
                double lat, longi;
                if (e != null) {
                    if (!firstTime) {
                        Toast.makeText(TripWithMeMain.this, "updating location failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    lat = 31.5;
                    longi = 34.75;
                    // first time no place - we use arbitrary start point
                } else {
                    lat = geoPoint.getLatitude();
                    longi = geoPoint.getLongitude();
                }
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
                        return new PointWithDistance(finalGeoPoint, distance, country.getFullName());
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
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
                        iterestPointHasChanged(saved);
                    }
                }).show();
            }
        }
        return true;
    }

    private void showRefreshMeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(layout.refreshmedialog);
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
        intervalEdit.setText(Long.toString(milisIntervalBetweenRefresh / 60L / 1000L));
        intervalEdit.addTextChangedListener(new TextWatcherAfterOnly() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    long typedToMilis = Integer.parseInt(s.toString().trim()) * 60L * 1000L;
                    if (typedToMilis > 0) {  // in big numbers there can be overflaw
                        milisIntervalBetweenRefresh = typedToMilis;
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
        return parseUtil.getPersonFromIDBlock(personID, lastInterestGeoForMsgs, remover, tapper);
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
            byUserLocation = false;
            iterestPointHasChanged(point.getPoint());
        }
    }

    private void iterestPointHasChanged(PointWithDistance point) {
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
        stopUpdating = true;
        // logout & delete user if anonymus
        parseUtil.deleteAnonymus();
    }

    // put user to offline on Stop - TODO need to fill ready conditions...
    @Override
    protected void onStop() {
        super.onStop();
        lastState = getStateString();
        if (lastState != null) {
            System.out.println("Updating online state to false, remembered: " + lastState);
            parseUtil.updateOnlineStateUserAndMe(false, me);
        }
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


    // put user to last recorded state on start - TODO need to fill ready conditions...
    // first time lastState == null so no fear we are updating all new one
    @Override
    protected void onStart() {
        super.onStart();
        if (lastState != null) {
            boolean lastStateBool = getStateBoolAfterNullCheck();
            if (me != null) {
                System.out.println("Updating state as last remembered: " + lastStateBool);
                parseUtil.updateOnlineStateUserAndMe(lastStateBool, me);
            }
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