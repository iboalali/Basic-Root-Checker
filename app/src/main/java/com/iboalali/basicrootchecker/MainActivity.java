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
import com.iboalali.basicrootchecker.databinding.ActivityMainNewBinding;
import com.jaredrummler.android.device.DeviceName;

public class MainActivity extends AppCompatActivity implements RootCheckerContract {

    private ActivityMainNewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_new);
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
        setSupportActionBar(binding.toolbarNew);

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.mainRootLayout.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.appBarNew.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.imageViewStatusNew.setBackgroundResource(R.drawable.ic_unknown_c);

        binding.fabVerifyRootNew.setOnClickListener(view -> {
            new RootChecker(MainActivity.this).execute();
            Snackbar.make(binding.rootLayoutNew, R.string.string_checking_for_root, Snackbar.LENGTH_SHORT).show();
        });

        DeviceName.with(this).request((info, error) -> {
            if (!Utils.equals(info.marketName, info.model)) {
                binding.textViewDeviceModelNameNew.setVisibility(View.VISIBLE);
                binding.textViewDeviceModelNameNew.setText(info.model);
            } else {
                binding.textViewDeviceModelNameNew.setVisibility(View.GONE);
            }

            binding.textViewDeviceMarketingNameNew.setText(info.marketName);
        });

        binding.textViewAndroidVersionNew.setText(String.format("%s %s %s",
                getResources().getString(R.string.textViewAndroidVersion),
                Build.VERSION.RELEASE,
                Utils.getAndroidName(this.getResources())
        ));
    }

    @Override
    public void onPreExecute() {
        binding.imageViewStatusNew.setVisibility(View.INVISIBLE);
        binding.progressbarLoadingNew.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPostExecute(Boolean result) {
        binding.progressbarLoadingNew.setVisibility(View.INVISIBLE);

        binding.imageViewStatusNew.setVisibility(View.VISIBLE);
        if (result != null && result) {
            binding.textViewRootStatusNew.setText(R.string.rootAvailable);
            binding.imageViewStatusNew.setImageResource(R.drawable.ic_success_c);
        } else {
            binding.textViewRootStatusNew.setText(R.string.rootNotAvailable);
            binding.imageViewStatusNew.setImageResource(R.drawable.ic_fail_c);
        }
    }
}
