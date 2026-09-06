package Multiplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetClient {
	
	public interface Listener {
		void onMessage(String line);
		void onDisconnected();
	}
	
	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	private final Listener listener;
	private volatile boolean open = true;
	
	public NetClient(String ip, int port, Listener listener) throws IOException
	{
		this.listener = listener;
		this.socket = new Socket(ip, port);
		this.socket.setTcpNoDelay(true);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(socket.getOutputStream(), true);
		Thread reader = new Thread(new Runnable() {
			public void run()
			{
				try
				{
					String line;
					while(open && (line = in.readLine()) != null)
					{
						listener.onMessage(line);
					}
				}
				catch(IOException e) {}
				finally
				{
					open = false;
					listener.onDisconnected();
				}
			}
		}, "mk-client-reader");
		reader.setDaemon(true);
		reader.start();
	}
	
	public void send(String line)
	{
		if(!open) return;
		synchronized(out)
		{
			out.println(line);
			out.flush();
		}
	}
	
	public boolean isOpen()
	{
		return open;
	}
	
	public void close()
	{
		open = false;
		try { socket.close(); } catch(IOException e) {}
	}
}
