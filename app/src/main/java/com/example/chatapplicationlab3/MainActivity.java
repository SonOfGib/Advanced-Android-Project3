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
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, MessageSendFragment.OnMessageSetListener {


    private PendingIntent mPendingIntent;
    private NfcAdapter nfcAdapter;
    private String username;
    private Context mContext;
    private SharedPreferences prefs;

    private final static String USER_PREF_KEY = "USER_PREF_KEY";
    private static final String KEY_MESSAGES = "KEY_MESSAGES";
    private final static String MESSAGE = "MESSAGE";
    private final static String MESSAGES = "MESSAGES";
    private final static String MODE = "MODE";
    private final static int KEY_SEND_MODE = 2;
    private final static int MESSAGE_SEND_MODE = 1;
    private final static int MESSAGE_RECIEVE_MODE = 0;


    //Message Display Fragment, Message Send Fragment, Key Share Fragment.

    private boolean mBounded = false;
    private KeyService mKeyService;
    private String mMessage;
    private ArrayList<Message> mMessages = new ArrayList<>();
    //mMode keeps track of which fragment is currently visible to the user.
    private int mMode = MESSAGE_RECIEVE_MODE;

    private boolean mStoreKeyWhenReady = false;
    private String mTempOwner;
    private String mTempPemKey;
    private String mPartner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent mIntent = new Intent(this, KeyService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String messageJson = prefs.getString(KEY_MESSAGES, "");
        if(!messageJson.equals("")){
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Message>>(){}.getType();
            mMessages = gson.fromJson(messageJson, type);
        }
        swapInMessageRecieveFrag();

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
                if(mMode != MESSAGE_RECIEVE_MODE) {
                    swapInMessageRecieveFrag();
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
                            mMessage, mKeyService.getSavedPartners());
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
                    Log.d("mMode is ?", ""+mMode);
                    //Send this fragment the public key and the username.
                    KeySendFragment keySendFragment = KeySendFragment.newInstance(mKeyService.getMyPublicKey());
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

                if(username== null || !username.equals(localUsername)) {
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
        Log.d("mConnection is null?", ""+ (mConnection == null));
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            readPayload(getIntent());
        }
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
        //save messages object.
        Gson gson = new Gson();
        String jsonString = gson.toJson(mMessages);

        prefs.edit().putString(KEY_MESSAGES, jsonString).apply();

    }

    void readPayload(Intent intent) {
        String payload = new String(
                ((NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0])
                        .getRecords()[0]
                        .getPayload());
        Toast.makeText(this, "Recieved NFC tag", Toast.LENGTH_LONG).show();

        //Lop off the 'en' language code.
        String jsonString = payload.substring(3);
        Log.d("Tag debug", jsonString);
        if(jsonString.equals("")){
            Log.d("Message Recieved?", "Message was empty!");
        }
        else {
            //Determine which json payload we've got here.

            try {
                JSONObject json = new JSONObject(jsonString);
                if(json.has("message")){
                    manageMessageJSON(json);
                }
                else if (json.has("key")){
                    manageKeyJson(json);
                }
                //else do nothing bc json is messed up.
            } catch (JSONException e) {
                Log.e("JSON Exception", "Convert problem", e);
            }
        }
    }

    private void manageKeyJson(JSONObject json){
        try {
            String owner = json.getString("user");
            String pemKey = json.getString("key");
            if(mBounded)
                mKeyService.storePublicKeyPEM(owner, pemKey);
            else{
                mStoreKeyWhenReady = true;
                mTempOwner = owner;
                mTempPemKey = pemKey;
            }
        }
        catch (JSONException e){
            Log.e("JSON Exception", "Key Problem", e);
        }

    }
    private void swapInMessageRecieveFrag(){
        mMode = MESSAGE_RECIEVE_MODE;

        MessageRecieveFragment msgRecvFragment = MessageRecieveFragment.newInstance(
                1, mMessages);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder,
                msgRecvFragment).commit();
    }

    private void manageMessageJSON(JSONObject json) {
        try {
            String sender = json.getString("from");
            String content = json.getString("message");
            //TODO Decrypt this message!
            Message message = new Message(sender, content);
            if (mMessages != null) {
                mMessages.add(message);
            } else {
                mMessages = new ArrayList<>();
                mMessages.add(message);
            }
            swapInMessageRecieveFrag();
        }catch (JSONException e){
            Log.e("JSON Exception", "Message Problem", e);
        }
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
                //TODO encrypt message.
                if(mMessage != null && mPartner != null){
                    payload = "{\"to\":\"" + mPartner + "\",\"from\":\""+ username + "\",\"message\""+
                            ":\""+ mMessage +"\"}";
                }

                break;
            case KEY_SEND_MODE:
                //Send currently 'set' message
                Log.d("KEY NDEF WRITE", "User tried to send NDEF in key mode.");
                String pubKey = mKeyService.getMyPublicKey();
                if(pubKey.equals("")){
                    Log.d("SEND EMPTY KEY", "KEY WAS EMPTY!");
                }
                else{
                    payload = "{\"user\":\""+ username +"\",\"key\":\""+ pubKey +"\"}";
                    Log.d("SENT KEY PAYLOAD", payload);
                }
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
            //If we have been requested to store a key on connection ...
            if(mStoreKeyWhenReady){
                mStoreKeyWhenReady = false;
                if(mTempOwner != null && mTempPemKey != null)
                    mKeyService.storePublicKeyPEM(mTempOwner, mTempPemKey);
            }
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
    public void onMessageSet(String message, String partner) {
        Toast.makeText(this,"Message set.", Toast.LENGTH_SHORT).show();
        mMessage = message;
        mPartner = partner;
    }
}