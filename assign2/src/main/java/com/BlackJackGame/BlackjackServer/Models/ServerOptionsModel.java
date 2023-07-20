package com.BlackJackGame.BlackjackServer.Models;

import java.util.Date;


public class ServerOptionsModel {

    private String gametype = "simple";
    private Integer timetowait = 30;
    private Integer playerspergame = 2;
    private Date initTime = new Date();
    private int nThreads = 2;
    private String pesistanceFile = "tokens.txt";
    
    public ServerOptionsModel() {}
    
    public ServerOptionsModel(String gametype, Integer playerspergame, Integer timetowait) {
        
        this.gametype = gametype;
        this.playerspergame = playerspergame;
        this.timetowait = timetowait;                
        
    }
    
    public String getGametype() {
        return gametype;
    }

    public void setGametype(String gametype) {
        this.gametype = gametype;
    }

    public Integer getPlayerspergame() {
        return playerspergame;
    }

    public void setPlayerspergame(Integer playerspergame) {
        this.playerspergame = playerspergame;
    }


    public Integer getTimetowait() {
        return timetowait;
    }

    public void setTimetowait(Integer timetowait) {
        this.timetowait = timetowait;
    }

    public long getTimeTarget() {
        return initTime.getTime() + timetowait*1000;
    }


    public long secondsRemaining() {
        Date target = new Date(initTime.getTime() + timetowait*1000);
        
        

        return (target.getTime() - initTime.getTime())/1000;
    }

    public void setThreads(int n) {
        this.nThreads = n;
    }
    public int getThreads() {
        return this.nThreads;
    }

    public void setPersistantFile(String path) {
        this.pesistanceFile = path;
    }
    public String getPersistantFile() {
        return this.pesistanceFile;
    }

    public boolean isGametypeRanked() {
        return !gametype.equals("simple");
    }

    public String toString() {
        return String.format("Threads=%d\nGametype=%s\nTimeLobby=%d\nPlayersPerGame=%d\nPersistanceFile=%s", 
            this.nThreads,
            this.gametype,
            this.timetowait,
            this.playerspergame,
            this.pesistanceFile);
    }
    
}
