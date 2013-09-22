package com.source.tripwithme.components;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseGeoPoint;

public class PointWithDistance {

    private final ParseGeoPoint parseGeoPoint;
    private final LatLng latLng;
    private String spacialName;
    private double distance;


    public PointWithDistance(ParseGeoPoint parseGeoPoint, double distance) {
        double lat = parseGeoPoint.getLatitude();
        double longi = parseGeoPoint.getLongitude();
        this.latLng = new LatLng(lat, longi);
        this.parseGeoPoint = parseGeoPoint;
        this.distance = distance;
    }

    public PointWithDistance(ParseGeoPoint geoPoint, double distance, String fullName) {
        this(geoPoint, distance);
        spacialName = fullName;
    }

    public String getSpecialName() {
        return spacialName;
    }

    public ParseGeoPoint getParseGeoPosition() {
        return parseGeoPoint;
    }

    public double getDistance() {
        return distance;
    }

    public boolean equals(Object other) {
        LatLng otherGeoPoint = ((PointWithDistance)other).getLatLng();
        return otherGeoPoint.equals(this.getLatLng());
    }


    public LatLng getLatLng() {
        return latLng;
    }

    public String toString() {
        return latLng.toString();
    }
}
