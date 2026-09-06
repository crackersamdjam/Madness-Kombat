package Multiplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import GameObject.Library;
import GameObject.Item.Item;
import GameObject.Player.Player;
import GameObject.Player.PlayerKeyset;
import GameObject.Weapon.Bullet;
import GameObject.Weapon.Explosion;
import GameObject.Weapon.Weapon;
import GameState.GameState;
import GameState.GameStateManager;
import GameState.LevelState.GameMode;
import GameState.LevelState.LevelState;
import Main.GamePanel;
import Main.MusicLoop;
import TileMap.TileMap;

public class ClientPlayState extends GameState {
	
	private static final int MODE_FORM = 0;
	private static final int MODE_CONNECTING = 1;
	private static final int MODE_LOBBY = 2;
	private static final int MODE_PLAYING = 3;
	private static final int MODE_ERROR = 4;
	
	private int mode = MODE_FORM;
	private int field = 0;
	private String ip = "127.0.0.1";
	private String name = "Player";
	private String error = "";
	private String phase = Protocol.PHASE_PLAYING;
	private String winnerName = "";
	private long countSec;
	private final ArrayList<String> lobbyNames = new ArrayList<String>();
	private TileMap tileMap;
	private final Library library = new Library();
	private final Set<Integer> seenExplosions = new HashSet<Integer>();
	private MusicLoop soundtrack;
	private boolean up, down, left, right, shoot;
	private volatile boolean connectFailed;
	private volatile String connectError = "";
	private volatile NetClient pendingClient;
	
	public ClientPlayState(GameStateManager gsm)
	{
		this.gsm = gsm;
	}
	
	public void init()
	{
		clearWorld();
		mode = MODE_FORM;
		field = 0;
		ip = "127.0.0.1";
		if(gsm.getPlayerNames() != null && gsm.getPlayerNames()[0] != null)
			name = gsm.getPlayerNames()[0];
		error = "";
		phase = Protocol.PHASE_PLAYING;
		winnerName = "";
		countSec = 0;
		lobbyNames.clear();
		seenExplosions.clear();
		up = down = left = right = shoot = false;
		connectFailed = false;
		connectError = "";
		pendingClient = null;
		if(soundtrack == null)
			soundtrack = new MusicLoop(getClass().getResource("/levelSoundtrack.wav"));
		else
			soundtrack.stop();
	}
	
	public void update()
	{
		gsm.bg.update();
		if(mode == MODE_CONNECTING)
		{
			if(connectFailed)
			{
				error = connectError;
				mode = MODE_ERROR;
			}
			else if(pendingClient != null)
			{
				NetworkSession.startClient(pendingClient, 0);
				NetworkSession.sendToServer(Protocol.LOGIN + "|" + Protocol.encodeName(name));
				pendingClient = null;
				mode = MODE_LOBBY;
			}
		}
		if(NetworkSession.isClient())
			drainServerMessages();
		if(mode == MODE_PLAYING)
		{
			NetworkSession.sendToServer(Protocol.INPUT + "|"
					+ bit(up) + "|" + bit(down) + "|" + bit(left) + "|" + bit(right) + "|" + bit(shoot));
		}
	}
	
	public void draw(Graphics2D g)
	{
		if(mode == MODE_PLAYING && tileMap != null)
			drawMatch(g);
		else
			drawMenu(g);
	}
	
