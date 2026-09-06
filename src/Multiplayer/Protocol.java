package Multiplayer;

public final class Protocol {
	
	public static final int PORT = 1331;
	public static final int MAX_PLAYERS = 4;
	
	public static final String LOGIN = "LOGIN";
	public static final String INPUT = "INPUT";
	public static final String LEAVE = "LEAVE";
	public static final String WELCOME = "WELCOME";
	public static final String LOBBY = "LOBBY";
	public static final String REJECT = "REJECT";
	public static final String START = "START";
	public static final String STATE = "STATE";
	public static final String END = "END";
	
	public static final String PHASE_COUNTDOWN = "COUNTDOWN";
	public static final String PHASE_PLAYING = "PLAYING";
	public static final String PHASE_TRANSITION = "TRANSITION";
	public static final String PHASE_PAUSE = "PAUSE";
	
	private Protocol() {}
	
	public static String sanitizeName(String name)
	{
		if(name == null) return "Player";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < name.length() && sb.length() < 16; i++)
		{
			char c = name.charAt(i);
			if(Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-')
				sb.append(c);
		}
		String cleaned = sb.toString().trim();
		if(cleaned.isEmpty()) return "Player";
		return cleaned;
	}
	
	public static String encodeName(String name)
	{
		return sanitizeName(name).replace(' ', '_');
	}
	
	public static String decodeName(String name)
	{
		if(name == null) return "Player";
		return name.replace('_', ' ');
	}
}
