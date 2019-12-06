package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//you are not allowed to change this class structure. However, you can add local functions!
public class Monitor implements Runnable {

	/**
	 * The state consists on vector timestamp and local variables of each process.
	 * In this class, a state is represented by messages (events) indices of each
	 * process. The message contains a local variable and vector timestamp, see
	 * Message class. E.g. if state.processesMessagesCurrentIndex contains {1, 2},
	 * it means that the state contains the second message (event) from process1 and
	 * the third message (event) from process2
	 */
	private class State {
		// Message indices of each process
		private int[] processesMessagesCurrentIndex;

		public State(int numberOfProcesses) {
			processesMessagesCurrentIndex = new int[numberOfProcesses];
		}

		public State(int[] processesMessagesCurrentIndex) {
			this.processesMessagesCurrentIndex = processesMessagesCurrentIndex;
		}

		{
			processesMessagesCurrentIndex = new int[numberOfProcesses];
		}

		public int[] getProcessesMessagesCurrentIndex() {
			return processesMessagesCurrentIndex;
		}

		public int getProcessMessageCurrentIndex(int processId) {
			return this.processesMessagesCurrentIndex[processId];
		}

		@Override
		public boolean equals(Object other) {
			State otherState = (State) other;

			// Iterate over processesMessagesCurrentIndex array
			for (int i = 0; i < numberOfProcesses; i++)
				if (this.processesMessagesCurrentIndex[i] != otherState.processesMessagesCurrentIndex[i])
					return false;

			return true;
		}
	}

	private int numberOfProcesses;
	private final int numberOfPredicates = 4;

	// Count of still running processes. The monitor starts to check predicates
	// (build lattice) whenever runningProcesses equals zero.
	private AtomicInteger runningProcesses;
	/*
	 * Q1, Q2, ..., Qn It represents the processes' queue. See distributed debugging
	 * algorithm from global state lecture!
	 */
	private List<List<Message>> processesMessages;

	// list of states
	private LinkedList<State> states;

	// The predicates checking results
	private boolean[] possiblyTruePredicatesIndex;
	private boolean[] definitelyTruePredicatesIndex;

	public Monitor(int numberOfProcesses) {
		this.numberOfProcesses = numberOfProcesses;

		runningProcesses = new AtomicInteger();
		runningProcesses.set(numberOfProcesses);

		processesMessages = new ArrayList<>(numberOfProcesses);
		for (int i = 0; i < numberOfProcesses; i++) {
			List<Message> tempList = new ArrayList<>();
			processesMessages.add(i, tempList);
		}

		states = new LinkedList<>();

		possiblyTruePredicatesIndex = new boolean[numberOfPredicates];// there
																		// are
																		// three
		// predicates
		for (int i = 0; i < numberOfPredicates; i++)
			possiblyTruePredicatesIndex[i] = false;

		definitelyTruePredicatesIndex = new boolean[numberOfPredicates];
		for (int i = 0; i < numberOfPredicates; i++)
			definitelyTruePredicatesIndex[i] = false;
	}

	/**
	 * receive messages (events) from processes
	 *
	 * @param processId
	 * @param message
	 */
	public void receiveMessage(int processId, Message message) {
		synchronized (processesMessages) {
			processesMessages.get(processId).add(message);
		}
	}

	/**
	 * Whenever a process terminates, it notifies the Monitor. Monitor only starts
	 * to build lattice and check predicates when all processes terminate
	 *
	 * @param processId
	 */
	public void processTerminated(int processId) {
		runningProcesses.decrementAndGet();
	}

	public boolean[] getPossiblyTruePredicatesIndex() {
		return possiblyTruePredicatesIndex;
	}

	public boolean[] getDefinitelyTruePredicatesIndex() {
		return definitelyTruePredicatesIndex;
	}

	@Override
	public void run() {
		// wait till all processes terminate
		while (runningProcesses.get() != 0)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		// create initial state (S00)
		State initialState = new State(numberOfProcesses);

		// check predicates for part (b)
		for (int predicateNo = 0; predicateNo < 3; predicateNo++) {
			System.out.printf("Predicate%d-----------------------------------\n", predicateNo);
			states.add(initialState); // add the initial state to states list
			buildLattice(predicateNo, 0, 1);
			states.clear();

		}

		if (numberOfProcesses > 2) {
			int predicateNo = 3;
			System.out.printf("Predicate%d-----------------------------------\n", predicateNo);
			states.add(initialState); // add the initial state to states list
			buildLattice(predicateNo, 0, 2);
			states.clear();
		}

	}

