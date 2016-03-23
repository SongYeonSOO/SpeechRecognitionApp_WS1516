package com.android.katharina.speechrecognitionapp;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.ListView;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.database.sqlite.SQLiteDatabase;

import org.alicebot.ab.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TextToSpeech.OnInitListener  {

    private static final int REQUEST_CODE = 1234;
    private ListView wordsList;
    private TextToSpeech tts;

    SQLiteDatabase db;
    final String CREATE_TABLE_TOKEN = "CREATE TABLE tbl_token ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "token TEXT,"
            + "value TEXT);";
    final String CREATE_TABLE_LOGCAT = "CREATE TABLE tbl_logcat ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "request TEXT,"
            + "response TEXT);";
    
    Bot bot;
    Chat chatSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Hilfsdatenbank für Dictionary (SQLite)
        File dbFile = this.getDatabasePath("DictVR.sqlite");
        if (dbFile.exists()) {
            //open
            db = openOrCreateDatabase("DictVR.sqlite", SQLiteDatabase.OPEN_READWRITE, null);
        } else {
            //create
            db = openOrCreateDatabase("DictVR.sqlite", SQLiteDatabase.CREATE_IF_NECESSARY, null);
        }
        db.execSQL("drop table if exists tbl_token;");
        db.execSQL(CREATE_TABLE_TOKEN);

        insertTokenInDict("RELPRO", new String[]{"ich"});
        insertTokenInDict("PRO", new String[]{"mit"});

        //AKTIONEN: TAKE -> Aufnahme
        insertTokenInDict("TAKE", new String[]{"haben", "habe", "hab", "hatte", "gehabt"});
        insertTokenInDict("TAKE", new String[]{"essen", "esse", "aß", "gegessen"});
        insertTokenInDict("TAKE", new String[]{"trinken", "trinke", "trink", "trank", "getrunken"});
        insertTokenInDict("TAKE", new String[]{"nehmen", "nehme", "nimm", "nahm", "genommen"});

        // NO: Anzahlangaben
        insertTokenInDict("NO:0", new String[]{"null", "0"});
        insertTokenInDict("NO:1", new String[]{"ein", "eine", "einer", "einem", "einen", "einer", "eins", "1"});
        insertTokenInDict("NO:2", new String[]{"zwei", "2"});
        insertTokenInDict("NO:3", new String[]{"drei", "3"});
        insertTokenInDict("NO:4", new String[]{"vier", "4"});
        insertTokenInDict("NO:5", new String[]{"fünf", "5"});
        insertTokenInDict("NO:6", new String[]{"sechs", "6"});
        insertTokenInDict("NO:7", new String[]{"sieben", "7"});
        insertTokenInDict("NO:8", new String[]{"acht", "8"});
        insertTokenInDict("NO:9", new String[]{"neun", "9"});

        //UNIT: Mengeneinheiten
        insertTokenInDict("UNIT:ml150", new String[]{"Tasse"});
        insertTokenInDict("UNIT:ml5", new String[]{"Schuss"});
        insertTokenInDict("UNIT:ml250", new String[]{"Glas"});
        insertTokenInDict("UNIT:g0", new String[]{"Gramm", "Gr", "gr"});
        insertTokenInDict("UNIT:mg0", new String[]{"Milligramm", "Mg", "mg"});
        insertTokenInDict("UNIT:kg0", new String[]{"Kilogramm", "Kg", "kg", "Kilo", "kilo"});
        insertTokenInDict("UNIT:ml0", new String[]{"Milliliter", "Ml", "ml"});
        insertTokenInDict("UNIT:l0", new String[]{"Liter", "L", "l"});

        //DRINKS: Flüssigkeiten
        insertTokenInDict("DRINK:d0", new String[]{"Wasser", "Leitungswasser", "Mineralwasser"});
        insertTokenInDict("DRINK:d1", new String[]{"Kaffee"});
        insertTokenInDict("DRINK:d2", new String[]{"Milch"});

        //Nahrungsmittel
        //FRUITS
        insertTokenInDict("FRUIT:f0", new String[]{"Obst", "Frucht", "Früchte"});
        insertTokenInDict("FRUIT:f1", new String[]{"Apfel"});
        insertTokenInDict("FRUIT:f2", new String[]{"Banane"});
        insertTokenInDict("FRUIT:f3", new String[]{"Clementine", "Klementine", "Mandarine"});

        //MEAL
        insertTokenInDict("MEAL:1", new String[]{"Pizza"});
        insertTokenInDict("MEAL:2", new String[]{"Schnitzel", "Schnitzl"});
        insertTokenInDict("MEAL:3", new String[]{"Rahmspinat"});
        insertTokenInDict("MEAL:4", new String[]{"Bratkartoffel", "Ofenkartoffel"});
        insertTokenInDict("MEAL:5", new String[]{"Salami"});

        //VEG
        insertTokenInDict("VEGETABLE:f0", new String[]{"Gemüse", "Hasenfutter", "Grünzeug"});
        insertTokenInDict("VEGETABLE:f1", new String[]{"Salat", "Blattsalat"});
        insertTokenInDict("VEGETABLE:f2", new String[]{"Spinat", "Blattspinat"});
        insertTokenInDict("VEGETABLE:f3", new String[]{"Kartoffel"});
        insertTokenInDict("VEGETABLE:f4", new String[]{"Tomate", "Fleischtomate", "Cocktailtomate"});

        String[] columns = {"id", "token", "value"};

        Cursor cursor = db.query("tbl_token", columns, null, null, null, null, "value");

        TextView t = new TextView(this);
        //t = (TextView) findViewById(R.id.DBTOKENS);

        String sValue = "";
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (!cursor.getString(2).equals(sValue)) {
                sValue = cursor.getString(2);
                t.append("\n" + cursor.getString(2) + ":");
            }
            t.append(cursor.getString(1) + "[" + cursor.getInt(0) + "], ");
            cursor.moveToNext();
        }
        cursor.close();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.bt_voice_rec);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceRecognitionActivity();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // Einbinden der Nombot-AIML-Dokumente
        File fileExt = new File(getExternalFilesDir(null).getAbsolutePath() + "/bots");
        //if (!fileExt.exists()) {
        ZipFileExtraction extract = new ZipFileExtraction();

        try {
            extract.unZipIt(getAssets().open("bots.zip"), getExternalFilesDir(null).getAbsolutePath() + "/");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //}

        tts = new TextToSpeech(this, this);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                final String path = getExternalFilesDir(null).getAbsolutePath();
                String botname="nombot";
                bot = new Bot(botname, path);
                chatSession = new Chat(bot);
                System.out.println("asyncTask");
                return "";
            }
            @Override
            protected void onPostExecute(String response) {
                if (response.isEmpty()) {
                    response = "There is no response";
                }
                //((TextView) findViewById(R.id.title_text)).setText(response);
            }
        }.execute();

    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void speakOut( String text ) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.GERMAN);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                //btnSpeak.setEnabled(true);
                //speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private int getID(String myVal, String myToken)
    {
        /*
        Cursor c = db.query("tbl_token", new String[]{"id","token =? AND value=?"}, new String[]{ myToken, myVal },null,null,null,null);
        if (c.moveToFirst()) //if the row exist then return the id
            return c.getInt(c.getColumnIndex("id"));
        */
        return -1;
    }

    private void insertTokenInDict(String myValue, String [] tokens)
    {
        for( int i=0; i<tokens.length; i++  )
        {
            //Save
            try {
                ContentValues values = new ContentValues();
                values.put("value", myValue);
                values.put("token", tokens[i]);
                //int id = getID(myValue,tokens[i]);
                //if(id==-1)
                db.insert("tbl_token", null, values);
            } catch (Exception e) {
                //catch code
            }
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_myday) {
            startActivity(new Intent(this, MyDayActivity.class));

        } else if (id == R.id.nav_db) {
            //Ansicht der Einträge in der SQLiteDb
        } else if (id == R.id.nav_setup) {
            //Einstellungen
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            startActivity(new Intent(Intent.ACTION_SEND));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Handle the action of the button being clicked
     */
    public void speakButtonClicked(View v)
    {
        startVoiceRecognitionActivity();
    }

    /**
     * Fire an intent to start the voice recognition activity.
     */
    private void startVoiceRecognitionActivity()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition Demo...");
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * Handle the results from the voice recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        TextView t= new TextView(this);
        t=(TextView)findViewById(R.id.INFO);
        t.setText("onActivityResult requestCode=" + requestCode + ", resultCode=" + resultCode + ":");

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            t.append( "RESULT_OK\n" );
            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS );
            for( int i=0; i<matches.size(); i++ )
            {
                t.append( i+1 + ":" + matches.get(i) + "\n" );
            }
            if( matches.size() > 0 )
            {
                recognizeVoice(matches);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //Ersatzwörterbuch
    // eventuell Wörterbucheinträge noch später verwenden für Speichern der Rückgabewerte
    // vom AIML-Match
    // -- aktuell nicht in Gebrauch
    protected String getValueFromDictionary( String myToken )
    {
        String ret="UNKNOWN";
        final Cursor cursor = db.rawQuery("SELECT value from tbl_token where token='" + myToken + "'", null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    ret = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return ret;
    }


    protected void recognizeVoice( ArrayList<String> matches )
    {
        TextView t= new TextView(this);
        t=(TextView)findViewById(R.id.TOKENS);
        t.setText("");
        int idx = 0;
        //User Request
        do{
            String request = matches.get(idx);
            //Bot Response
            String response = chatSession.multisentenceRespond(request);
            // Log relevant return-values to Table LOGCAT
            // for following display/ function ...
            // ...
            t.setText("Request: " + request + ", Response: " + response);
            if( (response == "") && (idx < matches.size()-1 ))
                idx++;
            else {
                speakOut( response );
                break;
            }
        }while(1==1);

        /*
        String [] foo = woerter.split(" ");
        String [] bed = new String[ foo.length ];

        for( int i=0; i<foo.length; i++ )
        {
            bed[ i ] = getValueFromDictionary(foo[i]);
        }
        for( int i=0; i<bed.length; i++ )
        {
            if (bed[i].equals("VEGETABLE:f0") || bed[i].equals("FRUIT:f0")) {
                t.append(i + 1 + ":   " + foo[i] + "/   " + bed[i] + "\n");
                t.append("Ich kenne " + foo[i] + " nicht. Bitte beschreibe es genauer.\n");
            }
            else
                t.append( i+1 + ":   " + foo[i] + "/   " + bed[i] + "\n" );
        }
        */
    }
}