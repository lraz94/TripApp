package com.source.tripwithme.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseGeoPoint;
import com.source.tripwithme.components.ListWithListeners;
import com.source.tripwithme.components.ListenerOnCollection;
import com.source.tripwithme.components.PointWithDistance;
import com.source.tripwithme.components.PointWithID;

public class InterestsDatabase implements ListenerOnCollection<PointWithID> {

    private static final String LATITUDE_FIELD = "lati";
    private static final String LONGITUDE_FIELD = "longi";
    private static final String SPECIAL_STRING_FIELD = "special";
    private static final String DISTANCE_FIELD = "distance";
    private static final String TABLE_NAME = "interests";
    private static final String ID_FIELD = "id";

    private static InterestsDatabase instance = null;
    private SQLiteDatabase db;
    private boolean iAmAdding;

    public static InterestsDatabase init(final Context context, final ListWithListeners<PointWithID> backedList,
                                         final DoneLoadingCallBack callBack) {
        if (instance == null) {
            return new InterestsDatabase(context, backedList, callBack);
        }
        // problem with quiting and getting back... ?
        backedList.addListListener(instance);
        instance.initAndFillListSeparateThread(context, callBack, backedList);
        return instance;
    }

    private InterestsDatabase(final Context context, final ListWithListeners<PointWithID> backedList,
                              final DoneLoadingCallBack callBack) {
        backedList.addListListener(this);
        initAndFillListSeparateThread(context, callBack, backedList);
    }

    private void initAndFillListSeparateThread(final Context context, final DoneLoadingCallBack callBack,
                                               final ListWithListeners<PointWithID> backedList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                iAmAdding = true;
                db = new DBHelper(context).getWritableDatabase();
                Cursor cursor =
                    db.query(TABLE_NAME, new String[]{ID_FIELD, LATITUDE_FIELD, LONGITUDE_FIELD, DISTANCE_FIELD,
                        SPECIAL_STRING_FIELD}, null, null, null, null, null);
                if (cursor.moveToFirst()) {
                    int placeOfID = cursor.getColumnIndex(ID_FIELD);
                    int placeOfLati = cursor.getColumnIndex(LATITUDE_FIELD);
                    int placeOfLangi = cursor.getColumnIndex(LONGITUDE_FIELD);
                    int placeOfSpecial = cursor.getColumnIndex(SPECIAL_STRING_FIELD);
                    int placeOfDiatance = cursor.getColumnIndex(DISTANCE_FIELD);
                    do {
                        long id = cursor.getLong(placeOfID);
                        double lati = cursor.getDouble(placeOfLati);
                        double longi = cursor.getDouble(placeOfLangi);
                        String special = cursor.getString(placeOfSpecial);
                        double distance = cursor.getDouble(placeOfDiatance);
                        PointWithID point = PointWithID
                            .recreate(new PointWithDistance(new ParseGeoPoint(lati, longi), distance, special), id);
                        backedList.add(point);
                    } while (cursor.moveToNext());
                }
                cursor.close();
                iAmAdding = false;
                callBack.doneLoading();
            }
        }).start();
    }

    @Override
    public void itemWasAdded(final PointWithID object) {
        if (!iAmAdding) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ContentValues values = new ContentValues();
                    PointWithDistance point = object.getPoint();
                    LatLng latLng = point.getLatLng();
                    values.put(ID_FIELD, object.getId());
                    values.put(LATITUDE_FIELD, latLng.latitude);
                    values.put(LONGITUDE_FIELD, latLng.longitude);
                    String speical = point.getSpecialName();
                    if (speical != null) {
                        values.put(SPECIAL_STRING_FIELD, speical);
                    }
                    values.put(DISTANCE_FIELD, point.getDistance());
                    if (db.insert(TABLE_NAME, null, values) != -1) {
                        System.out.println("inserted point added " + object);
                    }
                }
            }).start();
        }
    }

    @Override
    public void itemWasRemoved(PointWithID obj) {
        removeInSeparateThread(obj);
    }

    private void removeInSeparateThread(final PointWithID obj) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = db.delete(TABLE_NAME, ID_FIELD + " = " + obj.getId(), null);
                System.out.println("Deleted point: " + obj + " count: " + count);
            }
        }).start();
    }

    public void close() {
        db.close();
    }

    public interface DoneLoadingCallBack {

        void doneLoading();
    }

    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "interests_db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + TABLE_NAME + " ( " + ID_FIELD + " integer primary key, "
                       + LATITUDE_FIELD + " real, " + LONGITUDE_FIELD + " real, " + SPECIAL_STRING_FIELD +
                       " text, " + DISTANCE_FIELD + " real );");


        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // nothing
        }
    }
}
