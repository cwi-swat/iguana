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

package org.iguana.grammar.slot;

import iguana.utils.input.Input;
import org.iguana.datadependent.env.Environment;
import org.iguana.grammar.condition.Conditions;
import org.iguana.grammar.symbol.Position;
import org.iguana.parser.ParserRuntime;
import org.iguana.parser.descriptor.ResultOps;
import org.iguana.parser.gss.GSSNode;

import java.util.Collections;
import java.util.Set;

public class EndGrammarSlot<T> extends BodyGrammarSlot<T> {

	protected final NonterminalGrammarSlot nonterminal;

	public EndGrammarSlot(Position position, NonterminalGrammarSlot<T> nonterminal, String label,
			              String variable, Set<String> state, Conditions conditions, ParserRuntime runtime, ResultOps<T> ops) {
		this(position, nonterminal, label, -1, variable, -1, state, conditions, runtime, ops);
	}
	
	public EndGrammarSlot(Position position, NonterminalGrammarSlot<T> nonterminal, String label, int i1,
            			  String variable, int i2, Set<String> state, Conditions conditions,
                          ParserRuntime runtime, ResultOps<T> ops) {
		super(position, label, i1, variable, i2, state, conditions, runtime, ops);
		this.nonterminal = nonterminal;
    }
	
	public NonterminalGrammarSlot getNonterminal() {
		return nonterminal;
	}

	@Override
	public void execute(Input input, GSSNode<T> u, T result) {
		if (nonterminal.testFollow(input.charAt(ops.getRightIndex(result))))
            u.pop(input, this, result, ops);
	}
	
	@Override
	public boolean isEnd() {
		return true;
	}
	
	public Object getObject() {
		return null;
	}
	
	@Override
	public Set<Transition<T>> getTransitions() {
		return Collections.emptySet();
	}

	@Override
	public boolean addTransition(Transition transition) {
		return false;
	}

    /**
	 *
	 * Data-dependent GLL parsing
	 * 
	 */
	@Override
	public void execute(Input input, GSSNode<T> u, T node, Environment env) {
		if (nonterminal.testFollow(input.charAt(ops.getRightIndex(node))))
            u.pop(input, this, node, ops);
	}
	
	public void execute(Input input, GSSNode<T> u, T node, Object value) {
		if (nonterminal.testFollow(input.charAt(ops.getRightIndex(node))))
            u.pop(input, this, node, value, ops);
	}

}
