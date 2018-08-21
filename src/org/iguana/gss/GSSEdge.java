/*
 * Copyright (c) 2015, Ali Afroozeh and Anastasia Izmaylova, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this 
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 *
 */

package org.iguana.gss;

import iguana.utils.collections.hash.MurmurHash3;
import iguana.utils.input.Input;
import org.iguana.datadependent.env.Environment;
import org.iguana.datadependent.env.EnvironmentPool;
import org.iguana.grammar.slot.BodyGrammarSlot;
import org.iguana.parser.IguanaRuntime;
import org.iguana.result.Result;

public class GSSEdge<T extends Result> {

	private final BodyGrammarSlot returnSlot;
	private final T result;
	private final GSSNode<T> destination;
	private final Environment env;

	GSSEdge(BodyGrammarSlot slot, T result, GSSNode<T> destination, Environment env) {
		this.returnSlot = slot;
		this.result = result;
		this.destination = destination;
		this.env = env;
	}

	public T getResult() {
		return result;
	}

	public BodyGrammarSlot getReturnSlot() {
		return returnSlot;
	}

	public GSSNode<T> getDestination() {
		return destination;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;

		if (!(obj instanceof GSSEdge))
			return false;

		GSSEdge<?> other = (GSSEdge<?>) obj;

		// Because destination.getInputIndex() == node.getLeftExtent, and
		// node.getRightExtent() == source.getLeftExtent we don't use them here.
		return 	returnSlot == other.getReturnSlot()
				&& destination.getInputIndex() == other.getDestination().getInputIndex()
				&& destination.getGrammarSlot() == other.getDestination().getGrammarSlot();
	}

	@Override
	public int hashCode() {
		return MurmurHash3.fn().apply(returnSlot, destination.getInputIndex(), destination.getGrammarSlot());
	}

	@Override
	public String toString() {
		return String.format("(%s, %s, %s)", returnSlot, result, destination);
	}

	/*
	 *
	 * Does the following:
	 * (1) checks conditions associated with the return slot
	 * (2) checks whether the descriptor to be created has been already created (and scheduled) before
	 * (2.1) if yes, returns null
	 * (2.2) if no, creates one and returns it
	 *
	 */
	T addDescriptor(Input input, GSSNode<T> source, T result, IguanaRuntime<T> runtime) {
		int inputIndex = result.getIndex();

		BodyGrammarSlot returnSlot = getReturnSlot();
		GSSNode<T> destination = getDestination();

		Environment env = this.env;

		if (returnSlot.requiresBinding())
			env = returnSlot.doBinding(result, env);

		runtime.setEnvironment(env);

		if (returnSlot.getConditions().execute(input, source, inputIndex, runtime.getEvaluatorContext(), runtime)) {
			EnvironmentPool.returnToPool(env);
			return null;
		}

		env = runtime.getEnvironment();

		return returnSlot.getIntermediateNode2(getResult(), destination.getInputIndex(), result, env, runtime);
	}

}
