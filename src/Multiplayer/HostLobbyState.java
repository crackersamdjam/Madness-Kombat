package Multiplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import GameState.GameState;
import GameState.GameStateManager;
import Main.GamePanel;

public class HostLobbyState extends GameState {
	
	private String lanAddress = "127.0.0.1";
	
	public HostLobbyState(GameStateManager gsm)
	{
		this.gsm = gsm;
	}
	
	public void init()
	{
		String hostName = "Host";
		if(gsm.getPlayerNames() != null && gsm.getPlayerNames()[0] != null)
			hostName = gsm.getPlayerNames()[0];
		NetworkSession.startHost(hostName);
		lanAddress = NetworkSession.getLanAddress();
	}
	
	public void update()
	{
		gsm.bg.update();
	}
	
	public void draw(Graphics2D g)
	{
		gsm.bg.draw(g);
		g.setColor(Color.BLACK);
		g.setFont(new Font("Comic Sans MS", Font.PLAIN, 40));
		g.drawString("Host Game", 80, 140);
		g.setFont(new Font("Consolas", Font.PLAIN, 22));
		g.drawString("Your LAN address: " + lanAddress + "   Port: " + Protocol.PORT, 80, 200);
		g.drawString("Friends join from the menu with that IP.", 80, 232);
		String error = NetworkSession.getStatusMessage();
		if(error != null && !error.isEmpty())
		{
			g.setColor(Color.RED);
			g.drawString(error, 80, 264);
			g.setColor(Color.BLACK);
		}
		g.drawString("Players", 80, 310);
		ArrayList<NetworkSession.RosterEntry> roster = NetworkSession.getRoster();
		for(int i = 0; i < roster.size(); i++)
		{
			NetworkSession.RosterEntry entry = roster.get(i);
			String label = entry.playerId + ". " + entry.name;
			if(entry.playerId == 1) label += "  (you)";
			g.drawString(label, 80, 346 + i * 28);
		}
		g.setColor(roster.size() >= 2 ? Color.BLACK : Color.GRAY);
		g.drawString("[ENTER] Start match   [ESC] Cancel", 80, 520);
		if(roster.size() < 2)
		{
			g.setColor(Color.DARK_GRAY);
			g.drawString("Waiting for at least one other player...", 80, 556);
		}
	}
	
	public void keyPressed(int k)
	{
		if(k == KeyEvent.VK_ESCAPE)
		{
			NetworkSession.shutdown();
			gsm.setState(GameStateManager.MENUSTATE);
		}
		else if(k == KeyEvent.VK_ENTER)
		{
			if(NetworkSession.startMatch(gsm))
				gsm.setState(GameStateManager.LEVELSTATE);
		}
	}
	
	public void keyReleased(int k) {}
}
