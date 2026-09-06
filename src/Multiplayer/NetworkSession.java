package Multiplayer;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import GameObject.Library;
import GameObject.Player.Player;
import GameState.GameStateManager;
import GameState.LevelState.LevelState;

public class NetworkSession {
	
	public enum Role { OFFLINE, HOST, CLIENT }
	
	public static class RosterEntry {
		public final int playerId;
		public final int connectionId;
		public final String name;
		
		public RosterEntry(int playerId, int connectionId, String name)
		{
			this.playerId = playerId;
			this.connectionId = connectionId;
			this.name = name;
		}
	}
	
	private static class Incoming {
		final int connectionId;
		final String line;
		Incoming(int connectionId, String line)
		{
			this.connectionId = connectionId;
			this.line = line;
		}
	}
	
	private static Role role = Role.OFFLINE;
	private static NetServer server;
	private static NetClient client;
	private static int localPlayerId = 1;
	private static boolean gameStarted;
	private static String statusMessage = "";
	private static final ArrayList<RosterEntry> roster = new ArrayList<RosterEntry>();
	private static final ConcurrentLinkedQueue<Incoming> hostInbox = new ConcurrentLinkedQueue<Incoming>();
	private static final ConcurrentLinkedQueue<String> clientInbox = new ConcurrentLinkedQueue<String>();
	private static final ConcurrentLinkedQueue<Integer> hostDisconnects = new ConcurrentLinkedQueue<Integer>();
	private static final Map<Integer, RemoteInput> remoteInputs = new ConcurrentHashMap<Integer, RemoteInput>();
	private static Library snapshotLibrary = new Library();
	
	public static boolean isHost() { return role == Role.HOST; }
	public static boolean isClient() { return role == Role.CLIENT; }
	public static boolean isOnline() { return role != Role.OFFLINE; }
	public static boolean isGameStarted() { return gameStarted; }
	public static int getLocalPlayerId() { return localPlayerId; }
	public static String getStatusMessage() { return statusMessage; }
	public static Role getRole() { return role; }
	
	public static synchronized ArrayList<RosterEntry> getRoster()
	{
		return new ArrayList<RosterEntry>(roster);
	}
	
	public static synchronized void startHost(String hostName)
	{
		shutdown();
		try
		{
			server = new NetServer(Protocol.PORT, new NetServer.Listener() {
				public void onConnect(int connectionId) {}
				public void onMessage(int connectionId, String line)
				{
					if(line != null) hostInbox.add(new Incoming(connectionId, line));
				}
				public void onDisconnect(int connectionId)
				{
					hostDisconnects.add(connectionId);
				}
			});
			role = Role.HOST;
			localPlayerId = 1;
			gameStarted = false;
			roster.clear();
			roster.add(new RosterEntry(1, -1, Protocol.sanitizeName(hostName)));
			statusMessage = "";
		}
		catch(Exception e)
		{
			statusMessage = "Could not start server: " + e.getMessage();
			role = Role.OFFLINE;
		}
	}
	
	public static void startClient(NetClient connected, int playerId)
	{
		client = connected;
		role = Role.CLIENT;
		localPlayerId = playerId;
		gameStarted = false;
		statusMessage = "";
	}
	
	public static void attachClientListener(NetClient connected)
	{
		client = connected;
		role = Role.CLIENT;
	}
	
	public static NetClient getClient() { return client; }
	
	public static void offerClientMessage(String line)
	{
		if(line != null) clientInbox.add(line);
	}
	
	public static String pollClientMessage()
	{
		return clientInbox.poll();
	}
	
	public static void sendToServer(String line)
	{
		if(client != null) client.send(line);
	}
	
	public static void tick(GameStateManager gsm)
	{
		if(role == Role.HOST)
		{
			drainHostMessages();
			if(gameStarted && gsm.getCurrentState() == GameStateManager.LEVELSTATE)
				server.broadcast(WorldSnapshot.capture(gsm, snapshotLibrary).encode());
		}
	}
	
	public static void applyRemoteInputs()
	{
		for(Map.Entry<Integer, RemoteInput> entry : remoteInputs.entrySet())
		{
			Player player = findPlayer(entry.getKey());
			if(player == null) continue;
			RemoteInput in = entry.getValue();
			if(in.left) player.leftPressed();
			else player.leftReleased();
			if(in.right) player.rightPressed();
			else player.rightReleased();
			if(in.shoot) player.shootPressed();
			else player.shootReleased();
			if(in.jumpQueued)
			{
				player.upPressed();
				in.jumpQueued = false;
			}
			if(in.downQueued)
			{
				player.downPressed();
				in.downQueued = false;
			}
			if(in.downReleasedQueued)
			{
				player.downReleased();
				in.downReleasedQueued = false;
			}
		}
	}
	
	public static synchronized boolean startMatch(GameStateManager gsm)
	{
		if(role != Role.HOST || roster.size() < 2) return false;
		String[] names = gsm.getPlayerNames();
		if(names == null || names.length < Protocol.MAX_PLAYERS)
			names = new String[] {"Player 1", "Player 2", "Player 3", "Player 4"};
		ArrayList<Integer> preset = new ArrayList<Integer>();
		StringBuilder start = new StringBuilder();
		start.append(Protocol.START);
		start.append('|').append(Main.GamePanel.getLevelName());
		start.append('|').append(LevelState.getGameMode().name());
		start.append('|').append(roster.size());
		for(RosterEntry entry : roster)
		{
			names[entry.playerId - 1] = entry.name;
			preset.add(entry.playerId);
			start.append('|').append(entry.playerId);
			start.append(',').append(Protocol.encodeName(entry.name));
		}
		gsm.setPlayerNames(names);
		gsm.setPlayerPreset(preset);
		gameStarted = true;
		if(server != null) server.broadcast(start.toString());
		return true;
	}
	
