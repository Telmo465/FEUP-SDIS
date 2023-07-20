package com.BlackJackGame.BlackjackServer.Models;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import com.BlackJackGame.Models.MessageModel;

public class MessageServerModel extends MessageModel {
    
    private SocketChannel socket;

    public MessageServerModel() { }
    
    public MessageServerModel(SocketChannel socket) {
        this.socket = socket;
    }
        
    public MessageServerModel(String msg, SocketChannel socket){
        this.socket = socket;
        
        this.parse(msg);
    }
    
    public MessageServerModel(String msg, String token, SocketChannel socket){
        this.socket = socket;
        
        this.parse(msg);

        if(this.getToken().isBlank())
            this.setToken(token);
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public static ArrayList<MessageServerModel> fromMessage(String query, SocketChannel client) {
        ArrayList<MessageServerModel> result = new ArrayList<>();
        ArrayList<MessageModel> arr = MessageModel.fromMessage(query);

        arr.forEach(s -> {
            result.add(new MessageServerModel(s.toString(), client));
        });

        return result;
    }
}