package com.example.vishika;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.transition.Transition;

import androidx.annotation.Nullable;

public class LocketWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id, null, null);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager awm,
                                    int appWidgetId, String myUrl, String partnerUrl) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_locket);

        // Tapping widget opens the app
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetLocketTitle, pi);
        views.setOnClickPendingIntent(R.id.widgetMyLocket, pi);
        views.setOnClickPendingIntent(R.id.widgetPartnerLocket, pi);

        // Load my photo
        if (myUrl != null && !myUrl.isEmpty()) {
            AppWidgetTarget awt = new AppWidgetTarget(context, R.id.widgetMyLocket, views, appWidgetId) {
                @Override
                public void onResourceReady(Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    super.onResourceReady(resource, transition);
                }
            };
            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(myUrl)
                    .centerCrop()
                    .into(awt);
        }

        // Load partner photo
        if (partnerUrl != null && !partnerUrl.isEmpty()) {
            AppWidgetTarget awt2 = new AppWidgetTarget(context, R.id.widgetPartnerLocket, views, appWidgetId) {
                @Override
                public void onResourceReady(Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    super.onResourceReady(resource, transition);
                }
            };
            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(partnerUrl)
                    .centerCrop()
                    .into(awt2);
        }

        awm.updateAppWidget(appWidgetId, views);
    }
}
