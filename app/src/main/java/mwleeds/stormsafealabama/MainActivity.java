package mwleeds.stormsafealabama;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send mail to the developer
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getString(R.string.developer_email)});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        });

        Button findSheltersButton = (Button) findViewById(R.id.find_shelters_button);
        findSheltersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapsIntent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(mapsIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_copyright) {
            Intent copyrightIntent = new Intent(this, CopyrightNotice.class);
            startActivity(copyrightIntent);
            return true;
        } else if (id == R.id.action_settings) {
            //TODO
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
