package GameObject.Weapon;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import GameObject.GameObject;
import GameObject.Player.Player;
import GameObject.Player.Team;
import GameState.LevelState.GameMode;
import GameState.LevelState.LevelState;
import TileMap.TileMap;

public abstract class Bullet extends GameObject{
	
	private int damage;
	private double knockback;
	private boolean hitTerrain;
	private boolean removed;
	public static ArrayList<Bullet> BulletList = new ArrayList<Bullet>();
	private static HashMap<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>();
	private Team team;
	
	public static void updateBullets()
	{
		for(int i = 0; i < BulletList.size(); )
		{
			Bullet b = BulletList.get(i);
			b.update();
			
			if(b.removed)
				continue;
			
			for(int j = 0; j < Player.PlayerList.size(); j++)
			{
				Player p = Player.PlayerList.get(j);
				boolean condition = false;
				if(LevelState.getGameMode() == GameMode.LAST_MAN_STANDING)
				{
					condition = b.collidesWith(p);
				}
				else if(LevelState.getGameMode() == GameMode.TEAM)
				{
					condition = b.collidesWith(p) && b.getTeam() != p.getTeam();
				}
				if(condition)
				{
					b.impact(p);
					break;
				}
			}
			
			if(b.removed)
				continue;
			
			if(b.hitsTerrain())
			{
				b.hitTerrain();
				if(b.removed)
					continue;
			}
			
			i++;
		}
	}
	
	public static void drawBullets(java.awt.Graphics2D g)
	{
		if(!BulletList.isEmpty())
			for(int i = 0; i < BulletList.size(); i++)
				BulletList.get(i).draw(g);
	}
	
	public Bullet(int dmg, TileMap tileMap, double knockback, String imageName, Team team) {
		damage = dmg;
		setTileMap(tileMap);
		hitTerrain = false;
		removed = false;
		this.knockback = knockback;
		this.team = team;
		try {
			BufferedImage img = imageCache.get(imageName);
			if(img == null)
			{
				img = ImageIO.read(getClass().getResource(imageName));
				if(img != null)
					imageCache.put(imageName, img);
			}
			setImage(img);
			setSizeByImage();
			setHitBoxByImage();
		} catch (IOException e) {
			
		}
		if(getHitbox() == null)
			setHitBoxBySize();
		BulletList.add(this);
	}
	
	public void hitTerrain()
	{
		removeThis();
	}
	
	public void defaultImpact(Player p)
	{
		p.takeDamage(getDamage());
		p.addExternalForce(50, getKnockback(), 0);
		removeThis();
	}
	
	public abstract void impact(Player p);
	public int getDamage() { return damage; }
	public boolean hitsTerrain() { return hitTerrain; }
	public void removeThis() { 
		if(removed) return;
		removed = true;
		BulletList.remove(this);
	}
	
	@Override
	public void update()
	{
		setX(getX() + getXVel());
		setY(getY() + getYVel());
		updateHitbox();
		if(this.checkOutOfBound())
		{
			removeThis();
			return;
		}
		if(overlapsSolidTile())
			hitTerrain = true;
	}
	
	private boolean overlapsSolidTile()
	{
		TileMap m = getTileMap();
		if(m == null) return false;
		int ts = TileMap.getTileSize();
		if(ts <= 0) return false;
		int left = (int)Math.floor(getX() / ts);
		int right = (int)Math.floor((getX() + getWidth() - 1) / ts);
		int top = (int)Math.floor(getY() / ts);
		int bottom = (int)Math.floor((getY() + getHeight() - 1) / ts);
		for(int row = top; row <= bottom; row++)
		{
			for(int col = left; col <= right; col++)
			{
				if(m.getType(row, col) / 10 == 1)
					return true;
			}
		}
		return false;
	}
	
	public double getKnockback() { return knockback * getDirection(); }
	public Team getTeam() { return team; }
}
