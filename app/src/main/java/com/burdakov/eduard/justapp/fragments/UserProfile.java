package com.burdakov.eduard.justapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.burdakov.eduard.justapp.LoginActivity;
import com.burdakov.eduard.justapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserProfile extends Fragment {

    Button logOutButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.user_fragment, container, false);

        logOutButton = view.findViewById(R.id.B_logout);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLogout();
                mountMainActivity();
            }
        });


        return view;

    }

    public void requestLogout() {
        firebaseAuth.signOut();
    }


    public void mountMainActivity() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        startActivity(intent);
    }
}
