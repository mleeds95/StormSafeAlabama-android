package mwleeds.stormsafealabama;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class CopyrightNotice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copyright_notice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tv = (TextView) findViewById(R.id.copyright_text);
        tv.setText("");
        // Add a copyright notice for this app's code.
        tv.append(getString(R.string.app_copyright));
        // Add a copyright notice for the app's icon.
        tv.append(getString(R.string.icon_copyright));
        // Add the required copyright notice for the Google Play Services API
        String googleLicenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
        if (googleLicenseInfo != null) {
            tv.append(googleLicenseInfo);
        }
    }

}
