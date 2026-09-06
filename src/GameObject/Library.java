package GameObject;

import java.util.ArrayList;
import java.util.Random;
import GameObject.Item.*;
import GameObject.Player.Team;
import GameObject.Weapon.*;
import TileMap.TileMap;

public class Library {
	
	public static final int weaponCount = 15;
	public static final int itemCount = 5;
	
	private Random r = new Random();
	private ArrayList<Integer> weaponPreset;
	private ArrayList<Integer> itemPreset;
	private final String[] weaponNames = 
		{"Pistol", "Uzi", "Kalashnikova", "Shotgun", "Chainsaw", 
		 "Grenade", "Sniper", "RPG", "Punch Gun", "Molotov", "M4A1", 
		 "Shield", "PP-19 Bizon", "M14", "TNT"};
	private final String[] itemNames = 
		{"Health Relic", "Swiftness Boots", "Ammo Crate", "Cloth Armor", "Unlimited Ammo"};
	
	public Library() {
		weaponPreset = new ArrayList<Integer>();
		itemPreset = new ArrayList<Integer>();
		for(int i = 0; i < weaponCount; i++)
			weaponPreset.add(i);
		for(int i = 0; i < itemCount; i++)
			itemPreset.add(i);
	}
	
	public void newRandomSpawn(TileMap t)
	{
		int i = r.nextInt(2);
		switch(i)
		{
		case 0:
			newRandomItem(t);
			break;
		case 1:
			newRandomFirearm(t);
			break;
		}
	}
	
	public void newRandomItem(TileMap t)
	{
		if(itemPreset.isEmpty()) return;
		int index = r.nextInt(itemPreset.size());
		newItem(itemPreset.get(index), t);
	}
	
	public Item getNewRandomItem(TileMap t)
	{
		if(itemPreset.isEmpty()) return null;
		int index = r.nextInt(itemPreset.size());
		return getNewItem(itemPreset.get(index), t);
	}
	
	public void newItem(int index, TileMap t)
	{
		Item item = null;
		switch(index)
		{
		case 0:
			item = new HealthRelic(t);
			break;
		case 1:
			item = new SpeedBoots(t);
			break;
		case 2:
			item = new AmmoBox(t);
			break;
		case 3:
			item = new ClothArmor(t);
			break;
		case 4:
			item = new InfiniteAmmoBox(t);
			break;
		}
		item.spawnRandom(t.getFirearmSpawnPoints());
		item.updateHitbox();
	}
	
	public Item getNewItem(int index, TileMap t)
	{
		Item item = null;
		switch(index)
		{
		case 0:
			item = new HealthRelic(t);
			break;
		case 1:
			item = new SpeedBoots(t);
			break;
		case 2:
			item = new AmmoBox(t);
			break;
		case 3:
			item = new ClothArmor(t);
			break;
		case 4:
			item = new InfiniteAmmoBox(t);
			break;
		}
		item.spawnRandom(t.getFirearmSpawnPoints());
		item.updateHitbox();
		return item;
	}
	
	public void newRandomFirearm(TileMap t)
	{
		if(weaponPreset.isEmpty()) return;
		int index = r.nextInt(weaponPreset.size());
		newFirearm(weaponPreset.get(index), t);
	}
	
	public void newFirearm(int index, TileMap t)
	{
		Weapon f = null;
		switch(index)
		{
		case 0:
			f = new Pistol(t);
			break;
		case 1:
			f = new Uzi(t);
			break;
		case 2:
			f = new Kalashnikova(t);
			break;
		case 3:
			f = new Shotgun(t);
			break;
		case 4:
			f = new Chainsaw(t);
			break;
		case 5:
			f = new Grenade(t);
			break;
		case 6:
			f = new Sniper(t);
			break;
		case 7:
			f = new RPG(t);
			break;
		case 8:
			f = new PunchGun(t);
			break;
		case 9:
			f = new Molotov(t);
			break;
		case 10:
			f = new M4A1(t);
			break;
		case 11:
			f = new Shield(t);
			break;
		case 12:
			f = new Bizon(t);
			break;
		case 13:
			f = new M14(t);
			break;
		case 14:
			f = new TNT(t);
			break;
		}
		f.init();
		f.respawn();
	}
	
	public Weapon getRandomFirearm(TileMap t)
	{
		if(weaponPreset.isEmpty()) return null;
		int index = r.nextInt(weaponPreset.size());
		return getNewFirearm(weaponPreset.get(index), t);
	}
	
