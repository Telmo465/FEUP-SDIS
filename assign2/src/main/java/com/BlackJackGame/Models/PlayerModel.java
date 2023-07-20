package com.BlackJackGame.Models;

import java.util.ArrayList;

public class PlayerModel {
    protected String token = new String();
    protected Integer rank = 0;
    
    protected String gameId = new String();
    protected String play = new String();
    protected Integer gameScore = 0;
    protected ArrayList<String> cards = new ArrayList<>();
    protected String username = new String();
    protected String password = new String();

    
    public String getToken() {
        return token;
    }
   
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public Integer getRank() {
        return rank;
    }
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    public String getGameId() {
        return gameId;
    }
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    public Integer getGameScore() {
        return gameScore;
    }
    public void setGameScore(Integer score) {
        this.gameScore = score;
    }

    public ArrayList<String> getCards() {
        return cards;
    }
    public void setCards(ArrayList<String> playerCards) {
        this.cards = playerCards;
    }

    public void update(MessageModel resp) {
        this.token = resp.getToken();
        this.gameScore = resp.getRank();
        
        if(!resp.getGameId().isBlank()){
            this.gameId = resp.getGameId();
            this.gameScore = resp.getGameScore();
            this.cards = resp.getPlayerCards();
        } 
    }

    public String toString() {
        ArrayList<String> str = new ArrayList<>();

        if(!token.isBlank()) {
            str.add(String.format("token:%s", token));

            str.add(String.format("rank:%s", rank));

            str.add(String.format("username:%s", username));

            str.add(String.format("password:%s", password));


            if(!gameId.isBlank()) {
                str.add(String.format("gameId:%s", gameId));
                
                str.add(String.format("gameScore:%d", gameScore));
                
                if(cards.size() > 0)
                    str.add(String.format("playerCards:%d", String.join(",",cards)));                
            }
            return String.join( ";" , str);
        }

        return "";
    }
}
