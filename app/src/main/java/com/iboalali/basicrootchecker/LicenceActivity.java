package com.iboalali.basicrootchecker;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.iboalali.basicrootchecker.databinding.ActivityLicenceBinding;

/**
 * Created by Ibrahim on 17-Jul-15.
 */
public class LicenceActivity extends AppCompatActivity {

    private ActivityLicenceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_licence);
        initInstances();
    }

    private void initInstances() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding.collapsingToolbarLayout.setTitle(getResources().getString(R.string.action_licence));
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.rootLayout.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.appBarLicence.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