	public void keyPressed(int k)
	{
		if(k == KeyEvent.VK_ESCAPE)
		{
			leave();
			return;
		}
		if(mode == MODE_FORM || mode == MODE_ERROR)
		{
			if(k == KeyEvent.VK_ENTER)
			{
				connect();
				return;
			}
			if(k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN || k == KeyEvent.VK_TAB)
			{
				field = 1 - field;
				return;
			}
			if(k == KeyEvent.VK_BACK_SPACE)
			{
				if(field == 0 && ip.length() > 0) ip = ip.substring(0, ip.length() - 1);
				if(field == 1 && name.length() > 0) name = name.substring(0, name.length() - 1);
				return;
			}
			char c = keyToChar(k);
			if(c == 0) return;
			if(field == 0 && ip.length() < 32)
				ip += c;
			else if(field == 1 && name.length() < 16 && (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-'))
				name += c;
		}
		else if(mode == MODE_PLAYING)
		{
			applyKey(k, true);
		}
	}
	
	public void keyReleased(int k)
	{
		if(mode == MODE_PLAYING)
			applyKey(k, false);
	}
	
	private void connect()
	{
		error = "";
		connectFailed = false;
		connectError = "";
		pendingClient = null;
		mode = MODE_CONNECTING;
		final String target = ip.trim();
		final String playerName = Protocol.sanitizeName(name);
		name = playerName;
		Thread t = new Thread(new Runnable() {
			public void run()
			{
				try
				{
					NetClient client = new NetClient(target, Protocol.PORT, new NetClient.Listener() {
						public void onMessage(String line)
						{
							NetworkSession.offerClientMessage(line);
						}
						public void onDisconnected()
						{
							NetworkSession.offerClientMessage(Protocol.END);
						}
					});
					pendingClient = client;
				}
				catch(Exception e)
				{
					connectError = "Could not connect: " + e.getMessage();
					connectFailed = true;
				}
			}
		}, "mk-join");
		t.setDaemon(true);
		t.start();
	}
	
	private void drainServerMessages()
	{
		String line;
		while((line = NetworkSession.pollClientMessage()) != null)
			handleMessage(line);
	}
	
	private void handleMessage(String line)
	{
		if(line == null || line.isEmpty()) return;
		String[] parts = line.split("\\|", -1);
		String type = parts[0];
		if(Protocol.WELCOME.equals(type) && parts.length >= 2)
		{
			try { NetworkSession.startClient(NetworkSession.getClient(), Integer.parseInt(parts[1])); }
			catch(Exception e) {}
		}
		else if(Protocol.LOBBY.equals(type) && parts.length >= 2)
		{
			lobbyNames.clear();
			int n = 0;
			try { n = Integer.parseInt(parts[1]); } catch(Exception e) {}
			for(int i = 0; i < n && 2 + i < parts.length; i++)
			{
				String[] f = parts[2 + i].split(",", -1);
				if(f.length >= 2) lobbyNames.add(Protocol.decodeName(f[1]));
			}
			if(mode != MODE_PLAYING) mode = MODE_LOBBY;
		}
		else if(Protocol.REJECT.equals(type))
		{
			error = parts.length > 1 ? parts[1] : "Rejected by host";
			NetworkSession.shutdown();
			mode = MODE_ERROR;
		}
		else if(Protocol.START.equals(type) && parts.length >= 4)
		{
			beginMatch(parts[1], parts[2]);
		}
		else if(Protocol.STATE.equals(type) && mode == MODE_PLAYING)
		{
			WorldSnapshot snap = WorldSnapshot.decode(line);
			phase = snap.phase;
			countSec = snap.countSec;
			winnerName = snap.winnerName;
			if(Protocol.PHASE_COUNTDOWN.equals(phase))
				seenExplosions.clear();
			snap.apply(tileMap, library, seenExplosions);
		}
		else if(Protocol.END.equals(type))
		{
			leave();
		}
	}
	
	private void beginMatch(String mapName, String gameModeName)
	{
		try { LevelState.setGameMode(GameMode.valueOf(gameModeName)); }
		catch(Exception e) { LevelState.setGameMode(GameMode.LAST_MAN_STANDING); }
		GamePanel.setLevelName(mapName);
		GamePanel.setLevelFile(new File(GamePanel.getLevelFilePath()));
		clearWorld();
		tileMap = new TileMap(30);
		tileMap.loadTiles("/tileset.png");
		tileMap.loadMap(GamePanel.getLevelFile());
		seenExplosions.clear();
		phase = Protocol.PHASE_COUNTDOWN;
		mode = MODE_PLAYING;
		soundtrack.play();
	}
	
	private void drawMenu(Graphics2D g)
	{
		gsm.bg.draw(g);
		g.setColor(Color.BLACK);
		g.setFont(new Font("Comic Sans MS", Font.PLAIN, 40));
		g.drawString("Join Game", 80, 140);
		g.setFont(new Font("Consolas", Font.PLAIN, 22));
		if(mode == MODE_FORM || mode == MODE_ERROR)
		{
			drawField(g, "Host IP", ip, 200, field == 0);
			drawField(g, "Your name", name, 260, field == 1);
			g.setColor(Color.BLACK);
			g.drawString("[ENTER] Connect   [ESC] Back", 80, 360);
			if(mode == MODE_ERROR && error != null && !error.isEmpty())
			{
				g.setColor(Color.RED);
				g.drawString(error, 80, 400);
			}
		}
		else if(mode == MODE_CONNECTING)
		{
			g.drawString("Connecting to " + ip + "...", 80, 220);
		}
		else if(mode == MODE_LOBBY)
		{
			g.drawString("Connected. Waiting for the host to start.", 80, 210);
			g.drawString("Players", 80, 260);
			for(int i = 0; i < lobbyNames.size(); i++)
				g.drawString((i + 1) + ". " + lobbyNames.get(i), 80, 296 + i * 28);
			g.drawString("[ESC] Leave", 80, 460);
		}
	}
	
	private void drawMatch(Graphics2D g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
		tileMap.draw(g);
		Player.drawPlayers(g);
		Item.drawItems(g);
		Weapon.drawWeapons(g);
		Bullet.drawBullets(g);
		g.setColor(Color.BLACK);
		g.setFont(new Font("impact", Font.PLAIN, 60));
		if(Protocol.PHASE_COUNTDOWN.equals(phase))
		{
			String time = String.valueOf(countSec);
			g.drawString(time,
					GamePanel.WIDTH / 2 - g.getFontMetrics().stringWidth(time) / 2,
					GamePanel.HEIGHT / 2 + g.getFontMetrics().getHeight() / 2);
		}
		else if(Protocol.PHASE_TRANSITION.equals(phase))
		{
			String result = winnerName.isEmpty() ? "No one wins..." : winnerName + " wins!";
			g.drawString(result,
					GamePanel.WIDTH / 2 - g.getFontMetrics().stringWidth(result) / 2,
					GamePanel.HEIGHT / 2 + g.getFontMetrics().getHeight() / 2);
		}
		else if(Protocol.PHASE_PAUSE.equals(phase))
		{
			g.setFont(new Font("consolas", Font.PLAIN, 24));
			g.drawString("Host paused the game", 80, 140);
			g.drawString("[ESC] to leave", 80, 176);
		}
	}
	
	private void drawField(Graphics2D g, String label, String value, int y, boolean selected)
	{
		g.setColor(selected ? Color.RED : Color.BLACK);
		g.drawString(label + ": " + value + (selected ? "_" : ""), 80, y);
	}
	
	private void applyKey(int k, boolean pressed)
	{
		PlayerKeyset keys = gsm.getKeyset(0);
		if(k == keys.getUpKey()) up = pressed;
		else if(k == keys.getDownKey()) down = pressed;
		else if(k == keys.getLeftKey()) left = pressed;
		else if(k == keys.getRightKey()) right = pressed;
		else if(k == keys.getShootKey()) shoot = pressed;
	}
	
	private void leave()
	{
		if(soundtrack != null) soundtrack.stop();
		clearWorld();
		NetworkSession.shutdown();
		gsm.setState(GameStateManager.MENUSTATE);
	}
	
	private void clearWorld()
	{
		Player.PlayerList.clear();
		while(!Weapon.WeaponList.isEmpty()) Weapon.WeaponList.get(0).removeThis();
		Bullet.BulletList.clear();
		Item.ItemList.clear();
		Explosion.ExplosionList.clear();
	}
	
	private static String bit(boolean b)
	{
		return b ? "1" : "0";
	}
	
	private static char keyToChar(int k)
	{
		if(k >= KeyEvent.VK_A && k <= KeyEvent.VK_Z) return (char)('a' + (k - KeyEvent.VK_A));
		if(k >= KeyEvent.VK_0 && k <= KeyEvent.VK_9) return (char)('0' + (k - KeyEvent.VK_0));
		if(k >= KeyEvent.VK_NUMPAD0 && k <= KeyEvent.VK_NUMPAD9) return (char)('0' + (k - KeyEvent.VK_NUMPAD0));
		if(k == KeyEvent.VK_PERIOD || k == KeyEvent.VK_DECIMAL) return '.';
		if(k == KeyEvent.VK_MINUS) return '-';
		if(k == KeyEvent.VK_SPACE) return ' ';
		return 0;
	}
}
