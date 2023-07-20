package com.BlackJackGame.BlackjackServer.Models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.BlackJackGame.BlackjackServer.Server;
import com.BlackJackGame.Models.GameModel;
import com.BlackJackGame.Models.PlayerModel;

public class GameServerModel extends GameModel {



    private int minPlayers = 2; // dealer counts?

    private long lastUpdateSent = 0;


    public GameServerModel(long timeToWait) {

        this.setTimeToWait(timeToWait);

        UUID uuid = UUID.randomUUID();

        String gameId = uuid.toString();

        this.setGameId(gameId);
    }


    public void addPlayer(PlayerServerModel player) {
        playerList.put(player.getToken(), player);
        player.setGameId(getGameId());
        System.out.println("Player added to game: " + player.getToken());
    }

    public boolean startLobby() {
        // System.out.println("Game started");
        GameStatus status = this.getGameStatus();

        if( status == GameStatus.NOT_STARTED) {
            if(this.minPlayers == this.getPlayersCount()) {
                this.setGameStatus(GameStatus.WAITING_LOBBY);
                this.setTimeTarget((new Date()).getTime() + this.getTimeToWait()*1000);

                return true;
            }
        } 

        return false;
        
    }

    public void setGameStarted() {
        this.setGameStatus(GameStatus.STARTED);
    }

    public void setGameEnded(){
        this.setGameStatus(GameStatus.ENDED);
    }

    public long getLastUpdateSent() {
        return lastUpdateSent;
    }


    public void setLastUpdateSent(long lastUpdateSent) {
        this.lastUpdateSent = lastUpdateSent;
    }

    public boolean canStart() {
        GameStatus status = this.getGameStatus();
        long currentTime = (new Date()).getTime(); 

        if( status == GameStatus.WAITING_LOBBY) {
            return ( currentTime - this.getTimeTarget()) >= 0;
        } else
            return false;
    }


    public String getPlayersScore() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Game ID: " + getGameId());
        result.add("Game Status: " + getGameStatusString());
        result.add("Username | score | rank");

        try {
            Server.PlayersLock.lock();

            List<PlayerModel> orderedPlayers = this.getPlayerList().stream()
                .sorted((s,v) -> s.getRank().compareTo(v.getRank()))
                .toList();
            
            for (PlayerModel pl : orderedPlayers) {
                result.add("%s | %s | %s".formatted(pl.getUsername(),pl.getGameScore().toString(),pl.getRank().toString()));
            }

        } finally {
            Server.PlayersLock.unlock();
        }



        return String.join("\n", result);
    }

    
}
