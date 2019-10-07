package com.zebra.jamesswinton.warehouseexperiencedemowearable;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.zebra.jamesswinton.warehouseexperiencedemowearable.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.ACTION_DATAWEDGE_FROM_6_2;
import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.ENUMERATE_SCANNERS_ACTION;
import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.ENUMERATE_SCANNERS_RESULT;
import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.RESULT_ACTION;
import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.SCAN_ACTION;
import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.SET_SCANNER;
import static com.zebra.jamesswinton.warehouseexperiencedemowearable.DataWedgeUtilities.SET_SCANNER_COMMAND_ID;

public class MainActivity extends AppCompatActivity {

  // Debugging
  private static final String TAG = "MainActivity";

  // Constants
  private static final int DEFAULT_PADDING = 25;
  private static final Map<String, Boolean> demoBarcodes  = new HashMap<String, Boolean>() {{
    put("Demo-01", false); put("Demo-02", false); put("Demo-03", false); put("Demo-04", false);
    put("Demo-05", false); put("Demo-06", false);
  }};

  private static final String DEMO_01 = "Demo-01";
  private static final String DEMO_02 = "Demo-02";
  private static final String DEMO_03 = "Demo-03";
  private static final String DEMO_04 = "Demo-04";
  private static final String DEMO_05 = "Demo-05";
  private static final String DEMO_06 = "Demo-06";

  // Static Variables
  private static IntentFilter mDataWedgeIntentFilter = new IntentFilter();

  // Variables
  private ActivityMainBinding mDataBinding = null;
  private AlertDialog mGettingScannerListDialog = null;

  private int mDpAsPixels;
  private enum AnimationState { SUCCESS, ERROR, DUPLICATE, FINISHED }

  /**
   * Life Cycle Methods
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Init DataBinding
    mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    // Add DW Scan & Result Filter
    mDataWedgeIntentFilter.addAction(RESULT_ACTION);
    mDataWedgeIntentFilter.addAction(SCAN_ACTION);
    mDataWedgeIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);

    // Init Counter to Zero
    mDataBinding.scanCount.setText(String.format(getString(R.string.scan_progress), 0, demoBarcodes.size()));

    // Init DP Setting
    mDpAsPixels = getPaddingInDp(DEFAULT_PADDING);

    // Init Toolbar
    configureToolbar();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Register Receiver
    registerReceiver(mDataWedgeBroadcastReceiver, mDataWedgeIntentFilter);

    // Show Dialog whilst getting Scanners
    mGettingScannerListDialog = new AlertDialog.Builder(this)
            .setTitle("Finding Scanners")
            .setMessage("Please wait while we load available scanners...")
            .setCancelable(false)
            .create();
    mGettingScannerListDialog.show();

    // Get & Select Scanner
    Intent getScanners = new Intent();
    getScanners.setAction(ACTION_DATAWEDGE_FROM_6_2);
    getScanners.putExtra(ENUMERATE_SCANNERS_ACTION, "");
    sendBroadcast(getScanners);
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister Receiver
    unregisterReceiver(mDataWedgeBroadcastReceiver);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.toolbar_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // Handle Navigation Events
    switch(item.getItemId()) {
      case R.id.reset:
        resetDemo();
        break;
    } return true;
  }

  private void configureToolbar() {
    setSupportActionBar(mDataBinding.toolbarLayout.toolbar);
  }

  /**
   * DataWedge Broadcast Receiver
   */

