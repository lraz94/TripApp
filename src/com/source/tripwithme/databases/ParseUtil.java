package com.source.tripwithme.databases;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.parse.DeleteCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;
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
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;


public class ParseUtil {

    // Fields
    private static final String COUNTRY_ISO_KEY = "Country";
    private static final String PARSE_GEOPOINT_KEY = "GeoPoint";
    private static final String ONLINE_STATE_KEY = "OnlineState";
    private static final String PRIMARY_IMAGE_KEY = "PrimaryImage";
    private static final String USER_IMAGE_PREFIX = "userImage";
    private static final String IS_IN_FACEBOOK_KEY = "InFacebook";
    public static final int NUMBER_OF_SECONDARY_PHOTOS = 3;
    private static final boolean INITIAL_ANONYMUS_CHECKED_STATE = false;
    private static final int SLEEP_BETWEEN_FINDINGS_EFFECT = 100;
    public static final String FACEBOOK_TAG = "FacebookTag";

    private static final String PARSE_SAVE_TAG = "ParseSave";

    public static ParseUtil instance = null;
    private final Activity activity;
    private final CountryFactory countryFactory;
    private ParseUser currentUser;
    private TreeSet<String> friends;
    private int requestNumber;

    public static ParseUtil instance(Activity activity, CountryFactory countryFactory) {
        if (instance == null) {
            instance = new ParseUtil(activity, countryFactory);
        }
        return instance;
    }

    private ParseUtil(Activity activity, CountryFactory countryFactory) {
        this.activity = activity;
        this.countryFactory = countryFactory;
        this.friends = new TreeSet<String>();
        requestNumber = 0;
        init();
    }

    // Used to know if person is from old search and should be ignored
    public int getRequestNumber() {
        return requestNumber;
    }

    private void init() {
        // initialization
        Parse.initialize(activity, "ddMbk82hYo0end9jeX46OcBKlXXZc3TkLpTV8ti3",
                         "VEWI0oOYeNrG9QNRJOmhm9IUxuTLkUVIOHpXStlb");

        // ACL
        ParseACL.setDefaultACL(getPublicReadOnlyACL(), true);

        ParseAnalytics.trackAppOpened(activity.getIntent());

        ParseFacebookUtils.initialize("211814822312783");   // TODO put app id

    }

