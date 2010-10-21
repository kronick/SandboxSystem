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
	ArrayList<Worker> passengers = new ArrayList<Worker>();


	public Elevator(SandboxSystem parent, XY origin, int height) {
		this.parent = parent;
		this.origin = origin.get();
		this.baseFloor = parent.grid.positionToFloor(this.origin);
		this.height = height;
		this.width = 1;
		this.maxSpeed = 5;
		this.acceleration = .5f;
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

			// Draw call requests
			parent.fill(0,0,255,Math.abs(parent.frameCount%40-20) * 5 + 20);
			for(int i=0; i<requests.size(); i++) {
				parent.pushMatrix();
				parent.translate(0, (-requests.get(i)[0] - .5f) * parent.GRID_SIZE * 2);
				if(requests.get(i)[1] == -1) {
					parent.triangle(-parent.GRID_SIZE/4, -parent.GRID_SIZE/4, parent.GRID_SIZE/4, -parent.GRID_SIZE/4, 0, parent.GRID_SIZE/4);
				}
				else {
					parent.triangle(-parent.GRID_SIZE/4, parent.GRID_SIZE/4, parent.GRID_SIZE/4, parent.GRID_SIZE/4, 0, -parent.GRID_SIZE/4);
				}
				parent.rect(-width*parent.GRID_SIZE/2, -.25f * parent.GRID_SIZE*2, width*parent.GRID_SIZE/2, .75f * parent.GRID_SIZE*2);
				parent.popMatrix();
			}

			// Draw car
			if(pauseCounter == pause) {	// door closed
				parent.fill(0,0,0,180);
			}
			else						// Door open
				parent.noFill();

			parent.stroke(0,0,0);
			parent.strokeWeight(2);
			parent.rect(-width*parent.GRID_SIZE/2, -(position - parent.GRID_SIZE/2), width*parent.GRID_SIZE/2, -(position + parent.GRID_SIZE * 1.5f));
			//parent.rect(0, -position, width*parent.GRID_SIZE, parent.GRID_SIZE);

			/*
			if(pauseCounter == pause) { // Door closed
				parent.strokeWeight(.5f);
				parent.line(0, -(position - parent.GRID_SIZE/2), 0, -(position + parent.GRID_SIZE * 1.5f));
			}
			*/

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
			parent.fill(0,0,255);
			parent.text(nextFloor() + baseFloor, 0, -position - parent.GRID_SIZE);
		parent.popStyle();
		parent.popMatrix();
	}

	public void call(int floor, int direction) {
		int addFloor = floor-this.baseFloor;
		int addDirection = direction;

		// Set some limits
		if(addFloor < 0) addFloor = 0;
		if(addFloor > height-1) addFloor = height-1;
		if(addFloor == 0) addDirection = 1;
		if(addFloor == height-1) addDirection = -1;

 		Integer[] req = {new Integer(addFloor), new Integer(addDirection)};
		requests.add(req);
	}

	public void request(int floor) {
		if(floor - this.baseFloor < nextFloor()) call(floor, -1);
		else call(floor, 1);
	}

	public void update() {
		//Continue traveling in the same direction while there are remaining requests in that same direction.
		//If there are no further requests in that direction, then stop and become idle,
		//or change direction if there are requests in the opposite direction.

		if(direction == 0 && requests.size() > 0) {
			// Find the closest request and go there
			int minDistance = -1;
			int minFloor = -1;
			int minDir = -1;

			int dist = -1;
			for(int i=0; i<requests.size(); i++) {
				dist = Math.abs(nextFloor() - requests.get(i)[0]);
				if(minDistance == -1 || dist < minDistance ||
						(dist == minDistance &&	// Break a tie by continuing in current direction
								((nextFloor()-requests.get(i)[0]) < 0 && direction == 1) ||
								(nextFloor()-requests.get(i)[0]) > 0 && direction == -1)) {
					minDistance = Math.abs(nextFloor() - requests.get(i)[0]);
					minFloor = requests.get(i)[0];
					minDir = requests.get(i)[1];
				}
			}
			if(nextFloor() - minFloor == 0) direction = minDir;
			else if(nextFloor() - minFloor > 0) direction = -1;
			else direction = 1;
			//parent.println("Closest stop: Floor #" + minFloor + " Direction: " + minDir + " (Elevator direction: " + direction + ")");
		}

		// Figure out how to keep moving in the same direction or not
		boolean stopNextFloor = false;
		boolean moreRequestsSameDir = false;
		if(direction != 0) {	// Direction == 0 would mean the elevator is idle.
			boolean moreRequests = false;

			int floor, reqDir;
			//parent.println("Next floor: " + nextFloor());
			for(int i=0; i<requests.size(); i++) {
				floor = requests.get(i)[0];
				reqDir = requests.get(i)[1];

				//parent.println("Request for floor " + floor + " direction " + reqDir + " (Next floor: " + nextFloor() + ")");
				if((direction == 1 && floor >= nextFloor()) || (direction == -1 && floor <= nextFloor())) {
					moreRequests= true;
					if(direction == reqDir) {
						moreRequestsSameDir = true;
						if(floor == nextFloor()) stopNextFloor = true;
					}
				}
			}
			if(!moreRequests) direction = 0;
			if(!moreRequestsSameDir) {
				for(int i=0; i<requests.size(); i++) {
					floor = requests.get(i)[0];
					reqDir = requests.get(i)[1];
					if(floor == nextFloor()) stopNextFloor = true;
				}
			}
		}

		// Figure out velocity based on various state variables

		// Case 1: Arrived at a desitination floor
		if(stopNextFloor && distanceToNextFloor() < .1) {
			if(!moreRequestsSameDir) direction *= -1;	// Plan to switch directions if this is the last stop

			// Stop completely, wait for pause counter to go to 0
			velocity = 0;
			if(pauseCounter > 0) {
				pauseCounter--;
			}
			else {	// Once the car has waited long enough at this floor, remove any requests
				ArrayList<Integer[]> toRemove = new ArrayList<Integer[]>();
				int floor, reqDir;
				for(int i=0; i < requests.size(); i++) {
					floor = requests.get(i)[0];
					reqDir = requests.get(i)[1];
					if(floor == nextFloor()) {
						if(reqDir == direction) {
							toRemove.add(requests.get(i));
						}
					}
				}
				for(int i=0; i<toRemove.size(); i++) {
					requests.remove(toRemove.get(i));
				}
			}
		}
		// Case 2: // Traveling as normal
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

		// Move car
		position += velocity;

		// Move all passengers in car
		for(int i=0; i<passengers.size(); i++) {
			passengers.get(i).position = carPosition().get();
			passengers.get(i).position.x += -passengers.size()/2 + i;
		}
	}

	private float distanceToNextFloor() {
		return Math.abs(position - nextFloor() * parent.GRID_SIZE * 2);
	}
	private int nextFloor() {
		// Convert current position to floor number
		//if(direction == 0 || true) {
		return (int)Math.round((position/parent.GRID_SIZE/2));
		//}
		/*
		else {
			// Round up if moving up, round down if moving down
			if(direction > 0)
				return (int)Math.ceil(position/parent.GRID_SIZE/2);
			else
				return (int)Math.floor(position/parent.GRID_SIZE/2);
		}
		*/
	}
	public int floor() {
		return (int)Math.round((position/parent.GRID_SIZE/2)) + baseFloor;
	}

	public boolean servesFloor(int f) {
		return (f >= baseFloor && f< baseFloor + height);
	}

	public boolean stopped() { return pauseCounter != pause; }

	public XY carPosition() {
		return origin.get().subtract(new XY(0,position));
	}

	public boolean limitFloor() {
		return ((nextFloor() == height-1 && direction == 1) || (nextFloor() == 0 && direction == -1));
	}
}
