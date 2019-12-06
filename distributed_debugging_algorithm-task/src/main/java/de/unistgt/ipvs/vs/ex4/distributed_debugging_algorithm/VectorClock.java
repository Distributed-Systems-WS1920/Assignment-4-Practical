package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

//you are not allowed to change this class structure
public class VectorClock {

	protected int[] vectorClock;
	private int processId;
	private int numberOfProcesses;

	public VectorClock(int processId, int numberOfProcesses) {
		vectorClock = new int[numberOfProcesses];
		this.numberOfProcesses = numberOfProcesses;
		this.processId = processId;
	}

	VectorClock(VectorClock other) {
		vectorClock = other.vectorClock.clone();
		processId = other.processId;
		numberOfProcesses = other.numberOfProcesses;

	}

	public void increment() {
		// Increment the entry of the vector of this process
		vectorClock[processId]++;
	}

	public int[] get() {
		// Return vector clock
		return vectorClock;
	}

	public void update(VectorClock other) {
		int[] otherClock = other.get();

		// Iterate through all entries of vector clock
		for (int currentEntry = 0; currentEntry < this.numberOfProcesses; currentEntry++) {
			// If entry of other clock bigger
			if (otherClock[currentEntry] > this.vectorClock[currentEntry]) {
				// Overwrite my entry
				this.vectorClock[currentEntry] = otherClock[currentEntry];
			}		
		}
	}

	public boolean checkConsistency(int otherProcessId, VectorClock other) {
		// I'm always consistent with myself
		if(this.processId == otherProcessId) {
			return true;
		}
		
		// Get clock values as array
		int[] otherClock = other.get();
		
		// Other process has a higher time in my component
		if(otherClock[this.processId] > this.vectorClock[this.processId]) {
			return false;
		}
		
		// I have a higher time in the component of the other process
		if(this.vectorClock[otherProcessId] > otherClock[otherProcessId]) {
			return false;
		}
		
		return true;
	}

}
