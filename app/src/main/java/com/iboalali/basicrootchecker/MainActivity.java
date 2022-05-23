package com.iboalali.basicrootchecker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity implements RootCheckerContract {

    private ActivityMainBinding binding;

    private int fabBottomMargin = 0;

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
                    ValueAnimator valueAnimator = ValueAnimator.ofFloat(1f, 0f);
                    valueAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            Log.d("SplashScreen", "animation canceled");
                            splashScreenProvider.remove();
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Log.d("SplashScreen", "animation ended");
                            splashScreenProvider.remove();
                        }
                    });
                    valueAnimator.setStartDelay(exitTimeDelay);
                    valueAnimator.addUpdateListener(animation -> {
                        float animatedValue = (float) animation.getAnimatedValue();
                        splashScreenProvider.getView().setAlpha(animatedValue);
                        splashScreenProvider.getIconView().setAlpha(animatedValue);
                    });
                    valueAnimator.start();
                });

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
            binding.mainRootLayout.setPadding(insets.left, 0, insets.right, 0);
            binding.scrollContainer.setPadding(0, 0, 0, insets.bottom);
            binding.appBar.setPadding(0, insets.top, 0, 0);
            ViewGroup.LayoutParams layoutParams = binding.fabVerifyRoot.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                if (fabBottomMargin == 0) {
                    fabBottomMargin = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
                }
                ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = fabBottomMargin + insets.bottom;
            }
            binding.fabVerifyRoot.setLayoutParams(layoutParams);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.imageViewStatus.setImageResource(R.drawable.ic_unknown_c);

        binding.fabVerifyRoot.setOnClickListener(view -> {
            new RootChecker(MainActivity.this).execute();
            Snackbar.make(binding.mainRootLayout, R.string.string_checking_for_root, Snackbar.LENGTH_SHORT).show();
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

        binding.textViewAndroidVersion.setOnLongClickListener(copyContentLongClickListener);
        binding.textViewDeviceModelName.setOnLongClickListener(copyContentLongClickListener);
        binding.textViewDeviceMarketingName.setOnLongClickListener(copyContentLongClickListener);
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

    private final View.OnLongClickListener copyContentLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (v instanceof TextView) {
                CharSequence text = ((TextView) v).getText();
                if (text != null) {
                    String string = text.toString();
                    String label = "Text";
                    CharSequence contentDescription = v.getContentDescription();
                    if (contentDescription != null) {
                        label = contentDescription.toString();
                    }
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, string));
                    Snackbar.make(binding.mainRootLayout, R.string.toast_content_copied, Snackbar.LENGTH_SHORT).show();
                }

                return true;
            }

            return false;
        }
    };
}
