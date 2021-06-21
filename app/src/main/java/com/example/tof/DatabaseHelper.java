package com.example.tof;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "Colormap.db";
    private static final int DATABASE_VERSION = 2;

    // Table Columns
    private static final String KEY_ID = "id";
    private static final String KEY_RED = "red";
    private static final String KEY_GREEN = "green";
    private static final String KEY_BLUE = "blue";

    // Table status
    private boolean isJETInit = false;
    private boolean isRainbowInit = false;
    private boolean isSMOOTH_COOL_WARMInit = false;
    private boolean isViridisInit = false;
    private boolean isPlasmaInit = false;



    private static DatabaseHelper instance;
    private Context mContext;
    private static final String TAG = DatabaseHelper.class.getName();

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RAINBOW_TABLE = "CREATE TABLE " + ImageProcessor.COLORMAP.RAINBOW.toString() +
                " (" +
                KEY_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_RED + " INTEGER," +
                KEY_GREEN + " INTEGER," +
                KEY_BLUE + " INTEGER" +
                ");";
        String CREATE_JET_TABLE = "CREATE TABLE " + ImageProcessor.COLORMAP.JET.toString() +
                " (" +
                KEY_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_RED + " INTEGER," +
                KEY_GREEN + " INTEGER," +
                KEY_BLUE + " INTEGER" +
                ");";
        String CREATE_SMOOTH_COOL_WARM_TABLE = "CREATE TABLE " + ImageProcessor.COLORMAP.SMOOTH_COOL_WARM.toString() +
                " (" +
                KEY_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_RED + " INTEGER," +
                KEY_GREEN + " INTEGER," +
                KEY_BLUE + " INTEGER" +
                ");";
        String CREATE_VIRIDIS_TABLE = "CREATE TABLE " + ImageProcessor.COLORMAP.VIRIDIS.toString() +
                " (" +
                KEY_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_RED + " INTEGER," +
                KEY_GREEN + " INTEGER," +
                KEY_BLUE + " INTEGER" +
                ");";
        String CREATE_PLASMA_TABLE = "CREATE TABLE " + ImageProcessor.COLORMAP.PLASMA.toString() +
                " (" +
                KEY_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_RED + " INTEGER," +
                KEY_GREEN + " INTEGER," +
                KEY_BLUE + " INTEGER" +
                ");";
        db.execSQL(CREATE_JET_TABLE);
        db.execSQL(CREATE_SMOOTH_COOL_WARM_TABLE);
        db.execSQL(CREATE_VIRIDIS_TABLE);
        db.execSQL(CREATE_PLASMA_TABLE);
        db.execSQL(CREATE_RAINBOW_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            for (ImageProcessor.COLORMAP type: ImageProcessor.COLORMAP.values()){
                db.execSQL("DROP TABLE IF EXISTS " + type.toString());
            }
            onCreate(db);
        }
        Log.d(TAG, "onUpgrade");
    }

//    public void query(int id, SQLiteDatabase db) {
//        Cursor cursor = db.query(TABLE_JET, null, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
//        int red, green, blue;
//        while (cursor.moveToNext()) {
//            red = cursor.getInt(1);
//            green = cursor.getInt(2);
//            blue = cursor.getInt(3);
//            Log.e(TAG, "query result" + red + "green: " + green);
//        }
//        cursor.close();
//    }


    public void update(ImageProcessor.COLORMAP type){
        SQLiteDatabase db = getWritableDatabase();
        switch (type){
            case JET:
                if (!isJETInit && !checkTableStatus(db, ImageProcessor.COLORMAP.JET)){
                    updateFromCSV("jet.csv", ImageProcessor.COLORMAP.JET, db);
                }
                isJETInit = true;
                break;
            case RAINBOW:
                if (!isRainbowInit && !checkTableStatus(db, ImageProcessor.COLORMAP.RAINBOW)){
                    updateFromCSV("rainbow.csv", ImageProcessor.COLORMAP.RAINBOW, db);
                }
                isRainbowInit = true;
                break;
            case SMOOTH_COOL_WARM:
                if (!isSMOOTH_COOL_WARMInit && !checkTableStatus(db, ImageProcessor.COLORMAP.SMOOTH_COOL_WARM)){
                    updateFromCSV("smooth-cool-warm.csv", ImageProcessor.COLORMAP.SMOOTH_COOL_WARM, db);
                }
                isSMOOTH_COOL_WARMInit = true;
                break;
            case VIRIDIS:
                if (!isViridisInit && !checkTableStatus(db, ImageProcessor.COLORMAP.VIRIDIS)){
                    updateFromCSV("viridis.csv", ImageProcessor.COLORMAP.VIRIDIS, db);
                }
                isViridisInit = true;
                break;
            case PLASMA:
                if (!isPlasmaInit && !checkTableStatus(db, ImageProcessor.COLORMAP.PLASMA)){
                    updateFromCSV("plasma.csv", ImageProcessor.COLORMAP.PLASMA, db);
                }
                isPlasmaInit = true;
                break;
        }
        db.close();
    }

    private void updateFromCSV(String filename, ImageProcessor.COLORMAP type, SQLiteDatabase db) {
        try {
            InputStreamReader inputReader = new InputStreamReader(mContext.getAssets().open(filename));
            BufferedReader buffer = new BufferedReader(inputReader);
            String line;
            db.beginTransaction();
            int i = 0;
            while ((line = buffer.readLine()) != null) {
                String[] columns = line.split(",");
                String insertOp = "INSERT INTO " + type.toString() +
                        " ("+ KEY_ID + "," + KEY_RED + ","  + KEY_GREEN + "," + KEY_BLUE  + ") values (" +
                        (i++) + ", " +
                        columns[1] + ", " +
                        columns[2] + ", " +
                        columns[3] + "); ";
                db.execSQL(insertOp);
            }
            db.setTransactionSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * check whether the table has been init.
     * @param type of colormap
     * @return true if table has been init, otherwise false.
     */
    private boolean checkTableStatus(SQLiteDatabase database, ImageProcessor.COLORMAP type){

        Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE TYPE=\"table\" AND NAME= \"" + type.toString() + "\";", null);
        boolean fail = cursor == null || cursor.getCount() == 0;
        if (!fail){
            cursor = database.rawQuery("SELECT * from " + type.toString(), null);
            fail = cursor == null || cursor.getCount() == 0;
        }
        if (cursor != null)
            cursor.close();
        return !fail;
    }
}