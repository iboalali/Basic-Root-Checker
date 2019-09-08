package com.iboalali.basicrootchecker.components;

import android.os.AsyncTask;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

/**
 * Created by iboalali on 08-Sep-19.
 */
public class RootChecker extends AsyncTask<Void, Void, Boolean> {
    private RootCheckerContract mContract;

    public RootChecker(RootCheckerContract contract) {
        mContract = contract;
    }

    @Override
    protected void onPreExecute() {
        mContract.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Log.d("RootChecker", "Checking for root");
        Boolean result = Shell.rootAccess();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mContract.onPostExecute(result);
    }
}
