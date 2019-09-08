package com.iboalali.basicrootchecker.components;

/**
 * Created by iboalali on 08-Sep-19.
 */
public interface RootCheckerContract {

    void onPreExecute();

    void onPostExecute(Boolean result);
}
