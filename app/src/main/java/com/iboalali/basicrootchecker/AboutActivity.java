package com.iboalali.basicrootchecker;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.iboalali.basicrootchecker.databinding.ActivityAboutBinding;

/**
 * Created by Ibrahim on 17-Jul-15.
 */
public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_about);
        initInstances();
    }

    private void initInstances() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding.aboutAppVersion.setText(Utils.getAppVersionNumber(this));

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.rootLayout.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setSystemWindowLightMode(this, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.setSystemWindowLightMode(this, true);
    }
}