  private BroadcastReceiver mDataWedgeBroadcastReceiver = new BroadcastReceiver() {

    // Barcode Data
    private static final String SCAN_DATA = "com.symbol.datawedge.data_string";

    @Override
    public void onReceive(Context context, Intent intent) {
      // Validate Action
      if (intent.getAction() == null) {
        Log.e(TAG, "Intent Does Not Contain Action!");
        return;
      }

      // Get Result Intent (Enumerating Scanners)
      if (intent.getAction().equals(RESULT_ACTION)) {

        // Check Result is for Enumeration of Scanners
        if (intent.hasExtra(ENUMERATE_SCANNERS_RESULT)) {

          // Remove Dialog
          if (mGettingScannerListDialog != null && mGettingScannerListDialog.isShowing()) {
            mGettingScannerListDialog.dismiss();
          }

          // Get List of Scanners
          ArrayList<Bundle> scannerList = (ArrayList<Bundle>)
                  intent.getSerializableExtra(ENUMERATE_SCANNERS_RESULT);

          // Validate Prescence of Scanner(s)
          if (scannerList != null && !scannerList.isEmpty()) {

            List<String> scannerName = new ArrayList<>();
            List<String> scannerIndex = new ArrayList<>();

            // List Scanners
            for (Bundle scannerBundle : scannerList) {
              // Validate Scanner connected
              if (scannerBundle.getBoolean("SCANNER_CONNECTION_STATE")) {
                // Add Scanner to List for PickerDialog
                scannerName.add(scannerBundle.getString("SCANNER_NAME"));
                scannerIndex.add(String.valueOf(scannerBundle.getInt("SCANNER_INDEX")));
              }
            }

            // Show Picker Dialog
            showScannerPickerDialog(scannerName.toArray(new String[scannerName.size()]),
                    scannerIndex.toArray(new String[scannerIndex.size()]));

          }
        }
      }

      // Get Scan Intent
      if (intent.getAction().equals(SCAN_ACTION)) {
        // Log Receipt
        Log.i(TAG, "DataWedge Scan Intent Received");

        // Get Scanned Data
        String scannedData = intent.getStringExtra(SCAN_DATA);

        // Handle Scan Result
        handleScannedBarcode(scannedData);
      }
    }
  };

  /**
   * Show Scanner Picker Dialog
   */

  private void showScannerPickerDialog(String[] scannerNames, String[] scannerIndex) {
    new AlertDialog.Builder(this)
            .setTitle("Please Select a Scanner")
            .setItems(scannerNames, (dialogInterface, i) -> {
              // Log Selection
              Log.i(TAG, "Scanner Selected: " + scannerNames[i] + " (" + scannerIndex[i] + ")");

              // Set Scanner in DW
              Intent setScanner = new Intent();
              setScanner.setAction(ACTION_DATAWEDGE_FROM_6_2);
              setScanner.putExtra(SET_SCANNER, scannerIndex[i]);
              setScanner.putExtra("RECEIVE_RESULT","true");
              setScanner.putExtra("COMMAND_IDENTIFIER", SET_SCANNER_COMMAND_ID);
              sendBroadcast(setScanner);

              // Dismiss Dialog
              dialogInterface.dismiss();
            })
            .setPositiveButton("CANCEL", null)
            .setCancelable(false)
            .create().show();
  }

  /**
   * Barcode Logic
   */

  private void handleScannedBarcode(String scannedData) {
    // Verify Barcode is part of demo
    if (!demoBarcodes.containsKey(scannedData)) {
      Log.e(TAG, "Scanned barcode is not part of demo!");
      // Scan Animation Removed
      // showScanAnimation(AnimationState.ERROR);
      return;
    }

    // Verify if Barcode has already been scanned
    if (demoBarcodes.get(scannedData)) {
      Log.e(TAG, "Barcode has already been scanned!");
      // Scan Animation Removed
      // showScanAnimation(AnimationState.DUPLICATE);
      return;
    }

    // Barcode exists & has not been scanned before, update map value
    demoBarcodes.put(scannedData, true);

    // Scan Animation Removed
    // showScanAnimation(AnimationState.SUCCESS);

    // Update UI State
    updateUiState();

    // Handle Swap / Finish
    handleSwapAndFinishState();
  }

  /**
   * Scan State Logic
   */

  private void handleSwapAndFinishState() {
    // Get Total Scanned
    int scanCount = getScanCount();

    // Handle Finish Event
    if (scanCount == demoBarcodes.size()) {
      // Disable Scanning
      DataWedgeUtilities.disableProfile(this);

      // Show Dialog
      CustomDialog.showCustomDialog(
        this,
        CustomDialog.DialogType.SUCCESS,
        "Finished!",
        "You've scanned all available barcodes, please move to the next station!",
        "FINISH",
        (dialogInterface, i) -> {
          // Dialog Confirmed -> Dismiss, Re-enable Scanner & Reset Demo State
          dialogInterface.dismiss();
          DataWedgeUtilities.enableProfile(this);
          resetDemo();
        });
    }
  }

