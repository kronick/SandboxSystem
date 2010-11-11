package kronick.sandboxsystem;

public class Worker {
	XY position;
	XY oldPosition;
	XY dPosition;

	float maxSpeed;
	float xFormSpeed;

	XY target;
	SandboxSystem parent;

	int state;
	final static int IDLE = 0;
	final static int BUSY = 1;
	final static int MOVE = 2;
	final static int ELEVATORAPPROACH = 20;
	final static int ELEVATORWAIT = 21;
	final static int ELEVATORMOVE = 22;

	int currFloor;
	int destFloor;

	float xformProgress;

	Elevator targetElevator;
	Block focusBlock;

	float headJoint, shoulderJoint, legJoint;

	public Worker(SandboxSystem parent, XY position) {
		this.parent = parent;
		this.position = position;
		this.oldPosition = position.get();
		this.target = this.position.get();
		this.dPosition = new XY(0,0);

		maxSpeed = 10;
		xFormSpeed = .5f;

		headJoint = 0;
		shoulderJoint = 0;
		legJoint = 0;

		state = IDLE;
		xformProgress = 0;
		currFloor = parent.grid.positionToFloor(position);
		destFloor = currFloor;
	}

	public void draw() {
		if(focusBlock != null && parent.zoom > .5) {
			// Draw progress bar
			parent.pushMatrix();
			parent.translate((float)focusBlock.position.x, (float)focusBlock.position.y);
			parent.pushStyle();
			parent.fill(0,0,0,150);
			parent.noStroke();
			parent.arc(0,0,parent.GRID_SIZE,parent.GRID_SIZE, xformProgress/1 * -parent.PI*2, 0);
			parent.popStyle();
			parent.popMatrix();
		}

		parent.pushMatrix();
		parent.pushStyle();

		parent.translate((float)position.x, (float)position.y);
		parent.scale(parent.GRID_SIZE/10f);
		parent.stroke(0);
		parent.strokeWeight(.5f);
		parent.fill(255);

		parent.pushMatrix();
		parent.translate(0,-11.5f);
		parent.rotate(headJoint);
		parent.arc(0,0, 7,7, -parent.PI, 0);
		parent.arc(0,0, 5,5, 0, parent.PI);

		parent.rotate(shoulderJoint-headJoint);
		parent.translate(0,3);
		parent.beginShape();
		parent.vertex(-3, 0);
		parent.vertex(3, 0);
		parent.vertex(1.5f, 10f);
		parent.vertex(-1.5f, 10f);
		parent.endShape(parent.CLOSE);

		parent.translate(0,10.5f);
		parent.rotate(legJoint);
		parent.triangle(-1.5f, .5f, 1.5f, .5f, 0, 3);

		parent.popMatrix();

		parent.popStyle();
		parent.popMatrix();
	}

	public void move(XY dP) {
		if(dP.length() > maxSpeed) dP.normalize().scale(maxSpeed);
		position.translate(dP);
	}

	public void moveTarget(XY dP) {
		target.translate(dP);
	}

	public boolean setTarget(XY t) {
		this.target = t.get();
		return true;
	}

	public void goToElevator(Elevator e) {
		XY t = e.origin.get();
		t.y = position.y;
		if(t.x > position.x) t.x -= e.width * parent.GRID_SIZE;
		else t.x += e.width * parent.GRID_SIZE;

		setTarget(t);
		targetElevator = e;
		state = ELEVATORAPPROACH;
	}

	public void update() {
		if(state != ELEVATORMOVE)
			move(position.get().subtract(target).scale(-.1));

		dPosition = oldPosition.get().subtract(position);
		oldPosition = position.get();

		currFloor = parent.grid.positionToFloor(position);

		if(!elevatorState()) {
			if(dPosition.length2() > .1 && state != BUSY) state = MOVE;
			else {
				if(state == MOVE) state = IDLE;
				if(state != BUSY || focusBlock == null) {
					// See if there is an untransformed block at this location. If so, go into a busy state and reserve the block.
					Block _b;
					for(int i=0; i<parent.blocks.size(); i++) {
						_b = parent.blocks.get(i);
						if(parent.grid.positionToFloor(_b.position) == currFloor && Math.abs(_b.position.x - position.x) < .1 &&
							_b.workable()) {
							focusBlock = _b;
							break;
						}
					}
					if(focusBlock != null) {
						focusBlock.state = Block.XFORMING;
						state = BUSY;
						xformProgress = 0;
					}
					else {
						state = IDLE;
					}
				}
				else {
					// Busy! Transform the focusBlock until progress is 100%, change it's color, free it, and go idle.
					if(xformProgress < 1) {
						xformProgress += xFormSpeed;
					}
					else {
						parent.xformBlock(focusBlock);
						state = IDLE;
						focusBlock = null;
						xformProgress = 0;
					}

				}
			}
		}

		if(state == ELEVATORAPPROACH) {
			// If worker is close enough to elevator, call it and change state to ELEVATORWAIT
			if(dPosition.length2() < .1) {
				targetElevator.call(currFloor, destFloor > currFloor ? 1 : -1);
				state = ELEVATORWAIT;
			}
		}
		if(state == ELEVATORWAIT) {
			// Don't do anything until the elevator is at the current floor
			// Then set the target to the inside of the elevator, add self to list of passengers, and request the destination floor
			if(targetElevator.floor() == currFloor && targetElevator.stopped()) {
				// Also make sure it's moving in the right direction
				if((targetElevator.direction == 1 && destFloor > currFloor) ||
				   (targetElevator.direction == -1 && destFloor < currFloor) || targetElevator.requests.size() == 0) {
					XY t = targetElevator.carPosition();
					t.x += targetElevator.passengers.size();
					setTarget(t);
					state = ELEVATORMOVE;
					targetElevator.request(destFloor);
					targetElevator.passengers.add(this);
				}
			}
		}
		if(state == ELEVATORMOVE) {
			// Hang out until the elevator is at the destination floor (TODO: or highest floor)
			// Then get off the passenger list, move to the side, and go into idle state
			if((targetElevator.floor() == destFloor || targetElevator.limitFloor()) && targetElevator.stopped()) {
				targetElevator.passengers.remove(this);
				setTarget(position);
				moveTarget(new XY(targetElevator.width/2 * (int)parent.random(-1,1), 0));
				state = IDLE;
			}
		}

		//headJoint -= parent.random(-.01f,.01f);
		//if(headJoint < -2*parent.PI/6) headJoint = 0;

		// Bend joints according to motion
		headJoint = (float)dPosition.x * .1f;

		shoulderJoint = -(float)dPosition.x * .1f;
		legJoint = -(float)dPosition.x * .1f;

		if(shoulderJoint > parent.PI/2) shoulderJoint = parent.PI/2;
		if(shoulderJoint < -parent.PI/2) shoulderJoint = -parent.PI/2;
		if(legJoint > parent.PI/2) legJoint = parent.PI/2;
		if(legJoint < -parent.PI/2) legJoint = -parent.PI/2;

	}

	public boolean elevatorState() { return (state == ELEVATORMOVE || state == ELEVATORWAIT || state == ELEVATORAPPROACH); }
}
