package com.example.vishika;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class NeumorphicContainerLayout extends FrameLayout {
    private Paint paintMain;
    private Paint paintInnerShadowDark;
    private Paint paintInnerShadowLight;
    
    public NeumorphicContainerLayout(Context context) {
        super(context);
        init();
    }
    
    public NeumorphicContainerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        
        paintMain = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMain.setColor(Color.parseColor("#e7e7e7"));
        
        paintInnerShadowDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintInnerShadowDark.setStyle(Paint.Style.STROKE);
        paintInnerShadowDark.setStrokeWidth(12f);
        paintInnerShadowDark.setColor(Color.parseColor("#c5c5c5"));
        paintInnerShadowDark.setShadowLayer(12f, 6f, 6f, Color.parseColor("#c5c5c5"));
        
        paintInnerShadowLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintInnerShadowLight.setStyle(Paint.Style.STROKE);
        paintInnerShadowLight.setStrokeWidth(12f);
        paintInnerShadowLight.setColor(Color.parseColor("#ffffff"));
        paintInnerShadowLight.setShadowLayer(12f, -6f, -6f, Color.parseColor("#ffffff"));
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        
        RectF rect = new RectF(0, 0, w, h);
        float radius = 40f * getResources().getDisplayMetrics().density; // 40dp
        
        Path path = new Path();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        
        canvas.drawPath(path, paintMain);
        
        canvas.save();
        canvas.clipPath(path);
        
        canvas.drawPath(path, paintInnerShadowDark);
        canvas.drawPath(path, paintInnerShadowLight);
        
        canvas.restore();
        
        super.onDraw(canvas);
    }
}
