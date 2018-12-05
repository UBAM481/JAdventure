package com.jadventure.game.entities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.jadventure.game.DeathException;
import com.jadventure.game.Game;
import com.jadventure.game.GameBeans;
import com.jadventure.game.QueueProvider;
import com.jadventure.game.items.Item;
import com.jadventure.game.items.ItemStack;
import com.jadventure.game.items.Storage;
import com.jadventure.game.menus.BattleMenu;
import com.jadventure.game.menus.MainMenu;
import com.jadventure.game.monsters.Monster;
import com.jadventure.game.navigation.Coordinate;
import com.jadventure.game.navigation.ILocation;
import com.jadventure.game.navigation.LocationType;
import com.jadventure.game.repository.ItemRepository;
import com.jadventure.game.repository.LocationRepository;

/**
 * This class deals with the player and all of its properties.
 * Any method that changes a character or interacts with it should
 * be placed within this class. If a method deals with entities in general or
 * with variables not unique to the player, place it in the entity class.
 */
public class Player extends Entity {
    // @Resource
    protected static ItemRepository itemRepo = GameBeans.getItemRepository();
    protected static LocationRepository locationRepo = GameBeans.getLocationRepository();
    private ILocation location;
    private int xp;
    private int manaPool;
    private boolean auraOfValor;
    /** Player type */
    private String type;
    private static HashMap<String, Integer>characterLevels = new HashMap<String, Integer>();
    private Game game;

    private Pet pet;

    private static final String ACCESS_TOKEN = "rzo2fCEbz3AAAAAAAAAACVdzUZ86JX7Ul1Cd2m9qHDCu3G6Q5GZBUwwU1WXdCIDn";

    public Player() {
    	pet = new Pet();
    }
    public Pet getPet() {
    	return pet;
    }

    protected static void setUpCharacterLevels() {
        characterLevels.put("Sewer Rat", 5);
        characterLevels.put("Recruit", 3);
        characterLevels.put("Syndicate Member", 4);
        characterLevels.put("Brotherhood Member", 4);
    }

    public HashMap<String, Integer> getCharacterLevels() {
        return characterLevels;
    }

    public void setCharacterLevels(HashMap<String, Integer> newCharacterLevels) {
        this.characterLevels = newCharacterLevels;
    }

    public String getCurrentCharacterType() {
        return this.type;
    }
    
    public void setCurrentCharacterType(String newCharacterType) {
        this.type = newCharacterType;
    }

    public void setCharacterLevel(String characterType, int level) {
        this.characterLevels.put(characterType, level);
        pet.setLevel(level);
    }

    public int getCharacterLevel(String characterType) {
        int characterLevel = this.characterLevels.get(characterType);
        return characterLevel;
    }
    public int getManaPool(){ return manaPool;}
    public void setManaPool(int mana){ manaPool=mana;}
    public boolean getAuraOfValor() { return auraOfValor;}
    public void setAuraOfValor(boolean auraOfValor){ this.auraOfValor=auraOfValor;}

    protected static String getProfileFileName(String name) {
        return "json/profiles/" + name + "/" + name + "_profile.json";
    }

    public static boolean profileExists(String name) {
        File file = new File(getProfileFileName(name));
        return file.exists();
    }

