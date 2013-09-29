package com.source.tripwithme.main_ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.source.tripwithme.R;
import com.source.tripwithme.R.drawable;
import com.source.tripwithme.R.id;
import com.source.tripwithme.R.layout;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.components.Country;
import com.source.tripwithme.components.CountryFactory;
import com.source.tripwithme.components.ListWithListeners;
import com.source.tripwithme.components.ListenerOnCollection;
import com.source.tripwithme.components.PointWithDistance;
import com.source.tripwithme.components.PointWithID;
import com.source.tripwithme.databases.ParseUtil;
import com.source.tripwithme.visible_data.PersonVisibleData;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map.Entry;

public class MapAndOnIt implements ListenerOnCollection<PersonVisibleData>, OnMapClickListener, OnMarkerClickListener {

    private static final DecimalFormat DECIMAL_FORMAT_FIVE_POINT = new DecimalFormat("#.00000");
    private static final float ZOOM_AMOUNT_PERSON = 12;
    private static final String MAP_TAG = "MapHandeling";
    private final ListWithListeners<PointWithID> interests;  // won't be notified on change
    private final GoogleMap map;
    private final TripWithMeMain activityWithResources;
    private final HashMap<Marker, PersonVisibleData> personMap;
    private final BitmapDescriptor bitmapYou;
    private final BitmapDescriptor bitmapOnline;
    private final BitmapDescriptor bitmapOffline;
    private final BitmapDescriptor bitmapPin;
    private final CountryFactory countryFactory;
    private final ParseUtil parseUtil;
    private final PersonVisibleData me;
    private Marker userMarker;

    private boolean isOnline;

    // new target
    private boolean waitForFirstTapOnMap;
    private boolean waitForSecondTapOnMap;
    private Marker lastEncour;
    private Circle lastCircle;
    private Button radarBtn;
    private Button menu;

    // Find country
    private Country lastCountry;
    private FindCountryFromLatLng lastCountryThread;
    private Handler guiQuickHandler;

