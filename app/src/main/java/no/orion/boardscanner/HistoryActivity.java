package no.orion.boardscanner;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class HistoryActivity extends Activity {

    private static final String HISTORY = "HISTORY";
    private static final String TAG = HistoryActivity.class.getSimpleName();
    private ArrayList<String> history;

    private RecyclerView historyView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        history = new ArrayList<>();
        retrieveHistory();

        historyView = (RecyclerView) findViewById(R.id.history_view);
        layoutManager = new LinearLayoutManager(this);
        historyView.setLayoutManager(layoutManager);
        adapter = new HistoryViewAdapter(history);
        historyView.setAdapter(adapter);

    }

    private void retrieveHistory() {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        Set<String> set = sharedPreferences.getStringSet(HISTORY, null);
        if ( set != null ) {
            Log.d(TAG, String.format("onResume. Recovered HISTORY = %d no of items", set.size()));
            history.addAll(set);
        }
        else {
            history.clear();
        }
        Log.d(TAG, String.format("retrieveHistory. Retrieving HISTORY = %s", history.toString()));
    }
/*
    private void persistData() {
        Log.d(TAG, String.format("persistData. Persisting LENGTH = %.2f and HISTORY = %s", length, history.toString()));
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        putDouble(editor, TOTAL, length);

        Set<String> set = new LinkedHashSet<>();
        set.addAll(history);

        editor.putStringSet(HISTORY,set);
        editor.apply();
    }
*/
}
