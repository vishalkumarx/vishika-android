package com.example.vishika;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class DistanceWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH_DISTANCE = "com.example.vishika.ACTION_REFRESH_DISTANCE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetWithMap(context, appWidgetManager, appWidgetId, "Open app to sync...", null);
        }
    }

    public static void updateAppWidgetWithMap(Context context, AppWidgetManager appWidgetManager,
                                       int appWidgetId, String distanceText, android.graphics.Bitmap mapBitmap) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_distance);
        
        if (distanceText != null && !distanceText.isEmpty()) {
            views.setTextViewText(R.id.widgetDistance, distanceText);
        }
        
        if (mapBitmap != null) {
            views.setImageViewBitmap(R.id.widgetMapImage, mapBitmap);
            views.setViewVisibility(R.id.widgetIcon, android.view.View.GONE);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRefresh, pendingIntent);
        
        views.setOnClickPendingIntent(R.id.widgetMapImage, pendingIntent);
        views.setOnClickPendingIntent(R.id.distanceOverlay, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
