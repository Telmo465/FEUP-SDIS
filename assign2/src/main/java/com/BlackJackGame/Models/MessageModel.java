package com.BlackJackGame.Models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageModel {
    public static final String ActionConnect = "connect";
    public static final String ActionClose = "close";
    public static final String ActionReconnect = "reconnect";
    public static final String ActionPlay = "play";
    public static final String ActionGameStarted = "gameStarted";
    public static final String ActionGameEnd = "gameEnd";
    public static final String ResultSuccess = "success";
    public static final String ResultFail = "fail";
    public static final String ResultInfo = "info";
    // message type
    private boolean request = true;
    // message parameters
    private String action = new String();
    private String result = new String();
    private String token = new String();
    private String username = new String();
    private String password = new String();
    private String play = new String();
    private String gameId = new String();
    private Integer rank = 0;
    private String gameStatus = "notStarted";
    private long timeToWait = 0;
    private long timeTarget = 0;
    private Integer gameScore = 0;
    private Integer gamePlayers = 0;
    private Integer playerTurn = 0;
    private Integer gameTurn = 0;
    private ArrayList<String> dealerCards = new ArrayList<>();
    private ArrayList<String> playerCards = new ArrayList<>();
    private ArrayList<Integer> gamePlayersScore = new ArrayList<>();
    private ArrayList<Integer> gamePlayersNumberCards = new ArrayList<>();
    private ArrayList<ArrayList<String>> allPlayersCards = new ArrayList<>();
    

    public static ArrayList<MessageModel> fromMessage(String message) {
        ArrayList<MessageModel> arr = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\(([^\\)]+)\\)");
        Matcher m = pattern.matcher(message);
        
        while(m.find()) {
            MessageModel result = new MessageModel();
            result.parse(m.group());

            arr.add(result);
        }

        if(arr.size() == 0) {
            MessageModel result = new MessageModel();
            result.parse(message);
            arr.add(result);
        }
        
        return arr;
    }

    public MessageModel() { }

    public MessageModel(String message) {
        this.parse(message);
    }

    public void parse(String message) {
        message=message.replace("(", "");
        message=message.replace(")", "");
        
        ArrayList<String> tempArrStrings;
        ArrayList<Integer> tempArrIntegers;

        
        String[] messages = message.split(";");
        
        for (String msg : messages) {
            
            String[] property = msg.split(":");

            switch (property[0]) {
                case "action":
                    this.setAction(property[1]);
                break;
                case "result":
                this.setResult(property[1]);
                break;
                case "token":
                this.setToken(property[1]);
                break;
                case "username":
                this.setUsername(property[1]);
                break;
                case "password":
                this.setPassword(property[1]);
                break;
                case "play":
                this.setPlay(property[1]);
                break;
                case "gameId":
                this.setGameId(property[1]);
                break;
                case "gameStatus":
                this.setGameStatus(property[1]);
                break;
                case "timeToWait":
                this.setTimeToWait(Integer.parseInt(property[1]));
                break;
                case "timeTarget":
                this.setTimeTarget(Long.parseLong(property[1]));
                break;
                case "rank":
                this.setRank(Integer.parseInt(property[1]));
                break;
                case "gameScore":
                this.setGameScore(Integer.parseInt(property[1]));

                break;
                case "gamePlayers":
                this.setGamePlayers(Integer.parseInt(property[1]));

                break;
                case "playerTurn":
                this.setPlayerTurn(Integer.parseInt(property[1]));

                break;
                case "gameTurn":
                this.setGameTurn(Integer.parseInt(property[1]));

                break;
                case "dealerCards":
                    tempArrStrings = this.parseListStrings(property[1]);
                    this.setDealerCards(tempArrStrings);
                break;
                case "playerCards":
                    tempArrStrings = this.parseListStrings(property[1]);
                    this.setDealerCards(tempArrStrings);
                    
                break;
                case "gamePlayersScore":
                    tempArrIntegers = this.parseListInts(property[1]);
                    this.setGamePlayersScore(tempArrIntegers);

                break;
                case "gamePlayersNCards":
                    tempArrIntegers = this.parseListInts(property[1]);
                    this.setGamePlayersNumberCards(tempArrIntegers);

                break;
                case "allPlayersCards":
                    this.parseAllPlayersCards(property[1]);
                break;
            
                default:
                    break;
            }
            
        }
    }

    // exp "a,v,b,g,g,h"
    private ArrayList<String> parseListStrings(String msg) {
        ArrayList<String> result = new ArrayList<>(Arrays.asList(msg.split(",")));
        return result;
    }

    // exp "1,2,3,4,5,6"
    private ArrayList<Integer> parseListInts(String msg) {
        ArrayList<Integer> result = new ArrayList<Integer>();

        for (String item : msg.split(",")) {
            result.add(Integer.parseInt(item));
        }
        return result;
    }

    // exp "a,1,4|q,k|1,3"
    private void parseAllPlayersCards(String msg) {

        ArrayList<ArrayList<String>> result = new ArrayList<>();
        
        for (String item : msg.split("|")) {
            result.add(new ArrayList<>(Arrays.asList(item.split(","))));
        }


        this.setAllPlayersCards(result);
    }
    
    // getter seetter

    public boolean isRequest() {
        return request;
    }

    public void setRequest(boolean request) {
        this.request = request;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
        this.setRequest(true);
    }

    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
        this.setRequest(false);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlay() {
        return play;
    }

    public void setPlay(String play) {
        this.play = play;
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

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
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

    public ArrayList<String> getDealerCards() {
        return dealerCards;
    }

    public void setDealerCards(ArrayList<String> dealerCards) {
        this.dealerCards = dealerCards;
    }

    public ArrayList<String> getPlayerCards() {
        return playerCards;
    }

    public void setPlayerCards(ArrayList<String> playerCards) {
        this.playerCards = playerCards;
    }

    public Integer getGameScore() {
        return gameScore;
    }

    public void setGameScore(Integer gameScore) {
        this.gameScore = gameScore;
    }

    public Integer getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(Integer gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public ArrayList<Integer> getGamePlayersScore() {
        return gamePlayersScore;
    }

    public void setGamePlayersScore(ArrayList<Integer> gamePlayersScore) {
        this.gamePlayersScore = gamePlayersScore;
    }

    public ArrayList<Integer> getGamePlayersNumberCards() {
        return gamePlayersNumberCards;
    }

    public void setGamePlayersNumberCards(ArrayList<Integer> gamePlayersNumberCards) {
        this.gamePlayersNumberCards = gamePlayersNumberCards;
    }

    public Integer getPlayerTurn() {
        return playerTurn;
    }

    public void setPlayerTurn(Integer playerTurn) {
        this.playerTurn = playerTurn;
    }

    public Integer getGameTurn() {
        return gameTurn;
    }

    public void setGameTurn(Integer gameTurn) {
        this.gameTurn = gameTurn;
    }
    
    public ArrayList<ArrayList<String>> getAllPlayersCards() {
        return allPlayersCards;
    }

    public void setAllPlayersCards(ArrayList<ArrayList<String>> allPlayersCards) {
        this.allPlayersCards = allPlayersCards;
    }

    public String toString() {
        StringBuilder sb;

        ArrayList<String> properties = new ArrayList<>();

        //action or result
        if(this.request && !this.action.isEmpty()) {
            properties.add(String.format("action:%s", this.action));
        } else if(!this.result.isEmpty()){
            properties.add(String.format("result:%s", this.result));
        }
        

        // token
        if(!this.token.isEmpty()){
            properties.add(String.format("token:%s", this.token));
        }

        // username
        if(!this.username.isEmpty()){
            properties.add(String.format("username:%s", this.username));
        }

        // password
        if(!this.password.isEmpty()){
            properties.add(String.format("password:%s", this.password));
        }
        

        // play
        if(!this.play.isEmpty()){
            properties.add(String.format("play:%s", this.play));
        }
        

        // rank
        properties.add(String.format("rank:%d", this.rank));
        

        // gameId
        if(!this.gameId.isEmpty()){
            properties.add(String.format("gameId:%s", this.gameId));

            // gameStatus
            properties.add(String.format("gameStatus:%s", this.gameStatus));

            // timeToWait
            properties.add(String.format("timeToWait:%d", this.timeToWait));
            
            // timeTarget
            properties.add(String.format("timeTarget:%d", this.timeTarget));

            // gameScore
            properties.add(String.format("gameScore:%d", this.gameScore));

            
    
            // gamePlayers
            properties.add(String.format("gamePlayers:%d", this.gamePlayers));
            
    
            // playerTurn
            properties.add(String.format("playerTurn:%d", this.playerTurn));
            
    
            // gameTurn
            properties.add(String.format("gameTurn:%d", this.gameTurn));
            
    
            // dealerCards
            if(!this.dealerCards.isEmpty()){

                properties.add(String.format("dealerCards:%s", String.join(",", this.dealerCards)));
            }
            
    
            // playerCards
            if(!this.playerCards.isEmpty()){
                properties.add(String.format("playerCards:%s", this.playerCards));
            }
            
    
            // gamePlayersScore
            if(!this.gamePlayersScore.isEmpty()){
                sb = new StringBuilder();
                sb.append("gamePlayersScore:");
                sb.append( String.join(",", Arrays.toString(this.gamePlayersScore.toArray())));
                properties.add(sb.toString());
            }
            
    
            // gamePlayersNumberCards
            if(!this.gamePlayersNumberCards.isEmpty()){
                sb = new StringBuilder();
                sb.append("gamePlayersNumberCards:");
                sb.append( String.join(",", Arrays.toString(this.gamePlayersNumberCards.toArray())));
                properties.add(sb.toString());
            }
            
    
            // allPlayersCards
            if(!this.allPlayersCards.isEmpty()){
                sb = new StringBuilder();
                sb.append("allPlayersCards:");

                ArrayList<String> arr = new ArrayList<>();

                for (ArrayList<String> player_cards : this.allPlayersCards) {                    
                    arr.add(String.join(",", player_cards));
                }

                sb.append( String.join("|", String.join(",", arr)));

                properties.add(sb.toString());
            }
        }
        

        

        sb = new StringBuilder();

        sb.append("(");
        sb.append(String.join(";",properties));
        sb.append(")");

        return sb.toString();
    }


    
}
