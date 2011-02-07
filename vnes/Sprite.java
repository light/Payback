package vnes;
public class Sprite {

	private int sprX; // X coordinate
	private int sprY; // Y coordinate
	private Tile tile;
	private int sprCol; // Upper two bits of color
	private boolean vertFlip; // Vertical Flip
	private boolean horiFlip; // Horizontal Flip

	public int getSprX() {
		return sprX;
	}

	public void setSprX(int sprX) {
		this.sprX = sprX;
	}

	public int getSprY() {
		return sprY;
	}

	public void setSprY(int sprY) {
		this.sprY = sprY;
	}

	public Tile getTile() {
		return tile;
	}

	public void setTile(Tile tile) {
		this.tile = tile;
	}

	public int getSprCol() {
		return sprCol;
	}

	public void setSprCol(int sprCol) {
		this.sprCol = sprCol;
	}

	public boolean isVertFlip() {
		return vertFlip;
	}

	public void setVertFlip(boolean vertFlip) {
		this.vertFlip = vertFlip;
	}

	public boolean isHoriFlip() {
		return horiFlip;
	}

	public void setHoriFlip(boolean horiFlip) {
		this.horiFlip = horiFlip;
	}

}
