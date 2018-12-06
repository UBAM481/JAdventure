package com.jadventure.game.menus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.jadventure.game.DeathException;
import com.jadventure.game.Game;
import com.jadventure.game.GameModeType;
import com.jadventure.game.JAdventure;
import com.jadventure.game.QueueProvider;
import com.jadventure.game.entities.Player;

/**
 * The first menu displayed on user screen
 * @see JAdventure
 * This menu lets the player choose whether to load an exiting game,
 * start a new one, or exit to the terminal.
 */
public class MainMenu extends Menus implements Runnable {
    private static final String ACCESS_TOKEN = "rzo2fCEbz3AAAAAAAAAACVdzUZ86JX7Ul1Cd2m9qHDCu3G6Q5GZBUwwU1WXdCIDn";
    public MainMenu(Socket server, GameModeType mode){
        QueueProvider.startMessenger(mode, server);
    }

    public MainMenu() {
        start();
    }
    
    public void run() {
        start();
    }

    public void start() {
        this.menuItems.add(new MenuItem("Start", "Starts a new Game", "new"));
        this.menuItems.add(new MenuItem("Load", "Loads an existing Game"));
        this.menuItems.add(new MenuItem("Delete", "Deletes an existing Game"));
        this.menuItems.add(new MenuItem("Scoreboard", "See the best players around the world"));
        this.menuItems.add(new MenuItem("Exit", null, "quit"));
        
        while(true) {
            try {
                MenuItem selectedItem = displayMenu(this.menuItems);
                boolean exit = testOption(selectedItem);
                if (!exit) {
                    break;
                }
            } catch (DeathException e) {
                if (e.getLocalisedMessage().equals("close")) {
                    break;
                }
            }
        }
        QueueProvider.offer("EXIT");
    
    }

    private static boolean testOption(MenuItem m) throws DeathException {
        String key = m.getKey();
        switch (key){
            case "start":
                new ChooseClassMenu();
                break;
            case "scoreboard":
                displayScoreboard();
                return true ;
            case "exit":
                QueueProvider.offer("Goodbye!");
                return false;
            case "load":
                listProfiles();
                Player player = null;
                boolean exit = false;
                while (player == null) {
                    key = QueueProvider.take();
                    String[] keys = key.split(" ") ;
                    if( keys.length > 1 && keys[0].equals("cloud") && cloudContains( keys[1] )){
                        if( downloadProfile( keys[1] ) )
                            key = keys[1] ;

                    }
                    if (Player.profileExists(key)) {
                        player = Player.load(key);
                    }
                    else if (key.equals("exit") || key.equals("back")) {
                        exit = true;
                        break;
                    } else {
                        QueueProvider.offer("That user doesn't exist. Try again.");
                    }
                }
                if (exit) {
                    return true;
                }
                new Game(player, "old");
                break;
            case "delete":
                listProfiles();
                exit = false;
                while (!exit) {
                    key = QueueProvider.take().trim() ;
                    if (Player.profileExists(key)) {
                        String profileName = key;
                        QueueProvider.offer("Are you sure you want to delete " + profileName + "? y/n");
                        key = QueueProvider.take();
                        if (key.equals("y")) {
                            File profile = new File("json/profiles/" + profileName);
                            deleteDirectory(profile);
                            QueueProvider.offer("Profile Deleted");
                            return true;
                        } else {
                            listProfiles();
                            QueueProvider.offer("\nWhich profile do you want to delete?");
                        }
                    } else if (key.equals("exit") || key.equals("back")) {
                        exit = true;
                        break;
                    } else {
                        QueueProvider.offer("That user doesn't exist. Try again.");
                    }
                }
                break;
        }
        return true;
    }

    private static void displayScoreboard() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        ArrayList<Player> scoreboard = new ArrayList<Player>() ;
        QueueProvider.offer("Getting best players around the world. Get ready to level the competition up!");
        File scoreboardDirectory = new File("json/scoreboard");
        if( !scoreboardDirectory.exists() ) scoreboardDirectory.mkdirs() ;
        try {
            ListFolderResult result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    String username =  metadata.getPathLower().substring( 1 , metadata.getPathLower().length()-5 ) ;
                    if( username.equals("test") ) continue ;
                    downloadProfile( username, "json/scoreboard/" + username + ".json");

                    scoreboard.add( Player.load( username , true ) );
                }

                if (!result.getHasMore()) {
                    break;
                }

