package no.orion.boardscanner;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TOTAL = "TOTAL";
    private static final String SUBTRACT = "SUBTRACT";
    private static final String HISTORY = "HISTORY";
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int EXPECTED_BARCODE_LENGTH = 26;
    private static final int SCANDELAY = 1000; // 1 sec

    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private boolean subtract;
    private ArrayList<String> barcodeHistory;

    private double length;

    private BarcodeCallback callback = new BarcodeCallback() {
        private long lastTimestamp = 0;

        @Override
        public void barcodeResult(BarcodeResult result) {
            String resultText = result.getText();

            Log.d(TAG, "barcodeResult - Scanned: " + resultText);

            if (resultText == null) {
                return;
            }

            if ( System.currentTimeMillis() - lastTimestamp < SCANDELAY) {
                // Wait a sec...
                return;
            }

            beepManager.playBeepSoundAndVibrate();

            StringBuilder statusText = new StringBuilder();

            lastTimestamp = System.currentTimeMillis();

            if (resultText.length() != EXPECTED_BARCODE_LENGTH) {
                // Invalid length
                statusText.append("NOK (Invalid length) : ");
                statusText.append(resultText);
                barcodeView.setStatusText(statusText.toString());
                return;
            }

            // Invalid content
            if (!resultText.matches("^[0-9]+$")) {
                statusText.append("NOK (Invalid content) : ");
                statusText.append(resultText);
                barcodeView.setStatusText(statusText.toString());
                return;
            }

            statusText.append("OK: ");
            statusText.append(resultText);
            barcodeView.setStatusText(statusText.toString());

            // Update history
            if (subtract) {
                // Check if barcode was scanned previously. If not, reject/skip
                if (barcodeHistory.contains(resultText)) {
                    barcodeHistory.remove(resultText);
                }
                else {
                    Context context = getApplicationContext();
                    String message = "The barcode cannot be subtracted, since it was not previously scanned";
                    int duration = Toast.LENGTH_SHORT;

                    Toast.makeText(context, message, duration).show();

                    /*
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("Subtract " + resultText);
                    alert.setMessage("The barcode cannot be subtracted, since it was not previously scanned");
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    alert.show();
                    */
                    return;
                }
            }
            else {
                barcodeHistory.add(resultText);
            }

            String newScan = resultText.substring(20, 26);

            calculate(newScan);

            updateGUI();

            //Added preview of scanned barcode
            ImageView imageView = findViewById(R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    private void setOperandSubtract (boolean value) {
        subtract=value;

        if (value) {
            Log.d(TAG, "Subtraction selected");
        }
        else {
            Log.d(TAG, "Addition selected");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initialize history
        barcodeHistory = new ArrayList<>();

        if (savedInstanceState != null) {
            length = savedInstanceState.getDouble(TOTAL);
            subtract = savedInstanceState.getBoolean(SUBTRACT, false);
            barcodeHistory = savedInstanceState.getStringArrayList(HISTORY);
            printVariables("onCreate", "Recovered");
        }
        else {
            Log.d(TAG, "onCreate. State is null");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateGUI();

        barcodeView = findViewById(R.id.barcode_scanner);
        Collection<BarcodeFormat> formats = Collections.singletonList(BarcodeFormat.CODE_128);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(this);
        if (!beepManager.isVibrateEnabled()) {
            beepManager.setVibrateEnabled(true);
        }
        if (!beepManager.isBeepEnabled()) {
            beepManager.setBeepEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        /*
         Need to check permission at runtime from Android version 6.0
         https://developer.android.com/training/permissions/requesting.html
        */
        checkPermissions();

        super.onResume();
        barcodeView.resume();

        printVariables("onResume", "Current");

        retrievePersisted();

        printVariables("onResume", "Retrieved");

        updateGUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();

        printVariables("onPause", "Saving");

        persistData();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(TOTAL, length);
        outState.putBoolean(SUBTRACT, subtract);
        outState.putStringArrayList(HISTORY, barcodeHistory);
        printVariables("onSaveInstanceState", "Saving");
        // persistData();
    }

    @Override
    protected void onStop() {
        printVariables("onStop");
        super.onStop();
    }

    private void printVariables(String method) {
        printVariables(method,"");
    }

    private void printVariables(String method, String prefix) {
        String varString = String.format(Locale.US, " LENGTH = %.2f | OPERAND '-' = %b | HISTORY = %s (%d items)", length, subtract, Arrays.toString(barcodeHistory.toArray()), barcodeHistory.size());
        Log.d(TAG, method + " - " + prefix + varString);
    }
    /**
     * Make sure the application can use it's resources, using a runtime check of permissions
     */
    private void checkPermissions() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkPermissions - CAMERA permission already granted");
            return;
        }

        Log.d(TAG, "checkPermissions - CAMERA permission not granted");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CAMERA);
    }

    /**
     * Retrieve persisted data from shared preferences
     */
    private void retrievePersisted() {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        length = getDouble(sharedPreferences, TOTAL, 0.00);
        subtract = sharedPreferences.getBoolean(SUBTRACT, false);
        barcodeHistory = getHistory(sharedPreferences, HISTORY);

        printVariables("retrievePersisted", "Retrieved");
    }

    /**
     * Persist data in shared preferences
     */
    private void persistData() {
        printVariables("persistData", "Persisting");

        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        putDouble(editor, TOTAL, length);
        editor.putBoolean(SUBTRACT, subtract);
        putHistory(editor, HISTORY, barcodeHistory);

        editor.apply();
    }

    private void calculate (String value) {
        BigDecimal bdLength;
        if (!Double.isNaN(length)) {
            bdLength = new BigDecimal(Double.toString(length));
        }
        else {
            bdLength = new BigDecimal("0.00");
        }

        BigDecimal bdValue = new BigDecimal(value);

        bdValue = bdValue.multiply(new BigDecimal("0.01"));

        Log.d(TAG, String.format("Current length (BD): %s, value (inParam): %s, value (BD): %s", bdLength.toString(), value, bdValue.toString()));

        if (bdLength.compareTo(new BigDecimal("0.00")) != 0) {
            if (subtract) {
                if (bdValue.compareTo(bdLength) <= 0 ) {
                    bdLength = bdLength.subtract(bdValue);
                }
                else {
                    Log.e(TAG, String.format("Incorrect value (tried subtracting %s from %s)", bdValue.toString(), bdLength.toString()));
                }
            }
            else {
                bdLength = bdLength.add(bdValue);
            }
        }
        else {
            bdLength = bdValue;
        }

        length = bdLength.doubleValue();
    }

    /**
     * Update the GUI
     */
    private void updateGUI() {
        // Display the correct SUM
        String displayText;
        if (Double.isNaN(length)) {
            displayText="0.00";
        }
        else {
            displayText = String.format(Locale.US, "%.2f", length);
        }

        TextView total = findViewById(R.id.sum);
        total.setText(displayText);

        // Display correct operand selection in the view
        if (subtract) {
            RadioButton button = findViewById(R.id.operandsubtract);
            button.setChecked(true);
        }
        else {
            RadioButton button = findViewById(R.id.operandadd);
            button.setChecked(true);
        }
    }

    /**
     * There is no method to store doubles in a SharedPreferences editor. Storing as long
     *
     * @param editor The SharedPreferences.Editor to use
     * @param key The key to use
     * @param value The double value to store in the SharedPreferences.Editor
     */
    private void putDouble(final SharedPreferences.Editor editor, final String key, final double value) {
        editor.putLong(key, Double.doubleToRawLongBits(value));
    }

    /**
     * There is no method to store an arrayList in a SharedPreferences editor. Create one string and store it
     *
     * @param editor The SharedPreferences.Editor to use
     * @param key The key to use
     * @param value The ArrayList to store in the SharedPreferences.Editor
     */
    private void putHistory(final SharedPreferences.Editor editor, final String key, final ArrayList<String> value) {
        // Create one string containing the history
        StringBuilder prefHistoryString = new StringBuilder();
        for (String item : value) {
            prefHistoryString.append(item);
            prefHistoryString.append("|");
        }
        editor.putString(key, prefHistoryString.toString());
    }

    /**
     * Create the history from SharedPreferences (String to ArrayList)
     *
     * @param preferences The SharedPreferences
     * @param key The key storing the history
     * @return ArrayList containing the history items
     */
    private ArrayList<String> getHistory(final SharedPreferences preferences, final String key) {
        // Missing key -> return empty list
        if (!preferences.contains(key)) {
            return new ArrayList<>();
        }

        // Empty string -> return empty list
        String prefHistoryString = preferences.getString(key, "");
        if (prefHistoryString.isEmpty()) {
            return new ArrayList<>();
        }

        String[] prefHistoryItems = prefHistoryString.split("\\|");

        return new ArrayList<>(Arrays.asList(prefHistoryItems));
    }

    /**
     * Fetch the double value. The method fetches a long value and returns a double.
     *
     * @param preferences The SharedPreferences
     * @param key The key containing the double
     * @param defaultValue The default value to use in case no key is found
     * @return The double representation of the stored long
     */
    private double getDouble (final SharedPreferences preferences, final String key, final double defaultValue) {
        if ( !preferences.contains(key) ) {
            return defaultValue;
        }
        return Double.longBitsToDouble(preferences.getLong(key,0));
    }

    /**
     * Clear button implementation
     *
     * @param v The view calling the method
     */
    public void clear(View v) {
        Log.d(TAG, String.format("clear() - length = %f", length));
        if (Double.isNaN(length) || Double.compare(0.00, length) == 0) {
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Clear");
        alert.setMessage("Are you sure you want to clear the current result?");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                length = Double.NaN;
                updateGUI();
                barcodeHistory.clear();
                dialogInterface.dismiss();
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alert.show();
    }

    public void triggerScan(View v) {
        barcodeView.decodeSingle(callback);
    }

    /* TODO fix this later
      public void showHistory(View v) {
      Intent intent = new Intent(this, HistoryActivity.class);
      startActivity(intent);
      }
    */

    /**
     * Radio button listener/method
     *
     * @param v The view calling the method
     */
    public void onOperandChanged(View v) {
        boolean selected = ((RadioButton) v).isChecked();

        switch (v.getId()) {
            case R.id.operandadd:
                if (selected)
                    setOperandSubtract(false);
                break;
            case R.id.operandsubtract:
                if(selected)
                    setOperandSubtract(true);
                break;
        }
    }
}
