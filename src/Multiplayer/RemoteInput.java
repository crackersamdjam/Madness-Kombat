package Multiplayer;

public class RemoteInput {
	
	public boolean up;
	public boolean down;
	public boolean left;
	public boolean right;
	public boolean shoot;
	public boolean jumpQueued;
	public boolean downQueued;
	public boolean downReleasedQueued;
	
	public void set(boolean up, boolean down, boolean left, boolean right, boolean shoot)
	{
		if(up && !this.up) jumpQueued = true;
		if(down && !this.down) downQueued = true;
		if(!down && this.down) downReleasedQueued = true;
		this.up = up;
		this.down = down;
		this.left = left;
		this.right = right;
		this.shoot = shoot;
	}
}
