package Multiplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NetServer {
	
	public interface Listener {
		void onConnect(int connectionId);
		void onMessage(int connectionId, String line);
		void onDisconnect(int connectionId);
	}
	
	private final ServerSocket serverSocket;
	private final Listener listener;
	private final AtomicInteger nextId = new AtomicInteger(1);
	private final Map<Integer, Connection> connections = new ConcurrentHashMap<Integer, Connection>();
	private volatile boolean open = true;
	
	public NetServer(int port, Listener listener) throws IOException
	{
		this.listener = listener;
		this.serverSocket = new ServerSocket(port);
		Thread acceptThread = new Thread(new Runnable() {
			public void run()
			{
				while(open)
				{
					try
					{
						Socket socket = serverSocket.accept();
						socket.setTcpNoDelay(true);
						final int id = nextId.getAndIncrement();
						final Connection connection = new Connection(id, socket);
						connections.put(id, connection);
						Thread reader = new Thread(new Runnable() {
							public void run()
							{
								try
								{
									listener.onConnect(id);
									String line;
									while(open && (line = connection.in.readLine()) != null)
									{
										listener.onMessage(id, line);
									}
								}
								catch(IOException e) {}
								finally
								{
									removeConnection(id);
								}
							}
						}, "mk-client-" + id);
						reader.setDaemon(true);
						reader.start();
					}
					catch(IOException e)
					{
						if(open) e.printStackTrace();
					}
				}
			}
		}, "mk-server-accept");
		acceptThread.setDaemon(true);
		acceptThread.start();
	}
	
	public void send(int connectionId, String line)
	{
		Connection connection = connections.get(connectionId);
		if(connection == null) return;
		synchronized(connection.out)
		{
			connection.out.println(line);
			connection.out.flush();
		}
	}
	
	public void broadcast(String line)
	{
		for(Connection connection : connections.values())
		{
			synchronized(connection.out)
			{
				connection.out.println(line);
				connection.out.flush();
			}
		}
	}
	
	public void close()
	{
		open = false;
		try { serverSocket.close(); } catch(IOException e) {}
		for(Integer id : connections.keySet().toArray(new Integer[0]))
			removeConnection(id);
	}
	
	private void removeConnection(int id)
	{
		Connection connection = connections.remove(id);
		if(connection == null) return;
		try { connection.socket.close(); } catch(IOException e) {}
		listener.onDisconnect(id);
	}
	
	private static class Connection {
		final int id;
		final Socket socket;
		final BufferedReader in;
		final PrintWriter out;
		
		Connection(int id, Socket socket) throws IOException
		{
			this.id = id;
			this.socket = socket;
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.out = new PrintWriter(socket.getOutputStream(), true);
		}
	}
}
