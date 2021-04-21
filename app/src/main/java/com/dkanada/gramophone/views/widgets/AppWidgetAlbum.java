package com.dkanada.gramophone.views.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.dkanada.gramophone.R;
import com.dkanada.gramophone.glide.CustomGlideRequest;
import com.dkanada.gramophone.model.Song;
import com.dkanada.gramophone.service.MusicService;
import com.dkanada.gramophone.activities.MainActivity;
import com.dkanada.gramophone.util.ImageUtil;
import com.dkanada.gramophone.util.Util;

public class AppWidgetAlbum extends BaseAppWidget {
    public static final String NAME = "app_widget_album";

    private static AppWidgetAlbum mInstance;
    private Target<Bitmap> target;

    public static synchronized AppWidgetAlbum getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetAlbum();
        }

        return mInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Point p = Util.getScreenSize(context);

        imageSize = Math.min(p.x, p.y);
        cardRadius = context.getResources().getDimension(R.dimen.app_widget_card_radius);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    protected void defaultAppWidget(final Context context, final int[] appWidgetIds) {
        final RemoteViews appWidgetView = new RemoteViews(context.getPackageName(), R.layout.app_widget_album);

        appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        appWidgetView.setImageViewBitmap(R.id.image, createRoundedBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.default_album_art), imageSize, imageSize, cardRadius, cardRadius, cardRadius, cardRadius));
        appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, MaterialValueHelper.getPrimaryTextColor(context, false))));
        appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, MaterialValueHelper.getPrimaryTextColor(context, false))));
        appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, MaterialValueHelper.getPrimaryTextColor(context, false))));

        linkButtons(context, appWidgetView);
        pushUpdate(context, appWidgetIds, appWidgetView);
    }

    public void performUpdate(final MusicService service, final int[] appWidgetIds) {
        final RemoteViews appWidgetView = new RemoteViews(service.getPackageName(), R.layout.app_widget_album);

        final boolean isPlaying = service.isPlaying();
        final Song song = service.getCurrentSong();

        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE);
            appWidgetView.setTextViewText(R.id.title, song.title);
            appWidgetView.setTextViewText(R.id.text, getSongArtistAndAlbum(song));
        }

        int playPauseRes = isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
        appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, playPauseRes, MaterialValueHelper.getPrimaryTextColor(service, false))));

        appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_next_white_24dp, MaterialValueHelper.getPrimaryTextColor(service, false))));
        appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_previous_white_24dp, MaterialValueHelper.getPrimaryTextColor(service, false))));

        linkButtons(service, appWidgetView);

        Point p = Util.getScreenSize(service);
        imageSize = Math.min(p.x, p.y);
        cardRadius = service.getResources().getDimension(R.dimen.app_widget_card_radius);

        service.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (target != null) {
                    Glide.with(service).clear(target);
                }

                target = CustomGlideRequest.Builder
                        .from(service, song.primary, song.blurHash)
                        .bitmap().build()
                        .into(new CustomTarget<Bitmap>(imageSize, imageSize) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> glideAnimation) {
                                update(resource);
                            }

                            @Override
                            public void onLoadFailed(Drawable drawable) {
                                super.onLoadFailed(drawable);
                                update(null);
                            }

                            @Override
                            public void onLoadCleared(Drawable drawable) {
                                super.onLoadFailed(drawable);
                                update(null);
                            }

                            private void update(@Nullable Bitmap bitmap) {
                                final Drawable image = getAlbumArtDrawable(service.getResources(), bitmap);
                                final Bitmap roundedBitmap = createRoundedBitmap(image, imageSize, imageSize, cardRadius, cardRadius, cardRadius, cardRadius);

                                appWidgetView.setImageViewBitmap(R.id.image, roundedBitmap);
                                pushUpdate(service, appWidgetIds, appWidgetView);
                            }
                        });
            }
        });
    }

    private void linkButtons(final Context context, final RemoteViews views) {
        Intent action;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, MusicService.class);

        action = new Intent(context, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(context, 0, action, 0);
        views.setOnClickPendingIntent(R.id.clickable_area, pendingIntent);

        pendingIntent = buildPendingIntent(context, MusicService.ACTION_REWIND, serviceName);
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent);

        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE, serviceName);
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent);

        pendingIntent = buildPendingIntent(context, MusicService.ACTION_SKIP, serviceName);
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent);
    }
}