	public Weapon getNewFirearm(int index, TileMap t)
	{
		Weapon f = null;
		switch(index)
		{
		case 0:
			f = new Pistol(t);
			break;
		case 1:
			f = new Uzi(t);
			break;
		case 2:
			f = new Kalashnikova(t);
			break;
		case 3:
			f = new Shotgun(t);
			break;
		case 4:
			f = new Chainsaw(t);
			break;
		case 5:
			f = new Grenade(t);
			break;
		case 6:
			f = new Sniper(t);
			break;
		case 7:
			f = new RPG(t);
			break;
		case 8:
			f = new PunchGun(t);
			break;
		case 9:
			f = new Molotov(t);
			break;
		case 10:
			f = new M4A1(t);
			break;
		case 11:
			f = new Shield(t);
			break;
		case 12:
			f = new Bizon(t);
			break;
		case 13:
			f = new M14(t);
			break;
		case 14:
			f = new TNT(t);
			break;
		}
		f.init();
		return f;
	}
	
	public ArrayList<Integer> getWeaponPreset() { return weaponPreset; }
	public void setWeaponPreset(ArrayList<Integer> preset) { weaponPreset = preset; }
	public void setWeaponPreset(int[] preset)
	{
		weaponPreset.clear();
		for(int i = 0; i < preset.length; i++)
			weaponPreset.add(preset[i]);
	}
	public ArrayList<Integer> getItemPreset() { return itemPreset; }
	public void setItemPreset(ArrayList<Integer> preset) { itemPreset = preset; }
	public void setItemPreset(int[] preset)
	{
		itemPreset.clear();
		for(int i = 0; i < preset.length; i++)
			itemPreset.add(preset[i]);
	}
	public int getWeaponCount() { return weaponCount; }
	public int getItemCount() { return itemCount; }
	public String[] getWeaponNames() { return weaponNames; }
	public String[] getItemNames() { return itemNames; }
	public int getWeaponId(Weapon w)
	{
		Class<? extends Weapon> c = w.getClass();
		int id = -1;
		if(c.equals(Pistol.class)) return 0;
		if(c.equals(Uzi.class)) return 1;
		if(c.equals(Kalashnikova.class)) return 2;
		if(c.equals(Shotgun.class)) return 3;
		if(c.equals(Chainsaw.class)) return 4;
		if(c.equals(Grenade.class)) return 5;
		if(c.equals(Sniper.class)) return 6;
		if(c.equals(RPG.class)) return 7;
		if(c.equals(PunchGun.class)) return 8;
		if(c.equals(Molotov.class)) return 9;
		if(c.equals(M4A1.class)) return 10;
		if(c.equals(Shield.class)) return 11;
		if(c.equals(Bizon.class)) return 12;
		if(c.equals(M14.class)) return 13;
		if(c.equals(TNT.class)) return 14;
		return id;
	}
	
	public int getItemId(Item i)
	{
		Class<? extends Item> c = i.getClass();
		if(c.equals(HealthRelic.class)) return 0;
		if(c.equals(SpeedBoots.class)) return 1;
		if(c.equals(AmmoBox.class)) return 2;
		if(c.equals(ClothArmor.class)) return 3;
		if(c.equals(InfiniteAmmoBox.class)) return 4;
		return -1;
	}
	
	public Item createItem(int index, TileMap t)
	{
		switch(index)
		{
		case 0: return new HealthRelic(t);
		case 1: return new SpeedBoots(t);
		case 2: return new AmmoBox(t);
		case 3: return new ClothArmor(t);
		case 4: return new InfiniteAmmoBox(t);
		default: return null;
		}
	}
	
	public int getBulletId(Bullet b)
	{
		Class<? extends Bullet> c = b.getClass();
		if(c.equals(PistolBullet.class)) return 0;
		if(c.equals(RifleBullet.class)) return 1;
		if(c.equals(ShotgunPellet.class)) return 2;
		if(c.equals(SniperBullet.class)) return 3;
		if(c.equals(M14Bullet.class)) return 4;
		if(c.equals(RPG_Bullet.class)) return 5;
		return 0;
	}
	
	public Bullet createBullet(int index, TileMap t, Team team)
	{
		switch(index)
		{
		case 0: return new PistolBullet(t, team);
		case 1: return new RifleBullet(t, team);
		case 2: return new ShotgunPellet(t, team);
		case 3: return new SniperBullet(t, team);
		case 4: return new M14Bullet(t, team);
		case 5: return new RPG_Bullet(t, team);
		default: return new PistolBullet(t, team);
		}
	}
}
