package com.iboalali.basicrootchecker;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.iboalali.basicrootchecker.components.RootChecker;
import com.iboalali.basicrootchecker.components.RootCheckerContract;
import com.iboalali.basicrootchecker.databinding.ActivityMainBinding;
import com.jaredrummler.android.device.DeviceName;

public class MainActivity extends AppCompatActivity implements RootCheckerContract {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initInstances();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setSupportActionBar(binding.appToolbar);

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.mainRootLayout.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.appBar.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.imageViewStatus.setBackgroundResource(R.drawable.ic_unknown_c);

        binding.fabVerifyRoot.setOnClickListener(view -> {
            new RootChecker(MainActivity.this).execute();
            Snackbar.make(binding.rootLayout, R.string.string_checking_for_root, Snackbar.LENGTH_SHORT).show();
        });

        DeviceName.with(this).request((info, error) -> {
            if (!Utils.equals(info.marketName, info.model)) {
                binding.textViewDeviceModelName.setVisibility(View.VISIBLE);
                binding.textViewDeviceModelName.setText(info.model);
            } else {
                binding.textViewDeviceModelName.setVisibility(View.GONE);
            }

            binding.textViewDeviceMarketingName.setText(info.marketName);
        });

        binding.textViewAndroidVersion.setText(String.format("%s %s %s",
                getResources().getString(R.string.textViewAndroidVersion),
                Build.VERSION.RELEASE,
                Utils.getAndroidName(this.getResources())
        ));
    }

    @Override
    public void onPreExecute() {
        binding.imageViewStatus.setVisibility(View.INVISIBLE);
        binding.progressbarLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostExecute(Boolean result) {
        binding.progressbarLoading.setVisibility(View.INVISIBLE);

        binding.imageViewStatus.setVisibility(View.VISIBLE);
        if (result != null && result) {
            binding.textViewRootStatus.setText(R.string.rootAvailable);
            binding.imageViewStatus.setImageResource(R.drawable.ic_success_c);
        } else {
            binding.textViewRootStatus.setText(R.string.rootNotAvailable);
            binding.imageViewStatus.setImageResource(R.drawable.ic_fail_c);
        }
    }
}