  private void updateUiState() {
    // Get total
    int scanCount = getScanCount();

    // Update TextView State
    mDataBinding.scanCount.setText(String.format(getString(R.string.scan_progress), scanCount, demoBarcodes.size()));

    // Update ImageViews
    for (Map.Entry<String, Boolean> entry : demoBarcodes.entrySet()) {
      // If Barcode Scanned, Get Key && Update Corresponding ImageView
      if (entry.getValue()) {
        switch (entry.getKey()) {
          case DEMO_01:
            mDataBinding.barcodeTable.rowOneBarcodeOne.setImageResource(R.drawable.ic_success);
            mDataBinding.barcodeTable.rowOneBarcodeOne.setPadding(mDpAsPixels, mDpAsPixels,
                    mDpAsPixels, mDpAsPixels);
            break;
          case DEMO_02:
            mDataBinding.barcodeTable.rowOneBarcodeTwo.setImageResource(R.drawable.ic_success);
            mDataBinding.barcodeTable.rowOneBarcodeTwo.setPadding(mDpAsPixels, mDpAsPixels,
                    mDpAsPixels, mDpAsPixels);
            break;
          case DEMO_03:
            mDataBinding.barcodeTable.rowOneBarcodeThree.setImageResource(R.drawable.ic_success);
            mDataBinding.barcodeTable.rowOneBarcodeThree.setPadding(mDpAsPixels, mDpAsPixels,
                    mDpAsPixels, mDpAsPixels);
            break;
          case DEMO_04:
            mDataBinding.barcodeTable.rowTwoBarcodeOne.setImageResource(R.drawable.ic_success);
            mDataBinding.barcodeTable.rowTwoBarcodeOne.setPadding(mDpAsPixels, mDpAsPixels,
                    mDpAsPixels, mDpAsPixels);
            break;
          case DEMO_05:
            mDataBinding.barcodeTable.rowTwoBarcodeTwo.setImageResource(R.drawable.ic_success);
            mDataBinding.barcodeTable.rowTwoBarcodeTwo.setPadding(mDpAsPixels, mDpAsPixels,
                    mDpAsPixels, mDpAsPixels);
            break;
          case DEMO_06:
            mDataBinding.barcodeTable.rowTwoBarcodeThree.setImageResource(R.drawable.ic_success);
            mDataBinding.barcodeTable.rowTwoBarcodeThree.setPadding(mDpAsPixels, mDpAsPixels,
                    mDpAsPixels, mDpAsPixels);
            break;
        }
      }
    }
  }

  private int getScanCount() {
    int scanCount = 0;
    for (Boolean scanned : demoBarcodes.values()) {
      if (scanned) {
        scanCount++;
      }
    } return scanCount;
  }

  /**
   * Reset Logic
   */

  private void resetDemo() {
    // Loop array and reset all values to false
    for (Map.Entry<String, Boolean> entry : demoBarcodes.entrySet()) {
      entry.setValue(false);
    }

    // Update UI
    mDataBinding.scanCount.setText(String.format(getString(R.string.scan_progress), 0,
        demoBarcodes.size()));

    // Update Images
    mDataBinding.barcodeTable.rowOneBarcodeOne.setImageResource(R.drawable.ic_code_128_demo_01);
    mDataBinding.barcodeTable.rowOneBarcodeTwo.setImageResource(R.drawable.ic_code_128_demo_02);
    mDataBinding.barcodeTable.rowOneBarcodeThree.setImageResource(R.drawable.ic_code_128_demo_03);
    mDataBinding.barcodeTable.rowTwoBarcodeOne.setImageResource(R.drawable.ic_qr_code_demo_04);
    mDataBinding.barcodeTable.rowTwoBarcodeTwo.setImageResource(R.drawable.ic_pdf_417_demo_05);
    mDataBinding.barcodeTable.rowTwoBarcodeThree.setImageResource(R.drawable.ic_data_matrix_demo_06);

    // Apply Custom Paddings
    mDataBinding.barcodeTable.rowOneBarcodeOne.setPadding(getPaddingInDp(10), getPaddingInDp(10),
            getPaddingInDp(10), getPaddingInDp(10));
    mDataBinding.barcodeTable.rowOneBarcodeThree.setPadding(0,0,0,0);
  }

  /**
   * UI Logic
   */

  private int getPaddingInDp(int dp) {
    return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
  }
}