package kronick.sandboxsystem;

public class Block {
	public int blockColor;
	XY position;
	int sizeX, sizeY;

	float nodeScale;

	boolean free = true;
	boolean selected = false;

	SandboxSystem parent;

	Block(SandboxSystem parent, XY iorigin) {
		this(parent, iorigin, parent.GRID_SIZE);
	}

	Block(SandboxSystem parent, XY iorigin, int size) {
		this.parent = parent;
		this.position = new XY(iorigin);

		this.blockColor = parent.color(40, 255, parent.random(150,255));
		this.sizeX = size;
		this.sizeY = size;
	}

	void update() {

	}

	void draw() {
		parent.pushStyle();
		parent.noStroke();
		if(!selected)
			parent.fill(this.blockColor);
		else
			parent.fill(0,200,255);
		parent.noSmooth();
		parent.rectMode(parent.CENTER);
		parent.rect((float)this.position.x, (float)this.position.y, this.sizeX, this.sizeY);
		parent.smooth();
		parent.popStyle();
	}
}