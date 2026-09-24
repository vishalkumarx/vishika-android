package com.example.vishika;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommentsBottomSheet extends BottomSheetDialogFragment {

    private String postId;
    private String currentUserId;
    private String currentUserName;

    private RecyclerView recyclerView;
    private CommentsAdapter adapter;
    private List<FeedComment> commentList;
    private DatabaseReference commentsRef;
    private EditText etCommentInput;
    private TextView tvNoComments;
    
    private FeedComment replyingTo = null;

    public static CommentsBottomSheet newInstance(String postId, String currentUserId, String currentUserName) {
        CommentsBottomSheet fragment = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString("POST_ID", postId);
        args.putString("USER_ID", currentUserId);
        args.putString("USER_NAME", currentUserName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("POST_ID");
            currentUserId = getArguments().getString("USER_ID");
            currentUserName = getArguments().getString("USER_NAME");
        }
        commentsRef = FirebaseDatabase.getInstance().getReference()
                .child("feed_posts").child(postId).child("comments");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomDialog = (BottomSheetDialog) d;
            View bottomSheet = bottomDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                int height = displayMetrics.heightPixels;
                
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = (int) (height * 0.65); // 65% of screen height
                bottomSheet.setLayoutParams(layoutParams);
                
                BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight((int) (height * 0.65));
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_comments, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerViewComments);
        tvNoComments = view.findViewById(R.id.tvNoComments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        commentList = new ArrayList<>();
        
        adapter = new CommentsAdapter(getContext(), commentList, currentUserId, commentsRef, comment -> {
            replyingTo = comment;
            etCommentInput.setHint("Replying to " + comment.authorName + "...");
            etCommentInput.setText("@" + comment.authorName.replaceAll("\\s+", "") + " ");
            etCommentInput.setSelection(etCommentInput.getText().length());
            etCommentInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etCommentInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        
        recyclerView.setAdapter(adapter);

        etCommentInput = view.findViewById(R.id.etCommentInput);
        TextView tvPostComment = view.findViewById(R.id.tvPostComment);
        ImageView ivMyAvatar = view.findViewById(R.id.ivMyAvatar);

        loadMyAvatar(ivMyAvatar);

        tvPostComment.setOnClickListener(v -> postComment());

        loadComments();

        return view;
    }

    private void loadMyAvatar(ImageView imageView) {
        FirebaseDatabase.getInstance().getReference()
                .child("profiles")
                .child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileUrl = snapshot.child("imageUrl").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty() && getContext() != null) {
                            Glide.with(getContext())
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(imageView);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadComments() {
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    FeedComment comment = data.getValue(FeedComment.class);
                    if (comment != null) {
                        commentList.add(comment);
                    }
                }
                Collections.sort(commentList, (c1, c2) -> Long.compare(c1.timestamp, c2.timestamp));
                adapter.notifyDataSetChanged();
                if (!commentList.isEmpty()) {
                    tvNoComments.setVisibility(View.GONE);
                    recyclerView.scrollToPosition(commentList.size() - 1);
                } else {
                    tvNoComments.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void postComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        String commentId = commentsRef.push().getKey();
        if (commentId != null) {
            FeedComment newComment = new FeedComment(commentId, currentUserId, currentUserName, text, System.currentTimeMillis());

            if (replyingTo != null) {
                commentsRef.child(replyingTo.id).child("replies").child(commentId).setValue(newComment);
                adapter.expandComment(replyingTo.id);

                // Notify reply
                if (!replyingTo.authorId.equals(currentUserId)) {
                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                            .child("notifications").child(replyingTo.authorId);
                    String notifId = notifRef.push().getKey();
                    if (notifId != null) {
                        AppNotification notification = new AppNotification(notifId, "reply", currentUserId, currentUserName, "replied to your comment: " + text, System.currentTimeMillis(), postId);
                        notifRef.child(notifId).setValue(notification);
                    }
                }

                replyingTo = null;
                etCommentInput.setHint("Add a comment...");
            } else {
                commentsRef.child(commentId).setValue(newComment);

                // Notify comment
                FirebaseDatabase.getInstance().getReference().child("feed_posts").child(postId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String postAuthorId = snapshot.child("authorId").getValue(String.class);
                                if (postAuthorId != null && !postAuthorId.equals(currentUserId)) {
                                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                                            .child("notifications").child(postAuthorId);
                                    String notifId = notifRef.push().getKey();
                                    if (notifId != null) {
                                        AppNotification notification = new AppNotification(notifId, "comment", currentUserId, currentUserName, "commented on your post: " + text, System.currentTimeMillis(), postId);
                                        notifRef.child(notifId).setValue(notification);
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
        }
        
        etCommentInput.setText("");
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etCommentInput.getWindowToken(), 0);
    }
}
