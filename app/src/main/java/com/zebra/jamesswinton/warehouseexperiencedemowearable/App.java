package com.zebra.jamesswinton.warehouseexperiencedemowearable;

import android.app.Application;

public class App extends Application {

  // Debugging
  private static final String TAG = "ApplicationClass";

  // Constants


  // Static Variables


  // Variables


  @Override
  public void onCreate() {
    super.onCreate();

    // Init DataWedge
    DataWedgeUtilities.initDataWedgeProfile(getApplicationContext());
  }
}
