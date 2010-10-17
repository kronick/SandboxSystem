package kronick.sandboxsystem;

import java.util.ArrayList;

public class Elevator {
	XY origin;
	int height, width;	// In number of floors/blocks
	int baseFloor;		// Offset useful for synchronizing all elevators
	float maxSpeed;
	float acceleration;
	int pause;

	float velocity;
	float position;
	int direction;
	int pauseCounter;

	SandboxSystem parent;

	ArrayList<Integer[]> requests = new ArrayList<Integer[]>();


	public Elevator(SandboxSystem parent, XY origin, int height) {
		this(parent, origin, height, 0);
	}
	public Elevator(SandboxSystem parent, XY origin, int height, int baseFloor) {
		this.parent = parent;
		this.origin = origin.get();
		this.height = height;
		this.width = 1;
		this.maxSpeed = 10;
		this.acceleration = 1f;
		this.pause = 40;
		this.pauseCounter = this.pause;
		this.position = 0;
	}

	public boolean goToFloor(int f) {
		return true;
	}

	public void draw() {
		parent.pushMatrix();
		parent.pushStyle();
			// Draw frame from base to max height
			parent.translate((float)origin.x, (float)origin.y);
			parent.noStroke();
			parent.fill(0,0,255,80);
			parent.rectMode(parent.CORNERS);
			parent.rect(-width*parent.GRID_SIZE/2, parent.GRID_SIZE/2, width*parent.GRID_SIZE/2, -(height*2-.5f) * parent.GRID_SIZE);

			// Draw car
			if(pauseCounter == pause)
				parent.fill(0,0,0,180);
			else
				parent.noFill();

			parent.stroke(0,0,0);
			parent.strokeWeight(2);
			parent.rect(-width*parent.GRID_SIZE/2, -(position - parent.GRID_SIZE/2), width*parent.GRID_SIZE/2, -(position + parent.GRID_SIZE * 1.5f));
			//parent.rect(0, -position, width*parent.GRID_SIZE, parent.GRID_SIZE);

			if(direction != 0) {
				// Draw direction arrow above
				parent.pushMatrix();
				parent.noStroke();
				parent.fill(0,0,0);
				parent.translate(0, -position - parent.GRID_SIZE * 2.25f);
				if(direction == -1) {
					parent.triangle(-parent.GRID_SIZE/4, 0, parent.GRID_SIZE/4, 0, 0, parent.GRID_SIZE/2);
				}
				else {
					parent.triangle(-parent.GRID_SIZE/4, parent.GRID_SIZE/2, parent.GRID_SIZE/4, parent.GRID_SIZE/2, 0, 0);
				}
				parent.popMatrix();
			}
		parent.popStyle();
		parent.popMatrix();
	}

	public void call(int floor, int direction) {
		Integer[] req = {new Integer(floor-this.baseFloor), new Integer(direction)};
		requests.add(req);
	}

	public void update() {
		//Continue traveling in the same direction while there are remaining requests in that same direction.
		//If there are no further requests in that direction, then stop and become idle,
		//or change direction if there are requests in the opposite direction.
		if(direction == 0 && requests.size() > 0) {
			// Find the closest request and go there
			int minDistance = -1;
			for(int i=0; i<requests.size(); i++) {
				if(minDistance == -1 || Math.abs(nextFloor() - requests.get(i)[0]) < minDistance) {
					direction = nextFloor() - requests.get(i)[0] < 0 ? 1 : -1;
					minDistance = Math.abs(nextFloor() - requests.get(i)[0]);
					parent.println("Going in new direction: " + (direction > 0 ? "UP" : "DOWN"));
				}
			}
		}

		// Figure out how to keep moving in the same direction or not
		boolean stopNextFloor = false;
		if(direction != 0) {	// Direction == 0 would mean the elevator is idle.
			boolean moreRequestsSameDir = false;
			boolean moreRequests = false;

			int floor, reqDir;
			parent.println("Next floor: " + nextFloor());
			for(int i=0; i<requests.size(); i++) {
				floor = requests.get(i)[0];
				reqDir = requests.get(i)[1];

				parent.println("Request for floor " + floor + " direction " + reqDir);
				if((direction == 1 && floor >= nextFloor()) || (direction == -1 && floor <= nextFloor())) {
					moreRequests= true;
					if(direction == reqDir) {
						moreRequestsSameDir = true;
						if(floor == nextFloor()) stopNextFloor = true;
					}
				}
			}
			if(!moreRequests) direction = 0;
		}

		// Figure out velocity based on various state variables
		if(stopNextFloor) parent.println("Stopping at the next floor..." + distanceToNextFloor());
		if(stopNextFloor && distanceToNextFloor() < .1) {
			// It's there! Stop completely, wait for pause counter to go to 0
			velocity = 0;
			parent.println("Elevator has arrived!");
			if(pauseCounter > 0) {
				parent.println("waiting for passengers..." + pauseCounter);
				pauseCounter--;
			}
			else {	// Once the car has waited long enough at this floor, remove any requests
				ArrayList<Integer[]> toRemove = new ArrayList<Integer[]>();
				int floor, reqDir;
				for(int i=0; i < requests.size(); i++) {
					floor = requests.get(i)[0];
					reqDir = requests.get(i)[1];
					if(floor == nextFloor() && reqDir == direction) {
						toRemove.add(requests.get(i));
					}
				}
				for(int i=0; i<toRemove.size(); i++) {
					parent.println("removing call request...");
					requests.remove(toRemove.get(i));
				}
			}
		}
		else {
			pauseCounter = pause;

			if(direction != 0) {
				// Accelerate up to a maximum velocity
				float tempMaxSpeed = (!stopNextFloor ? maxSpeed : distanceToNextFloor()/parent.GRID_SIZE/2 * maxSpeed);
				if(Math.abs(velocity) < tempMaxSpeed) {
					velocity += acceleration * direction;
				}
				else velocity = tempMaxSpeed * direction;
			}
			else if(direction == 0) { velocity = 0; }
		}

		position += velocity;
	}

	private float distanceToNextFloor() {
		return Math.abs(position - nextFloor() * parent.GRID_SIZE * 2);
	}
	private int nextFloor() {
		// Convert current position to floor number
		if(direction == 0) {
			return (int)(position/parent.GRID_SIZE/2);
		}
		else {
			// Round up if moving up, round down if moving down
			if(direction > 0)
				return (int)Math.ceil(position/parent.GRID_SIZE/2);
			else
				return (int)Math.floor(position/parent.GRID_SIZE/2);
		}
	}
}
