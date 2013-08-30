package com.source.tripwithme.components;

import android.content.Context;
import com.parse.ParseGeoPoint;
import com.source.tripwithme.R.array;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class CountryFactory {

    private static CountryFactory instance;
    private final Context resourceProvider;
    private HashMap<String, CountryFullNameParseGeoPoint> map;
    private List<Country> countriesList;
    private String[] countriesListString;

    public synchronized static CountryFactory getLanguageFactorySigelton(Context resourceProvider) {
        if (instance == null) {
            instance = new CountryFactory(resourceProvider);
            instance.addAllToMap();
        }
        return instance;
    }


    private CountryFactory(Context resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public Country getByString(String language) {
        if (language == null || language.isEmpty()) {
            return null;
        }
        language = language.toLowerCase();
        int id = resourceProvider.getResources().getIdentifier(language, "drawable",
                                                               resourceProvider.getPackageName());
        if (id != 0) {
            CountryFullNameParseGeoPoint countryFullNameParseGeoPoint = map.get(language);
            return new Country(language, id, countryFullNameParseGeoPoint);
        } else {
            return null;
        }
    }

    public Country defaultCountry() {
        return getByString("un");
    }

    public synchronized String[] getAllCountriesString() {
        if (countriesListString == null) {
            countriesListString = resourceProvider.getResources().getStringArray(array.countries);
        }
        return countriesListString;
    }

    public synchronized List<Country> getAllCountries() {
        if (countriesList == null) {
            countriesList = new ArrayList<Country>();
            String[] countries = getAllCountriesString();
            for (String country : countries) {
                Country c = getByString(country);
                if (c != null) {
                    countriesList.add(c);
                }
            }
        }
        return countriesList;
    }

    public Country getByNetSearchBlock(ParseGeoPoint geoPoint) {
        String iso = new JsonGoogleApiSearch(geoPoint).execute();
        if (iso == null) {
            return defaultCountry();
        }
        Country conuntry = getByString(iso);
        if (conuntry == null) {
            return defaultCountry();
        }
        return conuntry;
    }


    private void addAllToMap() {
        map = new HashMap<String, CountryFullNameParseGeoPoint>();
        map.put("un",
                new CountryFullNameParseGeoPoint("Unknown Location", new ParseGeoPoint(0, 0)));
        map.put("AD".toLowerCase(),
                new CountryFullNameParseGeoPoint("ANDORRA", new ParseGeoPoint(42.5, 1.5000)));
        map.put("AE".toLowerCase(),
                new CountryFullNameParseGeoPoint("UNITED ARAB EMIRATES", new ParseGeoPoint(24, 54.0000)));
        map.put("AF".toLowerCase(),
                new CountryFullNameParseGeoPoint("AFGHANISTAN", new ParseGeoPoint(33, 65.0000)));
        map.put("AG".toLowerCase(),
                new CountryFullNameParseGeoPoint("ANTIGUA AND BARBUDA", new ParseGeoPoint(17.05, -61.8000)));
        map.put("AI".toLowerCase(),
                new CountryFullNameParseGeoPoint("ANGUILLA", new ParseGeoPoint(18.25, -63.1667)));
        map.put("AL".toLowerCase(),
                new CountryFullNameParseGeoPoint("ALBANIA", new ParseGeoPoint(41, 20.0000)));
        map.put("AM".toLowerCase(),
                new CountryFullNameParseGeoPoint("ARMENIA", new ParseGeoPoint(40, 45.0000)));
        map.put("AN".toLowerCase(),
                new CountryFullNameParseGeoPoint("NETHERLANDS ANTILLES", new ParseGeoPoint(12.25, -68.7500)));
        map.put("AO".toLowerCase(),
                new CountryFullNameParseGeoPoint("ANGOLA", new ParseGeoPoint(-12.5, 18.5000)));
        map.put("AR".toLowerCase(),
                new CountryFullNameParseGeoPoint("ARGENTINA", new ParseGeoPoint(-34, -64.0000)));
        map.put("AS".toLowerCase(),
                new CountryFullNameParseGeoPoint("AMERICAN SAMOA", new ParseGeoPoint(-14.3333, -170.0000)));
        map.put("AT".toLowerCase(),
                new CountryFullNameParseGeoPoint("AUSTRIA", new ParseGeoPoint(47.3333, 13.3333)));
        map.put("AU".toLowerCase(),
                new CountryFullNameParseGeoPoint("AUSTRALIA", new ParseGeoPoint(-27, 133.0000)));
        map.put("AW".toLowerCase(),
                new CountryFullNameParseGeoPoint("ARUBA", new ParseGeoPoint(12.5, -69.9667)));
        map.put("AZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("AZERBAIJAN", new ParseGeoPoint(40.5, 47.5000)));
        map.put("BA".toLowerCase(),
                new CountryFullNameParseGeoPoint("BOSNIA AND HERZEGOWINA", new ParseGeoPoint(44, 18.0000)));
        map.put("BB".toLowerCase(),
                new CountryFullNameParseGeoPoint("BARBADOS", new ParseGeoPoint(13.1667, -59.5333)));
        map.put("BD".toLowerCase(),
                new CountryFullNameParseGeoPoint("BANGLADESH", new ParseGeoPoint(24, 90.0000)));
        map.put("BE".toLowerCase(),
                new CountryFullNameParseGeoPoint("BELGIUM", new ParseGeoPoint(50.8333, 4.0000)));
        map.put("BF".toLowerCase(),
                new CountryFullNameParseGeoPoint("BURKINA FASO", new ParseGeoPoint(13, -2.0000)));
        map.put("BG".toLowerCase(),
                new CountryFullNameParseGeoPoint("BULGARIA", new ParseGeoPoint(43, 25.0000)));
        map.put("BH".toLowerCase(),
                new CountryFullNameParseGeoPoint("BAHRAIN", new ParseGeoPoint(26, 50.5500)));
        map.put("BI".toLowerCase(),
                new CountryFullNameParseGeoPoint("BURUNDI", new ParseGeoPoint(-3.5, 30.0000)));
        map.put("BJ".toLowerCase(), new CountryFullNameParseGeoPoint("BENIN", new ParseGeoPoint(9.5, 2.2500)));
        map.put("BM".toLowerCase(),
                new CountryFullNameParseGeoPoint("BERMUDA", new ParseGeoPoint(32.3333, -64.7500)));
        map.put("BN".toLowerCase(),
                new CountryFullNameParseGeoPoint("BRUNEI DARUSSALAM", new ParseGeoPoint(4.5, 114.6667)));
        map.put("BO".toLowerCase(),
                new CountryFullNameParseGeoPoint("BOLIVIA", new ParseGeoPoint(-17, -65.0000)));
        map.put("BR".toLowerCase(),
                new CountryFullNameParseGeoPoint("BRAZIL", new ParseGeoPoint(-10, -55.0000)));
        map.put("BS".toLowerCase(),
                new CountryFullNameParseGeoPoint("BAHAMAS", new ParseGeoPoint(24.25, -76.0000)));
        map.put("BT".toLowerCase(),
                new CountryFullNameParseGeoPoint("BHUTAN", new ParseGeoPoint(27.5, 90.5000)));
        map.put("BV".toLowerCase(),
                new CountryFullNameParseGeoPoint("BOUVET ISLAND", new ParseGeoPoint(-54.4333, 3.4000)));
        map.put("BW".toLowerCase(),
                new CountryFullNameParseGeoPoint("BOTSWANA", new ParseGeoPoint(-22, 24.0000)));
        map.put("BY".toLowerCase(),
                new CountryFullNameParseGeoPoint("BELARUS", new ParseGeoPoint(53, 28.0000)));
        map.put("BZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("BELIZE", new ParseGeoPoint(17.25, -88.7500)));
        map.put("CA".toLowerCase(),
                new CountryFullNameParseGeoPoint("CANADA", new ParseGeoPoint(60, -95.0000)));
        map.put("CC".toLowerCase(),
                new CountryFullNameParseGeoPoint("COCOS (KEELING) ISLANDS", new ParseGeoPoint(-12.5, 96.8333)));
        map.put("CD".toLowerCase(),
                new CountryFullNameParseGeoPoint("CONGO, Democratic Republic of (was Zaire)",
                                                 new ParseGeoPoint(0, 25.0000)));
        map.put("CF".toLowerCase(),
                new CountryFullNameParseGeoPoint("CENTRAL AFRICAN REPUBLIC", new ParseGeoPoint(7, 21.0000)));
        map.put("CG".toLowerCase(),
                new CountryFullNameParseGeoPoint("CONGO, Republic of", new ParseGeoPoint(-1, 15.0000)));
        map.put("CH".toLowerCase(),
                new CountryFullNameParseGeoPoint("SWITZERLAND", new ParseGeoPoint(47, 8.0000)));
        map.put("CI".toLowerCase(),
                new CountryFullNameParseGeoPoint("COTE D'IVOIRE", new ParseGeoPoint(8, -5.0000)));
        map.put("CK".toLowerCase(),
                new CountryFullNameParseGeoPoint("COOK ISLANDS", new ParseGeoPoint(-21.2333, -159.7667)));
        map.put("CL".toLowerCase(),
                new CountryFullNameParseGeoPoint("CHILE", new ParseGeoPoint(-30, -71.0000)));
        map.put("CM".toLowerCase(),
                new CountryFullNameParseGeoPoint("CAMEROON", new ParseGeoPoint(6, 12.0000)));
        map.put("CN".toLowerCase(), new CountryFullNameParseGeoPoint("CHINA", new ParseGeoPoint(35, 105.0000)));
        map.put("CO".toLowerCase(),
                new CountryFullNameParseGeoPoint("COLOMBIA", new ParseGeoPoint(4, -72.0000)));
        map.put("CR".toLowerCase(),
                new CountryFullNameParseGeoPoint("COSTA RICA", new ParseGeoPoint(10, -84.0000)));
        map.put("CU".toLowerCase(),
                new CountryFullNameParseGeoPoint("CUBA", new ParseGeoPoint(21.5, -80.0000)));
        map.put("CV".toLowerCase(),
                new CountryFullNameParseGeoPoint("CAPE VERDE", new ParseGeoPoint(16, -24.0000)));
        map.put("CX".toLowerCase(),
                new CountryFullNameParseGeoPoint("CHRISTMAS ISLAND", new ParseGeoPoint(-10.5, 105.6667)));
        map.put("CY".toLowerCase(), new CountryFullNameParseGeoPoint("CYPRUS", new ParseGeoPoint(35, 33.0000)));
        map.put("CZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("CZECH REPUBLIC", new ParseGeoPoint(49.75, 15.5000)));
        map.put("DE".toLowerCase(), new CountryFullNameParseGeoPoint("GERMANY", new ParseGeoPoint(51, 9.0000)));
        map.put("DJ".toLowerCase(),
                new CountryFullNameParseGeoPoint("DJIBOUTI", new ParseGeoPoint(11.5, 43.0000)));
        map.put("DK".toLowerCase(),
                new CountryFullNameParseGeoPoint("DENMARK", new ParseGeoPoint(56, 10.0000)));
        map.put("DM".toLowerCase(),
                new CountryFullNameParseGeoPoint("DOMINICA", new ParseGeoPoint(15.4167, -61.3333)));
        map.put("DO".toLowerCase(),
                new CountryFullNameParseGeoPoint("DOMINICAN REPUBLIC", new ParseGeoPoint(19, -70.6667)));
        map.put("DZ".toLowerCase(), new CountryFullNameParseGeoPoint("ALGERIA", new ParseGeoPoint(28, 3.0000)));
        map.put("EC".toLowerCase(),
                new CountryFullNameParseGeoPoint("ECUADOR", new ParseGeoPoint(-2, -77.5000)));
        map.put("EE".toLowerCase(),
                new CountryFullNameParseGeoPoint("ESTONIA", new ParseGeoPoint(59, 26.0000)));
        map.put("EG".toLowerCase(), new CountryFullNameParseGeoPoint("EGYPT", new ParseGeoPoint(27, 30.0000)));
        map.put("EH".toLowerCase(),
                new CountryFullNameParseGeoPoint("WESTERN SAHARA", new ParseGeoPoint(24.5, -13.0000)));
        map.put("ER".toLowerCase(),
                new CountryFullNameParseGeoPoint("ERITREA", new ParseGeoPoint(15, 39.0000)));
        map.put("ES".toLowerCase(), new CountryFullNameParseGeoPoint("SPAIN", new ParseGeoPoint(40, -4.0000)));
        map.put("ET".toLowerCase(),
                new CountryFullNameParseGeoPoint("ETHIOPIA", new ParseGeoPoint(8, 38.0000)));
        map.put("FI".toLowerCase(),
                new CountryFullNameParseGeoPoint("FINLAND", new ParseGeoPoint(64, 26.0000)));
        map.put("FJ".toLowerCase(), new CountryFullNameParseGeoPoint("FIJI", new ParseGeoPoint(-18, 175.0000)));
        map.put("FK".toLowerCase(),
                new CountryFullNameParseGeoPoint("FALKLAND ISLANDS (MALVINAS)", new ParseGeoPoint(-51.75, -59.0000)));
        map.put("FM".toLowerCase(),
                new CountryFullNameParseGeoPoint("MICRONESIA, FEDERATED STATES OF",
                                                 new ParseGeoPoint(6.9167, 158.2500)));
        map.put("FO".toLowerCase(),
                new CountryFullNameParseGeoPoint("FAROE ISLANDS", new ParseGeoPoint(62, -7.0000)));
        map.put("FR".toLowerCase(), new CountryFullNameParseGeoPoint("FRANCE", new ParseGeoPoint(46, 2.0000)));
        map.put("GA".toLowerCase(), new CountryFullNameParseGeoPoint("GABON", new ParseGeoPoint(-1, 11.7500)));
        map.put("GB".toLowerCase(),
                new CountryFullNameParseGeoPoint("UNITED KINGDOM", new ParseGeoPoint(54, -2.0000)));
        map.put("GD".toLowerCase(),
                new CountryFullNameParseGeoPoint("GRENADA", new ParseGeoPoint(12.1167, -61.6667)));
        map.put("GE".toLowerCase(),
                new CountryFullNameParseGeoPoint("GEORGIA", new ParseGeoPoint(42, 43.5000)));
        map.put("GF".toLowerCase(),
                new CountryFullNameParseGeoPoint("FRENCH GUIANA", new ParseGeoPoint(4, -53.0000)));
        map.put("GH".toLowerCase(), new CountryFullNameParseGeoPoint("GHANA", new ParseGeoPoint(8, -2.0000)));
        map.put("GI".toLowerCase(),
                new CountryFullNameParseGeoPoint("GIBRALTAR", new ParseGeoPoint(36.1833, -5.3667)));
        map.put("GL".toLowerCase(),
                new CountryFullNameParseGeoPoint("GREENLAND", new ParseGeoPoint(72, -40.0000)));
        map.put("GM".toLowerCase(),
                new CountryFullNameParseGeoPoint("GAMBIA", new ParseGeoPoint(13.4667, -16.5667)));
        map.put("GN".toLowerCase(),
                new CountryFullNameParseGeoPoint("GUINEA", new ParseGeoPoint(11, -10.0000)));
        map.put("GP".toLowerCase(),
                new CountryFullNameParseGeoPoint("GUADELOUPE", new ParseGeoPoint(16.25, -61.5833)));
        map.put("GQ".toLowerCase(),
                new CountryFullNameParseGeoPoint("EQUATORIAL GUINEA", new ParseGeoPoint(2, 10.0000)));
        map.put("GR".toLowerCase(), new CountryFullNameParseGeoPoint("GREECE", new ParseGeoPoint(39, 22.0000)));
        map.put("GS".toLowerCase(),
                new CountryFullNameParseGeoPoint("SOUTH GEORGIA AND THE SOUTH SANDWICH ISLANDS",
                                                 new ParseGeoPoint(-54.5, -37.0000)));
        map.put("GT".toLowerCase(),
                new CountryFullNameParseGeoPoint("GUATEMALA", new ParseGeoPoint(15.5, -90.2500)));
        map.put("GU".toLowerCase(),
                new CountryFullNameParseGeoPoint("GUAM", new ParseGeoPoint(13.4667, 144.7833)));
        map.put("GW".toLowerCase(),
                new CountryFullNameParseGeoPoint("GUINEA-BISSAU", new ParseGeoPoint(12, -15.0000)));
        map.put("GY".toLowerCase(), new CountryFullNameParseGeoPoint("GUYANA", new ParseGeoPoint(5, -59.0000)));
        map.put("HK".toLowerCase(),
                new CountryFullNameParseGeoPoint("HONG KONG", new ParseGeoPoint(22.25, 114.1667)));
        map.put("HM".toLowerCase(),
                new CountryFullNameParseGeoPoint("HEARD AND MC DONALD ISLANDS", new ParseGeoPoint(-53.1, 72.5167)));
        map.put("HN".toLowerCase(),
                new CountryFullNameParseGeoPoint("HONDURAS", new ParseGeoPoint(15, -86.5000)));
        map.put("HR".toLowerCase(),
                new CountryFullNameParseGeoPoint("CROATIA (local name: Hrvatska)",
                                                 new ParseGeoPoint(45.1667, 15.5000)));
        map.put("HT".toLowerCase(), new CountryFullNameParseGeoPoint("HAITI", new ParseGeoPoint(19, -72.4167)));
        map.put("HU".toLowerCase(),
                new CountryFullNameParseGeoPoint("HUNGARY", new ParseGeoPoint(47, 20.0000)));
        map.put("ID".toLowerCase(),
                new CountryFullNameParseGeoPoint("INDONESIA", new ParseGeoPoint(-5, 120.0000)));
        map.put("IE".toLowerCase(),
                new CountryFullNameParseGeoPoint("IRELAND", new ParseGeoPoint(53, -8.0000)));
        map.put("IL".toLowerCase(),
                new CountryFullNameParseGeoPoint("ISRAEL", new ParseGeoPoint(31.5, 34.7500)));
        map.put("IN".toLowerCase(), new CountryFullNameParseGeoPoint("INDIA", new ParseGeoPoint(20, 77.0000)));
        map.put("IO".toLowerCase(),
                new CountryFullNameParseGeoPoint("BRITISH INDIAN OCEAN TERRITORY", new ParseGeoPoint(-6, 71.5000)));
        map.put("IQ".toLowerCase(), new CountryFullNameParseGeoPoint("IRAQ", new ParseGeoPoint(33, 44.0000)));
        map.put("IR".toLowerCase(),
                new CountryFullNameParseGeoPoint("IRAN (ISLAMIC REPUBLIC OF)", new ParseGeoPoint(32, 53.0000)));
        map.put("IS".toLowerCase(),
                new CountryFullNameParseGeoPoint("ICELAND", new ParseGeoPoint(65, -18.0000)));
        map.put("IT".toLowerCase(),
                new CountryFullNameParseGeoPoint("ITALY", new ParseGeoPoint(42.8333, 12.8333)));
        map.put("JM".toLowerCase(),
                new CountryFullNameParseGeoPoint("JAMAICA", new ParseGeoPoint(18.25, -77.5000)));
        map.put("JO".toLowerCase(), new CountryFullNameParseGeoPoint("JORDAN", new ParseGeoPoint(31, 36.0000)));
        map.put("JP".toLowerCase(), new CountryFullNameParseGeoPoint("JAPAN", new ParseGeoPoint(36, 138.0000)));
        map.put("KE".toLowerCase(), new CountryFullNameParseGeoPoint("KENYA", new ParseGeoPoint(1, 38.0000)));
        map.put("KG".toLowerCase(),
                new CountryFullNameParseGeoPoint("KYRGYZSTAN", new ParseGeoPoint(41, 75.0000)));
        map.put("KH".toLowerCase(),
                new CountryFullNameParseGeoPoint("CAMBODIA", new ParseGeoPoint(13, 105.0000)));
        map.put("KI".toLowerCase(),
                new CountryFullNameParseGeoPoint("KIRIBATI", new ParseGeoPoint(1.4167, 173.0000)));
        map.put("KM".toLowerCase(),
                new CountryFullNameParseGeoPoint("COMOROS", new ParseGeoPoint(-12.1667, 44.2500)));
        map.put("KN".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAINT KITTS AND NEVIS", new ParseGeoPoint(17.3333, -62.7500)));
        map.put("KP".toLowerCase(),
                new CountryFullNameParseGeoPoint("KOREA, DEMOCRATIC PEOPLE'S REPUBLIC OF",
                                                 new ParseGeoPoint(40, 127.0000)));
        map.put("KR".toLowerCase(),
                new CountryFullNameParseGeoPoint("KOREA, REPUBLIC OF", new ParseGeoPoint(37, 127.5000)));
        map.put("KW".toLowerCase(),
                new CountryFullNameParseGeoPoint("KUWAIT", new ParseGeoPoint(29.3375, 47.6581)));
        map.put("KY".toLowerCase(),
                new CountryFullNameParseGeoPoint("CAYMAN ISLANDS", new ParseGeoPoint(19.5, -80.5000)));
        map.put("KZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("KAZAKHSTAN", new ParseGeoPoint(48, 68.0000)));
        map.put("LA".toLowerCase(),
                new CountryFullNameParseGeoPoint("LAO PEOPLE'S DEMOCRATIC REPUBLIC", new ParseGeoPoint(18, 105.0000)));
        map.put("LB".toLowerCase(),
                new CountryFullNameParseGeoPoint("LEBANON", new ParseGeoPoint(33.8333, 35.8333)));
        map.put("LC".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAINT LUCIA", new ParseGeoPoint(13.8833, -61.1333)));
        map.put("LI".toLowerCase(),
                new CountryFullNameParseGeoPoint("LIECHTENSTEIN", new ParseGeoPoint(47.1667, 9.5333)));
        map.put("LK".toLowerCase(),
                new CountryFullNameParseGeoPoint("SRI LANKA", new ParseGeoPoint(7, 81.0000)));
        map.put("LR".toLowerCase(),
                new CountryFullNameParseGeoPoint("LIBERIA", new ParseGeoPoint(6.5, -9.5000)));
        map.put("LS".toLowerCase(),
                new CountryFullNameParseGeoPoint("LESOTHO", new ParseGeoPoint(-29.5, 28.5000)));
        map.put("LT".toLowerCase(),
                new CountryFullNameParseGeoPoint("LITHUANIA", new ParseGeoPoint(56, 24.0000)));
        map.put("LU".toLowerCase(),
                new CountryFullNameParseGeoPoint("LUXEMBOURG", new ParseGeoPoint(49.75, 6.1667)));
        map.put("LV".toLowerCase(), new CountryFullNameParseGeoPoint("LATVIA", new ParseGeoPoint(57, 25.0000)));
        map.put("LY".toLowerCase(),
                new CountryFullNameParseGeoPoint("LIBYAN ARAB JAMAHIRIYA", new ParseGeoPoint(25, 17.0000)));
        map.put("MA".toLowerCase(),
                new CountryFullNameParseGeoPoint("MOROCCO", new ParseGeoPoint(32, -5.0000)));
        map.put("MC".toLowerCase(),
                new CountryFullNameParseGeoPoint("MONACO", new ParseGeoPoint(43.7333, 7.4000)));
        map.put("MD".toLowerCase(),
                new CountryFullNameParseGeoPoint("MOLDOVA, REPUBLIC OF", new ParseGeoPoint(47, 29.0000)));
        map.put("MG".toLowerCase(),
                new CountryFullNameParseGeoPoint("MADAGASCAR", new ParseGeoPoint(-20, 47.0000)));
        map.put("MH".toLowerCase(),
                new CountryFullNameParseGeoPoint("MARSHALL ISLANDS", new ParseGeoPoint(9, 168.0000)));
        map.put("MK".toLowerCase(),
                new CountryFullNameParseGeoPoint("MACEDONIA, THE FORMER YUGOSLAV REPUBLIC OF",
                                                 new ParseGeoPoint(41.8333, 22.0000)));
        map.put("ML".toLowerCase(), new CountryFullNameParseGeoPoint("MALI", new ParseGeoPoint(17, -4.0000)));
        map.put("MM".toLowerCase(),
                new CountryFullNameParseGeoPoint("MYANMAR", new ParseGeoPoint(22, 98.0000)));
        map.put("MN".toLowerCase(),
                new CountryFullNameParseGeoPoint("MONGOLIA", new ParseGeoPoint(46, 105.0000)));
        map.put("MO".toLowerCase(),
                new CountryFullNameParseGeoPoint("MACAU", new ParseGeoPoint(22.1667, 113.5500)));
        map.put("MP".toLowerCase(),
                new CountryFullNameParseGeoPoint("NORTHERN MARIANA ISLANDS", new ParseGeoPoint(15.2, 145.7500)));
        map.put("MQ".toLowerCase(),
                new CountryFullNameParseGeoPoint("MARTINIQUE", new ParseGeoPoint(14.6667, -61.0000)));
        map.put("MR".toLowerCase(),
                new CountryFullNameParseGeoPoint("MAURITANIA", new ParseGeoPoint(20, -12.0000)));
        map.put("MS".toLowerCase(),
                new CountryFullNameParseGeoPoint("MONTSERRAT", new ParseGeoPoint(16.75, -62.2000)));
        map.put("MT".toLowerCase(),
                new CountryFullNameParseGeoPoint("MALTA", new ParseGeoPoint(35.8333, 14.5833)));
        map.put("MU".toLowerCase(),
                new CountryFullNameParseGeoPoint("MAURITIUS", new ParseGeoPoint(-20.2833, 57.5500)));
        map.put("MV".toLowerCase(),
                new CountryFullNameParseGeoPoint("MALDIVES", new ParseGeoPoint(3.25, 73.0000)));
        map.put("MW".toLowerCase(),
                new CountryFullNameParseGeoPoint("MALAWI", new ParseGeoPoint(-13.5, 34.0000)));
        map.put("MX".toLowerCase(),
                new CountryFullNameParseGeoPoint("MEXICO", new ParseGeoPoint(23, -102.0000)));
        map.put("MY".toLowerCase(),
                new CountryFullNameParseGeoPoint("MALAYSIA", new ParseGeoPoint(2.5, 112.5000)));
        map.put("MZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("MOZAMBIQUE", new ParseGeoPoint(-18.25, 35.0000)));
        map.put("NA".toLowerCase(),
                new CountryFullNameParseGeoPoint("NAMIBIA", new ParseGeoPoint(-22, 17.0000)));
        map.put("NC".toLowerCase(),
                new CountryFullNameParseGeoPoint("NEW CALEDONIA", new ParseGeoPoint(-21.5, 165.5000)));
        map.put("NE".toLowerCase(), new CountryFullNameParseGeoPoint("NIGER", new ParseGeoPoint(16, 8.0000)));
        map.put("NF".toLowerCase(),
                new CountryFullNameParseGeoPoint("NORFOLK ISLAND", new ParseGeoPoint(-29.0333, 167.9500)));
        map.put("NG".toLowerCase(), new CountryFullNameParseGeoPoint("NIGERIA", new ParseGeoPoint(10, 8.0000)));
        map.put("NI".toLowerCase(),
                new CountryFullNameParseGeoPoint("NICARAGUA", new ParseGeoPoint(13, -85.0000)));
        map.put("NL".toLowerCase(),
                new CountryFullNameParseGeoPoint("NETHERLANDS", new ParseGeoPoint(52.5, 5.7500)));
        map.put("NO".toLowerCase(), new CountryFullNameParseGeoPoint("NORWAY", new ParseGeoPoint(62, 10.0000)));
        map.put("NP".toLowerCase(), new CountryFullNameParseGeoPoint("NEPAL", new ParseGeoPoint(28, 84.0000)));
        map.put("NR".toLowerCase(),
                new CountryFullNameParseGeoPoint("NAURU", new ParseGeoPoint(-0.5333, 166.9167)));
        map.put("NU".toLowerCase(),
                new CountryFullNameParseGeoPoint("NIUE", new ParseGeoPoint(-19.0333, -169.8667)));
        map.put("NZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("NEW ZEALAND", new ParseGeoPoint(-41, 174.0000)));
        map.put("OM".toLowerCase(), new CountryFullNameParseGeoPoint("OMAN", new ParseGeoPoint(21, 57.0000)));
        map.put("PA".toLowerCase(), new CountryFullNameParseGeoPoint("PANAMA", new ParseGeoPoint(9, -80.0000)));
        map.put("PE".toLowerCase(), new CountryFullNameParseGeoPoint("PERU", new ParseGeoPoint(-10, -76.0000)));
        map.put("PF".toLowerCase(),
                new CountryFullNameParseGeoPoint("FRENCH POLYNESIA", new ParseGeoPoint(-15, -140.0000)));
        map.put("PG".toLowerCase(),
                new CountryFullNameParseGeoPoint("PAPUA NEW GUINEA", new ParseGeoPoint(-6, 147.0000)));
        map.put("PH".toLowerCase(),
                new CountryFullNameParseGeoPoint("PHILIPPINES", new ParseGeoPoint(13, 122.0000)));
        map.put("PK".toLowerCase(),
                new CountryFullNameParseGeoPoint("PAKISTAN", new ParseGeoPoint(30, 70.0000)));
        map.put("PL".toLowerCase(), new CountryFullNameParseGeoPoint("POLAND", new ParseGeoPoint(52, 20.0000)));
        map.put("PM".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAINT PIERRE AND MIQUELON", new ParseGeoPoint(46.8333, -56.3333)));
        map.put("PR".toLowerCase(),
                new CountryFullNameParseGeoPoint("PUERTO RICO", new ParseGeoPoint(18.25, -66.5000)));
        map.put("PS".toLowerCase(),
                new CountryFullNameParseGeoPoint("PALESTINIAN TERRITORY, Occupied", new ParseGeoPoint(32, 35.2500)));
        map.put("PT".toLowerCase(),
                new CountryFullNameParseGeoPoint("PORTUGAL", new ParseGeoPoint(39.5, -8.0000)));
        map.put("PW".toLowerCase(),
                new CountryFullNameParseGeoPoint("PALAU", new ParseGeoPoint(7.5, 134.5000)));
        map.put("PY".toLowerCase(),
                new CountryFullNameParseGeoPoint("PARAGUAY", new ParseGeoPoint(-23, -58.0000)));
        map.put("QA".toLowerCase(),
                new CountryFullNameParseGeoPoint("QATAR", new ParseGeoPoint(25.5, 51.2500)));
        map.put("RE".toLowerCase(),
                new CountryFullNameParseGeoPoint("REUNION", new ParseGeoPoint(-21.1, 55.6000)));
        map.put("RO".toLowerCase(),
                new CountryFullNameParseGeoPoint("ROMANIA", new ParseGeoPoint(46, 25.0000)));
        map.put("RU".toLowerCase(),
                new CountryFullNameParseGeoPoint("RUSSIAN FEDERATION", new ParseGeoPoint(60, 100.0000)));
        map.put("RW".toLowerCase(), new CountryFullNameParseGeoPoint("RWANDA", new ParseGeoPoint(-2, 30.0000)));
        map.put("SA".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAUDI ARABIA", new ParseGeoPoint(25, 45.0000)));
        map.put("SB".toLowerCase(),
                new CountryFullNameParseGeoPoint("SOLOMON ISLANDS", new ParseGeoPoint(-8, 159.0000)));
        map.put("SC".toLowerCase(),
                new CountryFullNameParseGeoPoint("SEYCHELLES", new ParseGeoPoint(-4.5833, 55.6667)));
        map.put("SD".toLowerCase(), new CountryFullNameParseGeoPoint("SUDAN", new ParseGeoPoint(15, 30.0000)));
        map.put("SE".toLowerCase(), new CountryFullNameParseGeoPoint("SWEDEN", new ParseGeoPoint(62, 15.0000)));
        map.put("SG".toLowerCase(),
                new CountryFullNameParseGeoPoint("SINGAPORE", new ParseGeoPoint(1.3667, 103.8000)));
        map.put("SH".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAINT HELENA", new ParseGeoPoint(-15.9333, -5.7000)));
        map.put("SI".toLowerCase(),
                new CountryFullNameParseGeoPoint("SLOVENIA", new ParseGeoPoint(46, 15.0000)));
        map.put("SJ".toLowerCase(),
                new CountryFullNameParseGeoPoint("SVALBARD AND JAN MAYEN ISLANDS", new ParseGeoPoint(78, 20.0000)));
        map.put("SK".toLowerCase(),
                new CountryFullNameParseGeoPoint("SLOVAKIA", new ParseGeoPoint(48.6667, 19.5000)));
        map.put("SL".toLowerCase(),
                new CountryFullNameParseGeoPoint("SIERRA LEONE", new ParseGeoPoint(8.5, -11.5000)));
        map.put("SM".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAN MARINO", new ParseGeoPoint(43.7667, 12.4167)));
        map.put("SN".toLowerCase(),
                new CountryFullNameParseGeoPoint("SENEGAL", new ParseGeoPoint(14, -14.0000)));
        map.put("SO".toLowerCase(),
                new CountryFullNameParseGeoPoint("SOMALIA", new ParseGeoPoint(10, 49.0000)));
        map.put("SR".toLowerCase(),
                new CountryFullNameParseGeoPoint("SURINAME", new ParseGeoPoint(4, -56.0000)));
        map.put("ST".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAO TOME AND PRINCIPE", new ParseGeoPoint(1, 7.0000)));
        map.put("SV".toLowerCase(),
                new CountryFullNameParseGeoPoint("EL SALVADOR", new ParseGeoPoint(13.8333, -88.9167)));
        map.put("SY".toLowerCase(),
                new CountryFullNameParseGeoPoint("SYRIAN ARAB REPUBLIC", new ParseGeoPoint(35, 38.0000)));
        map.put("SZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("SWAZILAND", new ParseGeoPoint(-26.5, 31.5000)));
        map.put("TC".toLowerCase(),
                new CountryFullNameParseGeoPoint("TURKS AND CAICOS ISLANDS", new ParseGeoPoint(21.75, -71.5833)));
        map.put("TD".toLowerCase(), new CountryFullNameParseGeoPoint("CHAD", new ParseGeoPoint(15, 19.0000)));
        map.put("TF".toLowerCase(),
                new CountryFullNameParseGeoPoint("FRENCH SOUTHERN TERRITORIES", new ParseGeoPoint(-43, 67.0000)));
        map.put("TG".toLowerCase(), new CountryFullNameParseGeoPoint("TOGO", new ParseGeoPoint(8, 1.1667)));
        map.put("TH".toLowerCase(),
                new CountryFullNameParseGeoPoint("THAILAND", new ParseGeoPoint(15, 100.0000)));
        map.put("TJ".toLowerCase(),
                new CountryFullNameParseGeoPoint("TAJIKISTAN", new ParseGeoPoint(39, 71.0000)));
        map.put("TK".toLowerCase(),
                new CountryFullNameParseGeoPoint("TOKELAU", new ParseGeoPoint(-9, -172.0000)));
        map.put("TM".toLowerCase(),
                new CountryFullNameParseGeoPoint("TURKMENISTAN", new ParseGeoPoint(40, 60.0000)));
        map.put("TN".toLowerCase(), new CountryFullNameParseGeoPoint("TUNISIA", new ParseGeoPoint(34, 9.0000)));
        map.put("TO".toLowerCase(),
                new CountryFullNameParseGeoPoint("TONGA", new ParseGeoPoint(-20, -175.0000)));
        map.put("TR".toLowerCase(), new CountryFullNameParseGeoPoint("TURKEY", new ParseGeoPoint(39, 35.0000)));
        map.put("TT".toLowerCase(),
                new CountryFullNameParseGeoPoint("TRINIDAD AND TOBAGO", new ParseGeoPoint(11, -61.0000)));
        map.put("TV".toLowerCase(),
                new CountryFullNameParseGeoPoint("TUVALU", new ParseGeoPoint(-8, 178.0000)));
        map.put("TW".toLowerCase(),
                new CountryFullNameParseGeoPoint("TAIWAN", new ParseGeoPoint(23.5, 121.0000)));
        map.put("TZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("TANZANIA, UNITED REPUBLIC OF", new ParseGeoPoint(-6, 35.0000)));
        map.put("UA".toLowerCase(),
                new CountryFullNameParseGeoPoint("UKRAINE", new ParseGeoPoint(49, 32.0000)));
        map.put("UG".toLowerCase(), new CountryFullNameParseGeoPoint("UGANDA", new ParseGeoPoint(1, 32.0000)));
        map.put("UM".toLowerCase(),
                new CountryFullNameParseGeoPoint("UNITED STATES MINOR OUTLYING ISLANDS",
                                                 new ParseGeoPoint(19.2833, 166.6000)));
        map.put("US".toLowerCase(),
                new CountryFullNameParseGeoPoint("UNITED STATES", new ParseGeoPoint(38, -97.0000)));
        map.put("UY".toLowerCase(),
                new CountryFullNameParseGeoPoint("URUGUAY", new ParseGeoPoint(-33, -56.0000)));
        map.put("UZ".toLowerCase(),
                new CountryFullNameParseGeoPoint("UZBEKISTAN", new ParseGeoPoint(41, 64.0000)));
        map.put("VA".toLowerCase(),
                new CountryFullNameParseGeoPoint("VATICAN CITY STATE (HOLY SEE)", new ParseGeoPoint(41.9, 12.4500)));
        map.put("VC".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAINT VINCENT AND THE GRENADINES",
                                                 new ParseGeoPoint(13.25, -61.2000)));
        map.put("VE".toLowerCase(),
                new CountryFullNameParseGeoPoint("VENEZUELA", new ParseGeoPoint(8, -66.0000)));
        map.put("VG".toLowerCase(),
                new CountryFullNameParseGeoPoint("VIRGIN ISLANDS (BRITISH)", new ParseGeoPoint(18.5, -64.5000)));
        map.put("VI".toLowerCase(),
                new CountryFullNameParseGeoPoint("VIRGIN ISLANDS (U.S.)", new ParseGeoPoint(18.3333, -64.8333)));
        map.put("VN".toLowerCase(),
                new CountryFullNameParseGeoPoint("VIET NAM", new ParseGeoPoint(16, 106.0000)));
        map.put("VU".toLowerCase(),
                new CountryFullNameParseGeoPoint("VANUATU", new ParseGeoPoint(-16, 167.0000)));
        map.put("WF".toLowerCase(),
                new CountryFullNameParseGeoPoint("WALLIS AND FUTUNA ISLANDS", new ParseGeoPoint(-13.3, -176.2000)));
        map.put("WS".toLowerCase(),
                new CountryFullNameParseGeoPoint("SAMOA", new ParseGeoPoint(-13.5833, -172.3333)));
        map.put("YE".toLowerCase(), new CountryFullNameParseGeoPoint("YEMEN", new ParseGeoPoint(15, 48.0000)));
        map.put("YT".toLowerCase(),
                new CountryFullNameParseGeoPoint("MAYOTTE", new ParseGeoPoint(-12.8333, 45.1667)));
        map.put("ZA".toLowerCase(),
                new CountryFullNameParseGeoPoint("SOUTH AFRICA", new ParseGeoPoint(-29, 24.0000)));
        map.put("ZM".toLowerCase(),
                new CountryFullNameParseGeoPoint("ZAMBIA", new ParseGeoPoint(-15, 30.0000)));
        map.put("ZW".toLowerCase(),
                new CountryFullNameParseGeoPoint("ZIMBABWE", new ParseGeoPoint(-20, 30.0000)));

    }

    private class JsonGoogleApiSearch {

        private final double lat;
        private final double longi;

        public JsonGoogleApiSearch(ParseGeoPoint geoPoint) {
            this.lat = geoPoint.getLatitude();
            this.longi = geoPoint.getLongitude();
        }

        public String execute() {
            try {
                HttpGet httpGetRequest = new HttpGet("http://maps.googleapis.com/maps/api/geocode/json?latlng="
                                                     + lat + "," + longi + "&sensor=true");
                HttpResponse httpResponse = new DefaultHttpClient().execute(httpGetRequest);
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                String json = sb.toString();
                JSONObject jsonObj = new JSONObject(json);
                String Status = jsonObj.getString("status");
                if (Status.equalsIgnoreCase("OK")) {
                    JSONArray results = jsonObj.getJSONArray("results");
                    // ok says we have first result
                    JSONObject firstResult = results.getJSONObject(0);
                    JSONArray components = firstResult.getJSONArray("address_components");
                    for (int i = 0; i < components.length(); i++) {
                        JSONObject component = components.getJSONObject(i);
                        JSONArray types = component.getJSONArray("types");
                        if (types.length() > 0) {
                            String type = types.getString(0);
                            if (type.equals("country")) {
                                return component.getString("short_name");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
