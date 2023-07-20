package com.BlackJackGame.Models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameModel {

    public enum GameStatus {
        NOT_STARTED,
        WAITING_LOBBY,
        STARTED,
        ENDED
    }

    private GameStatus gameStatus = GameStatus.NOT_STARTED;
    private String gameId = new String();
    private Integer rank = 0;
    private Integer gameScore = 0;
    private Integer playerTurn = 0;
    private Integer gameTurn = 0;
    private long timeToWait = 0;
    private long timeTarget = 0;
        
    protected HashMap<String, PlayerModel> playerList = new HashMap<>();

    public String getGameStatusString() {
        if( this.gameStatus == GameStatus.NOT_STARTED) {
            return "notStarted";
        } 
        else if( this.gameStatus == GameStatus.WAITING_LOBBY) {
            return "waitingLobby";
        } 
        else if( this.gameStatus == GameStatus.STARTED) {
            return "started";
        } 
        else {
            return "ended";
        } 
    }
    
    public GameStatus getGameStatus() {
        return this.gameStatus;
    }
    public void setGameStatus(GameStatus gamestarted) {
        this.gameStatus = gamestarted;
    }

    public String getGameId() {
        return gameId;
    }
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public Integer getRank() {
        return rank;
    }
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getGameScore() {
        return gameScore;
    }
    public void setGameScore(Integer gameScore) {
        this.gameScore = gameScore;
    }

    public Integer getPlayerTurn() {
        return playerTurn;
    }

    public Integer getGameTurn() {
        return gameTurn;
    }
    public void setGameTurn(Integer gameTurn) {
        this.gameTurn = gameTurn;
    }
    
    public long getTimeToWait() {
        return timeToWait;
    }
    public void setTimeToWait(long timeToWait) {
        this.timeToWait = timeToWait;
    }
    
    public long getTimeTarget() {
        return timeTarget;
    }
    public void setTimeTarget(long timeTarget) {
        this.timeTarget = timeTarget;
    }
    
    public List<PlayerModel> getPlayerList() {
        return playerList.values().stream().toList();
    }
    
    public int getPlayersCount() {
        return playerList.size();
    }

    public void update(MessageModel msg) {
        // gameId

        if(!msg.getGameId().isBlank()) {

            this.gameId = msg.getGameId();

            // gameStatus
            switch(msg.getGameStatus()) {
                case "notStarted":
                    this.setGameStatus(GameStatus.NOT_STARTED);
                    break;
                case "waitingLobby":
                    this.setGameStatus(GameStatus.WAITING_LOBBY);
                    this.setTimeToWait(msg.getTimeToWait());
                    this.setTimeTarget(msg.getTimeTarget());
                    break;
                case "started":
                    this.setGameStatus(GameStatus.STARTED);                    
                    break;
                case "ended":
                    this.setGameStatus(GameStatus.ENDED);
                    break;
            }

            this.setGameScore(msg.getGameScore());
        }

    }

    public String toString() {
        ArrayList<String> arr = new ArrayList<>();

        if( this.gameStatus == GameStatus.NOT_STARTED) {
            arr.add("gameStatus:notStarted");
        } 
        else if( this.gameStatus == GameStatus.WAITING_LOBBY) {
            arr.add("gameStatus:waitingLobby");

            arr.add(String.format("timeToWait:%d", this.getTimeToWait()));
            arr.add(String.format("timeTarget:%d", this.getTimeTarget()));
        } 
        else if( this.gameStatus == GameStatus.STARTED) {
            arr.add("gameStatus:started");
        } 
        else if( this.gameStatus == GameStatus.ENDED) {
            arr.add("gameStatus:ended");
        } 

        if(!this.gameId.isEmpty())
            arr.add("gameId:"+this.gameId);

        return String.join(";", arr);
    }
}
