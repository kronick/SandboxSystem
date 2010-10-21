package kronick.sandboxsystem;

import java.util.ArrayList;

public class Arm {
	float r1, r2;
	XY goal;
	XY origin;
	XY end;
	double theta1, theta2;
	PID joint1, joint2;
	double velo1, velo2;

	double MASS = 5;
	double UFRIC = 0.85f;

	double nodeScale;

	Block cargo;
	boolean dropAtTarget = false;
	ArrayList<Block> reachableBlocks;

	SandboxSystem parent;

	Arm(SandboxSystem parent, float ir1, float ir2, XY iorigin) {
	this.parent = parent;
		this.r1 = ir1;
		this.r2 = ir2;
		this.theta1 = (float)Math.PI/2;							// Point straight up
		this.theta2 = (float)Math.PI;
		this.origin = new XY(iorigin);

		goal = new XY(0, -(this.r1+this.r2)/2);
		double[] t = IK(goal);
		this.theta1 = t[0];
		this.theta2 = t[1];


		this.nodeScale = 1/12. * Math.sqrt(parent.GRID_SIZE/20.);

		//joint1 = new PID(MASS*.005,0.000,MASS * .002,1);
		//joint2 = new PID(MASS*.005,0.000,MASS * .002,1);
		joint1 = new PID(MASS*.005,0.000,MASS * .005,1);
		joint2 = new PID(MASS*.005,0.000,MASS * .005,1);
	}

	void update() {
		// IK and physics
	double[] angles = new double[2];
		angles = IK(this.goal, -1);
		double d1 = joint1.update(this.theta1, angles[0]);
		double d2 = joint2.update(this.theta2, angles[1]);
		velo1 += d1/MASS;
		velo2 += d2/MASS;
		velo1 *= UFRIC;
		velo2 *= UFRIC;
		this.theta1 += velo1;
		this.theta2 += velo2;

		double t2 = this.theta2 + this.theta1 + Math.PI;
		this.end = new XY(this.r1 * Math.cos(this.theta1) + this.r2 * Math.cos(t2), this.r1 * Math.sin(this.theta1) + this.r2 * Math.sin(t2));
		this.end.translate(this.origin);

		// *** Deal with cargo
		if(this.cargo == null)	{
			if(this.goal.distance2(new XY(this.end.x - this.origin.x, this.end.y - this.origin.y)) < 25) {
				Block b;
				for(int i=parent.blocks.size()-1; i>=0; i--) {
					b = parent.blocks.get(i);

					if(b.state == Block.FREE	&& b.position.distance2(this.end) < 9 && this.dropAtTarget) {
						this.dropAtTarget = false;
						this.cargo = b;
						this.cargo.state = Block.MOVING;
					}
				}
			}
		}
		else {	// Cargo in hand
			if(this.dropAtTarget && this.goal.distance(new XY(this.end.x - this.origin.x, this.end.y - this.origin.y)) < 5) {
				this.cargo.position = new XY(this.goal.x + this.origin.x, this.goal.y + this.origin.y);
				this.cargo.state = Block.FREE;
				this.cargo = null;
				this.dropAtTarget = false;
			}
			else {
				this.cargo.position = this.end;
			}
		}

	}

