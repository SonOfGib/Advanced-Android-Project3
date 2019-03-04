package com.example.chatapplicationlab3;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


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

    // TODO: Rename and change types of parameters
    private String mUsername;
    private String mPrivateKey;
    private String mMessage;

    private OnMessageSetListener mListener;

    public MessageSendFragment() {
        // Required empty public constructor
    }


    public static MessageSendFragment newInstance(String username, String privateKey, String message) {
        MessageSendFragment fragment = new MessageSendFragment();
        Bundle args = new Bundle();
        args.putString(USERNAME, username);
        args.putString(PRIVATE_KEY, privateKey);
        args.putString(MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUsername = getArguments().getString(USERNAME);
            mPrivateKey = getArguments().getString(PRIVATE_KEY);
            mMessage = getArguments().getString(MESSAGE);
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
        Button setMessageBtn = rootView.findViewById(R.id.setMessageButton);
        setMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(messageEdit.getText().toString());
            }
        });
        return rootView;

    }


    public void onButtonPressed(String message) {
        if (mListener != null) {
            mListener.onMessageSet(message);
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
        void onMessageSet(String message);
    }
}
