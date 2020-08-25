package com.iboalali.basicrootchecker;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.iboalali.basicrootchecker.components.RootChecker;
import com.iboalali.basicrootchecker.components.RootCheckerContract;
import com.jaredrummler.android.device.DeviceName;

public class MainActivity extends AppCompatActivity implements RootCheckerContract {

    @Override
    public void onPreExecute() {
        imageView.setVisibility(View.INVISIBLE);
        progressBarLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostExecute(Boolean result) {
        progressBarLoading.setVisibility(View.INVISIBLE);

        imageView.setVisibility(View.VISIBLE);
        if (result != null && result) {
            textViewCheckForRoot.setText(R.string.rootAvailable);
            imageView.setImageResource(R.drawable.ic_success_c);
        } else {
            textViewCheckForRoot.setText(R.string.rootNotAvailable);
            imageView.setImageResource(R.drawable.ic_fail_c);
        }
    }

    private ConstraintLayout rootLayoutNew;
    private ProgressBar progressBarLoading;
    private ImageView imageView;
    private TextView textViewCheckForRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getDisplayMetrics().density > 1.5) {
            setContentView(R.layout.activity_main_new);
        } else {
            setContentView(R.layout.activity_main_new_small);
        }

        initInstances();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_licence) {
            Intent intent = new Intent(this, LicenceActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initInstances() {
        rootLayoutNew = findViewById(R.id.rootLayoutNew);
        Toolbar toolbar = findViewById(R.id.toolbarNew);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_navigation);
            setSupportActionBar(toolbar);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.primary));
        }

        progressBarLoading = findViewById(R.id.progressbarLoadingNew);
        textViewCheckForRoot = findViewById(R.id.textViewRootStatusNew);
        imageView = findViewById(R.id.imageViewStatusNew);
        imageView.setBackgroundResource(R.drawable.ic_unknown_c);

        FloatingActionButton fabVerifyRoot = findViewById(R.id.fabVerifyRootNew);
        fabVerifyRoot.setOnClickListener(view -> {
            // Verify root here
            new RootChecker(MainActivity.this).execute();
            Snackbar.make(rootLayoutNew, R.string.string_checking_for_root, Snackbar.LENGTH_SHORT)
                    .show();
        });

        DeviceName.with(this).request((info, error) -> {
            TextView textViewDeviceModel = findViewById(R.id.textViewDeviceModelNew);
            textViewDeviceModel.setText(info.marketName);
        });

        TextView textViewAndroidVersion = findViewById(R.id.textViewAndroidVersionNew);
        textViewAndroidVersion.setText(String.format("%s %s %s",
                getResources().getString(R.string.textViewAndroidVersion),
                Build.VERSION.RELEASE,
                Utils.getAndroidName(this.getResources())
        ));
    }
}
