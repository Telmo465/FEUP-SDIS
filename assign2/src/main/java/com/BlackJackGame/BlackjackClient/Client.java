package com.BlackJackGame.BlackjackClient;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.BlackJackGame.Models.GameModel;
import com.BlackJackGame.Models.MessageModel;
import com.BlackJackGame.Models.PlayerModel;
import com.BlackJackGame.Models.GameModel.GameStatus;

public class Client {
    public static enum ClientStatus {
        ERROR_CONNECTION,
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        WAITING_RESPONSE,
        WAITING,
        GAME_STARTED,
        GAME_ENDED
    }

    public static Selector selector;

    private ClientStatus status = ClientStatus.NOT_CONNECTED;

    private SocketChannel client;
    
    private GameModel game = new GameModel();    

    private PlayerModel player = new PlayerModel();

    public boolean inGame = false;
    public boolean connected = false;

    private Client() {
        try {
            client = SocketChannel.open(new InetSocketAddress("localhost", 5454));
            client.configureBlocking(false);
            client.register(Client.selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);

        } catch (IOException e) {
            // e.printStackTrace();
            this.setStatus(ClientStatus.ERROR_CONNECTION);
            System.out.println("unable to open socket!!");
        }
    }

    public void setSocket(SocketChannel sock) {
        client = sock;
    }
    
    public void stop() throws IOException {
        client.close();
    }

    public ClientStatus connect(int timeout) {
        if (this.getStatus() != ClientStatus.ERROR_CONNECTION) {
            this.setStatus(ClientStatus.CONNECTING);
            MessageModel msg = new MessageModel(getPlayer().toString());
            msg.setAction(MessageModel.ActionConnect);
            msg.setUsername(this.player.getUsername());  // Set the username field
            msg.setPassword(this.player.getPassword());  // Set the password field

            if (sendMessage(msg.toString()) > 0) {
                this.setStatus(ClientStatus.WAITING_RESPONSE);
            } else {
                setStatus(ClientStatus.NOT_CONNECTED);
            }
        }
        
        return getStatus();
    }

    public ClientStatus handleMessage(MessageModel msg) {
        player.update(msg);
        
        if (!this.connected && !msg.getToken().isBlank()){
            this.connected = true;
            this.setStatus(ClientStatus.CONNECTED);
        } 
        // messages like result:info may come out of order so ignore it
        else if(msg.isRequest() || msg.getResult().equals("success")) {
            game.update(msg);
            if(this.game.getGameStatus() == GameStatus.NOT_STARTED)
            this.setStatus(ClientStatus.CONNECTED);
            else if(this.game.getGameStatus() == GameStatus.WAITING_LOBBY)
                this.setStatus(ClientStatus.WAITING_RESPONSE);
            else if(this.game.getGameStatus() == GameStatus.STARTED)
                this.setStatus(ClientStatus.GAME_STARTED);
            else if(this.game.getGameStatus() == GameStatus.ENDED)
                this.setStatus(ClientStatus.GAME_ENDED);
        }
        
        return getStatus();
    }

    public int sendMessage(String msg) {
        ByteBuffer output = ByteBuffer.wrap(msg.getBytes());
        int bytesSent = 0;
        if (this.getStatus() != ClientStatus.ERROR_CONNECTION) {

            try {
                bytesSent = client.write(output);
                System.out.println("sending=" + msg);

            } catch (IOException e) {
                this.setStatus(ClientStatus.ERROR_CONNECTION);
                this.setStatus(status);
                System.out.println("connection failed: " + getToken());
            }
        }

        return bytesSent;
    }

    public PlayerModel getPlayer() {
        return player;
    }

