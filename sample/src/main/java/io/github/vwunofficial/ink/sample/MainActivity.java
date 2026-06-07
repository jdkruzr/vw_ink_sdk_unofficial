package io.github.vwunofficial.ink.sample;

import android.app.Activity;
import android.os.Bundle;

public final class MainActivity extends Activity {
    private SampleInkView inkView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inkView = new SampleInkView(this);
        setContentView(inkView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        inkView.post(new Runnable() {
            @Override
            public void run() {
                inkView.startViwoodsInk();
            }
        });
    }

    @Override
    protected void onPause() {
        inkView.stopViwoodsInk();
        super.onPause();
    }
}
