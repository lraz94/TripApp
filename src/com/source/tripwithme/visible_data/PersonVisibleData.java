package com.source.tripwithme.visible_data;

import android.graphics.Bitmap;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.parse.ParseGeoPoint;
import com.source.tripwithme.R.drawable;
import com.source.tripwithme.TripWithMeMain;
import com.source.tripwithme.components.Country;
import com.source.tripwithme.components.PointWithDistance;
import com.source.tripwithme.components.SocialNetwork;
import com.source.tripwithme.images_resolve.ImageResolver;
import com.source.tripwithme.images_resolve.TrivialImageResolver;

public class PersonVisibleData extends BaseVisibleData implements Comparable<PersonVisibleData> {

    private static final String UPDATE_PHOTOS_TAG = "UpdatePhotos";
    private final PersonDialogCreator personDialogCreator;
    private final PersonArrayAdapterCreator personArrayAdapterCreator;
    private final boolean onUserFriendsList;
    private SocialNetwork[] socialNetworks;
    private String uniqueChatID;
    private Country country;
    private String email;
    private boolean isCheckedIn;
    private Spanned spannedHistory;

    public PersonVisibleData(String name, String uniqueChatID, PointWithDistance address, String status,
                             ImageResolver photo, ImageResolver[] secondaryPhotos, String email,
                             RemoverCallback remover, TappedCallback tapper, Country primaryCountry,
                             SocialNetwork[] socialNetworks, boolean checkedIn, boolean onUserFriendsList) {
        super(name, address, status, photo, secondaryPhotos, remover, tapper);
        this.uniqueChatID = uniqueChatID;
        this.country = primaryCountry;
        this.socialNetworks = socialNetworks;
        this.isCheckedIn = checkedIn;
        this.email = email;
        personDialogCreator = new PersonDialogCreator(this);
        personArrayAdapterCreator = new PersonArrayAdapterCreator(this);
        spannedHistory = new SpannableString("");
        this.onUserFriendsList = onUserFriendsList;
    }

    public Country getCountry() {
        return country;
    }

    public SocialNetwork[] getSocialNetworks() {
        // TODO temp
        if (onUserFriendsList) {
            if (socialNetworks.length < 1) {
                socialNetworks = new SocialNetwork[1];
            }
            socialNetworks[0] = new SocialNetwork("facebook", drawable.facebookred);
        }
        return socialNetworks;
    }

    public boolean isCheckedIn() {
        return isCheckedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        isCheckedIn = checkedIn;
    }

    @Override
    public void paintActivePopup(TripWithMeMain activity, PersonVisibleData requester) {
        personDialogCreator.paintDialog(activity, requester);
    }

    @Override
    public void paintArrayAdapterView(View toConvertView) {
        personArrayAdapterCreator.convertView(toConvertView);
    }


    // Do i want to show email out there in the open?
    @SuppressWarnings("UnusedDeclaration")
    public String getEmail() {
        return email;
    }

    public void updatePhotos(Bitmap primary, Bitmap[] secondary) {
        if (primary != null) {
            Log.d(UPDATE_PHOTOS_TAG, "updating primary photo for " + name());
            this.primaryPhoto = new TrivialImageResolver(primary);
        }
        int length;
        if (secondary != null && (length = secondary.length) > 0) {
            for (int i = 0; i < length; i++) {
                Bitmap bitmap = secondary[i];
                if (bitmap != null) {
                    Log.d(UPDATE_PHOTOS_TAG, "updating photo ind " + i + " for " + name());
                    validateSpaceAndAddPhoto(bitmap, i);
                }
            }
        }
    }

    private void validateSpaceAndAddPhoto(Bitmap bitmap, int place) {
        int allPhotoFieldLength = secondaryPhotos.length;
        if (place >= allPhotoFieldLength) {
            ImageResolver[] temp = new ImageResolver[place + 1];
            System.arraycopy(secondaryPhotos, 0, temp, 0, allPhotoFieldLength);
            secondaryPhotos = temp;
        }
        secondaryPhotos[place] = new TrivialImageResolver(bitmap);
    }

    public void changeLoginDetails(String userName, String uniqueChatID, ImageResolver primaryPhoto,
                                   ImageResolver[] secondaryPhotos,
                                   String email) {
        setUniqueChatID(uniqueChatID);
        setUserNameAndEmail(userName, email);
        this.primaryPhoto = primaryPhoto;
        this.secondaryPhotos = secondaryPhotos;
    }

    public void updateCountryAndLocation(Country country, ParseGeoPoint geoPoint) {
        this.country = country;
        this.address = new PointWithDistance(geoPoint, 0);
    }


    public String getUniqueChatID() {
        return uniqueChatID;
    }

    public void setUniqueChatID(String uniqueChatID) {
        this.uniqueChatID = uniqueChatID;
    }

    public void setUserNameAndEmail(String username, String email) {
        this.name = username;
        this.email = email;
    }

    public void concatSpannableMessage(Spanned spannableMessage) {
        spannedHistory = (Spanned)TextUtils.concat(spannedHistory, spannableMessage, new SpannableString("\n"));
    }

    public Spanned getSpannedHistory() {
        return spannedHistory;
    }

    public void refreashHistoryText() {
        personDialogCreator.refreshHistoryTextIfCan();
    }


    // hashcode is uniqueChatID since i don't expect it to change except for current user which isn't in any hash set
    @Override
    public int hashCode() {
        return uniqueChatID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PersonVisibleData &&
               ((PersonVisibleData)o).uniqueChatID.equals(this.uniqueChatID);

    }

    @Override
    public int compareTo(PersonVisibleData another) {
        return this.uniqueChatID.compareTo(another.uniqueChatID);
    }

    public static PersonVisibleData minimalPersonByID(String personID) {
        return new PersonVisibleData(null, personID, null, null, null, null, null, null, null, null, null, false,
                                     false);

    }

    public boolean isOnUserFriendsList() {
        return onUserFriendsList;
    }
}
