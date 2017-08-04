package com.kyanogen.signature;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class Splash extends Activity {

    private static final int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Context mContext = this;

        ImageView logoImg = findViewById(R.id.company_logo);
        TextView textView = findViewById(R.id.splash_text);

        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(),
                "Exo2-Light.otf");
        textView.setTypeface(typeface);

        AlphaAnimation animation = new AlphaAnimation(0.2f, 1.0f);
        animation.setDuration(SPLASH_TIME_OUT);
        animation.setStartOffset(100);
        animation.setFillAfter(true);
        logoImg.startAnimation(animation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Splash.this, Signature.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    public void onBackPressed() {
    }
}