    public MapAndOnIt(GoogleMap map, TripWithMeMain activityWithResources, ListWithListeners<PointWithID> interests,
                      CountryFactory countryFactory, ParseUtil parseUtil, PersonVisibleData me) {
        this.map = map;
        this.activityWithResources = activityWithResources;
        this.interests = interests;
        this.countryFactory = countryFactory;
        this.me = me;
        personMap = new HashMap<Marker, PersonVisibleData>();
        map.setMyLocationEnabled(true);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        initButtonsOnMap();
        bitmapYou = BitmapDescriptorFactory.fromResource(drawable.you);
        bitmapOnline = BitmapDescriptorFactory.fromResource(drawable.personguidence);
        bitmapOffline = BitmapDescriptorFactory.fromResource(drawable.offlineuser);
        bitmapPin = BitmapDescriptorFactory.fromResource(drawable.pin);
        guiQuickHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                enableLocationsButtons();
            }
        };
        this.parseUtil = parseUtil;
        initMap();
    }

    private void initMap() {
        // init map if needed and set defaults
        if (activityWithResources.servicesConnected()) {
            try {
                MapsInitializer.initialize(activityWithResources);
            } catch (Exception e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activityWithResources);
                builder.setMessage(e.getMessage())
                    .setTitle("You've got error with Google Maps !!! message is: " + e.getMessage())
                    .setCancelable(true);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        // start temp location
        userMarker = addMapMarker(new LatLng(0, 0), "This is you!",
                                  "Temporary starting position (0,0)", bitmapYou);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        if (!marker.equals(userMarker)) {
            PersonVisibleData person = personMap.get(marker);
            if (person != null) {
                person.tap();
                return true;
            }
        }
        return false;

    }

    private void initButtonsOnMap() {
        Button secondscreenBtn = (Button)activityWithResources.findViewById(id.secondscreen);
        secondscreenBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                activityWithResources.openPeopleMenu();
            }
        });
        radarBtn = (Button)activityWithResources.findViewById(id.radar);
        radarBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                changeInterestTriggered();
            }
        });
        radarBtn.setEnabled(false);
        menu = (Button)activityWithResources.findViewById(id.buttonMenu);
        menu.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                activityWithResources.openOptionsMenu();
            }
        });
        menu.setEnabled(false);
        ToggleButton toggle = (ToggleButton)activityWithResources.findViewById(id.togglebutton);
        isOnline = me.isCheckedIn();
        toggle.setChecked(isOnline);
        toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isOnline = isChecked;
                checkedInStateChanged(isChecked);
                parseUtil.updateOnlineStateUserAndMe(isChecked, me);
            }
        });
        Button helpButton = (Button)activityWithResources.findViewById(id.helpbutton);
        helpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(activityWithResources);
                dialog.setContentView(layout.helpscreen);
                dialog.setTitle("Help Screen");
                dialog.show();
            }
        });

    }

    private void checkedInStateChanged(boolean onlineState) {
        if (!onlineState) {       // offline is for everybody
            userMarker.setIcon(bitmapOffline);
            for (Marker marker : personMap.keySet()) {
                marker.setIcon(bitmapOffline);
            }
        } else {    // online is just for online users
            userMarker.setIcon(bitmapYou);
            for (Entry<Marker, PersonVisibleData> entry : personMap.entrySet()) {
                PersonVisibleData person = entry.getValue();
                boolean state = person.isCheckedIn();
                if (state) {
                    entry.getKey().setIcon(bitmapOnline);
                } else {
                    entry.getKey().setIcon(bitmapOffline);
                }
            }
        }
    }


    public synchronized void showNewLocationOnMap(LatLng currentPlace) {
        // always remove since user is starting with fake marker until getting real one
        userMarker.remove();

        // move camera to currentPlace
        CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(currentPlace)
            .zoom(13)
            .build();
        CameraUpdate cu = CameraUpdateFactory.newCameraPosition(cameraPosition);
        map.animateCamera(cu);
        // put user marker
        userMarker = addMapMarker(currentPlace, "This is you!",
                                  getSnippetFromPoint(currentPlace), bitmapYou);
    }

    private String getSnippetFromPoint(LatLng currentPlace) {
        return "lat:" + DECIMAL_FORMAT_FIVE_POINT.format(currentPlace.latitude) + ", lng:" +
               DECIMAL_FORMAT_FIVE_POINT.format(currentPlace.longitude);
    }

    public void animatePersonAndShowDialog(final PersonVisibleData person) {
        LatLng latLng = person.address().getLatLng();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_AMOUNT_PERSON));
        person.paintActivePopup(activityWithResources, me);
    }

    @Override
    public void itemWasAdded(PersonVisibleData person) {
        if (person != null) {
            BitmapDescriptor bitmap;
            if (isOnline && person.isCheckedIn()) {
                bitmap = bitmapOnline;
            } else {
                bitmap = bitmapOffline;
            }
            // POSITION
            LatLng latLng = person.address().getLatLng();
            // PAINT
            Marker marker = addMapMarker(latLng, person.name(), getSnippetFromPoint(latLng), bitmap);
            // PUT IN MAP
            personMap.put(marker, person);
        }
    }

    private Marker addMapMarker(LatLng position, String title, String snippet, BitmapDescriptor bitmap) {
        MarkerOptions indicator = new MarkerOptions()
            .position(position)
            .title(title)
            .snippet(snippet)
            .icon(bitmap);
        Marker marker = map.addMarker(indicator);
        Log.d(MAP_TAG, "Added marker to map: id: " + marker.getId() + ", position: " + position + ", title: " + title +
                       ", snippet: " + snippet);
        return marker;
    }


    @Override
    public void itemWasRemoved(PersonVisibleData toRemove) {
        for (Marker marker : personMap.keySet()) {
            if (personMap.get(marker).equals(toRemove)) {
                personMap.remove(marker);    // from field map
                marker.remove();             // from google map
                return;
            }
        }
    }

    private void changeInterestTriggered() {
        cleanupEncourAndCircle();
        // create dialog
        final AlertDialog alertDialog = new AlertDialog.Builder(activityWithResources).create();
        alertDialog.setTitle("Choose interest point");
        alertDialog.setMessage("Please click on your interest point in map");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Go!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                waitForFirstTapOnMap = true;
                alertDialog.dismiss();
            }
        });
        alertDialog.setIcon(R.drawable.radar);
        alertDialog.show();
    }

    private void cleanupEncourAndCircle() {
        // cleanup
        if (lastCircle != null) {
            lastCircle.remove();
        }
        if (lastEncour != null) {
            lastEncour.remove();
        }
        waitForFirstTapOnMap = false;
        waitForSecondTapOnMap = false;
        lastCountry = null;
        lastCountryThread = null; // is already dead or button is not enabled
    }

    @Override
    public void onMapClick(final LatLng clicked) {
        if (waitForFirstTapOnMap) {
            waitForFirstTapOnMap = false;
            // remember click
            lastEncour = addMapMarker(clicked, "Interest point", "User Clicked", bitmapPin);
            // show dialog
            final AlertDialog alertDialog = new AlertDialog.Builder(activityWithResources).create();
            alertDialog.setTitle("Choose end point");
            alertDialog.setMessage("Please click on the end point");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Go!", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    waitForSecondTapOnMap = true;
                    // Find country
                    // don't allow user the try again until found ...
                    disableLocationButtons();
                    lastCountryThread = new FindCountryFromLatLng(clicked);
                    lastCountryThread.start();
                    alertDialog.dismiss();
                }
            });
            alertDialog.setIcon(R.drawable.radar);
            alertDialog.show();
        } else if (waitForSecondTapOnMap) {
            // clean
            waitForFirstTapOnMap = false;
            waitForSecondTapOnMap = false;
            new AsyncTaskWaiter(lastEncour.getPosition(), clicked).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public synchronized void drawCirclePaintAnimate(LatLng aroundPoint, double distInKms) {
        cleanupEncourAndCircle();
        lastEncour = addMapMarker(aroundPoint, "Interest point",
                                  "In " + TripWithMeMain.DECIMAL_FORMAT_TWO_POINTS.format(distInKms) + " Kms Radius",
                                  bitmapPin);
        lastCircle = map.addCircle(
            new CircleOptions().center(aroundPoint).radius(distInKms * 1000).strokeColor(Color.BLUE)
                .fillColor(0x30000000));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(aroundPoint, getZoom(distInKms)));

    }

    private float getZoom(double distInKms) {
        if (distInKms > 3000) {
            return 1;
        } else if (distInKms > 2000) {
            return 2;
        } else if (distInKms > 1500) {
            return 3;
        } else if (distInKms > 1200) {
            return 4;
        } else if (distInKms > 900) {
            return 5;
        } else {
            return 6;
        }
    }

    public void disableLocationButtons() {
        if (radarBtn != null) {
            radarBtn.setEnabled(false);
        }
        if (menu != null) {
            menu.setEnabled(false);
        }
    }


    public void enableLocationsButtons() {
        if (radarBtn != null) {
            radarBtn.setEnabled(true);
        }
        if (menu != null) {
            menu.setEnabled(true);
        }

    }

    private class FindCountryFromLatLng extends Thread {

        private final LatLng latLng;

        public FindCountryFromLatLng(LatLng latLng) {
            this.latLng = latLng;
        }

        @Override
        public void run() {
            ParseGeoPoint encourGeo = new ParseGeoPoint(latLng.latitude, latLng.longitude);
            lastCountry = countryFactory.getByNetSearchBlock(encourGeo);
            guiQuickHandler.sendEmptyMessage(0);
        }
    }

    private class AsyncTaskWaiter extends AsyncTask<Void, Void, PointWithDistance> {

        private final LatLng encour;
        private final LatLng clicked;
        private final ProgressDialog dialog;

        public AsyncTaskWaiter(LatLng encour, LatLng clicked) {
            this.encour = encour;
            this.clicked = clicked;
            dialog = new ProgressDialog(activityWithResources);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Please Wait...");
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.show();
        }

        @Override
        protected PointWithDistance doInBackground(Void... params) {
            // wait for country
            if (lastCountryThread != null && lastCountryThread.isAlive()) {
                try {
                    lastCountryThread.join();
                } catch (InterruptedException e) {
                    Log.e("Countries", "interupted in with last", e);
                }
            }
            ParseGeoPoint encourGeo = new ParseGeoPoint(encour.latitude, encour.longitude);
            ParseGeoPoint secondGeo = new ParseGeoPoint(clicked.latitude, clicked.longitude);
            double distInKms = encourGeo.distanceInKilometersTo(secondGeo);
            return new PointWithDistance(encourGeo, distInKms, lastCountry.getFullName());
        }

        @Override
        protected void onPostExecute(PointWithDistance pointWithDistance) {
            dialog.dismiss();
            // update TripWithMeMain and this afterwards
            interests.add(PointWithID.generate(pointWithDistance));
        }
    }
}
