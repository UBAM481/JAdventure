package com.jadventure.game.menus;

import com.jadventure.game.DeathException;
import com.jadventure.game.entities.Entity;
import com.jadventure.game.entities.Player;
import com.jadventure.game.entities.NPC;
import com.jadventure.game.monsters.Monster;
import com.jadventure.game.QueueProvider;
import com.jadventure.game.CharacterChange;
import com.jadventure.game.items.ItemStack;
import com.jadventure.game.items.Item;
import com.jadventure.game.GameBeans;
import com.jadventure.game.monsters.MonsterFactory;
import com.jadventure.game.navigation.ILocation;
import com.jadventure.game.navigation.Location;
import com.jadventure.game.prompts.CommandCollection;
import com.jadventure.game.repository.ItemRepository;
import com.jadventure.game.repository.LocationRepository;

import java.awt.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class BattleMenu extends Menus {

    private NPC opponent;
    private Player player;
    private Random random;
    private int armour;
    private double damage;
    private int escapeSuccessfulAttempts = 0;

    public BattleMenu(NPC opponent, Player player) throws DeathException {
        this.random = new Random();
        this.opponent = opponent;
        this.player = player;
        this.armour = player.getArmour();
        this.damage = player.getDamage();
        buildMenu();
        while (opponent.getHealth() > 0 &&
                player.getHealth() > 0 &&
                (escapeSuccessfulAttempts <= 0)) {
            QueueProvider.offer("\nWhat is your choice?");
            MenuItem selectedItem = displayMenu(this.menuItems);
            testSelected(selectedItem);
            if(selectedItem.command.equals("Random Teleport"))
                break;
        }
        if (player.getHealth() == 0) {
            QueueProvider.offer("You died... Start again? (y/n)");
            String reply = QueueProvider.take().toLowerCase();
            while (!reply.startsWith("y") && !reply.startsWith("n")) {
                QueueProvider.offer("You died... Start again? (y/n)");
                reply = QueueProvider.take().toLowerCase();
            }
            if (reply.startsWith("y")) {
                throw new DeathException("restart");
            } else if (reply.startsWith("n")) {
                throw new DeathException("close");
            }
        }  else if (opponent.getHealth() == 0) {
            int xp = opponent.getXPGain();
            this.player.setXP(this.player.getXP() + xp);
            int oldLevel = this.player.getLevel();
            int newLevel = (int) (0.075 * Math.sqrt(this.player.getXP()) + 1);
            this.player.setLevel(newLevel);
            this.player.getPet().setLevel(newLevel);
            

            // Iterates over the opponent's items and if there are any, drops them.
            // There are two loops due to a ConcurrentModification Exception that occurs
            // if you try to remove the item while looping through the npc's items.
            List<ItemStack> itemStacks = opponent.getStorage().getItemStack();
            List<String> itemIds = new ArrayList<>();
            for (ItemStack itemStack : itemStacks) {
                String itemId = itemStack.getItem().getId();
                itemIds.add(itemId);
            }
            for (String itemId : itemIds) {
                Item item = GameBeans.getItemRepository().getItem(itemId);
                opponent.removeItemFromStorage(item);
                this.player.getLocation().addItem(item);
                QueueProvider.offer("Your opponent dropped a " +
                        item.getName());
            }

            this.player.getLocation().remove(opponent);
            this.player.setGold(this.player.getGold() + opponent.getGold());
            QueueProvider.offer("You killed a " + opponent.getName() +
                    "\nYou have gained " + xp + " XP and " +
                    opponent.getGold() + " gold");
            this.player.setManaPool(this.player.getManaPool()+5);
            QueueProvider.offer("You have recovered 5 mana out of the monsters tears!");
            if(this.player.getManaPool()>50)
                this.player.setManaPool(50);
            if (oldLevel < newLevel) {
                QueueProvider.offer("You've are now level " + newLevel + "!");
                
                QueueProvider.offer("Pet leveled up to " + newLevel);
            }
            CharacterChange cc = new CharacterChange();
            cc.trigger(this.player, "kill", opponent.getName());
        }
    }

    private void buildMenu() {
        this.menuItems.add(new MenuItem("Attack",
                    "Attack " + opponent.getName() + "."));
        this.menuItems.add(new MenuItem("Defend",
                    "Defend against " + opponent.getName() + "'s attack."));
        this.menuItems.add(new MenuItem("Escape",
                    "Try and escape from " + opponent.getName()));
        this.menuItems.add(new MenuItem("Equip", "Equip an item"));
        this.menuItems.add(new MenuItem("Unequip", "Unequip an item"));
        this.menuItems.add(new MenuItem("View",
                    "View details about your character"));
        if(player.getGame().randomTeleportCheat)
            this.menuItems.add(new MenuItem("Random Teleport","Teleport a random location with special cheat code"));

    }

    private void testSelected(MenuItem m) {
        switch (m.getKey()) {
            case "attack": {
                   mutateStats(1, 0.5);
                   attack(player, opponent);
                   attack(opponent, player);
                   resetStats();
                   break;
            }
            case "defend": {
                   mutateStats(0.5, 1);
                   QueueProvider.offer("\nYou get ready to defend against " +
                           "the " + opponent.getName() + ".");
                   attack(player, opponent);
                   attack(opponent, player);
                   resetStats();
                   break;
            }
            case "escape": {
                       escapeSuccessfulAttempts = escapeAttempt(player,
                               opponent, escapeSuccessfulAttempts);
                   break;
            }
            case "equip": {
                   equip();
                   break;
            }
            case "unequip": {
                  unequip();
                  break;
            }
            case "view": {
                  viewStats();
                  break;
            }
            case "random teleport":{
                QueueProvider.offer("You are teleporting");
                randomteleportduringfight((Monster) opponent);
                break;
            }
            default: {
                  break;
            }
        }
    }

    private void randomteleportduringfight(Monster opponent) {
        LocationRepository locationRepo = GameBeans.getLocationRepository(player.getName());
        ILocation location = player.getLocation();
        location.removeMonster(opponent);
        ILocation newLocation = getRandomLocationDuringFight(location,opponent.getRandomTeleportPreventPoint());
        player.setLocation(newLocation);
        if ("test".equals(player.getName())) {
            QueueProvider.offer(player.getLocation().getCoordinate().toString());
        }
        double distance=Location.calculateDistanceBetweenTwoLocation(location,newLocation);
        if(distance>5)
            QueueProvider.offer("Wow!You jumped "+distance+" meter \n");
        else
            QueueProvider.offer("You teleported  " + (int)distance+" meter \n");


        player.getLocation().print();
        addMonsterAndItemDuringFight();

    }

    private void addMonsterAndItemDuringFight() {
        Random random = new Random();
        if (player.getLocation().getMonsters().size() == 0) {
            MonsterFactory monsterFactory = new MonsterFactory();
            int upperBound = random.nextInt(player.getLocation().getDangerRating() + 1);
            for (int i = 0; i < upperBound; i++) {
                Monster monster = monsterFactory.generateMonster(player);
                player.getLocation().addMonster(monster);
            }
        }
        if (player.getLocation().getItems().size() == 0) {
            int chance = random.nextInt(100);
            if (chance < 60) {
                addItemToLocationDuringFight();
            }
        }
        if (random.nextDouble() < 0.5) {
            List<Monster> monsters = player.getLocation().getMonsters();
            if (monsters.size() > 0) {
                int posMonster = random.nextInt(monsters.size());
                String monster = monsters.get(posMonster).monsterType;
                QueueProvider.offer("A " + monster + " is attacking you!");
                try {
                    player.attack(monster);
                } catch (DeathException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void addItemToLocationDuringFight() {
        ItemRepository itemRepo = GameBeans.getItemRepository();
        if (player.getHealth() < player.getHealthMax()/3) {
            player.getLocation().addItem(itemRepo.getRandomFood(player.getLevel()));
        } else {
            Random rand = new Random();
            int startIndex = rand.nextInt(3);
            switch (startIndex) {
                case 0:
                    player.getLocation().addItem(itemRepo.getRandomWeapon(player.getLevel()));
                    break;
                case 1:
                    player.getLocation().addItem(itemRepo.getRandomFood(player.getLevel()));
                    break;
                case 2:
                    player.getLocation().addItem(itemRepo.getRandomArmour(player.getLevel()));
                    break;
                case 3:
                    player.getLocation().addItem(itemRepo.getRandomPotion(player.getLevel()));
                    break;
            }
        }

    }

    private ILocation getRandomLocationDuringFight(ILocation location, int randomteleportpreventpoint) {
        Set<ILocation> exits = location.getExitsForTeleport(player.getStorage().getNumberOfItems(),randomteleportpreventpoint);
        int size = exits.size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(ILocation obj : exits) {
            if (i == item)
                return obj;
            i++;
        }
        return null;

    }

    private int escapeAttempt(Player player, NPC attacker,
            int escapeAttempts) {
        if (escapeAttempts == -10) {
            escapeAttempts = 0;
        }
        double playerEscapeLevel = player.getIntelligence() +
            player.getStealth() + player.getDexterity();
        double attackerEscapeLevel = attacker.getIntelligence() +
            attacker.getStealth() + attacker.getDexterity() +
            (attacker.getDamage() / playerEscapeLevel);
        double escapeLevel = playerEscapeLevel / attackerEscapeLevel;

        Random rand = new Random();
        int rawLuck = rand.nextInt(player.getLuck()*2) + 1;
        int lowerBound = 60 - rawLuck;
        int upperBound = 80 - rawLuck;
        double minEscapeLevel = (rand.nextInt((upperBound - lowerBound) + 1) +
                lowerBound) / 100.0;
        if (escapeLevel > minEscapeLevel && (escapeAttempts == 0)) {
            QueueProvider.offer("You have managed to escape the: " +
                    attacker.getName());
            return 1;
        } else if (escapeAttempts < 0) {
            QueueProvider.offer("You have tried to escape too many times!");
            return escapeAttempts - 1;
        } else {
            QueueProvider.offer("You failed to escape the: " +
                    attacker.getName());
            return escapeAttempts-1;
        }
    }

    private void attack(Entity attacker, Entity defender) {
        if (attacker.getHealth() == 0) {
            return;
        }
        double damage = attacker.getDamage();
        double critCalc = random.nextDouble();
        if (critCalc < attacker.getCritChance()) {
            damage += damage;
            QueueProvider.offer("Crit hit! Damage has been doubled!");
        }
        int healthReduction = (int) ((((3 * attacker.getLevel() / 50 + 2) *
                damage * damage / (defender.getArmour() + 1)/ 100) + 2) *
                (random.nextDouble() + 1));
        
        // if defender is player, the player's pet absorbs some of the damage player takes. 
        if(defender instanceof Player) 
        {
        	int absorbByPet = ((Player) defender).getPet().absorbPoint();
        	healthReduction = healthReduction - absorbByPet;
        	defender.setHealth(defender.getHealth() - healthReduction);
        	QueueProvider.offer(absorbByPet + " damage absorbed by pet!");
        }
        else
        {
        	defender.setHealth((defender.getHealth() - healthReduction));
        }
        if (defender.getHealth() < 0) {
            defender.setHealth(0);
        }
        QueueProvider.offer(healthReduction + " damage dealt!");
        if (attacker instanceof Player) {
        	
        	int petDamage = (int)((Player) attacker).getPet().getDamage();
        	ArrayList<Item> items = (ArrayList<Item>) player.getStorage().getItems();
        	boolean found = false;
        	int count;
        	for(count = 0 ; count < items.size() ; count++) {
        		if(items.get(count).getId().equals("xpetstone")) {
        			found = true;
        			break;
        		}
        	}
        	//if player has pet stone
        	if(found) {
        		//if pet's accumulated absorb point is enough, pet vomits the accumulated absorb point as damage to the enemy
        		if(player.getPet().getAccumulatedAbsorbPoint() == player.getLevel()*21) {
        			petDamage = player.getPet().getAccumulatedAbsorbPoint();
        			QueueProvider.offer("Pet inhales to vomit its accumulated absorb damage!!!!");
        			player.removeItemFromStorage(items.get(count));
        			player.getPet().resetAccumulatedAbsorbPoint();
        		}
        	}
            defender.setHealth(defender.getHealth() - petDamage);
            QueueProvider.offer(petDamage + " pet damage dealt!");
            if(((Player) attacker).getAuraOfValor()) {
                int x=(int)(java.lang.Math.random()*6+1);
                if(x==1)
                    QueueProvider.offer("You have the Aura of Valor, but the monster is too courageous. No damage dealt.");
                else if(x==2){
                    defender.setHealth(defender.getHealth() - 10);
                    QueueProvider.offer("Critical hit! You have the Aura of Valor, an extra 10 damage is dealt.");
                }
                else {
                    defender.setHealth(defender.getHealth() - 5);
                    QueueProvider.offer("You have the Aura of Valor, an extra 5 damage is dealt.");
                }

                ((Player) attacker).setManaPool(((Player) attacker).getManaPool()-10);
                if(((Player) attacker).getManaPool()<=0)
                    ((Player) attacker).setAuraOfValor(false);
            }
            if (defender.getHealth() < 0) {
                defender.setHealth(0);
            }
             
            QueueProvider.offer("The " + defender.getName() + "'s health is " +
                    defender.getHealth());
        } else {
            QueueProvider.offer("Your health is " + defender.getHealth());
        }
    }

    private void mutateStats(double damageMult, double armourMult) {
        armour = player.getArmour();
        damage = player.getDamage();
        player.setArmour((int) (armour * armourMult));
        player.setDamage(damage * damageMult);
    }

    private void resetStats() {
        player.setArmour(armour);
        player.setDamage(damage);
    }

    private void equip() {
        player.printStorage();
        QueueProvider.offer("What item do you want to use?");
        String itemName = QueueProvider.take();
        if (!itemName.equalsIgnoreCase("back")) {
            player.equipItem(itemName);
        }
    }

    private void unequip() {
        player.printEquipment();
        QueueProvider.offer("What item do you want to unequip?");
        String itemName = QueueProvider.take();
        if (!itemName.equalsIgnoreCase("back")) {
            player.dequipItem(itemName);
        }
    }

    private void viewStats() {
        QueueProvider.offer("\nWhat is your command? ex. View stats(vs), " +
                "View Backpack(vb), View Equipment(ve) ");
        String input = QueueProvider.take();
        switch (input) {
            case "vs":
            case "viewstats":
                player.getStats();
                break;
            case "ve":
            case "viewequipped":
                player.printEquipment();
                break;
            case "vb":
            case "viewbackpack":
                player.printStorage();
                break;
            case "back":
            case "exit":
                break;
            default:
                viewStats();
                break;
        }
    }

    public Player getPlayer() {
        return player;
    }
}
