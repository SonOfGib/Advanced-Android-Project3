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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, MessageSendFragment.OnMessageSetListener {

    PendingIntent mPendingIntent;
    NfcAdapter nfcAdapter;
    String username;
    Context mContext;
    SharedPreferences prefs;

    final static String USER_PREF_KEY = "USER_PREF_KEY";
    final static String MESSAGE = "MESSAGE";
    final static String MODE = "MODE";
    final static int KEY_SEND_MODE = 2;
    final static int MESSAGE_SEND_MODE = 1;
    final static int MESSAGE_RECIEVE_MODE = 0;

    //Message Display Fragment, Message Send Fragment, Key Share Fragment.

    boolean mBounded = false;
    KeyService mKeyService;
    private String mMessage;

    //mMode keeps track of which fragment is currently visible to the user.
    int mMode = MESSAGE_RECIEVE_MODE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState != null){
            mMode = savedInstanceState.getInt(MODE);
            mMessage = savedInstanceState.getString(MESSAGE);
        }

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
        username = prefs.getString(USER_PREF_KEY, null);
        if (username == null) {
            viewSwap.setEnabled(false);
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
                boolean committed = prefs.edit().putString(USER_PREF_KEY, localUsername).commit();
                if (committed) {
                    username = localUsername;
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
        //TextView disp = findViewById(R.id.messageDisplay);
        //disp.setText(payload);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putString(MESSAGE, mMessage);
        outState.putInt(MODE, mMode);
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NdefRecord record = NdefRecord.createTextRecord(null, "Debug message");
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