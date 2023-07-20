package com.BlackJackGame.BlackjackServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.BlackJackGame.BlackjackServer.Models.GameServerModel;
import com.BlackJackGame.BlackjackServer.Models.MessageServerModel;
import com.BlackJackGame.BlackjackServer.Models.PlayerServerModel;
import com.BlackJackGame.BlackjackServer.Models.ServerOptionsModel;



public class Server {
    
    public static boolean RankGameStarted = false;

    public static final Lock PlayersLock = new ReentrantLock();
    public static final HashMap<String,PlayerServerModel> Players = new HashMap<>();

    public static final Lock GamesLock = new ReentrantLock();
    public static final Condition GameEvent = GamesLock.newCondition();
    public static final HashMap<String,GameServerModel> Games = new HashMap<>();

    public static final ReentrantLock MessageQueueLock = new ReentrantLock();
    public static final Condition notEmpty = MessageQueueLock.newCondition();
    public static final ArrayDeque<MessageServerModel> MessageQueue = new ArrayDeque<>();

    public static final ServerOptionsModel ServerOptions = new ServerOptionsModel();

    public static final Timer TimerScheduler = new Timer(true);

    public static void main(String[] args) throws IOException {
        System.out.println("Welcome to BlackJackGame!");
        ProcessArgs(args,ServerOptions);

        System.out.println("Loading persistence");
        LoadPersistance();
        
        System.out.println(String.format("Launching %d threads", ServerOptions.getThreads()));


        ExecutorService executor = Executors.newFixedThreadPool( ServerOptions.getThreads()) ;

        for (int i = 0; i < ServerOptions.getThreads(); i++) {            
            Worker wk = new Worker();
            executor.execute(wk);
        }
        Server.TimerScheduler.scheduleAtFixedRate(new GameEventTask(), 1000, 1000);

        Server server = new Server();

        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();

    }

    private static ServerOptionsModel ProcessArgs(String[] args, ServerOptionsModel serverOptions) {
        
        if(args.length > 0) {
            for( String arg : args) {
    
                String[] options = arg.split("=");
    
                switch(options[0])  {
                    case "--threads":
                        serverOptions.setThreads(Integer.parseInt(options[1]));
                    break;
    
                    case "--simple":
                        serverOptions.setGametype("simple");
                    break;
    
                    case "--ranked":
                        serverOptions.setGametype("ranked");
                    break;
    
                    case "--players-per-game":
                        serverOptions.setPlayerspergame(Integer.parseInt(options[1]));
                    break;
    
                    case "--time-to-wait":
                        serverOptions.setTimetowait(Integer.parseInt(options[1]));
                    break;
    
                    case "--persistent-file":
                        serverOptions.setPersistantFile(options[1]);
                    break;
    
                    case "-h":
                        System.out.println("Options:");
                        System.out.println("\t--threads=N\t Number of worker threads");
                        System.out.println("\t--simple|--ranked\t Type of game simple or ranked");
                        System.out.println("\t--players-per-game=N\t Players per game");
                        System.out.println("\t--time-to-wait=N\t Time to wait in lobby");
                        System.out.println("\t--persistent-file=N\t File to load/save information of players");
                        System.exit(0);
                    break;
    
                    default: 
                    
                }
            }

        } else {
            System.out.println("To customise options check -h");
        }

        System.out.println("Server options:");
        System.out.println(serverOptions.toString());

        return serverOptions;
    }
    
