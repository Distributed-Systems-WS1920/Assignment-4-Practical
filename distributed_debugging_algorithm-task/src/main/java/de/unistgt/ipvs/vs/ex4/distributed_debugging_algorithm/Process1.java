package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

//you are not allowed to change this class structure
public class Process1 extends AbstractProcess {

	private VectorClock vectorClock;

	public Process1(Monitor monitor, AbstractProcess[] processes, int Id) {
		super(monitor, processes, Id);

		vectorClock = new VectorClock(this.Id, this.numberOfProcesses);
	}

	@Override
	public void run() {

		// send the initial state to Monitor
		Message message = new Message(new VectorClock(vectorClock), this.localVariable);
		monitor.receiveMessage(this.Id, message);

		// line 1
		this.localVariable = 5;
		this.vectorClock.increment();
		// notify the monitor
		message = new Message(new VectorClock(vectorClock), this.localVariable);
		monitor.receiveMessage(this.Id, message);

		// line 2
		this.vectorClock.increment();
		message = new Message(new VectorClock(vectorClock), this.localVariable);
		send(1, message); // send to process 1
		// notify the monitor
		monitor.receiveMessage(this.Id, message);
		
		// line 3
		this.localVariable = this.localVariable * 3;
		this.vectorClock.increment();
		// notify the monitor
		message = new Message(new VectorClock(vectorClock), this.localVariable);
		monitor.receiveMessage(this.Id, message);

		// line 4
		this.vectorClock.increment();
		message = new Message(new VectorClock(vectorClock), this.localVariable);
		send(1, message); // send to process 1
		// notify the monitor
		monitor.receiveMessage(this.Id, message);

		// line 5
		// receive
		Message receivedMessage = receive(1); // receive from process 1
		this.vectorClock.update(receivedMessage.getVectorClock());
		this.localVariable = receivedMessage.getLocalVariable() - this.localVariable;
		this.vectorClock.increment();

		// notify the monitor
		message = new Message(new VectorClock(vectorClock), this.localVariable);
		monitor.receiveMessage(this.Id, message);

		// Perform only for task c when there are 3 processes TODO: Remove if?
		// line 6
		if (this.vectorClock.get().length > 2) {
			this.vectorClock.increment();
			message = new Message(new VectorClock(vectorClock), this.localVariable);
			send(2, message); // send to process 2
			// notify the monitor
			monitor.receiveMessage(this.Id, message);

			// line 7
			receivedMessage = receive(2); // receive from process 2
			this.vectorClock.update(receivedMessage.getVectorClock());
			this.localVariable = receivedMessage.getLocalVariable() - this.localVariable;
			this.vectorClock.increment();

			// notify the monitor
			message = new Message(new VectorClock(vectorClock), this.localVariable);
			monitor.receiveMessage(this.Id, message);
		}
		
		System.out.println(this.localVariable);

		// send terminate signal
		monitor.processTerminated(this.Id);

		System.out.printf("process:%d , the local variable= %d\n", this.Id, this.localVariable);
	}

}
