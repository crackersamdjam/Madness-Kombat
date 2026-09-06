package Multiplayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import GameObject.Library;
import GameObject.Item.Item;
import GameObject.Player.Player;
import GameObject.Player.Team;
import GameObject.Weapon.Bullet;
import GameObject.Weapon.Explosion;
import GameObject.Weapon.Weapon;
import GameState.GameStateManager;
import GameState.LevelState.GameMode;
import GameState.LevelState.LevelState;
import GameState.LevelState.PlayingState;
import GameState.OptionState.ImageBlock;
import TileMap.TileMap;

public class WorldSnapshot {
	
	public String phase = Protocol.PHASE_PLAYING;
	public long countSec;
	public String winnerName = "";
	public final ArrayList<PlayerSnap> players = new ArrayList<PlayerSnap>();
	public final ArrayList<WeaponSnap> weapons = new ArrayList<WeaponSnap>();
	public final ArrayList<BulletSnap> bullets = new ArrayList<BulletSnap>();
	public final ArrayList<ItemSnap> items = new ArrayList<ItemSnap>();
	public final ArrayList<ExplosionSnap> explosions = new ArrayList<ExplosionSnap>();
	
	public static class PlayerSnap {
		public int id;
		public String name;
		public Team team;
		public double x, y;
		public int dir;
		public double hp;
		public int armor;
		public int score;
		public boolean invincible;
	}
	
	public static class WeaponSnap {
		public int netId;
		public int type;
		public double x, y;
		public int dir;
		public int ownerId;
		public int ammo;
	}
	
	public static class BulletSnap {
		public int netId;
		public int type;
		public double x, y;
		public int dir;
		public Team team;
	}
	
	public static class ItemSnap {
		public int netId;
		public int type;
		public double x, y;
	}
	
	public static class ExplosionSnap {
		public int netId;
		public double x, y;
		public int blastRadius;
	}
	
	public static WorldSnapshot capture(GameStateManager gsm, Library library)
	{
		WorldSnapshot snap = new WorldSnapshot();
		if(gsm.getCurrentState() != GameStateManager.LEVELSTATE) return snap;
		LevelState ls = (LevelState) gsm.getState(GameStateManager.LEVELSTATE);
		PlayingState ps = (PlayingState) ls.getState(LevelState.PLAYING);
		int sub = ls.getStateIndex();
		if(sub == LevelState.PAUSE)
			snap.phase = Protocol.PHASE_PAUSE;
		else if(sub == LevelState.TRANSITION)
			snap.phase = Protocol.PHASE_TRANSITION;
		else if(ps.isCountdown())
			snap.phase = Protocol.PHASE_COUNTDOWN;
		else
			snap.phase = Protocol.PHASE_PLAYING;
		snap.countSec = ps.getCountSec();
		if(sub == LevelState.TRANSITION)
		{
			Player winner = ps.getWinningPlayer();
			if(winner != null)
			{
				if(LevelState.getGameMode() == GameMode.TEAM)
					snap.winnerName = winner.getTeam().name() + " team";
				else
					snap.winnerName = winner.getName();
			}
		}
		for(Player p : Player.PlayerList)
		{
			PlayerSnap s = new PlayerSnap();
			s.id = p.getID();
			s.name = p.getName();
			s.team = p.getTeam();
			s.x = p.getX();
			s.y = p.getY();
			s.dir = p.getDirection();
			s.hp = p.getHitpoints();
			s.armor = p.getArmor();
			s.score = p.getScore();
			s.invincible = p.isInvincible();
			snap.players.add(s);
		}
		for(Weapon w : Weapon.WeaponList)
		{
			WeaponSnap s = new WeaponSnap();
			s.netId = w.getNetId();
			s.type = library.getWeaponId(w);
			if(s.type < 0) continue;
			s.x = w.getX();
			s.y = w.getY();
			s.dir = w.getDirection();
			s.ownerId = (w.isEquipped() && w.getPlayer() != null) ? w.getPlayer().getID() : -1;
			s.ammo = w.getAmmoCount();
			snap.weapons.add(s);
		}
		for(Bullet b : Bullet.BulletList)
		{
			BulletSnap s = new BulletSnap();
			s.netId = b.getNetId();
			s.type = library.getBulletId(b);
			s.x = b.getX();
			s.y = b.getY();
			s.dir = b.getDirection();
			s.team = b.getTeam();
			snap.bullets.add(s);
		}
		for(Item i : Item.ItemList)
		{
			ItemSnap s = new ItemSnap();
			s.netId = i.getNetId();
			s.type = library.getItemId(i);
			if(s.type < 0) continue;
			s.x = i.getX();
			s.y = i.getY();
			snap.items.add(s);
		}
		for(Explosion e : Explosion.ExplosionList)
		{
			ExplosionSnap s = new ExplosionSnap();
			s.netId = e.getNetId();
			s.x = e.getX();
			s.y = e.getY();
			s.blastRadius = e.getBlastRadius();
			snap.explosions.add(s);
		}
		return snap;
	}
	
