package com.imf.test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.imf.so.assets.load.AssetsSoLoadBy7zFileManager;
import com.imf.testLibrary.NativeTestLibrary;
import com.mainli.mylibrary.NativeLibTest;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.sample_text);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AssetsSoLoadBy7zFileManager.init(v.getContext());
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("源码引入:").append(NativeSourceTest.stringFromJNI()).append('\n');
                        stringBuilder.append("aar引入:").append(NativeLibTest.stringFromJNI()).append('\n');
                        stringBuilder.append("子工程引入:").append(NativeTestLibrary.stringFromJNI());
                        tv.setText(stringBuilder.toString());
                    }
                }, 100);
            }
        });

    }
}