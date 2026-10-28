package top.niunaijun.blackbox.fake.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.OutputStream;

public class WhiteScreenCameraActivity extends Activity {
    private static final String TAG = "WhiteScreenCameraAct";

    public static void show(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, WhiteScreenCameraActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting WhiteScreenCameraActivity", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.WHITE);

        
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCameraResultAndFinish();
            }
        });

        setContentView(layout);
        Log.d(TAG, "WhiteScreenCameraActivity created - showing full white screen");
    }

    @Override
    public void onBackPressed() {
        handleCameraResultAndFinish();
    }

    private void handleCameraResultAndFinish() {
        Intent intent = getIntent();
        if (intent != null) {
            Uri outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            Intent resultIntent = new Intent();

            Bitmap whiteBitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888);
            whiteBitmap.eraseColor(Color.WHITE);

            if (outputUri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(outputUri)) {
                    if (os != null) {
                        whiteBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                        os.flush();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error writing white image to output URI: " + outputUri, e);
                }
                resultIntent.setData(outputUri);
            } else {
                
                resultIntent.putExtra("data", whiteBitmap);
            }

            setResult(RESULT_OK, resultIntent);
        }
        finish();
    }
}
