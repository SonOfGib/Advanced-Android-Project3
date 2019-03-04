package com.example.chatapplicationlab3;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;
import java.lang.reflect.Type;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class KeyService extends Service {
    KeyPairGenerator kpg;
    KeyPair keys;
    Cipher cipher;
    BigInteger modulus;
    HashMap<String,String> partnerKeys;
    SharedPreferences prefs;
    private final IBinder mBinder = new LocalBinder();
    final String PARTNER_MAP_PREF = "PARTNER_MAP_PREF";
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        KeyService getService() {
            return KeyService.this;
        }
    }


    public KeyService() {
        partnerKeys = new HashMap<>();
    }

    KeyPair generateMyKeys(){
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            keys = kpg.generateKeyPair();
            KeyFactory fact = KeyFactory.getInstance("RSA");
            cipher = Cipher.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keys.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keys.getPublic();

            String privateKeyString = privateKey.getPrivateExponent().toString();
            String publicKeyString = publicKey.getPublicExponent().toString();

            Log.d("Public", publicKeyString);
            Log.d("Private", privateKeyString);
            Log.d("Mod", publicKey.getModulus().toString());
            RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(privateKey.getModulus(), new BigInteger(privateKeyString));
            privateKey = (RSAPrivateKey) fact.generatePrivate(privKeySpec);
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(publicKey.getModulus(), new BigInteger(publicKeyString));
            modulus = publicKey.getModulus();
            publicKey = (RSAPublicKey) fact.generatePublic(pubKeySpec);

            return keys;
        }catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    void storePublicKey(String partnerName, String publicKey){
        partnerKeys.put(partnerName,publicKey);
        //Save the map to preferences.
        String mapString = new JSONObject(partnerKeys).toString();
        prefs.edit().putString(PARTNER_MAP_PREF, mapString).apply();
    }

    RSAPublicKey getPublicKey(String partnerName){
        String pubkey = (String) partnerKeys.get(partnerName);
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus,new BigInteger(pubkey));
        try {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) fact.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt with the partners public key.
     * @param plainText The actual text.
     * @param partnerName The partners name.
     * @return The cipher text.
     */
    public String encrypt(String plainText, String partnerName){
        RSAPublicKey publicKey = getPublicKey(partnerName);
        try{
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedText = cipher.doFinal(plainText.getBytes());
            return Base64.encodeToString(encryptedText, Base64.DEFAULT);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt with our private key.
     * @param cipherText The encrypted text.
     * @return The decrypted text.
     */
    public String decrypt(String cipherText){
        byte[] encryptedText = Base64.decode(cipherText, Base64.DEFAULT);
        try{
            cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
            String decryptedText = new String(cipher.doFinal(encryptedText));
            return decryptedText;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String jsonString = prefs.getString(PARTNER_MAP_PREF, "");
        Gson gson = new Gson();
        HashMap<String, String> map = new HashMap<>();
        if(!jsonString.equals("")) {
            map = (HashMap<String, String>) gson.fromJson(jsonString, map.getClass());
            partnerKeys = map;
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}