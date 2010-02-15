
package com.wissen.race;

import com.wissen.race.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

/**
 * Race: a simple game that everyone can enjoy.
 * 
 * @author wissen16(Mayur Birari)
 * 
 */
public class Race extends Activity {

    private RaceView mRaceView;
    
    private static String ICICLE_KEY = "race-view";

    /**
     * Called when Activity is first created. Turns off the title bar, sets up
     * the content views, and fires up the RaceView.
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No Title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.race_layout);

        mRaceView = (RaceView) findViewById(R.id.race);
        mRaceView.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // We were just launched -- set up a new game
            mRaceView.setMode(RaceView.READY);
        } else {
            // We are being restored
            Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
            if (map != null) {
                mRaceView.restoreState(map);
            } else {
                mRaceView.setMode(RaceView.PAUSE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        mRaceView.setMode(RaceView.PAUSE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Store the game state
        outState.putBundle(ICICLE_KEY, mRaceView.saveState());
    }

}
