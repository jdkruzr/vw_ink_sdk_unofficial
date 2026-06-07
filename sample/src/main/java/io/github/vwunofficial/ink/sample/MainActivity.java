package io.github.vwunofficial.ink.sample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public final class MainActivity extends Activity {
    private SampleInkView inkView;
    private Button batchButton;
    private Button toolButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inkView = new SampleInkView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(Color.rgb(238, 238, 238));
        toolbar.setPadding(8, 8, 8, 8);

        Button clearButton = new Button(this);
        clearButton.setText("Clear");
        clearButton.setTextSize(12f);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inkView.clearInk();
            }
        });
        toolbar.addView(clearButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button resetButton = new Button(this);
        resetButton.setText("Reset");
        resetButton.setTextSize(12f);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inkView.resetStats();
            }
        });
        toolbar.addView(resetButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        batchButton = new Button(this);
        batchButton.setTextSize(12f);
        updateBatchButton();
        batchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inkView.cycleBatchSize();
                updateBatchButton();
            }
        });
        toolbar.addView(batchButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolButton = new Button(this);
        toolButton.setTextSize(12f);
        updateToolButton();
        toolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inkView.cycleTool();
                updateToolButton();
            }
        });
        toolbar.addView(toolButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(inkView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
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

    private void updateBatchButton() {
        batchButton.setText("Batch " + inkView.renderBatchSize());
    }

    private void updateToolButton() {
        toolButton.setText("Tool " + inkView.toolLabel());
    }
}