    private ParseACL getPublicReadOnlyACL() {
        ParseACL readOnlyACL = new ParseACL();
        readOnlyACL.setPublicReadAccess(true);
        readOnlyACL.setPublicWriteAccess(false);
        return readOnlyACL;
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
                        Log.e(PARSE_SAVE_TAG, "Sign up failed", e);
                        showErrorInAccountDialog(e, false);
                        // partial login is not good - we 'reset' by giving new user!
                        currentUser.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Log.e(PARSE_SAVE_TAG, "Error delete anonymus user", e);
                                }
                            }
                        });
                        ParseUser.logOut();
                        loginAnonymus();
                    }
                }
            });
        }
    }


    public void loginAndUpdateMe(final String username, String password, final PersonVisibleData me,
                                 final ActionDoneCallback callback) {
        final boolean isAnonymus = deleteAnonymusAndLogOut();
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                boolean result = loginParseDone(parseUser, e, me, username, callback);
                if (!result && isAnonymus) {
                    loginAnonymus();
                }
            }
        });
    }

    private void loginAnonymus() {
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(final ParseUser parseUser, ParseException e) {
                if (e == null) {
                    currentUser = parseUser;
                    Toast.makeText(activity, "New anonymus identity was given to you", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(PARSE_SAVE_TAG, "can't login anonymus in resotre", e);
                }
            }
        });

    }

    private boolean loginParseDone(ParseUser parseUser, ParseException e, PersonVisibleData me, String username,
                                   ActionDoneCallback callback) {
        if (e != null) {
            Log.e(PARSE_SAVE_TAG, "login parse failure", e);
            showErrorInAccountDialog(e, false);
            return false;
        } else {
            currentUser = parseUser;
            // update online state + location according to current state in app
            if (me != null) {
                setLocationNoSave(me);
                setStateNoSave(me);
                Log.d(PARSE_SAVE_TAG, "parse Saving in background at login in");
                currentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Log.e(PARSE_SAVE_TAG, "Error while save in login done update", e);

                    }
                });
            }

            // update self
            String email = currentUser.getEmail();
            ImageResolver primary =
                new ParseFileResolver(currentUser.getParseFile(PRIMARY_IMAGE_KEY), this);
            ImageResolver[] secondary = createPhotosResolvers(currentUser);
            if (me != null) {
                me.changeLoginDetails(username, currentUser.getObjectId(), primary, secondary, email);
            }

            callback.done();
            return true;
        }
    }

    public String[] getPermissions() {
        return new String[]{
            "user_about_me",
            // "offline_access",
            "friends_about_me",
            "user_photos",
            "friends_photos",
            "read_stream",
            "friends_status",
        };
    }

    public void loginWithFacebook(final PersonVisibleData me, final ActionDoneCallback callback) {
        try {
            ParseFacebookUtils.logIn(Arrays.asList(getPermissions()),
                                     activity, TripWithMeMain.REQUEST_CODE_FACEBOOK, new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException err) {
                    if (user == null) {
                        Log.d(FACEBOOK_TAG, "Uh oh. The user cancelled the Facebook login.");
                        Toast.makeText(activity,
                                       "Facebook app isn't installed, or youv'e canceled Login. Fix and Retry.",
                                       Toast.LENGTH_SHORT).show();
                    } else {
                        if (user.isNew()) {
                            Log.d(FACEBOOK_TAG, "User signed up and logged in through Facebook!");
                        } else {
                            Log.d(FACEBOOK_TAG, "User logged in through Facebook!");
                        }
                        boolean isAnonymus = deleteAnonymusAndLogOut();
                        boolean loginResult = loginParseDone(user, err, me, "facebook user", callback); // temp username
                        if (loginResult) {
                            user.put(IS_IN_FACEBOOK_KEY, true);
                            fixCredentialsSave(user, me);
                        } else {
                            if (isAnonymus) {
                                loginAnonymus();
                            }
                        }
                    }
                }

                private void fixCredentialsSave(final ParseUser parseUser, final PersonVisibleData me) {
                    Session session = ParseFacebookUtils.getSession();
                    Request request = Request.newMeRequest(session, new GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser graphUser, Response response) {
                            if (response.getError() == null) {
                                String username = getUsernameFromGraphUser(graphUser);
                                String email = username + "@facebookHiddenEmail.com";
                                parseUser.setUsername(username);
                                parseUser.setEmail(email);
                                me.setUserNameAndEmail(username, email);
                                Log.d(PARSE_SAVE_TAG, "parse Saving in background at facebook login in");
                                parseUser.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        Log.e(PARSE_SAVE_TAG, "Error while save facebook log in", e);
                                    }
                                });
                            } else {
                                Log.e("TripWithMeMain", "Error returned by facebook while quering me: " +
                                                        response.getError().getUserActionMessageId());
                            }
                        }
                    });
                    request.executeAsync();
                }
            });
        } catch (Exception e) {
            Log.e(PARSE_SAVE_TAG, "login with facebook failure", e);
            Toast.makeText(activity, "Problem with Facebook login", Toast.LENGTH_SHORT).show();
        }
    }


    private void setLocationNoSave(PersonVisibleData me) {
        if (me != null) {
            PointWithDistance address = me.address();
            if (address != null) {
                Log.d(PARSE_SAVE_TAG, "parse setting address no save " + address);
                currentUser.put(PARSE_GEOPOINT_KEY, address.getParseGeoPosition());
            }
            Country country = me.getCountry();
            if (country != null) {
                Log.d(PARSE_SAVE_TAG, "parse setting country no save " + country);
                currentUser.put(COUNTRY_ISO_KEY, country.name());
            }
        }
    }

    private void setStateNoSave(PersonVisibleData me) {

        if (me != null) {
            boolean checkedin = me.isCheckedIn();
            currentUser.put(ONLINE_STATE_KEY, checkedin);
            Log.d(PARSE_SAVE_TAG, "parse setting state to " + checkedin + " no save");
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
        requestNumber++;
        updateFriendsList();
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
            Log.e(PARSE_SAVE_TAG, "start get person from db parse exception", e);
        } catch (InterruptedException e) {
            Log.e(PARSE_SAVE_TAG, "start get person from db parse exception", e);

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
                                     getSocialNetworks(user), personOnline,
                                     findIfOnFriendsList(name), requestNumber);
    }

    private SocialNetwork[] getSocialNetworks(ParseUser user) {
        if (user.getBoolean(IS_IN_FACEBOOK_KEY)) {
            return new SocialNetwork[]{AllSocialNetworks.facebook()};
        } else {
            return new SocialNetwork[]{};
        }
    }

    private boolean findIfOnFriendsList(String name) {
        return friends.contains(name);
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
            Log.d(PARSE_SAVE_TAG, "parser save user in block in updateUserLocationAndMeBlock");
            currentUser.save();
        } catch (Exception e) {
            guiHandler.sendEmptyMessage(TripWithMeMain.SHOW_SAVE_LOCATION_ERROR_HANDLER);
            Log.e(PARSE_SAVE_TAG, "Error in parse save in location request", e);
        }
    }

    // synchronized and exception treated due on stop / on start
    public synchronized void updateOnlineStateUserAndMe(boolean online, PersonVisibleData me) {
        try {
            if (me != null) {
                me.setCheckedIn(online);
            }
            // me is updated - send it to server
            setStateNoSave(me);
            currentUser.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        Log.e(PARSE_SAVE_TAG, "Error while save update online state", e);
                    }
                }
            });
            Log.d(PARSE_SAVE_TAG, "parse save after set in updateOnlineStateUserAndMe, new state: " + online);
        } catch (Exception e1) {
            Log.e(PARSE_SAVE_TAG, "Error while update online state", e1);
        }
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
                        Log.e(PARSE_SAVE_TAG, "request password reset", e);
                        showErrorInAccountDialog(e, false);
                    }
                }
            }
        );

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
                    Log.d(PARSE_SAVE_TAG, "Update photos done with user.");
                } catch (ParseException e) {
                    Log.e(PARSE_SAVE_TAG, "update pics", e);
                } catch (IOException e) {
                    Log.e(PARSE_SAVE_TAG, "update pics", e);
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
        Log.d(PARSE_SAVE_TAG, "Finished saving pic to parse with Name: " + fileName + ", Key: " + fieldKey);
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
                        Log.e(PARSE_SAVE_TAG, "get initial details", e);
                        showErrorInAccountDialog(e, true);
                    } else {
                        currentUser = user;
                        try {
                            currentUser.saveInBackground();
                        } catch (Exception e1) {
                            Log.e(PARSE_SAVE_TAG, "can't save after creation", e1);
                        }
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
                                                 email, null, null, null, null, isOnline, false, 0);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            callback.details(new PersonVisibleData("Anonymus" + System.currentTimeMillis(), currentUser.getObjectId(),
                                                   null, null, new ParseFileResolver(null, this),
                                                   new ImageResolver[]{new ParseFileResolver(null, this),
                                                       new ParseFileResolver(null, this),
                                                       new ParseFileResolver(null, this)}, null, null, null, null, null,
                                                   INITIAL_ANONYMUS_CHECKED_STATE, false, 0));
        }
    }

    public PersonVisibleData getPersonFromDBlock(String personID, ParseGeoPoint parseGeoPositionCurrent,
                                                 RemoverCallback remover, TappedCallback tapper) {
        try {
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            ParseUser user = query.get(personID);
            if (user != null) {
                return getPersonDataFromParseUser(parseGeoPositionCurrent, remover, tapper, user);
            }
        } catch (ParseException e) {
            Log.e(PARSE_SAVE_TAG, "get person from db parse exception", e);
        }
        return null;
    }


    public boolean deleteAnonymusAndLogOut() {
        if (currentUser != null && ParseAnonymousUtils.isLinked(currentUser)) {
            currentUser.deleteInBackground(new DeleteCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        Log.e(PARSE_SAVE_TAG, "Error delete anonymus user", e);
                    }

                }
            });
            ParseUser.logOut();
            return true;
        } else {
            return false;
        }
    }

    public interface DetailsFoundCallback {

        void details(PersonVisibleData personVisibleData);
    }

    // out of the UI thread
    private void updateFriendsList() {
        friends = new TreeSet<String>();
        try {
            if (ParseFacebookUtils.isLinked(currentUser)) {
                Session session = ParseFacebookUtils.getSession();
                Request request = Request.newMyFriendsRequest(session, new GraphUserListCallback() {
                    @Override
                    public void onCompleted(List<GraphUser> users, Response response) {
                        try {
                            if (response.getError() == null) {
                                for (GraphUser user : users) {
                                    friends.add(getUsernameFromGraphUser(user));
                                }
                                // TODO temp print
                                Log.d(FACEBOOK_TAG, "Friends list fetched: " + friends);
                            } else {
                                Log.e(FACEBOOK_TAG, "Error returned by facebook while update friends list: " +
                                                    response.getError().getUserActionMessageId());
                            }
                        } catch (Exception e) {
                            Log.e(FACEBOOK_TAG, "Exception thrown in update friends list with facebook", e);
                        }
                    }
                });
                request.executeAndWait();
            }
        } catch (Exception e) {
            Log.e(FACEBOOK_TAG, "Exception thrown in update friends list with facebook", e);
        }
    }

    private String getUsernameFromGraphUser(GraphUser user) {
        String username = user.getUsername();
        if (username == null) {
            username = user.getName();
        }
        return username;
    }

    /*
    // remove after finished adding to map
    public void addNewUser(Country country) {
        System.out.println("Adding user in: " + country);
        String countryIsoName = country.name();
        String countryFullName = country.getFullName();
        ParseUser user = new ParseUser();
        user.setUsername(countryFullName + " person");
        user.setPassword(countryIsoName);
        user.setEmail(countryIsoName + "@source.com");
        user.put(PARSE_GEOPOINT_KEY, country.getGeoPoint());
        user.put(COUNTRY_ISO_KEY, countryIsoName);
        user.put(ONLINE_STATE_KEY, true);
        try {
            user.signUp();
            ParseUser.logOut();
        } catch (ParseException e) {
        }
    }
    */
}
