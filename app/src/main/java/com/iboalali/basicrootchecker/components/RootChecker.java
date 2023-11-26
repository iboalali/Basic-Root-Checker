package com.iboalali.basicrootchecker.components;

import android.util.Log;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by iboalali on 08-Sep-19.
 */
public class RootChecker {
    private final RootCheckerContract mContract;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private RootChecker(@NonNull RootCheckerContract contract) {
        mContract = contract;
    }

    public static RootChecker create(@NonNull RootCheckerContract contract) {
        return new RootChecker(contract);
    }

    public void run() {
        mContract.onPreExecute();

        executor.execute(() -> {
            Log.d("RootChecker", "Checking for root");
            Boolean result = Shell.rootAccess();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mContract.onResult(result);
        });
    }
}
