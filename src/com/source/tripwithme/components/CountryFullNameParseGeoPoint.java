package com.source.tripwithme.components;

import com.parse.ParseGeoPoint;

public class CountryFullNameParseGeoPoint {

    String fullName;
    ParseGeoPoint parseGeoPoint;

    public CountryFullNameParseGeoPoint(String fullname, ParseGeoPoint parseGeoPoint) {
        this.fullName = fullname;
        this.parseGeoPoint = parseGeoPoint;
    }
}
