package com.source.tripwithme.databases;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SignUpCallback;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.components.AllSocialNetworks;
import com.source.tripwithme.components.Country;
import com.source.tripwithme.components.CountryFactory;
import com.source.tripwithme.components.PointWithDistance;
import com.source.tripwithme.components.SocialNetwork;
import com.source.tripwithme.images_resolve.ImageResolver;
import com.source.tripwithme.images_resolve.ParseFileResolver;
import com.source.tripwithme.main_ui.UsersManagerUI.ActionDoneCallback;
import com.source.tripwithme.visible_data.PersonVisibleData;
import com.source.tripwithme.visible_data.RemoverCallback;
import com.source.tripwithme.visible_data.TappedCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


public class ParseUtil {

    // Fields
    private static final String COUNTRY_ISO_KEY = "Country";
    private static final String PARSE_GEOPOINT_KEY = "GeoPoint";
    private static final String ONLINE_STATE_KEY = "OnlineState";
    private static final String PRIMARY_IMAGE_KEY = "PrimaryImage";
    private static final String USER_IMAGE_PREFIX = "userImage";
    public static final int NUMBER_OF_SECONDARY_PHOTOS = 3;
    private static final boolean INITIAL_ANONYMUS_CHECKED_STATE = false;
    private static final int SLEEP_BETWEEN_FINDINGS_EFFECT = 100;

    public static ParseUtil instance = null;
    private final Activity activity;
    private final CountryFactory countryFactory;
    private ParseUser currentUser;

    public static ParseUtil instance(Activity activity, CountryFactory countryFactory) {
        if (instance == null) {
            instance = new ParseUtil(activity, countryFactory);
        }
        return instance;
    }

    private ParseUtil(Activity activity, CountryFactory countryFactory) {
        this.activity = activity;
        this.countryFactory = countryFactory;
        init();
    }


    private void init() {
        // initialization
        Parse.initialize(activity, "ddMbk82hYo0end9jeX46OcBKlXXZc3TkLpTV8ti3",
                         "VEWI0oOYeNrG9QNRJOmhm9IUxuTLkUVIOHpXStlb");

        // ACL
        ParseACL defaultACL = new ParseACL();
        // all objects are public read
        defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);

