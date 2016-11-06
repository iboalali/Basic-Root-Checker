package com.iboalali.basicrootchecker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.jaredrummler.android.device.DeviceName;

import eu.chainfire.libsuperuser.Shell;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    public static Boolean suAvailable;
    private Context context;

    static {
        suAvailable = false;
    }

    public class checkForRoot extends AsyncTask<Void, Void, Void> {
        private Context context = null;

        public checkForRoot setContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        protected void onPreExecute() {
            //fabRoot.setVisibility(View.GONE);
            //fabRootSuccess.setVisibility(View.GONE);
            //fabRootFail.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            progressBarLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Checking for root");
            suAvailable = Shell.SU.available();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBarLoading.setVisibility(View.GONE);

            imageView.setVisibility(View.VISIBLE);
            if (suAvailable){
                //fabRootSuccess.setVisibility(View.VISIBLE);
                textViewCheckForRoot.setText("Your Device has Root access");
                imageView.setBackgroundResource(R.drawable.ic_success_c);
                //imageView.setImageDrawable(getDrawable(R.drawable.ic_success_c));
            }else{
                //fabRootFail.setVisibility(View.VISIBLE);
                textViewCheckForRoot.setText("Your Device doesn't have Root access");
                imageView.setBackgroundResource(R.drawable.ic_fail_c);
            }
        }
    }


    private FloatingActionButton fabRoot;
    private FloatingActionButton fabRootSuccess;
    private FloatingActionButton fabRootFail;
    private CoordinatorLayout rootLayout;
    private ProgressBar progressBarLoading;
    private AdView mAdView;
    private ImageView imageView;
    private TextView textViewCheckForRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initInstances();
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }else if(id == R.id.action_licence){
            Intent intent = new Intent(this, LicenceActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initInstances() {
        mAdView = (AdView) findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        rootLayout = (CoordinatorLayout) findViewById(R.id.rootLayout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.mipmap.ic_launcher);
            setSupportActionBar(toolbar);
        }
        context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.primaryDark));
        }

        progressBarLoading = (ProgressBar) findViewById(R.id.progressbarLoading);
        //fabRoot = (FloatingActionButton) findViewById(R.id.fabRoot);
        //fabRootSuccess = (FloatingActionButton) findViewById(R.id.fabRootSuccess);
        //fabRootFail = (FloatingActionButton) findViewById(R.id.fabRootFail);
        textViewCheckForRoot = (TextView) findViewById(R.id.textViewCheckForRoot);
        imageView = (ImageView) findViewById(R.id.imageViewStatus);
        imageView.setBackgroundResource(R.drawable.ic_unknown_c);

        FloatingActionButton fabVerifyRoot = (FloatingActionButton) findViewById(R.id.fabVerifyRoot);
        fabVerifyRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Verify root here
                // ...

                (new checkForRoot()).setContext(context).execute();

                Snackbar.make(rootLayout, "Checking for Root...", Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        DeviceName.with(this).request(new DeviceName.Callback() {
            @Override
            public void onFinished(DeviceName.DeviceInfo info, Exception error) {
                TextView textViewDeviceModel = (TextView) findViewById(R.id.textViewDeviceModel);
                textViewDeviceModel.setText( getResources().getString(R.string.textViewDevice) + " " + info.marketName);
            }
        });



        TextView textViewAndroidVersion = (TextView) findViewById(R.id.textViewAndroidVersion);
        textViewAndroidVersion.setText(getResources().getString(R.string.textViewAndroidVersion) + " " + Build.VERSION.RELEASE + " " + Utils.getAndroidName(this, Build.VERSION.SDK_INT));


    }

}