    public static Player load(String name) {
        player = new Player();
        JsonParser parser = new JsonParser();
        String fileName = getProfileFileName(name);
        try {
            Reader reader = new FileReader(fileName);
            JsonObject json = parser.parse(reader).getAsJsonObject();
            player.setName(json.get("name").getAsString());
            player.setHealthMax(json.get("healthMax").getAsInt());
            player.setHealth(json.get("health").getAsInt());
            player.setArmour(json.get("armour").getAsInt());
            player.setDamage(json.get("damage").getAsInt());
            player.setLevel(json.get("level").getAsInt());
            player.setXP(json.get("xp").getAsInt());
            player.setStrength(json.get("strength").getAsInt());
            player.setIntelligence(json.get("intelligence").getAsInt());
            player.setDexterity(json.get("dexterity").getAsInt());
            player.setLuck(json.get("luck").getAsInt());
            player.setStealth(json.get("stealth").getAsInt());
            player.setCurrentCharacterType(json.get("type").getAsString());
            
            player.getPet().setLevel(player.getLevel());
            player.getPet().setHealth(player.getPet().calculateHealth());
            player.getPet().setDamage(player.getPet().calculateDamage());

            player.setManaPool(50);
            player.setAuraOfValor(true);
            
            HashMap<String, Integer> charLevels = new Gson().fromJson(json.get("types"), new TypeToken<HashMap<String, Integer>>(){}.getType());
            player.setCharacterLevels(charLevels);
            if (json.has("equipment")) {
                Map<String, EquipmentLocation> locations = new HashMap<>();
                locations.put("head", EquipmentLocation.HEAD);
                locations.put("chest", EquipmentLocation.CHEST);
                locations.put("leftArm", EquipmentLocation.LEFT_ARM);
                locations.put("leftHand", EquipmentLocation.LEFT_HAND);
                locations.put("rightArm", EquipmentLocation.RIGHT_ARM);
                locations.put("rightHand", EquipmentLocation.RIGHT_HAND);
                locations.put("bothHands", EquipmentLocation.BOTH_HANDS);
                locations.put("bothArms", EquipmentLocation.BOTH_ARMS);
                locations.put("legs", EquipmentLocation.LEGS);
                locations.put("feet", EquipmentLocation.FEET);
                HashMap<String, String> equipment = new Gson().fromJson(json.get("equipment"), new TypeToken<HashMap<String, String>>(){}.getType());
               Map<EquipmentLocation, Item> equipmentMap = new HashMap<>();
               for(Map.Entry<String, String> entry : equipment.entrySet()) {
                   EquipmentLocation el = locations.get(entry.getKey());
                   Item i = itemRepo.getItem(entry.getValue());
                   equipmentMap.put(el, i);
               }
               player.setEquipment(equipmentMap);
            }
            if (json.has("items")) {
                HashMap<String, Integer> items = new Gson().fromJson(json.get("items"), new TypeToken<HashMap<String, Integer>>(){}.getType());
                ArrayList<ItemStack> itemList = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    String itemID = entry.getKey();
                    int amount = entry.getValue();
                    Item item = itemRepo.getItem(itemID);
                    ItemStack itemStack = new ItemStack(amount, item);
                    itemList.add(itemStack);
                }
                float maxWeight = (float)Math.sqrt(player.getStrength()*300);
                player.setStorage(new Storage(maxWeight, itemList));
            }
            Coordinate coordinate = new Coordinate(json.get("location").getAsString());
            locationRepo = GameBeans.getLocationRepository(player.getName());
            player.setLocation(locationRepo.getLocation(coordinate));
            reader.close();
            setUpCharacterLevels();
        } catch (FileNotFoundException ex) {
            QueueProvider.offer( "Unable to open file '" + fileName + "'.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return player;
    }

    // This is known as the singleton pattern. It allows for only 1 instance of a player.
    private static Player player;
    
    public static Player getInstance(String playerClass){
        player = new Player();
        JsonParser parser = new JsonParser();
        String fileName = "json/original_data/npcs.json";
        try {
            Reader reader = new FileReader(fileName);
            JsonObject npcs = parser.parse(reader).getAsJsonObject().get("npcs").getAsJsonObject();
            JsonObject json = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : npcs.entrySet()) {
                if (entry.getKey().equals(playerClass)) {
                    json = entry.getValue().getAsJsonObject();
                }
            }

            player.setName(json.get("name").getAsString());
            player.setHealthMax(json.get("healthMax").getAsInt());
            player.setHealth(json.get("health").getAsInt());
            player.setArmour(json.get("armour").getAsInt());
            player.setDamage(json.get("damage").getAsInt());
            player.setLevel(json.get("level").getAsInt());
            
            player.setXP(json.get("xp").getAsInt());
            player.setStrength(json.get("strength").getAsInt());
            player.setIntelligence(json.get("intelligence").getAsInt());
            player.setDexterity(json.get("dexterity").getAsInt());
            
            player.getPet().setLevel(player.getLevel());
            player.getPet().setHealth(player.getPet().calculateHealth());
            player.getPet().setDamage(player.getPet().calculateDamage());

            player.setManaPool(50);
            player.setAuraOfValor(true);
            
            setUpVariables(player);
            JsonArray items = json.get("items").getAsJsonArray();
            for (JsonElement item : items) {
                player.addItemToStorage(itemRepo.getItem(item.getAsString()));
            }
            Random rand = new Random();
            int luck = rand.nextInt(3) + 1;
            player.setLuck(luck);
            player.setStealth(json.get("stealth").getAsInt());
            player.setIntro(json.get("intro").getAsString());
            if (player.getName().equals("Recruit")) {
                player.type = "Recruit";
            } else if (player.getName().equals("Sewer Rat")) {
                player.type = "Sewer Rat";
            } else {
                QueueProvider.offer("Not a valid class");
            }
            reader.close();
            setUpCharacterLevels();
        } catch (FileNotFoundException ex) {
            QueueProvider.offer( "Unable to open file '" + fileName + "'.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return player;
    } 

    public int getXP() {
        return xp;
    }

    public void setXP(int xp) {
        this.xp = xp;
    }

    public static void setUpVariables(Player player) {
        float maxWeight = (float)Math.sqrt(player.getStrength()*300);
        player.setStorage(new Storage(maxWeight));
    }

    public void getStats(){
        Item weapon = itemRepo.getItem(getWeapon());
        String weaponName = weapon.getName();
        if (weaponName.equals(null)) {
            weaponName = "hands";
        }
        String message = "\nPlayer name: " + getName();
              message += "\nType: " + type;
              message += "\nCurrent weapon: " + weaponName;
              message += "\nGold: " + getGold();
              message += "\nHealth/Max: " + getHealth() + "/" + getHealthMax();
              message += "\nDamage/Armour: " + getDamage() + "/" + getArmour();
              message += "\nStrength: " + getStrength();
              message += "\nIntelligence: " + getIntelligence();
              message += "\nDexterity: " + getDexterity();
              message += "\nLuck: " + getLuck();
              message += "\nStealth: " + getStealth();
              message += "\nXP: " + getXP();
              message += "\n" + getName() + "'s level: " + getLevel();
             
              message += "\nPet level: " + player.getPet().getLevel();
              message += "\nPet Health: " + player.getPet().getHealth();
              message += "\nPet Damage: " + player.getPet().getDamage();

              message += "\nCurrent mana: " + player.getManaPool();
              if(auraOfValor)
                  message += "\nYou have Aura of Valor(+5 raw damage)";
              if(!auraOfValor)
                  message += "\nYou do not have Aura of Valor.";
        QueueProvider.offer(message);
    }

    public void printBackPack() {
        storage.display();
    }

    public void save() {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("healthMax", getHealthMax());
        jsonObject.addProperty("health", getHealthMax());
        jsonObject.addProperty("armour", getArmour());
        jsonObject.addProperty("damage", getDamage());
        jsonObject.addProperty("level", getLevel());
        jsonObject.addProperty("xp", getXP());
        jsonObject.addProperty("strength", getStrength());
        jsonObject.addProperty("intelligence", getIntelligence());
        jsonObject.addProperty("dexterity", getDexterity());
        jsonObject.addProperty("luck", getLuck());
        jsonObject.addProperty("stealth", getStealth());
        jsonObject.addProperty("weapon", getWeapon());
        jsonObject.addProperty("type", getCurrentCharacterType());
        HashMap<String, Integer> items = new HashMap<String, Integer>();
        for (ItemStack item : getStorage().getItemStack()) {
            items.put(item.getItem().getId(), item.getAmount());
        }
        JsonElement itemsJsonObj = gson.toJsonTree(items);
        jsonObject.add("items", itemsJsonObj);
        Map<EquipmentLocation, String> locations = new HashMap<>();
        locations.put(EquipmentLocation.HEAD, "head");
        locations.put(EquipmentLocation.CHEST, "chest");
        locations.put(EquipmentLocation.LEFT_ARM, "leftArm");
        locations.put(EquipmentLocation.LEFT_HAND, "leftHand");
        locations.put(EquipmentLocation.RIGHT_ARM, "rightArm");
        locations.put(EquipmentLocation.RIGHT_HAND, "rightHand");
        locations.put(EquipmentLocation.BOTH_HANDS, "BothHands");
        locations.put(EquipmentLocation.BOTH_ARMS, "bothArms");
        locations.put(EquipmentLocation.LEGS, "legs");
        locations.put(EquipmentLocation.FEET, "feet");
        HashMap<String, String> equipment = new HashMap<>();
        Item hands = itemRepo.getItem("hands");
        for (Map.Entry<EquipmentLocation, Item> item : getEquipment().entrySet()) {
            if (item.getKey() != null && !hands.equals(item.getValue()) && item.getValue() != null) {
                equipment.put(locations.get(item.getKey()), item.getValue().getId());
            }
        }
        JsonElement equipmentJsonObj = gson.toJsonTree(equipment);
        jsonObject.add("equipment", equipmentJsonObj);
        JsonElement typesJsonObj = gson.toJsonTree(getCharacterLevels());
        jsonObject.add("types", typesJsonObj);
        Coordinate coordinate = getLocation().getCoordinate();
        String coordinateLocation = coordinate.x+","+coordinate.y+","+coordinate.z;
        jsonObject.addProperty("location", coordinateLocation);

        String fileName = getProfileFileName(getName());
        new File(fileName).getParentFile().mkdirs();
        try {
            Writer writer = new FileWriter(fileName);
            gson.toJson(jsonObject, writer);
            writer.close();
            locationRepo = GameBeans.getLocationRepository(getName());
            locationRepo.writeLocations();
            QueueProvider.offer("\nYour game data was saved.");
        } catch (IOException ex) {
            QueueProvider.offer("\nUnable to save to file '" + fileName + "'.");
        }

        QueueProvider.offer("Now you can even save your profile to cloud and play anywhere you want!!!");
        QueueProvider.offer("Would you like to save your profile to cloud?(y/n)");
        String answer = QueueProvider.take().trim().toLowerCase() ;
        if( answer.length() == 0 || answer.charAt(0) != 'y' ){
            QueueProvider.offer("Did'nt expect that reaction but thats OK.");
            return;
        }

        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        /*      TO DO
                -This is not so smart because now any username can be uploaded once
                password system is required.

                -consider the test profile
        */

        boolean alreadyExist = false ;

        try {
            ListFolderResult result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    if( metadata.getPathLower().equals( "/" + getName() + ".json") ){
                        //QueueProvider.offer("This name is already taken!");
                        alreadyExist = true ;
                        break;
                    }
                }

                if (alreadyExist || !result.getHasMore()) {
                    break;
                }

                result = client.files().listFolderContinue(result.getCursor());
            }
        }catch ( Exception e){
            QueueProvider.offer("Your profile can not be uploaded at the moment. Please try again later.");
            return ;
        }

        try (InputStream in = new FileInputStream(fileName)) {
            if(MainMenu.cloudContains( getName()) )
                client.files().deleteV2("/" + getName() + ".json" ) ;

            FileMetadata metadata = client.files().uploadBuilder( "/" + getName() + ".json" ).uploadAndFinish(in);
        }catch( Exception e ){
            QueueProvider.offer( e.getMessage() + " | " + e.getLocalizedMessage() );
            QueueProvider.offer("Your profile can not be uploaded at the moment. Please try again later.");
            return;
        }
        QueueProvider.offer("Successfully uploaded your profile. Now you can continue to play anywhere.");
    }
    public static int calculateDistance(String source, String target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        int sourceLength = source.length();
        int targetLength = target.length();
        if (sourceLength == 0) return targetLength;
        if (targetLength == 0) return sourceLength;
        int[][] dist = new int[sourceLength + 1][targetLength + 1];
        for (int i = 0; i < sourceLength + 1; i++) {
            dist[i][0] = i;
        }
        for (int j = 0; j < targetLength + 1; j++) {
            dist[0][j] = j;
        }
        for (int i = 1; i < sourceLength + 1; i++) {
            for (int j = 1; j < targetLength + 1; j++) {
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                dist[i][j] = Math.min(Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1), dist[i - 1][j - 1] + cost);
                if (i > 1 &&
                        j > 1 &&
                        source.charAt(i - 1) == target.charAt(j - 2) &&
                        source.charAt(i - 2) == target.charAt(j - 1)) {
                    dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost);
                }
            }
        }
        return dist[sourceLength][targetLength];
    }
    public List<Item> searchItem(String itemName, List<Item> itemList) {
        List<Item> items = new ArrayList<>();
        for (Item item : itemList) {
            String testItemName = item.getName();
            if (calculateDistance( itemName , item.getName()) < 2) {
                char answer = 'y' ;
                if( calculateDistance( itemName , item.getName()) == 1 ){
                    QueueProvider.offer("Did you mean " + item.getName() + "? (y/n)" ) ;
                    answer = QueueProvider.take().toLowerCase().charAt(0) ;
                }
                if( answer == 'y' )
                    items.add(item);
            }
        }
        return items;
    }

    public List<Item> searchItem(String itemName, Storage storage) {
        return storage.search(itemName);
    }
    
    public List<Item> searchEquipment(String itemName, Map<EquipmentLocation, Item> equipment) {
        List<Item> items = new ArrayList<>();
        for (Item item : equipment.values()) {
            if (item != null && calculateDistance(item.getName(), itemName) < 2) {
                char answer = 'y' ;
                if( calculateDistance( itemName , item.getName()) == 1 ){
                    QueueProvider.offer("Did you mean " + item.getName() + "? (y/n)" ) ;
                    answer = QueueProvider.take().toLowerCase().charAt(0) ;
                }
                if( answer == 'y' )
                    items.add(item);
            }
        }
        return items;
    }

    public void pickUpItem(String itemName) {
        List<Item> items = searchItem(itemName, getLocation().getItems());
        if (! items.isEmpty()) {
            Item item = items.get(0);
            addItemToStorage(item);
            location.removeItem(item);
            QueueProvider.offer(item.getName()+ " picked up");
        }
    }

    public void dropItem(String itemName) {
        List<Item> itemMap = searchItem(itemName, getStorage());
        if (itemMap.isEmpty()) {
            itemMap = searchEquipment(itemName, getEquipment());
        }
        if (!itemMap.isEmpty()) {
            Item item = itemMap.get(0);
            Item itemToDrop = itemRepo.getItem(item.getId());
            Item weapon = itemRepo.getItem(getWeapon());
            String wName = weapon.getName();

            if (itemName.equals(wName)) {
                dequipItem(wName);
            }
            removeItemFromStorage(itemToDrop);
            location.addItem(itemToDrop);
            QueueProvider.offer(item.getName() + " dropped");
        }
    }

    public void equipItem(String itemName) {
        List<Item> items = searchItem(itemName, getStorage());
        if (!items.isEmpty()) {
            Item item = items.get(0);
            if (getLevel() >= item.getLevel()) {
                Map<String, String> change = equipItem(item.getPosition(), item);
                if(item.getId().charAt(0) != 'x')
                	QueueProvider.offer(item.getName()+ " equipped");
                printStatChange(change);
            } else {
                QueueProvider.offer("You do not have the required level to use this item");
            }
        } else {
            QueueProvider.offer("You do not have that item");
        }
    }

    public void dequipItem(String itemName) {
         List<Item> items = searchEquipment(itemName, getEquipment());
         if (!items.isEmpty()) {
            Item item = items.get(0);
            Map<String, String> change = unequipItem(item);
            QueueProvider.offer(item.getName()+" unequipped");
	        printStatChange(change);
         }
    }

    private void printStatChange(Map<String, String> stats) {
         Set<Entry<String, String>> set = stats.entrySet();
         Iterator<Entry<String, String>> iter = set.iterator();
         while (iter.hasNext()) {
              Entry<String, String> me = iter.next();
              double value = Double.parseDouble((String) me.getValue());
              switch ((String) me.getKey()) {
                  case "damage": {
                          if (value >= 0.0) {
                              QueueProvider.offer(me.getKey() + ": " + this.getDamage() + " (+" + me.getValue() + ")");
                          } else {
                              QueueProvider.offer(me.getKey() + ": " + this.getDamage() + " (" + me.getValue() + ")");
                          }
                          break;
                    }
                    case "health": {
                          if (value >= 0) {
                              QueueProvider.offer(me.getKey() + ": " + this.getHealth() + " (+" + me.getValue() + ")");
                          } else {
                              QueueProvider.offer(me.getKey() + ": " + this.getHealth() + " (" + me.getValue() + ")");
                          }
                          break;
                    }
                    case "armour": {
                          if (value >= 0) {
                              QueueProvider.offer(me.getKey() + ": " + this.getArmour() + " (+" + me.getValue() + ")");
                          } else {
                              QueueProvider.offer(me.getKey() + ": " + this.getArmour() + " (" + me.getValue() + ")");
                          }
                          break;
                    }
                    case "maxHealth": {
                          if (value  >= 0) {
                              QueueProvider.offer(me.getKey() + ": " + this.getHealthMax() + " (+" + me.getValue() + ")");
                          } else {
                              QueueProvider.offer(me.getKey() + ": " + this.getHealthMax() + " (" + me.getValue() + ")");
                          }
                          break;
                    }
              }
         }
    }

    public void inspectItem(String itemName) {
        List<Item> itemMap = searchItem(itemName, getStorage());
        if (itemMap.isEmpty()) {
            itemMap = searchItem(itemName, getLocation().getItems());
        }
        if (!itemMap.isEmpty()) {
            Item item = itemMap.get(0);
            item.display();
        } else {
            QueueProvider.offer("Item doesn't exist within your view.");
        }
    }

    public ILocation getLocation() {
        return location;
    }

    public void setLocation(ILocation location) {
        this.location = location;
    }

    public LocationType getLocationType() {
    	return getLocation().getLocationType();
    }

    public void attack(String opponentName) throws DeathException {
        Monster monsterOpponent = null;
        NPC npcOpponent = null;
        List<Monster> monsters = getLocation().getMonsters();
        List<NPC> npcs = getLocation().getNpcs();
        for (int i = 0; i < monsters.size(); i++) {
             if (monsters.get(i).monsterType.equalsIgnoreCase(opponentName)) {
                 monsterOpponent = monsters.get(i);
             }
        }
        for (int i=0; i < npcs.size(); i++) {
            if (npcs.get(i).getName().equalsIgnoreCase(opponentName)) {
                npcOpponent = npcs.get(i);
            }
        }
        if (monsterOpponent != null) {
            monsterOpponent.setName(monsterOpponent.monsterType);
            new BattleMenu(monsterOpponent, this);
        } else if (npcOpponent != null) {
            new BattleMenu(npcOpponent, this);
        } else {
             QueueProvider.offer("Opponent not found");
        }
    }

    public boolean hasItem(Item item) {
        List<Item> searchEquipment = searchEquipment(item.getName(), getEquipment());
        List<Item> searchStorage = searchItem(item.getName(), getStorage());
        return !(searchEquipment.size() == 0 && searchStorage.size() == 0);
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}
