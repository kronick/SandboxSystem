package kronick.sandboxsystem;

public class Block {
	public int blockColor;
	public int[][] xformedColors;
	XY position;
	int sizeX, sizeY;

	float nodeScale;

	boolean free = true;
	boolean selected = false;

	int state;
	static final int FREE     = 0;
	static final int MOVING   = 1;
	static final int XFORMING = 2;

	int type;
	static final int NEW     = 0;
	static final int XFORMED = 1;
	static final int STALE   = 2;

	int age;

	SandboxSystem parent;

	Block(SandboxSystem parent, XY iorigin) {
		this(parent, iorigin, parent.GRID_SIZE);
	}

	Block(SandboxSystem parent, XY iorigin, int size) {
		this.parent = parent;
		this.position = new XY(iorigin);

		this.blockColor = parent.color(40, 255, parent.random(150,255));
		this.xformedColors = new int[4][4];
		this.sizeX = size;
		this.sizeY = size;
		this.state = FREE;
		this.type  = NEW;
		age = 100000;
	}

	void update() {
		age++;
		if(age > 10000 && type == XFORMED) { type = STALE; }
	}

	void setXFormed() {
		type = Block.XFORMED;
		state = Block.FREE;
		age = 0;
	}
	void draw() {
		parent.pushStyle();
		parent.noStroke();
		if(!selected)
			parent.fill(this.blockColor);
		else
			parent.fill(0,200,255);
		parent.rectMode(parent.CENTER);

		if(parent.inView((float)position.x + sizeX/2, (float)position.y + sizeY/2) || parent.inView((float)position.x - sizeX/2, (float)position.y - sizeY/2) ||
			parent.inView((float)position.x + sizeX/2, (float)position.y - sizeY/2) || parent.inView((float)position.x - sizeX/2, (float)position.y + sizeY/2))
			if(type != XFORMED) {
				parent.rect((float)this.position.x, (float)this.position.y, this.sizeX, this.sizeY);
			}
			else {
				for(int i=0; i<xformedColors.length; i++) {
					for(int j=0; j<xformedColors[i].length; j++) {
						parent.fill(xformedColors[i][j]);
						//parent.stroke(xformedColors[i][j]);
						parent.rect((float)this.position.x - 3*this.sizeX/8 + i*this.sizeX/4,
								    (float)this.position.y - 3*this.sizeY/8 + j*this.sizeY/4,
								    this.sizeX/4 + 1, this.sizeY/4 + 1);
					}
				}
			}
		parent.popStyle();
	}

	public boolean workable() {
		return (type != XFORMED && state == FREE);
	}
}