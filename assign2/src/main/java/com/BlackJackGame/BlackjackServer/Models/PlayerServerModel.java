package com.BlackJackGame.BlackjackServer.Models;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.BlackJackGame.Models.PlayerModel;

public class PlayerServerModel extends PlayerModel{

    private Boolean connectionState = false;
    private SocketChannel socket;
    private SelectionKey key;
    private long lastUpdateSent = 0;

    public PlayerServerModel(String token, String username, String password, int rank) {        
        this.token = token;
        this.username = username;
        this.password = password;
        this.setRank(rank);
        this.setConnectionState(false);
    }
    
    public PlayerServerModel(String token, SocketChannel socket, String username, String password) {        
        this.token = token;
        this.socket = socket;
        this.username = username;
        this.password = password;
        this.setConnectionState(socket.isConnected());
    }

    public void setConnectionState(boolean connectionState) {
        this.connectionState = connectionState;
    }
    
    public Boolean getConnectionState() {
        return connectionState;
    }
    public SocketChannel getSocket() {
        return socket;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public SelectionKey getKey() {
        return key;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }
    
    public long getLastUpdateSent() {
        return lastUpdateSent;
    }

    public void setLastUpdateSent(long lastUpdateSent) {
        this.lastUpdateSent = lastUpdateSent;
    }

    public String toString() {
        return super.toString() + ";connectionState:" + (this.connectionState ? "connected":"disconnected");
    }
    
}
