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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLicenceBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_licence);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding.collapsingToolbarLayout.setTitle(getResources().getString(R.string.action_licence));
        binding.collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.TextAppearance_iboalali_Title_Collapsed_Noto);
        binding.collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.TextAppearance_iboalali_Title_Expanded_Noto);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.rootLayout.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.appBarLicence.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