    public void setPlayer(PlayerModel player) {
        this.player = player;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public String getToken() {
        return this.player.getToken();
    }

    public void setUsername( String u) {
        this.player.setUsername(u);
    }
    public void setPassword( String u) {
        this.player.setPassword(u);
    }

    public String getUsername() {
        return this.player.getUsername();
    }
    public String getPassword() {
        return this.player.getPassword();
    }

    private MessageModel getMessage() {
        MessageModel m = new MessageModel(this.player.toString());
        
        if(this.game.getGameStatus() == GameStatus.STARTED) {
            m.parse(this.game.toString());
        }

        return m;
    }



    public static void main(String[] args) throws IOException, InterruptedException {
        int numberClients = 2;
        String persistenceFile = "tokens.txt";
        boolean interactive = false;
        System.err.println("Arguments: [<interactive> <numberClients> <persistantFile>]");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            if( args.length > 0) 
                interactive = args[0].equals("i");
            if( args.length > 1)
                numberClients = Integer.parseInt(args[1]);
            if( args.length > 2)
                persistenceFile = args[2];
                
        } catch (Exception e) {
            System.err.println("Error! Arguments: [<numberClients> <persistantFile>]");
        }

        Random rad = new Random();       
    

        Client.selector = Selector.open();
        HashMap<String, Client> clients = new HashMap<>();
        List<PlayerModel> players = getPlayersFromPersistance(persistenceFile);

        // generate more playes in case persistance doesnt have enough
        if(players.size() < numberClients) {
            int missing = numberClients - players.size();
            for (int i = 0; i < missing; i++) {
                PlayerModel p = new PlayerModel();
                p.setUsername("%s%s%s%s%s".formatted(
                    (char) (rad.nextInt(26) + 'a'),
                    (char) (rad.nextInt(26) + 'a'),
                    (char) (rad.nextInt(26) + 'a'),
                    (char) (rad.nextInt(26) + 'a'),
                    (char) (rad.nextInt(26) + 'a')));
                p.setPassword("%s%s".formatted(
                    (char) (rad.nextInt(26) + 'a'),
                    (char) (rad.nextInt(26) + 'a')));
                players.add(p);
            }
        }

        List<Client> lstClients = new ArrayList<Client>();

        System.out.println("sending connect messages from %d clients \n".formatted(numberClients));
        // connect clients
        for (int i = 0; i < numberClients; i++) {

            Client client = new Client();

            if( interactive) {
                System.out.println("client starting!");
    
                System.out.print("Enter username for client " + i + ": ");
                String username = reader.readLine();
    
                System.out.print("Enter password for client " + i + ": ");
                String password = reader.readLine();
    
                client.setUsername(username);  // Set the username for each client
                client.setPassword(password);  // Set the password for each client

                Optional<PlayerModel> p = players.stream().filter(s -> s.getUsername().equals(username)).findFirst();

                if( !p.isEmpty()) {
                    PlayerModel pm = p.get();
                    if(pm.getPassword().equals(password))
                        client.setPlayer(pm);
                    else {
                        System.err.println("Password missmatch exiting");
                        System.exit(0);
                    }


                }

 
            } else
                client.setPlayer(players.get(i));
            
            client.connect(1000);
            lstClients.add(client);

        }
        Thread.sleep(rad.nextInt(200));

        System.out.println("waiting for all clients to be connected \n");
        boolean allConnected =false;
        while(!allConnected) {
            List<Client> notConnected = lstClients.stream()
                .filter(s-> s.getStatus() == ClientStatus.WAITING_RESPONSE)
                .toList();

            ClientsResponse(selector,clients,notConnected.size(), 2000);

            lstClients.stream()
                .filter(s-> s.getStatus() == ClientStatus.WAITING_RESPONSE)
                .forEach(s -> {
                    s.connect(1000);
                });


            allConnected = clients.size() == numberClients;
        }
        System.out.println("all clients connected \n");

        // wait for game start
        System.out.println("waitting for game to start \n");
        boolean allInGame =false;
        while(!allInGame) {

            for(Client cl : clients.values()){
                if(cl.getStatus() != ClientStatus.GAME_STARTED)
                    cl.setStatus(ClientStatus.WAITING_RESPONSE);
            }
            
            ClientsResponse(selector,clients,numberClients,2000);
            
            Thread.sleep(rad.nextInt(200));

            allInGame = numberClients == clients.values().stream().filter(s -> s.getStatus() == ClientStatus.GAME_STARTED).count();
        }
        System.out.println("game started \n");

        System.out.println("sending plays \n");
        // send plays
        for (int i = 0; i < 5; i++) {
            for (Client cl : clients.values()) {
                MessageModel msg = cl.getMessage();
                msg.setAction(MessageModel.ActionPlay);
                if(interactive) {
                    System.out.println("Player "+ cl.getUsername() + " send play: ");
                    String t = reader.readLine();
                    msg.setPlay(t);
                } else
                    msg.setPlay("A");
                cl.sendMessage(msg.toString());
                cl.setStatus(ClientStatus.WAITING_RESPONSE);
            }
            Thread.sleep(rad.nextInt(200));

            ClientsResponse(selector,clients,numberClients, 5000);
        }
        System.out.println("plays ended \n");

        // close clients
        System.out.println("closing clients \n");
        List<Client> clientsToDisconnect = clients.values().stream()
            .filter(s -> s.getStatus() != ClientStatus.GAME_ENDED && s.getStatus() != ClientStatus.ERROR_CONNECTION)
            .toList();

        for (Client client : clientsToDisconnect) {
            System.out.println("client disconnect! \n");

            MessageModel msg = client.getMessage();
            msg.setAction(MessageModel.ActionClose);
            if(client.sendMessage(msg.toString()) > 0)
                client.setStatus(ClientStatus.WAITING_RESPONSE);                 
        }
        Thread.sleep(rad.nextInt(200));

        ClientsResponse(selector,clients,clientsToDisconnect.size(),2000);

        Thread.sleep(rad.nextInt(200));

        // close clients
        for (Client client : clients.values()) {
            client.stop();
        }

        System.out.println("Game finished");
    }

