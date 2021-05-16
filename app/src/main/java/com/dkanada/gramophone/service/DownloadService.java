package com.dkanada.gramophone.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.dkanada.gramophone.App;
import com.dkanada.gramophone.BuildConfig;
import com.dkanada.gramophone.database.Cache;
import com.dkanada.gramophone.model.Song;
import com.dkanada.gramophone.service.notifications.DownloadNotification;
import com.dkanada.gramophone.util.MusicUtil;
import com.dkanada.gramophone.util.PreferenceUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DownloadService extends Service {
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String EXTRA_SONG = PACKAGE_NAME + ".extra.song";

    private Executor executor;
    private Handler handler;
    private DownloadNotification notification;

    @Override
    public void onCreate() {
        super.onCreate();

        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }

        executor = Executors.newFixedThreadPool(4);
        handler = new Handler(looper);
        notification = new DownloadNotification(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return super.onStartCommand(null, flags, startId);
        Song song = intent.getParcelableExtra(EXTRA_SONG);

        executor.execute(() -> {
            try {
                URL url = new URL(MusicUtil.getDownloadUri(song));
                URLConnection connection = url.openConnection();

                String cache = PreferenceUtil.getInstance(App.getInstance()).getLocationCache();
                File download = new File(cache, "download/" + song.id);
                File audio = new File(MusicUtil.getFileUri(song));

                download.getParentFile().mkdirs();
                download.createNewFile();
                audio.getParentFile().mkdirs();
                audio.createNewFile();

                InputStream input = connection.getInputStream();
                OutputStream output = new FileOutputStream(download);

                connection.connect();

                byte[] data = new byte[1048576];
                int count;

                notification.start(song, connection.getContentLength());
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                    notification.update(count);
                }

                input.close();
                output.close();

                input = new FileInputStream(download);
                output = new FileOutputStream(audio);

                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                input.close();
                output.close();

                download.delete();
                App.getDatabase().cacheDao().insertCache(new Cache(song));
                notification.stop(song);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}