package com.example.chatapplicationlab3;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class KeySendFragment extends Fragment {

    private static final String PEM_KEY = "PEM_KEY";
    private String mPemKey;
    public KeySendFragment() {
        // Required empty public constructor
    }


    public static KeySendFragment newInstance(String pemKey) {
        KeySendFragment fragment = new KeySendFragment();
        Bundle args = new Bundle();
        args.putString(PEM_KEY, pemKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPemKey = getArguments().getString(PEM_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_key_send, container, false);
        TextView pubKeyDisp = rootView.findViewById(R.id.publicKeyDisplay);
        if(mPemKey != null)
            pubKeyDisp.setText(mPemKey);
        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
