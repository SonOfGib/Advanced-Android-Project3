package com.example.chatapplicationlab3;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, MessageSendFragment.OnMessageSetListener {

    PendingIntent mPendingIntent;
    NfcAdapter nfcAdapter;
    String username;
    Context mContext;
    SharedPreferences prefs;

    final static String USER_PREF_KEY = "USER_PREF_KEY";
    final static String MESSAGE = "MESSAGE";
    final static String MESSAGES = "MESSAGES";
    final static String MODE = "MODE";
    final static int KEY_SEND_MODE = 2;
    final static int MESSAGE_SEND_MODE = 1;
    final static int MESSAGE_RECIEVE_MODE = 0;


    //Message Display Fragment, Message Send Fragment, Key Share Fragment.

    private boolean mBounded = false;
    private KeyService mKeyService;
    private String mMessage;
    private ArrayList<Message> mMessages = new ArrayList<>();
    //mMode keeps track of which fragment is currently visible to the user.
    int mMode = MESSAGE_RECIEVE_MODE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent mIntent = new Intent(this, KeyService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);


        mContext = this;
        Intent intent = new Intent(this, MainActivity.class);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.setNdefPushMessageCallback(this, this);
        //Button submitBtn = findViewById(R.id.submitButton);

        updateUsername();
        Button viewMsgSwap = findViewById(R.id.sendModeButton);
        Button viewKeySwap = findViewById(R.id.sendKeysViewButton);
        Button viewMsgDispSwap = findViewById(R.id.viewMessageButton);
        viewMsgDispSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMode != MESSAGE_RECIEVE_MODE){
                    mMode = MESSAGE_RECIEVE_MODE;
                    MessageRecieveFragment msgRecvFragment = MessageRecieveFragment.newInstance(1);

                    getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder,
                            msgRecvFragment).commit();
                }
            }
        });
        viewMsgSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMode != MESSAGE_SEND_MODE){
                    mMode = MESSAGE_SEND_MODE;
                    //Send this fragment the private key and the username.
                    MessageSendFragment msgSendFragment = MessageSendFragment.newInstance(username,
                            "DEBUG KEY", mMessage);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder,
                            msgSendFragment).commit();
                }
            }
        });
        viewKeySwap.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //swap modes if necessary, otherwise do nothing on click.
                if(mMode != KEY_SEND_MODE) {
                    mMode = KEY_SEND_MODE;
                    //Send this fragment the public key and the username.
                    KeySendFragment keySendFragment = KeySendFragment.newInstance();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentHolder, keySendFragment).commit();

                }
            }
        });

        Button setUsername = findViewById(R.id.setUsernameButton);
        setUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildUsernameDialog();
            }
        });

    }

    private void updateUsername() {
        Button viewSwap = findViewById(R.id.sendModeButton);
        Button keySwap = findViewById(R.id.sendKeysViewButton);
        username = prefs.getString(USER_PREF_KEY, null);
        if (username == null) {
            viewSwap.setEnabled(false);
            keySwap.setEnabled(false);
            Toast.makeText(this, "No username found, please set one!",
                    Toast.LENGTH_SHORT).show();
        } else {
            viewSwap.setEnabled(true);
            TextView usernameLabel = findViewById(R.id.usernameDisplayLabel);
            usernameLabel.setText(username);
        }
    }

    private void buildUsernameDialog() {
        //Preparing views
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_layout, (ViewGroup) findViewById(R.id.layout_root));
        //layout_root should be the name of the "top-level" layout node in the dialog_layout.xml file.
        final EditText usernameBox = layout.findViewById(R.id.usernameField);

        //Building dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(layout);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String localUsername = usernameBox.getText().toString();
                //Check to make sure we arent entering the same username again.
                if(!username.equals(localUsername)) {
                    //Generate keys with our username.
                    //TODO Consider saving previous username-keypairs in case the user wants to swap back.
                    mKeyService.generateMyKeys();
                    boolean committed = prefs.edit().putString(USER_PREF_KEY, localUsername).commit();
                    if (committed) {
                        username = localUsername;
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateUsername();
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUsername();
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            readPayload(getIntent());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    void readPayload(Intent intent) {
        String payload = new String(
                ((NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0])
                        .getRecords()[0]
                        .getPayload());
        Toast.makeText(this, "Recieved NFC tag", Toast.LENGTH_LONG).show();
        //Add message to list and then show messageFragment
        Log.d("Message was", payload);
        //TextView disp = findViewById(R.id.messageDisplay);
        //disp.setText(payload);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(MESSAGE, mMessage);
        outState.putInt(MODE, mMode);
        outState.putParcelableArrayList(MESSAGES, mMessages);
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            mMode = savedInstanceState.getInt(MODE);
            mMessage = savedInstanceState.getString(MESSAGE);
            mMessages = savedInstanceState.getParcelableArrayList(MESSAGES);
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String payload = "";
        switch(mMode){
            case MESSAGE_RECIEVE_MODE:
                //Uh do nothing?
                Log.d("RECIEVE NDEF WRITE", "User tried to send NDEF in recieve mode.");
                break;
            case MESSAGE_SEND_MODE:
                //Send currently 'set' message
                Log.d("SEND NDEF WRITE", "User tried to send NDEF in write mode.");
                //TODO Deal with partnername info.
                //TODO encrypt message.
                payload = "{\"to\":\"partnername\",\"from\":\"username\",\"message\""+
                        ":\""+ mMessage +"\"}";
                break;
            case KEY_SEND_MODE:
                //Send currently 'set' message
                Log.d("KEY NDEF WRITE", "User tried to send NDEF in key mode.");
                break;
        }
        NdefRecord record = NdefRecord.createTextRecord(null, payload);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

        return msg;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mKeyService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            KeyService.LocalBinder mLocalBinder = (KeyService.LocalBinder) service;
            mKeyService = mLocalBinder.getService();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    @Override
    public void onMessageSet(String message) {
        Toast.makeText(this,"Message set.", Toast.LENGTH_SHORT).show();
        mMessage = message;
    }
}