package mwleeds.stormsafealabama;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DBOpenHelper extends SQLiteOpenHelper {

    // constants for database name and version
    private static final String DATABASE_NAME = "BARA_locations.db";
    private static final int DATABASE_VERSION = 1;

    // constants for table and column names
    public static final String TABLE_LOCATIONS = "locations";
    public static final String LOCATION_GUID = "_id";
    public static final String LOCATION_NAME = "name";
    public static final String FLOOR = "floor";
    public static final String LOCATION_DESCRIPTION = "description";
    public static final String LATITUDE = "lat";
    public static final String LONGITUDE = "long";
    public static final String ADDRESS = "address";
    public static final String COMMENTS = "comments";

    public static final String[] ALL_COLUMNS =
            {LOCATION_GUID, LOCATION_NAME, FLOOR, LOCATION_DESCRIPTION,
                    LATITUDE, LONGITUDE, ADDRESS, COMMENTS};

    // SQL to create table
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + " (" +
            LOCATION_GUID + " INTEGER PRIMARY KEY, " +
            LOCATION_NAME + " TEXT, " +
            FLOOR + " TEXT, " +
            LOCATION_DESCRIPTION + " TEXT, " +
            LATITUDE + " REAL, " +
            LONGITUDE + " REAL, " +
            ADDRESS + " TEXT, " +
            COMMENTS + " TEXT)";

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }
}
