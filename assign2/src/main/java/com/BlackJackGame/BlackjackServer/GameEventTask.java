package com.BlackJackGame.BlackjackServer;

import java.util.Date;
import java.util.TimerTask;

public class GameEventTask extends TimerTask {
    
    private void startRankedGame() {

        if(Server.ServerOptions.getGametype().equals("ranked") && Server.RankGameStarted == false) {
            long currentTime = new Date().getTime();

            if(Server.ServerOptions.getTimeTarget() <= currentTime){
                Server.RankGameStarted = true;
            }
        }

    }


    @Override
    public void run() {

        Server.SavePersistance();

        if(Server.ServerOptions.getGametype().equals("ranked")) {
            if(!Server.RankGameStarted)
                startRankedGame();
        }
        
        Server.GamesLock.lock();
        try {
            
            Server.GameEvent.signal();
        } finally {
            Server.GamesLock.unlock();
        }

        Server.MessageQueueLock.lock();
        try {
            Server.notEmpty.signal();
        } finally {
            Server.MessageQueueLock.unlock();
        }

    }
}
