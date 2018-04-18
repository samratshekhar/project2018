package com.trovami.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trovami.R;
import com.trovami.models.Notification;
import com.trovami.models.NotificationReq;
import com.trovami.models.RDBSchema;
import com.trovami.models.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UserFragment extends Fragment {
    private static final String TAG = "UserFragment";

    private UserFragmentListener mListener;
    private FirebaseAuth mAuth;
    private ProgressDialog mDialog;
    private List<User> mUnfolllowedUsers;
    private List<String> mSentReq = new ArrayList<>();
    private User mCurrentUser;

    public UserFragment() {
        // Required empty public constructor
    }

    public static UserFragment newInstance() {
        UserFragment fragment = new UserFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mDialog = new ProgressDialog(getContext());
        mDialog.setMessage("Logging in...");
        mDialog.setCancelable(false);
        mDialog.show();
        setupFirebaseAuth();
        fetchNotifications();
        fetchCurrentUser();
    }

    private void fetchNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                if(iterator.hasNext()) {
                    // notifications found, fetch followers and following
                    DataSnapshot singleSnapshot = iterator.next();
                    Notification notification = singleSnapshot.getValue(Notification.class);
                    for (Map.Entry<String, NotificationReq> entry : notification.to.entrySet()) {
                        System.out.println(entry.getKey() + "/" + entry.getValue());
                        NotificationReq sentReq = entry.getValue();
                        mSentReq.add(sentReq.to);
                    }
                } else {
                    //TODO: handle no notifications here
                }
                fetchCurrentUser();
                mDialog.dismiss();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                //TODO: handle no notifications here
                mDialog.dismiss();
            }
        };
        Notification.getNotificationsById(currentUser.getUid(), listener);
    }

    private void fetchCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                if(iterator.hasNext()) {
                    // user found, fetch followers and following
                    DataSnapshot singleSnapshot = iterator.next();
                    mCurrentUser = singleSnapshot.getValue(User.class);
                    fetchUnfollowedUsers();
                } else {
                    // TODO: handle user req here
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: handle user req here
                mDialog.dismiss();
            }
        };
        User.getUserById(currentUser.getUid(), listener);
    }

    private void fetchUnfollowedUsers() {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mUnfolllowedUsers = new ArrayList<>();
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    User user = singleSnapshot.getValue(User.class);
                    boolean isUnfollowing = isUnfollowed(user.uid);
                    if(isUnfollowing) {
                        mUnfolllowedUsers.add(user);
                    }
                }
                // TODO: update adapter here;
                generateFollowReq(mUnfolllowedUsers.get(0));
                mDialog.dismiss();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: handle error
                mDialog.dismiss();
            }
        };
        User.getUsers(listener);
    }

    private boolean isUnfollowed(String uid) {
        boolean isAlreadyFollowing = mCurrentUser.following.containsKey(uid);
        boolean isReqSent = mSentReq.contains(uid);
        boolean isCurrentUser = mCurrentUser.uid.equals(uid);
        if (isAlreadyFollowing || isReqSent || isCurrentUser) return false;
        return true;
    }

    private void generateFollowReq(User user) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        NotificationReq senderReq = new NotificationReq();
        senderReq.to = user.uid;
        senderReq.status = "pending";

        NotificationReq receiverReq = new NotificationReq();
        receiverReq.from = mCurrentUser.uid;
        receiverReq.status = "pending";


        // TODO: add notification entry for current user (to)
        DatabaseReference senderRef = database.child(RDBSchema.Notification.TABLE_NAME).child(mCurrentUser.uid).child("to");
        senderRef.child(user.uid).setValue(senderReq);


        // TODO: add notification entry for end user(from)
        DatabaseReference receiverRef = database.child(RDBSchema.Notification.TABLE_NAME).child(user.uid).child("from");
        receiverRef.child(mCurrentUser.uid).setValue(receiverReq);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_user, container, false);
        return v;
    }

    private void setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UserFragmentListener) {
            mListener = (UserFragmentListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface UserFragmentListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
