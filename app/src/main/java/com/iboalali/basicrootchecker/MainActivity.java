package com.iboalali.basicrootchecker;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.iboalali.basicrootchecker.components.RootChecker;
import com.iboalali.basicrootchecker.components.RootCheckerContract;
import com.iboalali.basicrootchecker.databinding.ActivityMainBinding;
import com.jaredrummler.android.device.DeviceName;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity implements RootCheckerContract {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SplashScreen.installSplashScreen(this)
                .setOnExitAnimationListener(splashScreenProvider -> {
                    Log.d("SplashScreen", String.format("currentTime is %s ", System.currentTimeMillis()));

                    long startMillis = splashScreenProvider.getIconAnimationStartMillis();
                    Log.d("SplashScreen", String.format("startMillis is %s ", startMillis));

                    if (startMillis == 0) {
                        splashScreenProvider.remove();
                        return;
                    }

                    long timeDiff = startMillis - System.currentTimeMillis();
                    Log.d("SplashScreen", String.format("timeDiff is %s ", timeDiff));
                    Log.d("SplashScreen", String.format("animation duration is %s ", splashScreenProvider.getIconAnimationDurationMillis()));

                    long exitTimeDelay;
                    if (timeDiff <= 0) {
                        exitTimeDelay = splashScreenProvider.getIconAnimationDurationMillis() + timeDiff;
                    } else {
                        exitTimeDelay = splashScreenProvider.getIconAnimationDurationMillis();
                    }

                    Log.d("SplashScreen", String.format("exitTimeDelay is %s ", exitTimeDelay));
                    AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
                    alphaAnimation.setDuration(200);
                    alphaAnimation.setStartOffset(exitTimeDelay);
                    alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            Log.d("SplashScreen", "animation ended");
                            splashScreenProvider.remove();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });

                    Log.d("SplashScreen", "start hiding splash screen animation");
                    splashScreenProvider.getView().startAnimation(alphaAnimation);
                });

        try {
            // for debugging purposes
            //
            //noinspection ConstantConditions
            Field themeIdField = getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("mThemeResource");
            themeIdField.setAccessible(true);
            //noinspection ConstantConditions
            int themeId = (int) themeIdField.get(this);
            Log.d("SplashScreen", "onCreate: theme id = " + themeId);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                boolean lightStatusBar = (this.getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                Log.d("SplashScreen", "onCreate: lightStatusBar = " + lightStatusBar);
            }
        } catch (Exception e) {
            Log.d("SplashScreen", "couldn't read the theme id");
            e.printStackTrace();
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initInstances();
    }

    private void initInstances() {
        // Start of workaround:
        // this is here because when the splash screen theme is being used, the status bar is not in
        // light mode, event though reading it's value does indicate that it is set to light mode
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(!isNight);
        // End of workaround

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding.appToolbar.setOnMenuItemClickListener(item -> {
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

            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.mainRootLayout.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.appBar.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.imageViewStatus.setImageResource(R.drawable.ic_unknown_c);

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

        binding.textViewAndroidVersion.setText(String.format("%s %s",
                getResources().getString(R.string.textViewAndroidVersion),
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
