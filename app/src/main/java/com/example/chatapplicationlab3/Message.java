package com.example.chatapplicationlab3;

import android.os.Parcel;
import android.os.Parcelable;

class Message implements Parcelable {

    private String sender;
    private String content;

    Message(String sender, String content){
        this.sender = sender;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    private Message(Parcel in) {
        String[] data = new String[2];
        in.readStringArray(data);
        this.sender = data[0];
        this.content = data[1];
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.sender,
                                           this.content });
    }
}
