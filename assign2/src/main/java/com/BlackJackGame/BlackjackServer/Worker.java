package com.BlackJackGame.BlackjackServer;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.BlackJackGame.BlackjackServer.Models.GameServerModel;
import com.BlackJackGame.BlackjackServer.Models.MessageServerModel;
import com.BlackJackGame.BlackjackServer.Models.PlayerServerModel;
import com.BlackJackGame.Models.MessageModel;
import com.BlackJackGame.Models.PlayerModel;
import com.BlackJackGame.Models.GameModel.GameStatus;

public class Worker extends Server implements Runnable {
    private long threadId = Thread.currentThread().getId();
    @Override
    public void run() {

        threadId = Thread.currentThread().getId();

        System.out.println(String.format("Worker %d reporting for duty!", threadId));
        
        while(true) {
            // check if there are messages to process
            try {
                checkMessageQueue();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // work on game
            checkOnGames();
        }
    }

    private void checkMessageQueue() throws IOException {

        MessageServerModel msg = null;

        try {
            Server.MessageQueueLock.lock();
                
            if(Server.MessageQueue.isEmpty()) {

                try {
                    Server.notEmpty.await();

                } catch (InterruptedException e) {
                    System.out.println(String.format("Worker %d: woke up due to interruption, let's see what message queue has for us", threadId));

                }

            }

            msg = Server.MessageQueue.poll();
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
                Server.MessageQueueLock.unlock();
        }
        
        if(msg != null)
            processMessageQueue(msg);
    }

    private void checkOnGames() {
        boolean ranked = Server.ServerOptions.isGametypeRanked();
        if(ranked) {
            checkOnGamesRanked(); 
            
        } else {
            checkOnGamesSimple();
        }

    }
        
    private void checkOnGamesRanked() {
        // only one lobby
        
        if(Server.RankGameStarted ) {
            processGameStartRanked();
        }
    }

    private void processGameStartRanked() {
        List<MessageServerModel> messages = new ArrayList<MessageServerModel>();
        
        if( Server.RankGameStarted == true) {

                try {
                    Server.PlayersLock.lock();
                    int limit = Server.Players.size()<Server.ServerOptions.getThreads() ?
                        Server.Players.size()/Server.ServerOptions.getThreads() : Server.Players.size();
                    List<PlayerServerModel> notInGame = Server.Players.values().stream()
                        .filter(s -> s.getConnectionState())
                        .filter(s -> s.getGameId().isBlank())
                        .sorted((s,v) -> s.getRank().compareTo(v.getRank()))
                        .limit(limit)
                        .toList();

                    // in ranked mode we need to add all the needed players to start the game, better wait to see if more join
                    if(notInGame.size() > 0 && notInGame.size() % Server.ServerOptions.getPlayerspergame() == 0) {
                        System.out.println(String.format("Worker %d: process game start for %d players", threadId,notInGame.size()));
                        messages.addAll(addPlayersToGames(notInGame));
                    }

                } catch (Exception e) {
                    System.err.println("error: " + e.getMessage());
                }
                finally {
                    Server.PlayersLock.unlock();
                }

                if(messages.size() > 0)
                    sendUpdatePlayers(messages);
        }
    }
    
    private void checkOnGamesSimple() {
        
        
        processStartLobby();

        processStartGame();


    }


    private void processStartLobby() {
        // simple games start lobby countdown as soon as there are enough players
        List<GameServerModel> games = new ArrayList<>();

        try {
            Server.GamesLock.lock();
                

            //get list of games not started and start those that can be started
            List<GameServerModel> gamesToStart = Server.Games.values().stream()
                .filter(s -> s.getGameStatus() == GameStatus.NOT_STARTED)
                .collect(Collectors.toList());

            long currentTime = (new Date()).getTime();

            for (GameServerModel g : gamesToStart) {
                if(g.startLobby()){
                    games.add(g);

                    // only take as much as the thread should take and leave the rest for the other threads
                    if(games.size() >= Server.Games.size()/Server.ServerOptions.getThreads() )
                        break;
                }
            }
    
            if( games.size() > 0) {
                List<MessageServerModel> messages = new ArrayList<MessageServerModel>();
    
                for( GameServerModel game : games) {
                    String gameStr = game.toString();
        
                    try {
                        Server.PlayersLock.lock();
                        for( PlayerModel player: game.getPlayerList()) {
                            PlayerServerModel playerServer = (PlayerServerModel) player;
        
                            MessageServerModel msg = new MessageServerModel(playerServer.toString(), playerServer.getSocket());
                            msg.parse(gameStr);
                            msg.setResult(MessageModel.ResultInfo);
        
                            messages.add(msg);
        
                        }
                    } finally {
                        Server.PlayersLock.unlock();
                    }

                    game.setLastUpdateSent(currentTime);

                }

                if(messages.size() > 0)
                    System.out.println(String.format("Worker %d: process game start lobby for %d players", threadId,messages.size()));
    
                sendUpdatePlayers(messages);
    
            }                                
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Server.GamesLock.unlock();
        }
    }

    private void processStartGame() {
        // simple games start lobby countdown as soon as there are enough players

        List<GameServerModel> games = new ArrayList<>();

        try {

            Server.GamesLock.lock();

            //get list of games not started and start those that can be started
            List<GameServerModel> gamesToStart = Server.Games.values().stream()
                .filter(s -> s.getGameStatus() == GameStatus.WAITING_LOBBY)
                .collect(Collectors.toList());

            long currentTime = (new Date()).getTime();

            for (GameServerModel g : gamesToStart) {
                if(g.canStart()){
                    games.add(g);

                    // only take as much as the thread should take and leave the rest for the other threads
                    if(games.size() >= Server.Games.size()/Server.ServerOptions.getThreads() )
                        break;
                }
            }
    
            if( games.size() > 0) {
                List<MessageServerModel> messages = new ArrayList<MessageServerModel>();
    
                for( GameServerModel game : games) {
                    game.setGameStarted();
                    String gameStr = game.toString();
        
                    try {
                        Server.PlayersLock.lock();
                        for( PlayerModel player: game.getPlayerList()) {
                            PlayerServerModel playerServer = (PlayerServerModel) player;
        
                            MessageServerModel msg = new MessageServerModel(playerServer.toString(), playerServer.getSocket());
                            msg.parse(gameStr);
                            msg.setAction(MessageModel.ActionGameStarted);
        
                            messages.add(msg);
        
                        }
                    } finally {
                        Server.PlayersLock.unlock();
                    }

                    game.setLastUpdateSent(currentTime);
                }

                if(messages.size() > 0)
                    System.out.println(String.format("Worker %d: process game start game for %d players", threadId,messages.size()));
    
                sendUpdatePlayers(messages);
    
            }                                
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Server.GamesLock.unlock();
        }

    }

    private void sendUpdatePlayers(List<MessageServerModel> messages) {

        for (MessageServerModel msg : messages) {
            try {
                //send response
                this.sendMessage(msg);
    
                System.out.println(String.format("Worker %d: sending update to %s with %s", threadId, msg.getToken(),msg.toString()));
    
    
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }


    private void processMessageQueue(MessageServerModel msg) {
        
        if(msg != null) {

            System.out.println(String.format("Worker %d: got a message: %s", threadId, msg.toString()));

            
            switch(msg.getAction()) {
                case MessageServerModel.ActionConnect:
                    processConnectAction(msg);
                break;
                case MessageServerModel.ActionClose:
                    processCloseAction(msg);
                break;
                case MessageServerModel.ActionPlay:
                    processPlayAction(msg);
                break;
                default:

                break;
            }
        } else {
            System.out.println(String.format("Worker %d: no message for me :(", threadId));

        }
    }

    private void processConnectAction(MessageServerModel msg) {
        Server.PlayersLock.lock();
        PlayerServerModel player = null;

        System.out.println(String.format("Worker %d: Processing connect action from %s", threadId, msg.getUsername()));
        
        try {
            
            // Check if the username already exists in the file
            if( !msg.getUsername().isBlank() && !msg.getPassword().isBlank()) {

                List<PlayerServerModel> players = Server.Players.values().stream()
                    .filter(s -> s.getUsername().equals(msg.getUsername()))
                    .toList();

                if( players.size() == 1) {
                    player = players.get(0);
                    if( player.getPassword().equals(msg.getPassword())) {
                        player.setConnectionState(true);
                        player.setSocket(msg.getSocket());
                    } else
                        player = null;

                } else {
                    // Generate a new token for the new player
                    String token = UUID.randomUUID().toString();
                    player = new PlayerServerModel(token, msg.getSocket(), msg.getUsername(), msg.getPassword());
                    System.out.println(String.format("Worker %d: Creating new player %s ", threadId, player.toString()));
                    
                }
                if(player != null) {
                    // Proceed with processing the connect action as a new player
                    Server.Players.put(player.getToken(), player);
                    addPlayerToGame(player);
                }

            } else
                System.err.println("connection request has no username or password: " + msg.toString());                

        } 
        catch (Exception e) {
            e.printStackTrace();
        } 
        finally {
            Server.PlayersLock.unlock();
        }
           
        try {
            // Send response
            MessageServerModel result = new MessageServerModel(msg.getSocket());

            if( player != null){
                result.setAction(MessageServerModel.ResultSuccess);
                result.parse(player.toString());
            }
            else
                result.setAction(MessageServerModel.ResultFail);

            this.sendMessage(result);
            System.out.println(String.format("Worker %d: responded to %s with %s", threadId, result.getToken(), result.toString()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processCloseAction(MessageServerModel msg) {
        PlayerServerModel player = null;
        SocketChannel oldSocket = null;
        ArrayList<MessageServerModel> messages = new ArrayList<>();


        System.out.println(String.format("Worker %d: Processing close action from %s", threadId, msg.getToken()));

        // check if the client already exists
        
        Server.PlayersLock.lock();

        try {

            if(Server.Players.containsKey(msg.getToken())){
                player = Server.Players.get(msg.getToken());

                if(player.getGameId().isBlank()) {
                    MessageServerModel result = new MessageServerModel(player.toString(), msg.getSocket());
                    result.setResult(MessageServerModel.ResultSuccess);
                    messages.add(result);
                } else {
                    messages.addAll(stopGame(player.getGameId()));
                }



                oldSocket = player.getSocket();
                
                player.setConnectionState(false);

                player.setSocket(null);

            } else {
                player = new PlayerServerModel(msg.getToken(), null, msg.getUsername(), msg.getPassword());
                Server.Players.put(player.getToken(), player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Server.PlayersLock.unlock();
        }

        try {
            //send response
            if(messages.size() > 0) {
                sendUpdatePlayers(messages);
                System.out.println(String.format("Worker %d: responded to Close action from  %s", threadId, msg.getToken()));
            }

            // deal with old socket
            if(oldSocket != null) {
                if(oldSocket.isConnected()){
                    oldSocket.close();
                }
            }                

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processPlayAction(MessageServerModel msg) {
        PlayerServerModel player = null;
        SocketChannel socket = null;
        
        boolean success = true;
        
                System.out.println(String.format("Worker %d: Processing play action from %s", threadId, msg.getToken()));

        // check if the client already exists
        Server.PlayersLock.lock();

        try {
            if(Server.Players.containsKey(msg.getToken())){
                player = Server.Players.get(msg.getToken());

                socket = player.getSocket();
                player.setRank(player.getRank()+1);

            } else {
                System.out.println("player not in players: " + msg.toString());
                socket = msg.getSocket();
                success = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Server.PlayersLock.unlock();
        }

        try {
            MessageServerModel result = null;
            //send response
            if(success) {
                result = new MessageServerModel(player.toString(),socket);
                result.setResult(MessageServerModel.ResultSuccess);
            } else {
                result = new MessageServerModel(msg.toString(),socket);
                result.setResult(MessageServerModel.ResultFail);
            }
            this.sendMessage(result);

            System.out.println(String.format("Worker %d: responded to %s with %s", threadId, result.getToken(),result.toString()));
          

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPlayerToGame(PlayerServerModel player) {
        
        if( Server.ServerOptions.getGametype().equals("ranked")
            && Server.ServerOptions.secondsRemaining() > 0) {
                // we can only add players to games if the game
                return;
        }

        Server.GamesLock.lock();
        try {
            List<GameServerModel> games = Server.Games.values().stream()
                .filter( s -> s.getGameStatus() == GameStatus.NOT_STARTED)
                .filter( s -> s.getPlayersCount() < Server.ServerOptions.getPlayerspergame())
                .collect(Collectors.toList());
            
            GameServerModel game = new GameServerModel(Server.ServerOptions.getTimetowait());;
            
            if(games.size() > 0) {
                game = games.get(0);
            }

            game.addPlayer(player);
            long currentTime = (new Date()).getTime();
            game.setLastUpdateSent(currentTime);

            Server.Games.put(game.getGameId(), game);

        } finally {
            Server.GamesLock.unlock();
        }
    }

    private ArrayList<MessageServerModel> addPlayersToGames(List<PlayerServerModel> players) {
        ArrayList<MessageServerModel> messages = new ArrayList<>();

        Server.GamesLock.lock();
        try {
            long currentTime = (new Date()).getTime();

            if(players.size()>=Server.ServerOptions.getPlayerspergame()) {
                int nGames = players.size()/Server.ServerOptions.getPlayerspergame();
                // in case we have more threads than players
                if( nGames == 0)
                    nGames = 1;
    
                for( int i = 0 ; i<nGames ; i++) {
                    GameServerModel game = new GameServerModel(Server.ServerOptions.getTimetowait());;
                    game.setGameStarted();
                    
                    for (int j = 0; j < Server.ServerOptions.getPlayerspergame(); j++) {                                    
                        PlayerServerModel p = players.get(j + i*Server.ServerOptions.getPlayerspergame());
                        p.setLastUpdateSent(currentTime);
                        game.addPlayer(p);
    
                        MessageServerModel m = new MessageServerModel(p.toString(), p.getSocket());
                        m.parse(game.toString());
                        m.setAction(MessageModel.ActionGameStarted);
                        messages.add(m);
                    }
    
                    game.setLastUpdateSent(currentTime);
    
                    Server.Games.put(game.getGameId(), game);
                }
            }
        } finally {
            Server.GamesLock.unlock();
        }

        return messages;
    }

    private ArrayList<MessageServerModel> stopGame(String gameId) {
        ArrayList<MessageServerModel> messages = new ArrayList<>();

        try {
            Server.GamesLock.lock();

            GameServerModel game = Server.Games.get(gameId);

            if(game != null) {
                
                if( game.getGameStatus() != GameStatus.ENDED) {
                    System.out.println("Ending game id %s".formatted(gameId));
                    game.setGameEnded();
                    String gameScore = game.getPlayersScore();
                    System.out.println("Game %s ended with the following score:\n%s".formatted(gameId,gameScore));

                    try {
                        Server.PlayersLock.lock();

                        for (PlayerModel pl : game.getPlayerList()) {
                            PlayerServerModel ps = (PlayerServerModel) pl;

                            MessageServerModel msg = new MessageServerModel(ps.toString(), ps.getSocket());
                            msg.parse(game.toString());
                            msg.setAction(MessageModel.ActionGameEnd);

                            messages.add(msg);                            
                        }
                    } finally {
                        Server.PlayersLock.unlock();
                    }
                }
            }
        } finally {
            Server.GamesLock.unlock();
        }

        Server.printPlayersStatus();

        return messages;
    }

}