    public static void ClientsResponse(Selector selector,HashMap<String,Client> clients,int max_clients, long timeout) throws InterruptedException {
        Random rad = new Random();
        long startTime = new Date().getTime(); 
        
        int waiting_resp = (int) clients.values().stream().filter(s -> s.getStatus() == ClientStatus.WAITING_RESPONSE).count();
        
        if(waiting_resp == 0) {
            waiting_resp = max_clients;
        }
        
        int lastWaitingRespCount = 0;

        while( waiting_resp > 0) {
            try {
                if(lastWaitingRespCount != waiting_resp) {
                    System.out.println("Waiting for response " + waiting_resp + "/" + clients.size());
                    lastWaitingRespCount = waiting_resp;
                }
                selector.select();
                ArrayList<SelectionKey> selectedKeys = new ArrayList<>(selector.selectedKeys());
                selectedKeys.removeIf(s -> ((Client) s.attachment()).getStatus() != ClientStatus.WAITING_RESPONSE);
                waiting_resp = selectedKeys.size();
    
                for (SelectionKey key : selectedKeys) {
                    SocketChannel clSocket = (SocketChannel) key.channel();
                    
                    if (key.isReadable()) {
                        ArrayList<MessageModel> resp = ReadBuffers(clSocket,max_clients);
                        if(resp.size() > 0)
                            HandleListMessages(resp, clients);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Thread.sleep(rad.nextInt(200));

            long checkTime = new Date().getTime(); 
            if( checkTime - startTime > timeout)
                break;
        }
    }

    public static ArrayList<MessageModel> ReadBuffers(SocketChannel socket, int max_clients) {
        ByteBuffer input = ByteBuffer.allocate(512);
        ArrayList<MessageModel> resp = new ArrayList<>();


        String response = new String();

        try {

            long n = 1;
            
            while(n > 0) {
                n = socket.read(input);
                if(n > 0 ){ 
                    input.flip();
                    String t = new String(input.array()).trim();
                    if(!t.isEmpty())
                        response += t;
                    input.flip();
                }
            }

            if(!response.isBlank()) {
                System.out.println("from buffers got: %d bytes".formatted(response.length()));
                resp.addAll(MessageModel.fromMessage(response));
                
                for (MessageModel messageModel : resp) {
                    System.out.println("response=" + messageModel.toString());                    
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resp;
    }

    public static void HandleListMessages(ArrayList<MessageModel> msgs, HashMap<String,Client> clients) {

        Set<SelectionKey> keys = selector.keys();

        for (MessageModel msg : msgs) {
            Client cl = clients.get(msg.getToken());
            
            if(cl == null) {
                for( SelectionKey k : keys) {
                    Client t = (Client) k.attachment();
                    PlayerModel p = t.getPlayer();
                    
                    if(p.getUsername().equals(msg.getUsername()) && msg.getPassword().equals(p.getPassword())) {
                        t.handleMessage(msg);
                        clients.put(t.getToken(), t);
                        break;
                    } 
                }
            } else {                
                cl.handleMessage(msg);
            }
        }
    }

    private static List<PlayerModel> getPlayersFromPersistance(String persistenceFile) {
        List<PlayerModel> players = new ArrayList<PlayerModel>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(persistenceFile));
            String line;
            while ((line = reader.readLine()) != null) {

                String[] info = line.split(";");
                PlayerModel player = new PlayerModel();
                player.setToken(info[0]); 
                player.setUsername(info[1]); 
                player.setPassword(info[2]); 
                player.setRank(Integer.parseInt(info[3]));

                players.add(player);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return players;
    }

}