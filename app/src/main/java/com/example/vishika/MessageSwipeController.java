package com.example.vishika;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class MessageSwipeController extends ItemTouchHelper.Callback {

    private final Context context;
    private final ChatAdapter.OnReplySwipeListener swipeListener;
    private Drawable replyIcon;
    private boolean swipeBack = false;
    private boolean isSwipeToReplyTriggered = false;

    public MessageSwipeController(Context context, ChatAdapter.OnReplySwipeListener swipeListener) {
        this.context = context;
        this.swipeListener = swipeListener;
        try {
            this.replyIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_revert);
        } catch (Exception e) {
            this.replyIcon = null;
        }
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Handled in onChildDraw to prevent actual dismissal
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
        
        View itemView = viewHolder.itemView;
        
        if (replyIcon != null && Math.abs(dX) > 0) {
            int iconMargin = (itemView.getHeight() - replyIcon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + replyIcon.getIntrinsicHeight();
            
            int iconLeft, iconRight;
            if (dX > 0) { // Swiping right
                iconLeft = itemView.getLeft() + iconMargin;
                iconRight = iconLeft + replyIcon.getIntrinsicWidth();
            } else { // Swiping left
                iconRight = itemView.getRight() - iconMargin;
                iconLeft = iconRight - replyIcon.getIntrinsicWidth();
            }
            
            int alpha = (int) (Math.min(Math.abs(dX) / (itemView.getWidth() / 4f), 1f) * 255);
            replyIcon.setAlpha(alpha);
            replyIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            replyIcon.draw(c);
            
            if (Math.abs(dX) > itemView.getWidth() / 4f) {
                isSwipeToReplyTriggered = true;
            } else {
                isSwipeToReplyTriggered = false;
            }
        }
        
        float maxSwipe = itemView.getWidth() / 4f;
        float finalDx = Math.max(-maxSwipe, Math.min(dX, maxSwipe));
        
        super.onChildDraw(c, recyclerView, viewHolder, finalDx, dY, actionState, isCurrentlyActive);
    }

    private void setTouchListener(final Canvas c, final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final float dX, final float dY, final int actionState, final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
            if (swipeBack) {
                if (isSwipeToReplyTriggered) {
                    if (viewHolder instanceof ChatAdapter.ViewHolder) {
                        int position = viewHolder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            ChatAdapter adapter = (ChatAdapter) recyclerView.getAdapter();
                            if (adapter != null) {
                                swipeListener.onReplySwiped(adapter.getMessageAt(position));
                            }
                        }
                    }
                    isSwipeToReplyTriggered = false;
                }
            }
            return false;
        });
    }
}