    public static void LoadPersistance() {
        ArrayList<String> playersInfo = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(Server.ServerOptions.getPersistantFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                playersInfo.add(line.trim());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Server.PlayersLock.lock();

            for (String p : playersInfo) {
                String[] info = p.split(";");
                // token;username;password;rank
                try {
                    if( !Server.Players.containsKey(info[0])) {
                        PlayerServerModel player = new PlayerServerModel(info[0], info[1], info[2], Integer. parseInt(info[3]));
                        Server.Players.put(player.getToken(), player);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Unable to create player from token entry: " + p);
                }
            }
        } finally {
            Server.PlayersLock.unlock();
        }



    }

    public static void SavePersistance() {
        ArrayList<String> playersInfo = new ArrayList<>();

        try {
            Server.PlayersLock.lock();

            for (PlayerServerModel p : Server.Players.values()) {
                String[] info = new String[] {
                    p.getToken(),
                    p.getUsername(),
                    p.getPassword(),
                    p.getRank().toString()
                };
                
                playersInfo.add(String.join(";", info));
            }
        } finally {
            Server.PlayersLock.unlock();
        }

        try {
            FileWriter writer = new FileWriter(Server.ServerOptions.getPersistantFile(), false);
            
            for (String s : playersInfo) {
                writer.write(s+"\n");                
            }            
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {

        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", 5454));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println(String.format("Listening on localhost:%d", 5454));

        Listen(selector, serverSocket);
    }

    private void Listen(Selector selector, ServerSocketChannel serverSocket){

        printPlayersStatus();

        while (true) {
            try {
                System.out.println("Waiting for new connection/messages");
                selector.select() ;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        NewConnection(selector, serverSocket, key);
                    }

                    if (key.isReadable()) {
                        ReadFromConnection(key);
                    }

                    iter.remove();
                }
    
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
    }




    private void NewConnection(Selector selector, ServerSocketChannel serverSocket, SelectionKey key) throws IOException {
        
        
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        System.out.println("New Client Connected.");

    }

    private void ReadFromConnection(SelectionKey key) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        ArrayList<MessageServerModel> lstMsg = new ArrayList<>();
        String query = new String();

        String token = "";

        SocketChannel client = (SocketChannel) key.channel();
        Object attch = key.attachment();

        if (attch != null)
            token = (String) attch;

        int r = 1;
        try {

            while(r > 0) {
                r = client.read(buffer);
                if(r > 0 ){ 
                    buffer.flip();
                    String t = new String(buffer.array()).trim();
                    if(!t.isEmpty())
                        query += t;
                    buffer.flip();
                }
                    
            }


            if(!query.isBlank()) {
                System.out.println("from buffers got: %d bytes".formatted(query.length()));
                lstMsg.addAll(MessageServerModel.fromMessage(query,client));
                
                for (MessageServerModel messageModel : lstMsg) {
                    System.out.println("Message Received from client: " + messageModel.toString());                    
                }
            }

        } catch (IOException e) {
            client.close();
            System.out.println("Not accepting client messages anymore");
            MessageServerModel messageModel = new MessageServerModel();
            messageModel.setToken(token);
            messageModel.setAction("close");

            lstMsg.add(messageModel);
            
        }
        if (lstMsg.size() > 0) {
            
            System.out.println(String.format("Sending message to queue from %s",token));
            try {
                Server.MessageQueueLock.lock();
                Server.MessageQueue.addAll(lstMsg);
    
                Server.notEmpty.signal();
    
            } finally {
                Server.MessageQueueLock.unlock();
            }
        }
    }

    protected void sendMessage(MessageServerModel msg) throws IOException {
        SocketChannel sock = msg.getSocket();

        try {
            
            if( sock.isOpen() && sock.isConnected() ) {
    
                String str = msg.toString();
    
                ByteBuffer bf = ByteBuffer.wrap(str.getBytes());
                int rw = sock.write(bf);
    
                System.out.println( String.format("wrote %d bytes expected %d",rw,str.getBytes().length));
            }
        } catch (Exception e) {
            System.err.println("ERROR unable to send message %s\n  errorMessage: %s".formatted(msg.toString(), e.getMessage()));
        }

    }

    public static void printPlayersStatus() {
        List<String> lstTxt = new ArrayList<>();

        lstTxt.add("************ Status **************");
        lstTxt.add("   *********  Games ***********");
        try {
            GamesLock.lock();
            lstTxt.add("number of games: %d".formatted(Games.size()));

            for (GameServerModel game : Games.values()) {
                lstTxt.add(game.getPlayersScore());
            }

        } finally {
            GamesLock.unlock();
        }
        try {
            PlayersLock.lock();

            lstTxt.add("   ********* players *********");
            lstTxt.add(String.format("Number of players: %d",Server.Players.size()));
            

            List<PlayerServerModel> orderedPlayers = Server.Players.values().stream()
                .sorted((s,v) -> v.getRank().compareTo(s.getRank()))
                .toList();

            for (PlayerServerModel player : orderedPlayers) {
                lstTxt.add(String.format("%d - %s",player.getRank(),player.getUsername()));
            }
        } finally {
            PlayersLock.unlock();
        }

        System.out.println(String.join("\n",lstTxt));
    }

}
