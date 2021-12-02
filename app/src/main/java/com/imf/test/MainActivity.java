package com.imf.test;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.imf.so.assets.load.AssetsSoLoadBy7zFileManager;
import com.imf.so.assets.load.NeedDownloadSoListener;
import com.imf.so.assets.load.bean.SoFileInfo;
import com.imf.testLibrary.NativeTestLibrary;
import com.mainli.blur.BitmapBlur;
import com.mainli.mylibrary.NativeLibTest;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fullScreen();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        mCache = findViewById(R.id.cache_text);
        ImageView image = findViewById(R.id.image);
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image);
        final TextView downloadTextView = findViewById(R.id.download_text);
        updateCacheDir();
        TextView tv = findViewById(R.id.sample_text);
        AssetsSoLoadBy7zFileManager.init(this, new NeedDownloadSoListener() {
            @Override
            public void onNeedDownloadSoInfo(File saveLibsDir, List<SoFileInfo> list) {
                downloadTextView.setText(new StringBuffer().append("需要下载:\n").append(list));
            }
        });
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder stringBuilder = new StringBuilder();
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadSourceLib();
                        stringBuilder.append("源码引入: ").append(NativeSourceTest.stringFromJNI()).append('\n');
                        stringBuilder.append("aar引入: ").append(NativeLibTest.stringFromJNI()).append('\n');
                        stringBuilder.append("子工程引入: ").append(NativeTestLibrary.stringFromJNI()).append('\n');
                        stringBuilder.append("Maven引入: >背景图片变模糊<");
                        tv.setText(stringBuilder.toString());
                        image.setImageBitmap(BitmapBlur.blur(bitmap, 9));
                        updateCacheDir();
                    }
                }, 100);
            }
        });

    }

    private void loadSourceLib() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NativeSourceTest.load();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NativeSourceTest.load();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NativeSourceTest.load();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NativeSourceTest.load();
            }
        }).start();
        NativeSourceTest.load();
    }

    private void updateCacheDir() {
        mCache.setText(new StringBuilder().append("缓存目录情况:\n").append(getDirInfo(getJniLibs())));
    }

    public void onClearSoFile(View view) {
        File saveLibsDir = getJniLibs();
        if (deleteDir(saveLibsDir)) {
            Toast.makeText(this, "成功删除: " + saveLibsDir, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "删除失败: " + saveLibsDir, Toast.LENGTH_SHORT).show();
        }
        updateCacheDir();
    }

    public void onExit(View view) {
        System.exit(0);
    }

    private File getJniLibs() {
        return getDir("jniLibs", 0);
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

    private String getDirInfo(File file) {
        if (file.isDirectory()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(file.getName()).append("(dir):[\n");
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                stringBuilder.append(getDirInfo(child)).append("\n");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            return stringBuilder.insert(0, "\t- ").append(" ]").toString();
        } else {
            return "\t\t* " + file.getName();
        }
    }

    private void fullScreen() {
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                //内容伸展到stateBar下
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //内容伸展到NavigationBar下
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        //分别设置stateBar与NavigationBar背景颜色
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}