package com.jadventure.game.entities;

public class Pet extends Entity 
{		
	private int accumulatedAbsorbPoint;
	public Pet() {
		super.setLevel(1);
		accumulatedAbsorbPoint = 0;
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
		if(accumulatedAbsorbPoint <= getLevel()*21 - getLevel()*3)
			accumulatedAbsorbPoint = accumulatedAbsorbPoint + getLevel()*3;
		return super.getLevel()*3;
	}
	public int getAccumulatedAbsorbPoint() {
		return accumulatedAbsorbPoint;
	}
	public void resetAccumulatedAbsorbPoint() {
		accumulatedAbsorbPoint = 0;
	}
}