        ParseAnalytics.trackAppOpened(activity.getIntent());
    }

    public void showErrorInAccountDialog(ParseException e, boolean fatal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String title = "Error in account activation";
        String msg = e.getMessage();
        if (fatal) {
            title = "Fatal " + title;
            msg += ". Application will now exit...";
        }
        builder.setTitle(title).setMessage(msg).setCancelable(true);
        AlertDialog dialog = builder.create();
        if (fatal) {
            dialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    System.exit(1);
                }
            });
        }
        if (!activity.isFinishing()) {
            dialog.show();
        }
    }


    public void signupIfNeeded(final PersonVisibleData me, String username, String password, String email,
                               final ActionDoneCallback callback) {
        if (isSignedUp()) {
            callback.done();
        } else {
            setDetailsAndUpdateMe(me, username, password, email);
            setLocationNoSave(me);
            setStateNoSave(me);
            currentUser.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        callback.done();
                        me.setUniqueChatID(currentUser.getObjectId());
                    } else {
                        e.printStackTrace();
                        showErrorInAccountDialog(e, false);
                    }

                }
            });
        }
    }

    public void loginAndUpdateMe(final String username, String password, final PersonVisibleData me,
                                 final ActionDoneCallback callback) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                    showErrorInAccountDialog(e, false);
                } else {
                    currentUser = parseUser;

                    // update online state + location according to current state in app
                    if (me != null) {
                        setLocationNoSave(me);
                        setStateNoSave(me);
                        System.out.println("parse Saving in background at login in");
                        currentUser.saveInBackground();
                    }

                    // update self
                    String email = currentUser.getEmail();
                    ImageResolver primary =
                        new ParseFileResolver(currentUser.getParseFile(PRIMARY_IMAGE_KEY), ParseUtil.this);
                    ImageResolver[] secondary = createPhotosResolvers(currentUser);
                    if (me != null) {
                        me.changeLoginDetails(username, currentUser.getObjectId(), primary, secondary, email);
                    }

                    callback.done();


                }
            }
        });
    }

    private void setLocationNoSave(PersonVisibleData me) {
        if (me != null) {
            PointWithDistance address = me.address();
            if (address != null) {
                System.out.println("parse setting address no save " + address);
                currentUser.put(PARSE_GEOPOINT_KEY, address.getParseGeoPosition());
            }
            Country country = me.getCountry();
            if (country != null) {
                System.out.println("parse setting country no save " + country);
                currentUser.put(COUNTRY_ISO_KEY, country.name());
            }
        }
    }

    private void setStateNoSave(PersonVisibleData me) {

        if (me != null) {
            boolean checkedin = me.isCheckedIn();
            currentUser.put(ONLINE_STATE_KEY, checkedin);
            System.out.println("parse setting state to " + checkedin + " no save");
        }
    }


    private void setDetailsAndUpdateMe(PersonVisibleData me, String username, String password, String email) {
        if (me != null) {
            me.setUserNameAndEmail(username, email);
        }
        currentUser.setUsername(username);
        currentUser.setPassword(password);
        currentUser.setEmail(email);
    }

    // Must run in separte thread from outside!!!
    public void startGettingPeopleBlock(ParseGeoPoint parseGeoPositionCurrent, double interestRadios,
                                        OneFoundCallback<PersonVisibleData> oneFoundCallback,
                                        RemoverCallback remover, TappedCallback tapper) {
        String thisUserObjectId = currentUser.getObjectId();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereWithinKilometers(PARSE_GEOPOINT_KEY, parseGeoPositionCurrent, interestRadios);
        try {
            List<ParseUser> list = query.find();
            for (ParseUser user : list) {
                if (user.getObjectId().equals(thisUserObjectId)) {
                    continue;
                }
                PersonVisibleData personVisibleData =
                    getPersonDataFromParseUser(parseGeoPositionCurrent, remover, tapper, user);
                oneFoundCallback.found(personVisibleData);

                // TODO effect ...
                Thread.sleep(SLEEP_BETWEEN_FINDINGS_EFFECT);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();

        }
    }

    private PersonVisibleData getPersonDataFromParseUser(ParseGeoPoint parseGeoPositionCurrent, RemoverCallback remover,
                                                         TappedCallback tapper, ParseUser user) {
        String name = user.getUsername();
        ParseGeoPoint position = user.getParseGeoPoint(PARSE_GEOPOINT_KEY);
        Country country = null;
        String countryFromParse = user.getString(COUNTRY_ISO_KEY);
        if (countryFromParse != null) {
            country = countryFactory.getByString(countryFromParse);
        }
        if (country == null) {
            country = countryFactory.defaultCountry();
        }
        String email = user.getEmail();
        double distFromMe = 0;
        if (position != null && parseGeoPositionCurrent != null) {
            distFromMe = position.distanceInKilometersTo(parseGeoPositionCurrent);
        }
        boolean personOnline = user.getBoolean(ONLINE_STATE_KEY);
        String stateStr;
        if (personOnline) {
            stateStr = "Ready to talk";
        } else {
            stateStr = "Busy...";
        }
        ImageResolver primaryImage = new ParseFileResolver(user.getParseFile(PRIMARY_IMAGE_KEY), this);
        ImageResolver[] resolvers = createPhotosResolvers(user);
        return new PersonVisibleData(name, user.getObjectId(), new PointWithDistance(position, distFromMe), stateStr,
                                     primaryImage, resolvers, email, remover, tapper, country,
                                     new SocialNetwork[]{AllSocialNetworks.getSocialNetwork(null)}, personOnline);
    }


    private ImageResolver[] createPhotosResolvers(ParseUser user) {
        ImageResolver[] resolvers = new ImageResolver[NUMBER_OF_SECONDARY_PHOTOS];
        for (int i = 0; i < NUMBER_OF_SECONDARY_PHOTOS; i++) {
            resolvers[i] = new ParseFileResolver(user.getParseFile(USER_IMAGE_PREFIX + (i + 1)), this);
        }
        return resolvers;
    }

    public synchronized void updateUserLocationAndMeBlock(ParseGeoPoint geoPoint, Country country,
                                                          PersonVisibleData me, Handler guiHandler) {
        if (me != null) {
            me.updateCountryAndLocation(country, geoPoint);
        }
        // after me is updated update user
        setLocationNoSave(me);
        try {
            System.out.println("parser save user in block in updateUserLocationAndMeBlock");
            currentUser.save();
        } catch (Exception e) {
            guiHandler.sendEmptyMessage(TripWithMeMain.SHOW_SAVE_LOCATION_ERROR_HANDLER);
            System.out.println("Error in parse save in location request");
            e.printStackTrace();

        }
    }

    public synchronized void updateOnlineStateUserAndMe(boolean online, PersonVisibleData me) {
        if (me != null) {
            me.setCheckedIn(online);
        }
        // me is updated - send it to server
        setStateNoSave(me);
        currentUser.saveInBackground();
        System.out.println("parse save after set in updateOnlineStateUserAndMe, new state: " + online);
    }

    public boolean isSignedUp() {
        return currentUser != null && !ParseAnonymousUtils.isLinked(currentUser);
    }

    public void requestPasswordResetShowToast(String emailForReset,
                                              final ActionDoneCallback callback) {
        ParseUser.requestPasswordResetInBackground(
            emailForReset,
            new RequestPasswordResetCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(activity, "Please check your mail for Parse notification", Toast.LENGTH_LONG)
                            .show();
                        callback.done();
                    } else {
                        e.printStackTrace();
                        showErrorInAccountDialog(e, false);
                    }
                }
            }
        );

    }

    public void deleteAnonymus() {
        if (!isSignedUp()) {
            currentUser.deleteEventually();
            ParseUser.logOut();
        }
    }


    // Shall it be synchronized from outside? we call the server with save...
    public void updatePicsAndMe(final Bitmap primary, final Bitmap[] secondary, final PersonVisibleData me) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (me != null) {
                    me.updatePhotos(primary, secondary);
                }
                try {
                    if (primary != null) {
                        createSaveParseFile(primary, "Primary.png", PRIMARY_IMAGE_KEY);
                    }
                    if (secondary != null) {
                        for (int i = 0; i < secondary.length; i++) {
                            Bitmap bitmap = secondary[i];
                            if (bitmap != null) {
                                int indexOfPhoto = i + 1;
                                createSaveParseFile(bitmap, "Image" + indexOfPhoto + ".png",
                                                    USER_IMAGE_PREFIX + indexOfPhoto);
                            }
                        }
                    }
                    System.out.println("Update photos done with user.");
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void createSaveParseFile(Bitmap bitmap, String fileName, String fieldKey)
        throws ParseException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] data = stream.toByteArray();
        stream.close();
        ParseFile imageFile = new ParseFile(fileName, data);
        imageFile.save();
        currentUser.put(fieldKey, imageFile);
        currentUser.save();
        System.out.println("Finished saving pic to parse with Name: " + fileName + ", Key: " + fieldKey);
    }


    public void getInitialDetailBackground(final DetailsFoundCallback callback) {
        // first we start the user
        currentUser = ParseUser.getCurrentUser();
        final boolean signUpState = isSignedUp();
        if (!signUpState) {
            // details will return only after login
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e != null) {
                        showErrorInAccountDialog(e, true);
                    } else {
                        currentUser = user;
                        actualGetInitialDetails(callback, signUpState);
                    }
                }
            });
        } else { // return details now
            actualGetInitialDetails(callback, signUpState);
        }
    }

    // last real signup state
    private void actualGetInitialDetails(final DetailsFoundCallback callback, boolean signUpState) {
        if (signUpState) {
            new AsyncTask<Void, Void, PersonVisibleData>() {
                @Override
                protected void onPostExecute(PersonVisibleData personVisibleData) {
                    callback.details(personVisibleData);
                }

                @Override
                protected PersonVisibleData doInBackground(Void... params) {
                    String name = currentUser.getUsername();
                    String email = currentUser.getEmail();
                    boolean isOnline = currentUser.getBoolean(ONLINE_STATE_KEY);
                    ImageResolver primaryImage =
                        new ParseFileResolver(currentUser.getParseFile(PRIMARY_IMAGE_KEY), ParseUtil.this);
                    ImageResolver[] resolvers = createPhotosResolvers(currentUser);
                    return new PersonVisibleData(name, currentUser.getObjectId(), null, null, primaryImage, resolvers,
                                                 email, null, null, null, null, isOnline);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            callback.details(
                new PersonVisibleData("Anonymus" + System.currentTimeMillis(), currentUser.getObjectId(), null, null,
                                      new ParseFileResolver(null, this),
                                      new ParseFileResolver[]{new ParseFileResolver(null, this),
                                          new ParseFileResolver(null, this), new ParseFileResolver(null, this)},
                                      null, null, null, null, null, INITIAL_ANONYMUS_CHECKED_STATE));
        }
    }

    public PersonVisibleData getPersonFromIDBlock(String personID, ParseGeoPoint parseGeoPositionCurrent,
                                                  RemoverCallback remover, TappedCallback tapper) {
        try {
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            ParseUser user = query.get(personID);
            if (user != null) {
                return getPersonDataFromParseUser(parseGeoPositionCurrent, remover, tapper, user);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface DetailsFoundCallback {

        void details(PersonVisibleData personVisibleData);
    }


    //// TODO remove after finished adding to map
    //public void addNewUser(Country country) {
    //    System.out.println("Adding user in: " + country);
    //    String countryIsoName = country.name();
    //    String countryFullName = country.getFullName();
    //    ParseUser user = new ParseUser();
    //    user.setUsername(countryFullName + " person");
    //    user.setPassword(countryIsoName);
    //    user.setEmail(countryIsoName + "@source.com");
    //    user.put(PARSE_GEOPOINT_KEY, country.getGeoPoint());
    //    user.put(COUNTRY_ISO_KEY, countryIsoName);
    //    user.put(ONLINE_STATE_KEY, true);
    //    try {
    //        user.signUp();
    //        ParseUser.logOut();
    //    } catch (ParseException e) {
    //    }
    //}

}
