package com.imf.test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.imf.so.assets.load.AssetsSoLoadBy7zFileManager;
import com.imf.testLibrary.NativeTestLibrary;
import com.mainli.mylibrary.NativeLibTest;

import java.io.File;

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

    public void onClearSoFile(View view) {
        File saveLibsDir = getDir("jniLibs", 0);
        if (deleteDir(saveLibsDir)) {
            Toast.makeText(this, "成功删除: " + saveLibsDir, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "删除失败: " + saveLibsDir, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     */
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
}