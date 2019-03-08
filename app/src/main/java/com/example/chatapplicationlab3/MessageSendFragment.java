package com.example.chatapplicationlab3;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashSet;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MessageSendFragment.OnMessageSetListener} interface
 * to handle interaction events.
 * Use the {@link MessageSendFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessageSendFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String USERNAME = "USERNAME";
    private static final String PRIVATE_KEY = "PRIVATE_KEY";

    private static final String MESSAGE = "MESSAGE";
    private static final String PARTNERS = "PARTNERS";

    // TODO: Rename and change types of parameters
    private String mUsername;
    private ArrayList<String> mPartnerList;
    private String mMessage;
    private String mPartner;

    private OnMessageSetListener mListener;

    public MessageSendFragment() {
        // Required empty public constructor
    }


    public static MessageSendFragment newInstance(String username, String message,
                                                  ArrayList<String> parnters) {
        MessageSendFragment fragment = new MessageSendFragment();
        Bundle args = new Bundle();
        args.putString(USERNAME, username);
        args.putString(MESSAGE, message);
        args.putStringArrayList(PARTNERS, parnters);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUsername = getArguments().getString(USERNAME);
            mMessage = getArguments().getString(MESSAGE);
            mPartnerList = getArguments().getStringArrayList(PARTNERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_message_send, container, false);
        final EditText messageEdit = rootView.findViewById(R.id.messageEditText);
        if(mMessage != null)
            messageEdit.setText(mMessage);
        if(mPartnerList != null && getActivity() != null){
            final Spinner partnerSpinner = rootView.findViewById(R.id.recipientSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.support_simple_spinner_dropdown_item, mPartnerList);
            partnerSpinner.setAdapter(adapter);
            partnerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mPartner = (String) partnerSpinner.getAdapter().getItem(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //Do Nothing
                }
            });
        }
        Button setMessageBtn = rootView.findViewById(R.id.setMessageButton);
        setMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(messageEdit.getText().toString(), mPartner);
            }
        });
        return rootView;

    }


    public void onButtonPressed(String message, String partner) {
        if (mListener != null) {
            mListener.onMessageSet(message,partner);
            mMessage = message;
            //Update args with new message.
            Bundle args = this.getArguments();
            if(args == null)
                args = new Bundle();
            args.putString(MESSAGE, mMessage);
            this.setArguments(args);
        }
    }

    String getMessage(){
        return mMessage;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMessageSetListener) {
            mListener = (OnMessageSetListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnMessageSetListener {
        // TODO: Update argument type and name
        void onMessageSet(String message, String partner);
    }
}