                result = client.files().listFolderContinue(result.getCursor());
            }
        }catch ( Exception e ){
            QueueProvider.offer("Can not display scoreboard at the moment. Please try again later.");
            return;
        }

        Player[] scoreboardArr = new Player[scoreboard.size()] ;
        for( int i=0 ; i<scoreboard.size() ; i++ ) scoreboardArr[i] = scoreboard.get(i) ;

        Arrays.sort(scoreboardArr, new Comparator<Player>() {
            @Override
            public int compare(Player player, Player t1) {
                return t1.getXP() - player.getXP() ;
            }
        });

        QueueProvider.offer("\n==========  TOP 10 PLAYERS AROUND THE WORLD ==========");
        QueueProvider.offer("Rank   Name             Character Type     XP     Gold");
        for( int i=0 ; i<scoreboardArr.length && i < 10 ; i++ ){
            Player player = scoreboardArr[i] ;
            String line = "  " + (i+1) + ".   " + player.getName() ;
            for( int j=0 ; j < 17-player.getName().length(); j++ ) line += " " ;
            line += player.getCurrentCharacterType() ;
            for( int j=0 ; j < 19 - player.getCurrentCharacterType().length(); j++ ) line += " " ;
            line += player.getXP() ;
            for( int j=0 ; j < 7 - (""+player.getXP()).length(); j++ ) line += " " ;
            line += player.getGold() ;
            QueueProvider.offer( line ) ;
        }
        QueueProvider.offer("");
        return;
    }

    private static void downloadProfile( String key , String filePath ){
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        File file = new File( filePath );

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                return;
            }
        }
        try {
            OutputStream out = new FileOutputStream( filePath );
            client.files().downloadBuilder("/" + key + ".json").download( out );
        }catch (Exception e){
            return;
        }
        return;
    }


    private static boolean downloadProfile( String key ){
//        QueueProvider.offer("downloading file: " + "/" + key + ".json" + " to " + "json/profiles/" + key + "/" + key + "_profile.json");
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        if( Player.profileExists( key ) ){
            QueueProvider.offer("This operation will override your local profile:" + key);
            QueueProvider.offer("Would you like to continue?(y/n)");
            String ans = QueueProvider.take().trim().toLowerCase() ;
            if( ans.length() == 0 || ans.charAt(0) != 'y' ){
                QueueProvider.offer("Loading canceled");
                return false;
            }
        }else {

            File parentFolder = new File("json/profiles/" + key ) ;
            File file = new File("json/profiles/" + key + "/" + key + "_profile.json");
            if( !parentFolder.exists() )
                parentFolder.mkdirs() ;
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    QueueProvider.offer("Can not load your profile at the moment. Please try again later.");
                    return false;
                }
            }
        }

        try {
            OutputStream out = new FileOutputStream("json/profiles/" + key + "/" + key + "_profile.json");
            client.files().downloadBuilder("/" + key + ".json").download( out );
        }catch (Exception e){
            QueueProvider.offer("Can not load your profile at the moment. Please try again later.");
            return false ;
//            QueueProvider.offer( e.getMessage() + " | " + e.getLocalizedMessage() );
        }
        QueueProvider.offer("Profile files downloaded successfully");
        return true ;
    }
    public static boolean cloudContains( String key ){
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        try {
            ListFolderResult result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    if( key.equals( metadata.getPathLower().substring( 1 , metadata.getPathLower().length()-5 ) ) )
                        return true ;
                }

                if (!result.getHasMore()) {
                    break;
                }

                result = client.files().listFolderContinue(result.getCursor());
            }
        }catch ( Exception e ){
            QueueProvider.offer("Can not reach your profile at the moment. Please try again later.");
            return false;
        }

        return false ;

    }
    private static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    private static void listProfiles() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        QueueProvider.offer("Local profiles:");
        try {
                File file = new File("json/profiles");
                String[] profiles = file.list();
        
            int i = 1;
            for (String name : profiles) {
                if (new File("json/profiles/" + name).isDirectory()) {
                    QueueProvider.offer("  " + name);
                }
                    i += 1;
            }
            QueueProvider.offer("Online profiles: (Please type cloud <profile name> to avoid ambiguity)");
            try {
                ListFolderResult result = client.files().listFolder("");
                while (true) {
                    for (Metadata metadata : result.getEntries()) {
                        QueueProvider.offer( " " + metadata.getPathLower().substring( 1 , metadata.getPathLower().length()-5 ) );
                    }

                    if (!result.getHasMore()) {
                        break;
                    }

                    result = client.files().listFolderContinue(result.getCursor());
                }
            }catch ( Exception e){
                QueueProvider.offer("Your profile can not be uploaded at the moment. Please try again later.");
                return ;
            }
            QueueProvider.offer("\nWhat is the name of the avatar you want to select? Type 'back' to go back");
        } catch (NullPointerException e) {
            QueueProvider.offer("No profiles found. Type \"back\" to go back.");
        }
    }
}
