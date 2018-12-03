package com.jadventure.game.entities;

public class Pet extends Entity 
{		
	public Pet() {
		super.setLevel(1);
	}
	//to override getDamage method from entity
	public double getDamage() {
        return calculateDamage();
    }
	//to override getHealth method from entity
	public int getHealth() {
		return calculateHealth();
	}
	public int calculateHealth()
	{
		return super.getLevel()*5;
	}
	public int calculateDamage() 
	{
		return super.getLevel()*3;
	}
	public int absorbPoint()
	{
		return super.getLevel()*3;
	}
}
