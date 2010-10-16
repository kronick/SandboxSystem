package kronick.sandboxsystem;

public class PID {
	  public double pK, iK, dK; // Gains
	  public double pTerm, iTerm, dTerm; // Terms
	  public double iMax;
	  public double PIDsum; // pTerm + iTerm + kTerm
	  public float error;

	  PID(double p, double i, double d, double iMax) {
	    this.pK = p;
	    this.iK = i;
	    this.dK = d;
	    this.iMax = iMax;
	    this.error = 0;
	  }

	  double update(double current, double goal) {
		  double err = goal - current;
	    this.pTerm = err * this.pK;                 // error * gain
	    this.iTerm += err * this.iK;                // add error * gain
	    if(Math.abs(iTerm) > this.iMax)
	      iTerm = (iTerm < 0 ? -1 : 1) * this.iMax;
	    this.dTerm = (err - this.error) * this.dK;  // changeInError * gain
	    this.PIDsum = this.pTerm + this.iTerm + this.dTerm;
	    return this.PIDsum;
	  }
	}