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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.aboutAppVersion.setText(Utils.getAppVersionNumber(this));

        binding.collapsingToolbarLayout.setTitle(getResources().getString(R.string.action_about));
        binding.collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.TextAppearance_iboalali_Title_Collapsed_Noto);
        binding.collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.TextAppearance_iboalali_Title_Expanded_Noto);

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBar.setPadding(0, insets.top, 0, 0);
            binding.rootLayout.setPadding(insets.left, 0, insets.right, 0);
            binding.scrollContainer.setPadding(0, 0, 0, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