	public String encode()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(Protocol.STATE);
		sb.append('|').append(phase);
		sb.append('|').append(countSec);
		sb.append('|').append(Protocol.encodeName(winnerName.isEmpty() ? "-" : winnerName));
		sb.append("|P|").append(players.size());
		for(PlayerSnap p : players)
		{
			sb.append('|').append(p.id);
			sb.append(',').append(Protocol.encodeName(p.name));
			sb.append(',').append(p.team.ordinal());
			sb.append(',').append(fmt(p.x));
			sb.append(',').append(fmt(p.y));
			sb.append(',').append(p.dir);
			sb.append(',').append(fmt(p.hp));
			sb.append(',').append(p.armor);
			sb.append(',').append(p.score);
			sb.append(',').append(p.invincible ? 1 : 0);
		}
		sb.append("|W|").append(weapons.size());
		for(WeaponSnap w : weapons)
		{
			sb.append('|').append(w.netId);
			sb.append(',').append(w.type);
			sb.append(',').append(fmt(w.x));
			sb.append(',').append(fmt(w.y));
			sb.append(',').append(w.dir);
			sb.append(',').append(w.ownerId);
			sb.append(',').append(w.ammo);
		}
		sb.append("|B|").append(bullets.size());
		for(BulletSnap b : bullets)
		{
			sb.append('|').append(b.netId);
			sb.append(',').append(b.type);
			sb.append(',').append(fmt(b.x));
			sb.append(',').append(fmt(b.y));
			sb.append(',').append(b.dir);
			sb.append(',').append(b.team == null ? 0 : b.team.ordinal());
		}
		sb.append("|I|").append(items.size());
		for(ItemSnap i : items)
		{
			sb.append('|').append(i.netId);
			sb.append(',').append(i.type);
			sb.append(',').append(fmt(i.x));
			sb.append(',').append(fmt(i.y));
		}
		sb.append("|E|").append(explosions.size());
		for(ExplosionSnap e : explosions)
		{
			sb.append('|').append(e.netId);
			sb.append(',').append(fmt(e.x));
			sb.append(',').append(fmt(e.y));
			sb.append(',').append(e.blastRadius);
		}
		return sb.toString();
	}
	
	public static WorldSnapshot decode(String line)
	{
		WorldSnapshot snap = new WorldSnapshot();
		String[] t = line.split("\\|", -1);
		if(t.length < 8 || !Protocol.STATE.equals(t[0])) return snap;
		int i = 1;
		snap.phase = t[i++];
		snap.countSec = parseLong(t[i++]);
		snap.winnerName = Protocol.decodeName(t[i++]);
		if("-".equals(snap.winnerName)) snap.winnerName = "";
		if(i >= t.length || !"P".equals(t[i++])) return snap;
		int n = parseInt(t[i++]);
		for(int a = 0; a < n && i < t.length; a++)
		{
			String[] f = t[i++].split(",", -1);
			if(f.length < 10) continue;
			PlayerSnap p = new PlayerSnap();
			p.id = parseInt(f[0]);
			p.name = Protocol.decodeName(f[1]);
			p.team = teamAt(parseInt(f[2]));
			p.x = parseDouble(f[3]);
			p.y = parseDouble(f[4]);
			p.dir = parseInt(f[5]);
			p.hp = parseDouble(f[6]);
			p.armor = parseInt(f[7]);
			p.score = parseInt(f[8]);
			p.invincible = parseInt(f[9]) == 1;
			snap.players.add(p);
		}
		if(i >= t.length || !"W".equals(t[i++])) return snap;
		n = parseInt(t[i++]);
		for(int a = 0; a < n && i < t.length; a++)
		{
			String[] f = t[i++].split(",", -1);
			if(f.length < 7) continue;
			WeaponSnap w = new WeaponSnap();
			w.netId = parseInt(f[0]);
			w.type = parseInt(f[1]);
			w.x = parseDouble(f[2]);
			w.y = parseDouble(f[3]);
			w.dir = parseInt(f[4]);
			w.ownerId = parseInt(f[5]);
			w.ammo = parseInt(f[6]);
			snap.weapons.add(w);
		}
		if(i >= t.length || !"B".equals(t[i++])) return snap;
		n = parseInt(t[i++]);
		for(int a = 0; a < n && i < t.length; a++)
		{
			String[] f = t[i++].split(",", -1);
			if(f.length < 6) continue;
			BulletSnap b = new BulletSnap();
			b.netId = parseInt(f[0]);
			b.type = parseInt(f[1]);
			b.x = parseDouble(f[2]);
			b.y = parseDouble(f[3]);
			b.dir = parseInt(f[4]);
			b.team = teamAt(parseInt(f[5]));
			snap.bullets.add(b);
		}
		if(i >= t.length || !"I".equals(t[i++])) return snap;
		n = parseInt(t[i++]);
		for(int a = 0; a < n && i < t.length; a++)
		{
			String[] f = t[i++].split(",", -1);
			if(f.length < 4) continue;
			ItemSnap it = new ItemSnap();
			it.netId = parseInt(f[0]);
			it.type = parseInt(f[1]);
			it.x = parseDouble(f[2]);
			it.y = parseDouble(f[3]);
			snap.items.add(it);
		}
		if(i >= t.length || !"E".equals(t[i++])) return snap;
		n = parseInt(t[i++]);
		for(int a = 0; a < n && i < t.length; a++)
		{
			String[] f = t[i++].split(",", -1);
			if(f.length < 4) continue;
			ExplosionSnap e = new ExplosionSnap();
			e.netId = parseInt(f[0]);
			e.x = parseDouble(f[1]);
			e.y = parseDouble(f[2]);
			e.blastRadius = parseInt(f[3]);
			snap.explosions.add(e);
		}
		return snap;
	}
	
	public void apply(TileMap tileMap, Library library, Set<Integer> seenExplosions)
	{
		Set<Integer> livePlayers = new HashSet<Integer>();
		for(PlayerSnap s : players)
		{
			livePlayers.add(s.id);
			Player p = findPlayer(s.id);
			if(p == null)
			{
				java.awt.image.BufferedImage img = imageFor(s.id);
				p = new Player(img, tileMap, s.id, s.score, s.team);
				p.init();
			}
			p.setName(s.name);
			p.setTeam(s.team);
			p.setX(s.x);
			p.setY(s.y);
			p.setDirection(s.dir);
			p.setHitpoints(s.hp);
			p.setArmor(s.armor);
			p.setScore(s.score);
			if(s.invincible)
			{
				if(!p.isInvincible()) p.setInvincible();
			}
			else if(p.isInvincible())
			{
				p.setVulnerable();
			}
			p.updateHitbox();
			p.getGraphics().update();
		}
		for(int i = Player.PlayerList.size() - 1; i >= 0; i--)
		{
			Player p = Player.PlayerList.get(i);
			if(!livePlayers.contains(p.getID()))
			{
				p.playDeathSound();
				p.removeThis();
			}
		}
		
		Set<Integer> liveWeapons = new HashSet<Integer>();
		for(WeaponSnap s : weapons)
		{
			if(s.type < 0) continue;
			liveWeapons.add(s.netId);
			Weapon w = findWeapon(s.netId);
			if(w == null)
			{
				w = library.getNewFirearm(s.type, tileMap);
				if(w == null) continue;
				w.setNetId(s.netId);
				w.mute();
			}
			w.setX(s.x);
			w.setY(s.y);
			w.setDirection(s.dir);
			w.setAmmoCount(s.ammo);
			if(s.ownerId >= 0)
			{
				Player owner = findPlayer(s.ownerId);
				if(owner != null)
				{
					w.setPlayer(owner);
					w.setEquipped(true);
				}
			}
			else
			{
				w.setEquipped(false);
				w.setPlayer(null);
			}
			w.updateHitbox();
		}
		for(int i = Weapon.WeaponList.size() - 1; i >= 0; i--)
		{
			Weapon w = Weapon.WeaponList.get(i);
			if(!liveWeapons.contains(w.getNetId()))
				w.removeThis();
		}
		
		Set<Integer> liveBullets = new HashSet<Integer>();
		for(BulletSnap s : bullets)
		{
			liveBullets.add(s.netId);
			Bullet b = findBullet(s.netId);
			if(b == null)
			{
				b = library.createBullet(s.type, tileMap, s.team);
				b.setNetId(s.netId);
			}
			b.setX(s.x);
			b.setY(s.y);
			b.setDirection(s.dir);
			b.updateHitbox();
		}
		for(int i = Bullet.BulletList.size() - 1; i >= 0; i--)
		{
			Bullet b = Bullet.BulletList.get(i);
			if(!liveBullets.contains(b.getNetId()))
				b.removeThis();
		}
		
		Set<Integer> liveItems = new HashSet<Integer>();
		for(ItemSnap s : items)
		{
			if(s.type < 0) continue;
			liveItems.add(s.netId);
			Item it = findItem(s.netId);
			if(it == null)
			{
				it = library.createItem(s.type, tileMap);
				if(it == null) continue;
				it.setNetId(s.netId);
			}
			it.setX(s.x);
			it.setY(s.y);
			it.updateHitbox();
		}
		for(int i = Item.ItemList.size() - 1; i >= 0; i--)
		{
			Item it = Item.ItemList.get(i);
			if(!liveItems.contains(it.getNetId()))
				it.removeThis();
		}
		
		for(ExplosionSnap s : explosions)
		{
			if(seenExplosions.add(s.netId))
				new Explosion(s.x, s.y, 0, s.blastRadius, true);
		}
		Explosion.updateExplosions();
	}
	
	private static Player findPlayer(int id)
	{
		for(Player p : Player.PlayerList)
			if(p.getID() == id) return p;
		return null;
	}
	
	private static Weapon findWeapon(int netId)
	{
		for(Weapon w : Weapon.WeaponList)
			if(w.getNetId() == netId) return w;
		return null;
	}
	
	private static Bullet findBullet(int netId)
	{
		for(Bullet b : Bullet.BulletList)
			if(b.getNetId() == netId) return b;
		return null;
	}
	
	private static Item findItem(int netId)
	{
		for(Item i : Item.ItemList)
			if(i.getNetId() == netId) return i;
		return null;
	}
	
	private static java.awt.image.BufferedImage imageFor(int playerId)
	{
		if(ImageBlock.PlayerImages.isEmpty()) return null;
		return ImageBlock.PlayerImages.get((playerId - 1) % ImageBlock.PlayerImages.size());
	}
	
	private static Team teamAt(int ordinal)
	{
		Team[] values = Team.values();
		if(ordinal < 0 || ordinal >= values.length) return Team.BLUE;
		return values[ordinal];
	}
	
	private static String fmt(double v)
	{
		return String.format(Locale.US, "%.1f", v);
	}
	
	private static int parseInt(String s)
	{
		try { return Integer.parseInt(s); } catch(Exception e) { return 0; }
	}
	
	private static long parseLong(String s)
	{
		try { return Long.parseLong(s); } catch(Exception e) { return 0; }
	}
	
	private static double parseDouble(String s)
	{
		try { return Double.parseDouble(s); } catch(Exception e) { return 0; }
	}
}
