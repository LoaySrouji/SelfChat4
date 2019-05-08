package com.example.selfchat4;

public class Message {
    public String Id, Timestamp, Content;

    public Message(String Id, String Timestamp, String Content)
    {
        this.Content = Content;
        this.Timestamp = Timestamp;
        this.Id = Id;
    }
    @Override
    public String toString()
    {
        return this.Timestamp + "#" + Content;
    }
}