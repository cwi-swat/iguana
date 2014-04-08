package org.jgll.regex.matcher;

import org.jgll.regex.automaton.State;

public interface Transitions {
	
	/**
	 * Returns the transition id based on the given input character.
	 * 
	 * @return -1 if no corresponding transition id found
	 */
	public int getTransitionId(int v);
	
	public State move(int v);
}
