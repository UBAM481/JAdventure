package com.jadventure.game.entities;

import com.jadventure.game.entities.Pet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PetTest {

    @Test
    public void newPetDamage() {
        Pet pet = new Pet();
        double expected_damage = 3;
        double actual_damage = pet.getDamage(); 									//0 is for deprecation
        assertEquals("Failure - new pet not properly created", expected_damage, actual_damage, 0);
    }
    @Test
    public void newPetHealth() {
        Pet pet = new Pet();
        int expected_health = 5;
        int actual_health = pet.getHealth(); 												
        assertEquals("Failure - new pet not properly created", expected_health, actual_health, 0);
    }
    @Test
    public void leveledPetDamage() {
    	Pet pet = new Pet();
    	pet.setLevel(3);
    	double expected_damage = 9;
    	double actual_damage = pet.getDamage();
    	assertEquals("Failure - pet damage not properly calculated", expected_damage, actual_damage, 0);
    }
    @Test
    public void leveledPetHealth() {
    	Pet pet = new Pet();
    	pet.setLevel(3);
    	double expected_health = 15;
    	double actual_health= pet.getHealth();
    	assertEquals("Failure - pet health not properly calculated", expected_health, actual_health, 0);
    }
    @Test
    public void newPetAbsorbPoint() {
    	Pet pet = new Pet();
    	int expected_absorbPoint = 3;
    	int actual_absorbPoint = pet.absorbPoint();
    	assertEquals("Failure - new pet not properly created", expected_absorbPoint, actual_absorbPoint);
    }
    @Test
    public void leveledPetAbsorbPoint() {
    	Pet pet = new Pet();
    	pet.setLevel(3);
    	int expected_absorbPoint = 9;
    	int actual_absorbPoint = pet.absorbPoint();
    	assertEquals("Failure - pet absorb point not properly calculated", expected_absorbPoint, actual_absorbPoint);
    }
}