	public void draw() {

		// *** Draw arm
		parent.pushMatrix();
			parent.rectMode(parent.CENTER);

			parent.translate((float)this.origin.x, (float)this.origin.y);

			// Draw range
			/*
			noFill();
			stroke(0,0,120,50);
			ellipse(0,0, 2 * (this.r1 + this.r2), 2 * (this.r1 + this.r2));
			ellipse(0,0, 2 * (this.r1 - this.r2), 2 * (this.r1 - this.r2));
			 */

			// First joint and arm
			parent.noStroke();
			//line(0,0, this.r1 * cos(this.theta1), this.r1 * sin(this.theta1))
			parent.noFill();
			parent.stroke(0,0,255,40);
			parent.strokeWeight(parent.GRID_SIZE/2);
			parent.ellipse(0,0,parent.GRID_SIZE*2, parent.GRID_SIZE*2);
			//parent.ellipse(0,0,parent.GRID_SIZE*5, parent.GRID_SIZE*5);
			//parent.ellipse(0,0,parent.GRID_SIZE*8, parent.GRID_SIZE*8);
			parent.pushMatrix();
				parent.fill(150,150,00);
				//stroke(0,0,255);
				parent.noStroke();
				parent.rotate((float)(this.theta1+Math.PI/2));
				parent.beginShape();
				parent.vertex((float)(-this.r1 * this.nodeScale / 2),0);
				parent.vertex((float)(-this.r2 * this.nodeScale / 2), -this.r1);
				parent.vertex((float)(this.r2 * this.nodeScale / 2), -this.r1);
				parent.vertex((float)(this.r1 * this.nodeScale / 2), 0);
				parent.endShape(parent.CLOSE);
			parent.popMatrix();
			//fill(100 - abs(d1)/.05 * 100,100,200);
			parent.ellipse(0,0,(float)(this.r1 * this.nodeScale), (float)(this.r1 * this.nodeScale));

			// Second joint and arm
			parent.pushMatrix();
				parent.translate((float)(this.r1 * Math.cos(this.theta1)), (float)(this.r1 * Math.sin(this.theta1)));
				parent.noStroke();

				double t2 = this.theta2 + this.theta1 + Math.PI;
				parent.pushMatrix();
					parent.fill(150,150,00);
					//stroke(0,0,0);
					parent.noStroke();
					parent.rotate((float)(t2+Math.PI/2));
					parent.beginShape();
					parent.vertex((float)(-this.r2 * this.nodeScale / 2), 0);
					parent.vertex((float)(-this.r2 * this.nodeScale / 4), -this.r2);
					parent.vertex((float)(this.r2 * this.nodeScale / 4), -this.r2);
					parent.vertex((float)(this.r2 * this.nodeScale / 2), 0);
					parent.endShape(parent.CLOSE);
				parent.popMatrix();
				//fill(100 - abs(d2)/.05 * 100,100,200);
				parent.ellipse(0,0,(float)(this.r2 * this.nodeScale), (float)(this.r2 * this.nodeScale));


				// Draw cargo box
				float s = this.cargo == null ? 1.5f : 1.1f;
				parent.stroke(0,0,0);
				parent.strokeWeight(60 * (float)this.nodeScale);
				parent.noFill();
				parent.rect((float)(this.r2 * Math.cos(t2)-1), (float)(this.r2 * Math.sin(t2)), parent.GRID_SIZE*s, parent.GRID_SIZE*s);

			parent.popMatrix();
			parent.strokeWeight(1);

			this.end = new XY(this.r1 * Math.cos(this.theta1) + this.r2 * Math.cos(t2), this.r1 * Math.sin(this.theta1) + this.r2 * Math.sin(t2));
			this.end.translate(this.origin);

			// Draw target
			parent.pushMatrix();
				parent.translate((float)this.goal.x, (float)this.goal.y);

				if(this.goal.distance(new XY(this.end.x - this.origin.x, this.end.y - this.origin.y)) > 3) {
					parent.noStroke();
					parent.fill(0,0,255,Math.abs(parent.frameCount%10-5) * 20 + 50);
					parent.rect(0,0, parent.GRID_SIZE*1.5f, parent.GRID_SIZE*1.5f);
				}
			parent.popMatrix();
		parent.popMatrix();

	}

	boolean setGoal(XY g) {
		if(g.length() < .001)
			return false;
		else {
			this.goal = new XY(g);
			return true;
		}
	}

	double[] IK(XY goal) {
		return IK(goal, -1);
	}
	double[] IK(XY goal, int sign) {
		goal = new XY(goal);
		if(goal.length() > this.r1 + this.r2 ) {
			goal.normalize();
			goal.scale(.99*(this.r1 + this.r2));
		}

		if(goal.length() < this.r1 - this.r2 ) {
			goal.normalize();
			goal.scale(1.01*(this.r1 - this.r2));
		}
		double[] angles = new double[2];
		double d = goal.length();

		if(goal.x < 0) {
			sign *= -1;
		}


		double a = sign * Math.acos(((r1*r1) + Math.pow(d,2) - r2*r2) / (2 * r1 * d)) + Math.atan(goal.y / goal.x);
		double b = sign * Math.acos((r1*r1 + r2*r2 - d*d) / (2 * r1 * r2));

		if(a == a && b == b) { angles[0] = a; angles[1] = b; }

		if(goal.x < 0) {
			angles[0] -= Math.PI;
			//angles[1] += PI;
		}

		return angles;
	}
}