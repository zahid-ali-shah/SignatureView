package com.kyanogen.signature;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.kyanogen.signatureview.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Signature extends AppCompatActivity {

    private SignatureView signatureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);//see xml layout
        signatureView = findViewById(R.id.signature_view);

        int colorPrimary = ContextCompat.getColor(this, R.color.colorAccent);
        signatureView.setPenColor(colorPrimary);
        // or like signatureView.setPenColor(Color.RED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_signature, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                InfoDialog();
                return true;
            case R.id.action_clear:
                signatureView.clearCanvas();//Clear SignatureView
                Toast.makeText(getApplicationContext(),
                        "Clear canvas", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_download:
                File directory = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File file = new File(directory, System.currentTimeMillis() + ".png");
                FileOutputStream out = null;
                Bitmap bitmap = signatureView.getSignatureBitmap();
                try {
                    out = new FileOutputStream(file);
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } else {
                        throw new FileNotFoundException();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.flush();
                            out.close();

                            if (bitmap != null) {
                                Toast.makeText(getApplicationContext(),
                                        "Image saved successfully at " + file.getPath(),
                                        Toast.LENGTH_LONG).show();
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                                    new MyMediaScanner(this, file);
                                } else {
                                    ArrayList<String> toBeScanned = new ArrayList<String>();
                                    toBeScanned.add(file.getAbsolutePath());
                                    String[] toBeScannedStr = new String[toBeScanned.size()];
                                    toBeScannedStr = toBeScanned.toArray(toBeScannedStr);
                                    MediaScannerConnection.scanFile(this, toBeScannedStr, null,
                                            null);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            case R.id.action_has_signature:
                Toast.makeText(getApplicationContext(),
                        "SignatureView is empty: " + signatureView.isBitmapEmpty(),
                        Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class MyMediaScanner implements
            MediaScannerConnection.MediaScannerConnectionClient {

        private MediaScannerConnection mSC;
        private File file;

        MyMediaScanner(Context context, File file) {
            this.file = file;
            mSC = new MediaScannerConnection(context, this);
            mSC.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            mSC.scanFile(file.getAbsolutePath(), null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mSC.disconnect();
        }
    }

    public void InfoDialog() {
        String infoMessage = "App version : " + BuildConfig.VERSION_NAME;
        infoMessage = infoMessage + "\n\n" + "SignatureView library version : " +
                com.kyanogen.signatureview.BuildConfig.VERSION_NAME;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.vInfo)
                .setMessage(infoMessage)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }
}