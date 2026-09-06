package Main;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundEffect {
	
	public static boolean MUTE;
	
	private URL url;
	private Clip clip;
	
	public SoundEffect(URL url)
	{
		this.url = url;
	}
	
	public void play()
	{
		if(MUTE || url == null) return;
		try {
			if(clip == null || !clip.isOpen())
			{
				AudioInputStream stream = AudioSystem.getAudioInputStream(url);
				clip = AudioSystem.getClip();
				clip.open(stream);
				try { stream.close(); } catch(IOException e) {}
			}
			if(clip.isRunning())
				clip.stop();
			clip.setFramePosition(0);
			clip.start();
		} catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
			clip = null;
		}
	}
	
	public void stop()
	{
		if(clip != null)
		{
			try {
				clip.stop();
				clip.close();
			} catch(Exception e) {}
			clip = null;
		}
	}
}