	public void buildLattice(int predicateNo, int process_i_id, int process_j_id) {
		// Fringe for the next states
		LinkedList<State> statesToCheck = new LinkedList<State>();
		LinkedList<State> checkedStates = new LinkedList<State>();

		// Add initial state
		statesToCheck.add(this.states.get(0));

		// Clear our global states. This should contain a maximum of one element,
		// which is processed by checkPredicate() and not read elsewhere
		this.states.clear();

		// Traverse all states which have a child for which the predicate is false
		// If we find a path through lattice where predicate stays false
		// -> We can know that definitelyTrue is false
		boolean lastPredSet = false;
		while (!statesToCheck.isEmpty()) {
			// Process all reachable states from current state
			for (State currentReachableState : findReachableStates(statesToCheck.removeFirst())) {

				// Add only this state to our global states-List
				this.states.add(currentReachableState);

				// Check if predicate is true in this state
				boolean currentPredicate = checkPredicate(predicateNo, process_i_id, process_j_id);

				// Predicate is false for current child -> possible way without predicate
				// becoming true
				if (!currentPredicate) {
					// Add this node to states that should be checked because there is a way though
					// the lattice (only if the state was not added before)
					if (!checkedStates.contains(currentReachableState)) {
						statesToCheck.addLast(currentReachableState);
					}

				} else {
					// Found one state where predicate is true -> can set possibbleTrue predicate
					this.possiblyTruePredicatesIndex[predicateNo] = true;
				}

				lastPredSet = currentPredicate;

				// Clear global states again to ensure max. 1 element for checkPredicate()
				this.states.clear();
				// Mark state as already checked
				checkedStates.add(currentReachableState);
			}
		}

		// Last state to check was a final state, save the predicate
		definitelyTruePredicatesIndex[predicateNo] = lastPredSet;
	}

	/**
	 * find all reachable states starting from a given state
	 *
	 * @param state
	 * @return list of all reachable states
	 */
	private LinkedList<State> findReachableStates(State state) {
		// Init new Linked-List for States
		LinkedList<State> reachableStates = new LinkedList<>();

		// Get current state like ID S_00 or S_10
		int messageIdsInCurrentState[] = state.getProcessesMessagesCurrentIndex();

		// Check all processes for their next message, get its state and check if that
		// state is reachable
		// Stefan: For each process
		OUTER: for (int processId = 0; processId < this.numberOfProcesses; processId++) {
			// Get all Messages from process
			List<Message> processMessages = this.processesMessages.get(processId);

			// Get next message ID of current process (like S_01 or S_10)
			int nextMsgId = messageIdsInCurrentState[processId] + 1;

			// Skip if there is no next message from that process
			if (nextMsgId >= processMessages.size()) {
				continue;
			}

			// Get the next message object from current process using the ID
			Message msg = processMessages.get(nextMsgId);

			// Check if the new state (after processing the next message of
			// the current process) is still consistent (compare next vector clock with
			// the current clocks of all processes)
			for (int otherProcessId = 0; otherProcessId < this.numberOfProcesses; otherProcessId++) {
				if (otherProcessId != processId) {
					VectorClock otherCurrentClock = this.processesMessages.get(otherProcessId).get(messageIdsInCurrentState[otherProcessId])
							.getVectorClock();
					boolean consistent = msg.getVectorClock().checkConsistency(otherProcessId, otherCurrentClock);

					if (!consistent) {
						// Skip this state, check next process for its next message
						continue OUTER;
					}
				}
			}

			// Copy current State-ID
			int newIndices[] = Arrays.copyOf(messageIdsInCurrentState, messageIdsInCurrentState.length);
			// Adjust State-ID according to taken message
			newIndices[processId]++;
			// Create new State using new ID
			State newState = new State(newIndices);
			// Add new State to reachable states
			reachableStates.add(newState);
		}

		return reachableStates;
	}

	/**
	 * - check a predicate and return true if the predicate is **definitely** True.
	 * - To simplify the code, we check the predicates only on local variables of
	 * two processes. Therefore, process_i_Id and process_j_id refer to the
	 * processes that have the local variables in the predicate. The predicate0,
	 * predicate1 and predicate2 contain the local variables from process1 and
	 * process2. whilst the predicate3 contains the local variables from process1
	 * and process3.
	 *
	 * @param predicateNo: which predicate to validate
	 * @return true if predicate is definitely true else return false
	 */
	private boolean checkPredicate(int predicateNo, int process_i_Id, int process_j_id) {
		boolean predicate = false;

		for (State reachableState : this.states) {

			int msg_i_id = reachableState.getProcessMessageCurrentIndex(process_i_Id);
			int msg_j_id = reachableState.getProcessMessageCurrentIndex(process_j_id);
			Message msg_i = this.processesMessages.get(process_i_Id).get(msg_i_id);
			Message msg_j = this.processesMessages.get(process_j_id).get(msg_j_id);

			switch (predicateNo) {
			case 0:
				predicate = Predicate.predicate0(msg_i, msg_j);
				break;

			case 1:
				predicate = Predicate.predicate1(msg_i, msg_j);
				break;

			case 2:
				predicate = Predicate.predicate2(msg_i, msg_j);
				break;

			case 3:
				predicate = Predicate.predicate3(msg_i, msg_j);
				break;
			}

			break;
		}

		return predicate;
	}

}
