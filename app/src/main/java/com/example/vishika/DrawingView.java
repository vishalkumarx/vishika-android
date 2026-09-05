package com.example.vishika;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.DashPathEffect;
import android.view.ScaleGestureDetector;
import android.graphics.Matrix;
import android.graphics.RectF;
import java.util.UUID;


import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawingView extends View {

    public static class DrawObject {
        public String type = "path";
    public String text = "";
        public String imageUrl = null;
        public transient android.graphics.Bitmap imageBitmap = null;
        public transient com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap> glideTarget = null;
        public float x = 0;
        public float y = 0;
        public float scale = 1.0f;
        public float rotation = 0.0f;
        public Path path;
        public Paint paint;
        public String firebaseKey;
        public int currentColor;

        public DrawObject(int color, float width, String key) {
            this.path = new Path();
            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setStrokeWidth(width);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setAntiAlias(true);
            this.firebaseKey = key;
            this.currentColor = color;
        }
    }

    private List<DrawObject> myStrokes = new ArrayList<>();
    private List<DrawObject> partnerStrokes = new ArrayList<>();
    
    private DrawObject currentLocalStroke;
    private String currentStrokeKey;
    public String currentTool = "path";
    public String currentImageUrl = null;
    
    private int currentColor = Color.parseColor("#FF4081");
    private float currentWidth = 12f;

    private DatabaseReference myStrokesRef;
    private DatabaseReference partnerStrokesRef;
    
    private boolean isSetup = false;

    private ScaleGestureDetector scaleDetector;
    private DrawObject activeObject = null;
    private float lastTouchX, lastTouchY;
    private float lastRotationAngle = 0;
    private boolean isResizing = false;
    
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setupScaleDetector(context);
    }
    
    private void setupScaleDetector(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (activeObject != null) {
                    activeObject.scale *= detector.getScaleFactor();
                    // Limit scale
                    activeObject.scale = Math.max(0.1f, Math.min(activeObject.scale, 10.0f));
                    invalidate();
                    return true;
                }
                return false;
            }
        });
    }



    public void setCurrentColor(int color) {
        this.currentColor = color;
    }

    public void setCurrentWidth(float width) {
        this.currentWidth = width;
    }

    public void undo() {
        if (!isSetup || myStrokes.isEmpty()) return;
        DrawObject lastStroke = myStrokes.remove(myStrokes.size() - 1);
        if (lastStroke.firebaseKey != null) {
            myStrokesRef.child(lastStroke.firebaseKey).removeValue();
        }
        invalidate();
    }

    public void setup(DatabaseReference db, String myId, String partnerId) {
        myStrokesRef = db.child("drawings").child(myId).child("strokes");
        partnerStrokesRef = db.child("drawings").child(partnerId).child("strokes");
        isSetup = true;
        
        // Load my past strokes
        myStrokesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myStrokes.clear();
                for (DataSnapshot strokeSnap : snapshot.getChildren()) {
                    loadStrokeFromSnapshot(strokeSnap, myStrokes);
                }
                invalidate();
                
                // Now that we loaded existing, we can optionally listen for remote undoes (e.g. from another session)
                myStrokesRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {} // Ignore, we add locally
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        String key = snapshot.getKey();
                        myStrokes.removeIf(s -> s.firebaseKey != null && s.firebaseKey.equals(key));
                        invalidate();
                    }
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        // Listen for partner strokes
        partnerStrokesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                // If it already exists in the list (e.g. due to rapid loading), skip
                for (DrawObject s : partnerStrokes) {
                    if (s.firebaseKey != null && s.firebaseKey.equals(snapshot.getKey())) return;
                }
                
                String colorStr = snapshot.child("color").getValue(String.class);
                Float width = snapshot.child("width").getValue(Float.class);
                String type = snapshot.child("type").getValue(String.class);
                
                int color = Color.parseColor("#7C4DFF"); // default purple
                if (colorStr != null) {
                    try { color = Color.parseColor(colorStr); } catch (Exception ignored) {}
                }
                if (width == null) width = 12f;
                if (type == null) type = "path";

                final DrawObject stroke = new DrawObject(color, width, snapshot.getKey());
                stroke.type = type;
                
                float screenWidth = getResources().getDisplayMetrics().widthPixels;
                float screenHeight = getResources().getDisplayMetrics().heightPixels;
                

                if ("text".equals(type) || "sticker".equals(type) || "image".equals(type)) {
                    stroke.text = snapshot.child("text").getValue(String.class);
                    stroke.imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    Double rx = getSafeDouble(snapshot, "x");
                    Double ry = getSafeDouble(snapshot, "y");
                    Double scale = getSafeDouble(snapshot, "scale");
                    Double rotation = getSafeDouble(snapshot, "rotation");
                    
                    if (rx != null && ry != null) {
                        float screenX = rx.floatValue() > 2.0f ? rx.floatValue() : rx.floatValue() * screenWidth;
                        float screenY = ry.floatValue() > 2.0f ? ry.floatValue() : ry.floatValue() * screenHeight;
                        stroke.x = screenX;
                        stroke.y = screenY;
                    }
                    if (scale != null) stroke.scale = scale.floatValue();
                    if (rotation != null) stroke.rotation = rotation.floatValue();
                }

                partnerStrokes.add(stroke);
                invalidate();
                
                // Wake screen for 10 seconds so the partner can see the drawing on lock screen
                try {
                    android.os.PowerManager pm = (android.os.PowerManager) getContext().getSystemService(android.content.Context.POWER_SERVICE);
                    if (pm != null) {
                        android.os.PowerManager.WakeLock wakeLock = pm.newWakeLock(
                                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                "Vishika:DrawWakeLock"
                        );
                        wakeLock.acquire(10000); 
                    }
                } catch (Exception ignored) {}

                if ("path".equals(type)) {
                    // Listen to points added to this stroke
                    snapshot.getRef().child("points").addChildEventListener(new ChildEventListener() {
                        boolean isFirst = true;
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot ptSnap, String prevChild) {
                            Double rx = getSafeDouble(ptSnap, "x");
                            Double ry = getSafeDouble(ptSnap, "y");
                            if (rx != null && ry != null) {
                                float screenW = getResources().getDisplayMetrics().widthPixels;
                                float screenH = getResources().getDisplayMetrics().heightPixels;
                                
                                // If it's old absolute data (e.g. > 2.0), just use it directly, otherwise treat as ratio
                                float screenX = rx.floatValue() > 2.0f ? rx.floatValue() : rx.floatValue() * screenW;
                                float screenY = ry.floatValue() > 2.0f ? ry.floatValue() : ry.floatValue() * screenH;
                                
                                float localX = screenX;
                                float localY = screenY;
                                if (isFirst) {
                                    stroke.path.moveTo(localX, localY);
                                    isFirst = false;
                                } else {
                                    stroke.path.lineTo(localX, localY);
                                }
                                invalidate();
                            }
                        }
                        @Override
                        public void onChildChanged(@NonNull DataSnapshot s, String p) {}
                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot s) {}
                        @Override
                        public void onChildMoved(@NonNull DataSnapshot s, String p) {}
                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                partnerStrokes.removeIf(s -> s.firebaseKey != null && s.firebaseKey.equals(key));
                invalidate();
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void loadStrokeFromSnapshot(DataSnapshot strokeSnap, List<DrawObject> listToAddTo) {
        String colorStr = strokeSnap.child("color").getValue(String.class);
        Float width = strokeSnap.child("width").getValue(Float.class);
        String type = strokeSnap.child("type").getValue(String.class);
        
        int color = Color.parseColor("#FF4081");
        if (colorStr != null) {
            try { color = Color.parseColor(colorStr); } catch (Exception ignored) {}
        }
        if (width == null) width = 12f;
        if (type == null) type = "path";

        DrawObject stroke = new DrawObject(color, width, strokeSnap.getKey());
        stroke.type = type;
        
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float screenHeight = getResources().getDisplayMetrics().heightPixels;
        

        if ("text".equals(type) || "sticker".equals(type) || "image".equals(type)) {
            stroke.text = strokeSnap.child("text").getValue(String.class);
            stroke.imageUrl = strokeSnap.child("imageUrl").getValue(String.class);
            Double rx = getSafeDouble(strokeSnap, "x");
            Double ry = getSafeDouble(strokeSnap, "y");
            Double scale = getSafeDouble(strokeSnap, "scale");
            Double rotation = getSafeDouble(strokeSnap, "rotation");
            
            if (rx != null && ry != null) {
                float screenX = rx.floatValue() > 2.0f ? rx.floatValue() : rx.floatValue() * screenWidth;
                float screenY = ry.floatValue() > 2.0f ? ry.floatValue() : ry.floatValue() * screenHeight;
                stroke.x = screenX;
                stroke.y = screenY;
            }
            if (scale != null) stroke.scale = scale.floatValue();
            if (rotation != null) stroke.rotation = rotation.floatValue();
            
        } else {
            boolean isFirst = true;
            for (DataSnapshot ptSnap : strokeSnap.child("points").getChildren()) {
                Double rx = getSafeDouble(ptSnap, "x");
                Double ry = getSafeDouble(ptSnap, "y");
                if (rx != null && ry != null) {
                    float screenX = rx.floatValue() > 2.0f ? rx.floatValue() : rx.floatValue() * screenWidth;
                    float screenY = ry.floatValue() > 2.0f ? ry.floatValue() : ry.floatValue() * screenHeight;
                    
                    float localX = screenX;
                    float localY = screenY;
    
                    if (isFirst) {
                        stroke.path.moveTo(localX, localY);
                        isFirst = false;
                    } else {
                        stroke.path.lineTo(localX, localY);
                    }
                }
            }
        }
        listToAddTo.add(stroke);
    }

    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawObjects(canvas, myStrokes);
        drawObjects(canvas, partnerStrokes);
        
        if (activeObject != null) {
            List<DrawObject> activeList = new ArrayList<>();
            activeList.add(activeObject);
            drawObjects(canvas, activeList);
            
            // Draw bounding box
            Paint boxPaint = new Paint();
            boxPaint.setColor(Color.LTGRAY);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(3);
            boxPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
            
            canvas.save();
            canvas.translate(activeObject.x, activeObject.y);
            canvas.rotate(activeObject.rotation);
            canvas.scale(activeObject.scale, activeObject.scale);
            
            float halfWidth = 150f;
            float halfHeight = 150f;
            if ("image".equals(activeObject.type)) {
                if (activeObject.imageBitmap != null) {
                    halfWidth = activeObject.imageBitmap.getWidth() / 2f + 20;
                    halfHeight = activeObject.imageBitmap.getHeight() / 2f + 20;
                }
            } else {
                float textSize = "sticker".equals(activeObject.type) ? 120f : 80f;
                Paint textPaint = new Paint(activeObject.paint);
                textPaint.setTextSize(textSize);
                if (activeObject.text != null && !activeObject.text.isEmpty()) {
                    halfWidth = (textPaint.measureText(activeObject.text) / 2) + 40;
                    halfHeight = (textPaint.descent() - textPaint.ascent()) / 2 + 40;
                } else {
                    halfWidth = Math.max(150f, textSize);
                    halfHeight = Math.max(100f, textSize);
                }
            }
            
            canvas.drawRect(-halfWidth, -halfHeight, halfWidth, halfHeight, boxPaint);
            
            // Draw close button top left
            Paint btnPaint = new Paint();
            btnPaint.setColor(Color.RED);
            btnPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(-halfWidth, -halfHeight, 30, btnPaint);
            btnPaint.setColor(Color.WHITE);
            btnPaint.setStrokeWidth(5);
            canvas.drawLine(-halfWidth - 10, -halfHeight - 10, -halfWidth + 10, -halfHeight + 10, btnPaint);
            canvas.drawLine(-halfWidth + 10, -halfHeight - 10, -halfWidth - 10, -halfHeight + 10, btnPaint);
            
            // Draw resize button bottom right
            btnPaint.setColor(Color.parseColor("#2196F3"));
            btnPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(halfWidth, halfHeight, 30, btnPaint);
            btnPaint.setColor(Color.WHITE);
            btnPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(halfWidth, halfHeight, 12, btnPaint);
            
            canvas.restore();
        }
    }

    private void drawObjects(Canvas canvas, List<DrawObject> objects) {
        for (DrawObject s : objects) {
            if ("image".equals(s.type)) {
                canvas.save();
                canvas.translate(s.x, s.y);
                canvas.rotate(s.rotation);
                canvas.scale(s.scale, s.scale);
                
                if (s.imageBitmap != null) {
                    canvas.drawBitmap(s.imageBitmap, -s.imageBitmap.getWidth() / 2f, -s.imageBitmap.getHeight() / 2f, null);
                } else if (s.imageUrl != null && !s.imageUrl.isEmpty()) {
                    if (s.text == null || (!s.text.equals("loading") && !s.text.equals("failed"))) {
                        s.text = "loading";
                        s.glideTarget = new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                            @Override
                            public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                                int maxDim = 800;
                                if (resource.getWidth() > maxDim || resource.getHeight() > maxDim) {
                                    float ratio = Math.min((float)maxDim / resource.getWidth(), (float)maxDim / resource.getHeight());
                                    s.imageBitmap = android.graphics.Bitmap.createScaledBitmap(resource, (int)(resource.getWidth() * ratio), (int)(resource.getHeight() * ratio), true);
                                } else {
                                    s.imageBitmap = resource;
                                }
                                invalidate();
                            }
                            @Override
                            public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                            
                            @Override
                            public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                                s.text = "failed"; // Allows retry logic if needed later
                                invalidate();
                            }
                        };
                        // Use getApplicationContext() so the request isn't paused if the Activity/Service lifecycle pauses
                        com.bumptech.glide.Glide.with(getContext().getApplicationContext())
                            .asBitmap()
                            .load(s.imageUrl)
                            .into(s.glideTarget);
                    }
                    Paint p = new Paint(); p.setColor(Color.LTGRAY); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
                    canvas.drawRect(-100, -100, 100, 100, p);
                }
                canvas.restore();
            } else if ("text".equals(s.type) || "sticker".equals(s.type)) {
                canvas.save();
                canvas.translate(s.x, s.y);
                canvas.rotate(s.rotation);
                canvas.scale(s.scale, s.scale);
                
                Paint textPaint = new Paint(s.paint);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setTextSize("sticker".equals(s.type) ? 120f : 80f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                
                // Draw text centered at 0,0 since we translated
                // Adjust Y to center text vertically
                float textOffset = (textPaint.descent() + textPaint.ascent()) / 2;
                if (s.text != null) {
                    canvas.drawText(s.text, 0, -textOffset, textPaint);
                }
                
                if (s == activeObject && "text".equals(s.type)) {
                    if ((System.currentTimeMillis() % 1000) < 500) {
                        float textWidth = textPaint.measureText(s.text);
                        canvas.drawText("|", textWidth / 2 + 5, -textOffset, textPaint);
                    }
                    postInvalidateDelayed(100); // keep cursor blinking
                }
                
                canvas.restore();
            } else {
                canvas.drawPath(s.path, s.paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        
        if (!"path".equals(currentTool)) {
            // Let the activity handle text/sticker placement via click listener
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                currentStrokeKey = myStrokesRef != null ? myStrokesRef.push().getKey() : null;
                currentLocalStroke = new DrawObject(currentColor, currentWidth, currentStrokeKey);
                currentLocalStroke.path.moveTo(x, y);
                myStrokes.add(currentLocalStroke);
                
                if (isSetup && currentStrokeKey != null) {
                    Map<String, Object> strokeData = new HashMap<>();
                    strokeData.put("color", String.format("#%06X", (0xFFFFFF & currentColor)));
                    strokeData.put("width", currentWidth);
                    myStrokesRef.child(currentStrokeKey).setValue(strokeData);
                    pushPointToFirebase(x, y);
                    if (myStrokesRef.getParent() != null) {
                        myStrokesRef.getParent().child("last_updated").setValue(System.currentTimeMillis());
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentLocalStroke != null) {
                    currentLocalStroke.path.lineTo(x, y);
                    pushPointToFirebase(x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentLocalStroke != null) {
                    currentLocalStroke.path.lineTo(x, y);
                    pushPointToFirebase(x, y);
                    invalidate();
                    currentLocalStroke = null;
                }
                break;
        }
        return true;
    }
    
    private void pushPointToFirebase(float x, float y) {
        if (isSetup && myStrokesRef != null && currentStrokeKey != null) {
            float screenX = x;
            float screenY = y;
            
            float screenWidth = getResources().getDisplayMetrics().widthPixels;
            float screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            Map<String, Object> point = new HashMap<>();
            point.put("x", screenX / screenWidth);
            point.put("y", screenY / screenHeight);
            myStrokesRef.child(currentStrokeKey).child("points").push().setValue(point);
        }
    }

    public void updateActiveObjectColor(int color) {
        if (activeObject != null && "text".equals(activeObject.type)) {
            activeObject.currentColor = color;
            activeObject.paint.setColor(color);
            invalidate();
        }
    }

    public void updateActiveObjectText(String text) {
        if (activeObject != null) {
            if (text.trim().isEmpty() && activeObject.text != null && !activeObject.text.trim().isEmpty()) {
                // Prevent OS keyboard hiding/focus loss from accidentally wiping text.
                // If they want to delete, they can use the delete ('X') button.
                return;
            }
            activeObject.text = text;
            invalidate();
        }
    }

    public boolean handleInteractiveTouch(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        
        float x = event.getX();
        float y = event.getY();
        
        // Calculate multi-touch rotation
        if (event.getPointerCount() == 2) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                lastRotationAngle = angle;
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && activeObject != null) {
                activeObject.rotation += (angle - lastRotationAngle);
                lastRotationAngle = angle;
            }
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (activeObject == null) {
                    DrawObject tappedObject = null;
                    for (int i = myStrokes.size() - 1; i >= 0; i--) {
                        DrawObject s = myStrokes.get(i);
                        if ("text".equals(s.type) || "sticker".equals(s.type) || "image".equals(s.type)) {
                            float halfWidth = 150f;
                            float halfHeight = 150f;
                            if ("image".equals(s.type)) {
                                if (s.imageBitmap != null) {
                                    halfWidth = s.imageBitmap.getWidth() / 2f + 20;
                                    halfHeight = s.imageBitmap.getHeight() / 2f + 20;
                                }
                            } else {
                                float textSize = "sticker".equals(s.type) ? 120f : 80f;
                                Paint textPaint = new Paint(s.paint);
                                textPaint.setTextSize(textSize);
                                if (s.text != null && !s.text.isEmpty()) {
                                    halfWidth = (textPaint.measureText(s.text) / 2) + 40;
                                    halfHeight = (textPaint.descent() - textPaint.ascent()) / 2 + 40;
                                } else {
                                    halfWidth = Math.max(150f, textSize);
                                    halfHeight = Math.max(100f, textSize);
                                }
                            }
                            
                            // Un-rotate the point for precise AABB hit detection
                            float dx = x - s.x;
                            float dy = y - s.y;
                            float unrotatedX = (float) (dx * Math.cos(Math.toRadians(-s.rotation)) - dy * Math.sin(Math.toRadians(-s.rotation)));
                            float unrotatedY = (float) (dx * Math.sin(Math.toRadians(-s.rotation)) + dy * Math.cos(Math.toRadians(-s.rotation)));
                            
                            if (Math.abs(unrotatedX) < halfWidth * s.scale && Math.abs(unrotatedY) < halfHeight * s.scale) {
                                tappedObject = s;
                                myStrokes.remove(i);
                                if (s.firebaseKey != null && myStrokesRef != null) {
                                    myStrokesRef.child(s.firebaseKey).removeValue();
                                }
                                break;
                            }
                        }
                    }
                    
                    if (tappedObject != null) {
                        activeObject = tappedObject;
                        if (getContext() instanceof DrawActivity) {
                            if ("text".equals(activeObject.type)) {
                                ((DrawActivity) getContext()).openKeyboardForActiveObject(activeObject.text);
                            } else {

                            }
                        }
                    } else {
                        // Create new active object
                        activeObject = new DrawObject(currentColor, currentWidth, java.util.UUID.randomUUID().toString());
                        activeObject.type = currentTool;
                        activeObject.x = x;
                        activeObject.y = y;
                        if ("image".equals(currentTool)) activeObject.imageUrl = currentImageUrl;
                        
                        if (getContext() instanceof DrawActivity) {
                            if ("text".equals(currentTool)) {
                                ((DrawActivity) getContext()).openKeyboardForActiveObject("");
                            } else {

                            }
                        }
                    }
                } else {
                    // Check if clicked inside bounds
                    float rawDx = x - activeObject.x;
                    float rawDy = y - activeObject.y;
                    
                    // Un-rotate the point to match the bounding box's local coordinate system
                    float dx = (float) (rawDx * Math.cos(Math.toRadians(-activeObject.rotation)) - rawDy * Math.sin(Math.toRadians(-activeObject.rotation)));
                    float dy = (float) (rawDx * Math.sin(Math.toRadians(-activeObject.rotation)) + rawDy * Math.cos(Math.toRadians(-activeObject.rotation)));
                    
                    float halfWidth = 150f;
                    float halfHeight = 150f;
                    if ("image".equals(activeObject.type)) {
                        if (activeObject.imageBitmap != null) {
                            halfWidth = activeObject.imageBitmap.getWidth() / 2f + 20;
                            halfHeight = activeObject.imageBitmap.getHeight() / 2f + 20;
                        }
                    } else {
                        float textSize = "sticker".equals(activeObject.type) ? 120f : 80f;
                        Paint textPaint = new Paint(activeObject.paint);
                        textPaint.setTextSize(textSize);
                        if (activeObject.text != null && !activeObject.text.isEmpty()) {
                            halfWidth = (textPaint.measureText(activeObject.text) / 2) + 40;
                            halfHeight = (textPaint.descent() - textPaint.ascent()) / 2 + 40;
                        } else {
                            halfWidth = Math.max(150f, textSize);
                            halfHeight = Math.max(100f, textSize);
                        }
                    }
                    
                    float boundX = halfWidth * activeObject.scale;
                    float boundY = halfHeight * activeObject.scale;
                    
                     float hitRadius = 60f; // radius in pixels around the button center
                     
                     // Delete button is at top-left corner: (-boundX, -boundY) in local unrotated coords
                     float deleteButtonX = -boundX;
                     float deleteButtonY = -boundY;
                     float distToDelete = (float) Math.hypot(dx - deleteButtonX, dy - deleteButtonY);
                     
                     // Resize button is at bottom-right corner: (boundX, boundY)
                     float resizeButtonX = boundX;
                     float resizeButtonY = boundY;
                     float distToResize = (float) Math.hypot(dx - resizeButtonX, dy - resizeButtonY);
                     
                     if (distToDelete <= hitRadius) {
                         // Clicked delete button
                         activeObject = null;
                         if (getContext() instanceof DrawActivity) {
                             ((DrawActivity) getContext()).closeInputPanels();
                         }
                     } else if (distToResize <= hitRadius) {
                         // Clicked resize button
                         isResizing = true;
                    } else if (Math.abs(dx) > boundX && Math.abs(dy) > boundY) {
                        // Clicked outside completely
                        commitActiveObject();
                    } else if (Math.abs(dx) > boundX + 40 || Math.abs(dy) > boundY + 40) {
                        // Clicked outside, commit it
                        commitActiveObject();
                    }
                }
                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (activeObject != null && event.getPointerCount() == 1) {
                    if (isResizing) {
                        float halfWidth = 150f;
                        float halfHeight = 150f;
                        if ("image".equals(activeObject.type)) {
                            if (activeObject.imageBitmap != null) {
                                halfWidth = activeObject.imageBitmap.getWidth() / 2f + 20;
                                halfHeight = activeObject.imageBitmap.getHeight() / 2f + 20;
                            }
                        } else {
                            float textSize = "sticker".equals(activeObject.type) ? 120f : 80f;
                            Paint textPaint = new Paint(activeObject.paint);
                            textPaint.setTextSize(textSize);
                            if (activeObject.text != null && !activeObject.text.isEmpty()) {
                                halfWidth = (textPaint.measureText(activeObject.text) / 2) + 40;
                                halfHeight = (textPaint.descent() - textPaint.ascent()) / 2 + 40;
                            } else {
                                halfWidth = Math.max(150f, textSize);
                                halfHeight = Math.max(100f, textSize);
                            }
                        }
                        
                        float distance = (float) Math.hypot(x - activeObject.x, y - activeObject.y);
                        float baseDistance = (float) Math.hypot(halfWidth, halfHeight);
                        if (baseDistance > 0) {
                            activeObject.scale = Math.max(0.1f, Math.min(10.0f, distance / baseDistance));
                        }
                        
                        float baseAngle = (float) Math.toDegrees(Math.atan2(halfHeight, halfWidth));
                        float currentAngle = (float) Math.toDegrees(Math.atan2(y - activeObject.y, x - activeObject.x));
                        activeObject.rotation = currentAngle - baseAngle;
                    } else {
                        activeObject.x += (x - lastTouchX);
                        activeObject.y += (y - lastTouchY);
                    }
                    invalidate();
                }
                lastTouchX = x;
                lastTouchY = y;
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isResizing = false;
                return true;
        }
        return true;
    }
    

    private Double getSafeDouble(com.google.firebase.database.DataSnapshot snap, String key) {
        if (!snap.hasChild(key)) return null;
        Object val = snap.child(key).getValue();
        if (val instanceof Number) return ((Number)val).doubleValue();
        return null;
    }

    public void commitActiveObject() {
        if (activeObject == null || !isSetup) return;
        
        if ("text".equals(activeObject.type) || "sticker".equals(activeObject.type)) {
            if (activeObject.text == null || activeObject.text.trim().isEmpty()) {
                activeObject = null;
                invalidate();
                return;
            }
        } else if ("image".equals(activeObject.type)) {
            if (activeObject.imageUrl == null || activeObject.imageUrl.trim().isEmpty()) {
                activeObject = null;
                invalidate();
                return;
            }
        }
        
        String key = myStrokesRef != null ? myStrokesRef.push().getKey() : null;
        if (key != null) {
            activeObject.firebaseKey = key;
            activeObject.currentColor = currentColor;
            myStrokes.add(activeObject);
            
            float screenWidth = getResources().getDisplayMetrics().widthPixels;
            float screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("type", activeObject.type);
            data.put("text", activeObject.text);
            data.put("imageUrl", activeObject.imageUrl);
            data.put("color", String.format("#%06X", (0xFFFFFF & activeObject.currentColor)));
            data.put("width", activeObject.paint.getStrokeWidth());
            data.put("x", activeObject.x / screenWidth);
            data.put("y", activeObject.y / screenHeight);
            data.put("scale", activeObject.scale);
            data.put("rotation", activeObject.rotation);
            
            myStrokesRef.child(key).setValue(data);
            if (myStrokesRef.getParent() != null) {
                myStrokesRef.getParent().child("last_updated").setValue(System.currentTimeMillis());
            }
        }
        
        if (getContext() instanceof DrawActivity) {
            ((DrawActivity) getContext()).closeInputPanels();
        }
        
        activeObject = null;
        invalidate();
    }

    public void clearMyCanvas() {
        if (!isSetup) {
            myStrokes.clear();
            partnerStrokes.clear();
            invalidate();
            return;
        }
        myStrokesRef.removeValue(); 
        myStrokes.clear();
        
        if (partnerStrokesRef != null) {
            partnerStrokesRef.removeValue();
            partnerStrokes.clear();
        }
        
        invalidate();
    }
}
