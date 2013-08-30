package com.source.tripwithme.components;

import com.parse.ParseGeoPoint;

public class Country extends ResourceFlagedItem {


    private final CountryFullNameParseGeoPoint countryFullNameParseGeoPoint;

    public Country(String name, int resouce, CountryFullNameParseGeoPoint fullNameLatLng) {
        super(name, resouce);
        this.countryFullNameParseGeoPoint = fullNameLatLng;
    }

    public String getFullName() {
        if (countryFullNameParseGeoPoint != null) {
            return countryFullNameParseGeoPoint.fullName;
        }
        return null;
    }

    public ParseGeoPoint getGeoPoint() {
        if (countryFullNameParseGeoPoint != null) {
            return countryFullNameParseGeoPoint.parseGeoPoint;
        }
        return null;
    }

    public String toString() {
        return getFullName();
    }
}
