package com.iboalali.basicrootchecker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.jaredrummler.android.device.DeviceName;

import com.iboalali.basicrootchecker.BillingManager;
import com.iboalali.basicrootchecker.BillingManager.BillingUpdatesListener;
import com.topjohnwu.superuser.Shell;

import java.util.List;

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
            imageView.setVisibility(View.INVISIBLE);
            progressBarLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Checking for root");
            suAvailable = Shell.rootAccess();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBarLoading.setVisibility(View.INVISIBLE);

            imageView.setVisibility(View.VISIBLE);
            if (suAvailable){
                //fabRootSuccess.setVisibility(View.VISIBLE);
                textViewCheckForRoot.setText("Your Device has Root access");
                imageView.setImageResource(R.drawable.ic_success_c);
                //imageView.setImageDrawable(getDrawable(R.drawable.ic_success_c));
            }else{
                //fabRootFail.setVisibility(View.VISIBLE);
                textViewCheckForRoot.setText("Your Device doesn't have Root access");
                imageView.setImageResource(R.drawable.ic_fail_c);
            }
        }
    }


    private FloatingActionButton fabRoot;
    private FloatingActionButton fabRootSuccess;
    private FloatingActionButton fabRootFail;
    private CoordinatorLayout rootLayout;
    private ConstraintLayout rootLayoutNew;
    private ProgressBar progressBarLoading;
    private ImageView imageView;
    private TextView textViewCheckForRoot;
    private BillingManager mBillingManager;
    private UpdateListener updateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);

        initInstances();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBillingManager != null
                && mBillingManager.getBillingClientResponseCode() == BillingClient.BillingResponse.OK) {
            mBillingManager.queryPurchases();
        }

    }

    @Override
    public void onDestroy() {
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
        //rootLayout = (CoordinatorLayout) findViewById(R.id.rootLayout);
        rootLayoutNew = findViewById(R.id.rootLayoutNew);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarNew);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_navigation);
            setSupportActionBar(toolbar);
        }
        context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.primary));
        }


        updateListener = new UpdateListener();
        mBillingManager = new BillingManager(this, updateListener);

        progressBarLoading = findViewById(R.id.progressbarLoadingNew);
        textViewCheckForRoot = findViewById(R.id.textViewRootStatusNew);
        imageView =  findViewById(R.id.imageViewStatusNew);
        imageView.setBackgroundResource(R.drawable.ic_unknown_c);

        FloatingActionButton fabVerifyRoot = findViewById(R.id.fabVerifyRootNew);
        fabVerifyRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Verify root here
                // ...

                (new checkForRoot()).setContext(context).execute();

                Snackbar.make(rootLayoutNew, "Checking for Root...", Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        DeviceName.with(this).request(new DeviceName.Callback() {
            @Override
            public void onFinished(DeviceName.DeviceInfo info, Exception error) {
                TextView textViewDeviceModel = findViewById(R.id.textViewDeviceModelNew);
                textViewDeviceModel.setText(info.marketName);
            }
        });


        TextView textViewAndroidVersion = findViewById(R.id.textViewAndroidVersionNew);
        textViewAndroidVersion.setText(String.format("%s %s %s",
                getResources().getString(R.string.textViewAndroidVersion),
                Build.VERSION.RELEASE,
                Utils.getAndroidName(this, Build.VERSION.SDK_INT)));

        Button donation_button = findViewById(R.id.donationButtonNew);
        donation_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    }

    private class UpdateListener implements BillingUpdatesListener {

        @Override
        public void onBillingClientSetupFinished() {

        }

        @Override
        public void onConsumeFinished(String token, int result) {

        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchases) {

        }
    }

}
