package com.jadventure.game.entities;

public class Pet extends Entity 
{		
	public Pet() {
		super.setHealth(0);
		super.setDamage(0);
	}
	public int calculateHealth()
	{
		return super.getLevel()*5;
	}
	public int calculateDamage() 
	{
		return super.getLevel()*3;
	}
}