	public static synchronized void shutdown()
	{
		if(server != null)
		{
			try { server.broadcast(Protocol.END); } catch(Exception e) {}
			server.close();
			server = null;
		}
		if(client != null)
		{
			try { client.send(Protocol.LEAVE); } catch(Exception e) {}
			client.close();
			client = null;
		}
		role = Role.OFFLINE;
		gameStarted = false;
		localPlayerId = 1;
		statusMessage = "";
		roster.clear();
		remoteInputs.clear();
		hostInbox.clear();
		clientInbox.clear();
		hostDisconnects.clear();
	}
	
	public static String getLanAddress()
	{
		try
		{
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while(ifaces.hasMoreElements())
			{
				NetworkInterface iface = ifaces.nextElement();
				if(!iface.isUp() || iface.isLoopback()) continue;
				Enumeration<InetAddress> addrs = iface.getInetAddresses();
				while(addrs.hasMoreElements())
				{
					InetAddress addr = addrs.nextElement();
					if(addr instanceof Inet4Address && !addr.isLoopbackAddress())
						return addr.getHostAddress();
				}
			}
			return InetAddress.getLocalHost().getHostAddress();
		}
		catch(Exception e)
		{
			return "127.0.0.1";
		}
	}
	
	private static void drainHostMessages()
	{
		Integer disconnected;
		while((disconnected = hostDisconnects.poll()) != null)
			handleDisconnect(disconnected);
		Incoming incoming;
		while((incoming = hostInbox.poll()) != null)
			handleHostMessage(incoming.connectionId, incoming.line);
	}
	
	private static synchronized void handleHostMessage(int connectionId, String line)
	{
		if(line == null || line.isEmpty()) return;
		String[] parts = line.split("\\|", -1);
		String type = parts[0];
		if(Protocol.LOGIN.equals(type))
		{
			if(gameStarted)
			{
				if(server != null) server.send(connectionId, Protocol.REJECT + "|Match already started");
				return;
			}
			if(roster.size() >= Protocol.MAX_PLAYERS)
			{
				if(server != null) server.send(connectionId, Protocol.REJECT + "|Server is full");
				return;
			}
			String name = Protocol.sanitizeName(parts.length > 1 ? Protocol.decodeName(parts[1]) : "Player");
			int playerId = nextPlayerId();
			roster.add(new RosterEntry(playerId, connectionId, name));
			remoteInputs.put(playerId, new RemoteInput());
			if(server != null)
			{
				server.send(connectionId, Protocol.WELCOME + "|" + playerId + "|" + Protocol.encodeName(name));
				server.broadcast(encodeLobby());
			}
		}
		else if(Protocol.INPUT.equals(type) && parts.length >= 6)
		{
			int playerId = playerIdForConnection(connectionId);
			if(playerId <= 1) return;
			RemoteInput input = remoteInputs.get(playerId);
			if(input == null)
			{
				input = new RemoteInput();
				remoteInputs.put(playerId, input);
			}
			input.set(flag(parts[1]), flag(parts[2]), flag(parts[3]), flag(parts[4]), flag(parts[5]));
		}
		else if(Protocol.LEAVE.equals(type))
		{
			handleDisconnect(connectionId);
		}
	}
	
	private static synchronized void handleDisconnect(int connectionId)
	{
		Iterator<RosterEntry> it = roster.iterator();
		int removedId = -1;
		while(it.hasNext())
		{
			RosterEntry entry = it.next();
			if(entry.connectionId == connectionId)
			{
				removedId = entry.playerId;
				it.remove();
				remoteInputs.remove(entry.playerId);
				break;
			}
		}
		if(removedId > 0 && gameStarted)
		{
			Player player = findPlayer(removedId);
			if(player != null) player.die();
		}
		if(!gameStarted && server != null)
			server.broadcast(encodeLobby());
	}
	
	private static int nextPlayerId()
	{
		boolean[] used = new boolean[Protocol.MAX_PLAYERS + 1];
		for(RosterEntry entry : roster)
			used[entry.playerId] = true;
		for(int i = 1; i <= Protocol.MAX_PLAYERS; i++)
			if(!used[i]) return i;
		return -1;
	}
	
	private static int playerIdForConnection(int connectionId)
	{
		for(RosterEntry entry : roster)
			if(entry.connectionId == connectionId) return entry.playerId;
		return -1;
	}
	
	private static String encodeLobby()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(Protocol.LOBBY).append('|').append(roster.size());
		for(RosterEntry entry : roster)
			sb.append('|').append(entry.playerId).append(',').append(Protocol.encodeName(entry.name));
		return sb.toString();
	}
	
	private static boolean flag(String s)
	{
		return "1".equals(s);
	}
	
	private static Player findPlayer(int id)
	{
		for(Player p : Player.PlayerList)
			if(p.getID() == id) return p;
		return null;
	}
}
