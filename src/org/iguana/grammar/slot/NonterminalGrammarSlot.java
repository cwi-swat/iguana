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
import org.iguana.datadependent.ast.Expression;
import org.iguana.datadependent.env.Environment;
import org.iguana.grammar.slot.lookahead.FollowTest;
import org.iguana.grammar.slot.lookahead.LookAheadTest;
import org.iguana.grammar.symbol.Nonterminal;
import org.iguana.gss.GSSNode;
import org.iguana.gss.GSSNodeData;
import org.iguana.gss.lookup.GSSNodeLookup;
import org.iguana.gss.lookup.GSSNodeLookup.GSSNodeCreator;
import org.iguana.parser.ParserRuntime;
import org.iguana.util.Configuration.EnvironmentImpl;
import org.iguana.util.ParserLogger;

import java.util.ArrayList;
import java.util.List;


public class NonterminalGrammarSlot<T> extends AbstractGrammarSlot<T> {
	
	private final Nonterminal nonterminal;
	
	private final List<BodyGrammarSlot<T>> firstSlots;

	private final GSSNodeLookup<T> nodeLookup;

	private LookAheadTest<T> lookAheadTest;

	private FollowTest followTest;

	public NonterminalGrammarSlot(Nonterminal nonterminal) {
		this.nonterminal = nonterminal;
		this.nodeLookup = GSSNodeLookup.getNodeLookup();
		this.firstSlots = new ArrayList<>();
	}
	
	public void addFirstSlot(BodyGrammarSlot<T> slot) {
		firstSlots.add(slot);
	}
	
	public List<BodyGrammarSlot<T>> getFirstSlots() {
		return firstSlots;
	}
	
	private List<BodyGrammarSlot<T>> getFirstSlots(int v) {
		return lookAheadTest.get(v);
	}
	
	public void setLookAheadTest(LookAheadTest<T> lookAheadTest) {
		this.lookAheadTest = lookAheadTest;
	}

	public void setFollowTest(FollowTest followTest) {
		this.followTest = followTest;
	}
	
	boolean testFollow(int v) {
		return followTest.test(v);
	}
	
	public Nonterminal getNonterminal() {
		return nonterminal;
	}

	public NonterminalNodeType getNodeType() {
		return nonterminal.getNodeType();
	}

	public String[] getParameters() {
		return nonterminal.getParameters();
	}

	public int countGSSNodes() {
		return nodeLookup.size();
	}
	
	@Override
	public String toString() {
		return nonterminal.toString();
	}
	
	@Override
	public boolean isFirst() {
		return true;
	}

    @Override
    public int getPosition() {
        throw new UnsupportedOperationException();
    }

    public GSSNode<T> getGSSNode(int i) {
		return nodeLookup.get(this, i);
	}
	
	public Iterable<GSSNode<T>> getGSSNodes() {
		return nodeLookup.getNodes();
	}

	@Override
	public void reset(Input input) {
		nodeLookup.reset(input);
	}
	
	public void create(Input input, BodyGrammarSlot<T> returnSlot, GSSNode<T> u, T node, Expression[] arguments, Environment env, ParserRuntime<T> runtime) {
        int i = runtime.getResultOps().getRightIndex(node, u);
		
		if (arguments == null) {
			GSSNode<T> gssNode = nodeLookup.get(i);
			// No GSS node labelled (slot, k) exits
			if (gssNode == null) {
				gssNode = new GSSNode<>(this, i);
				ParserLogger.getInstance().gssNodeAdded(gssNode);

				List<BodyGrammarSlot<T>> firstSlots = getFirstSlots(input.charAt(i));
				if (firstSlots != null)
					for (int j = 0; j < firstSlots.size(); j++) {
						BodyGrammarSlot<T> slot = firstSlots.get(j);
						if (!slot.getConditions().execute(input, gssNode, i, runtime)) {
							runtime.scheduleDescriptor(slot, gssNode, runtime.getResultOps().dummy(), env);
						}
					}
			} else {
				ParserLogger.getInstance().log("GSSNode found: %s", gssNode);
			}
			gssNode.createGSSEdge(input, returnSlot, u, node, env, runtime); // Record environment on the edge
			nodeLookup.put(i, gssNode);
			return;
		}
		
		GSSNodeData<Object> data = new GSSNodeData<>(runtime.evaluate(arguments, env, input));
		
		GSSNodeCreator<T> creator = gssNode -> {
			if (gssNode == null) {
				
				gssNode = new GSSNode<>(this, i, data);

				ParserLogger.getInstance().gssNodeAdded(gssNode);
				ParserLogger.getInstance().log("GSSNode created: %s(%s)", gssNode, data);
				
				gssNode.createGSSEdge(input, returnSlot, u, node, env, runtime);

				Environment newEnv;
				
				if (runtime.getConfiguration().getEnvImpl() == EnvironmentImpl.ARRAY)
					newEnv = runtime.getEmptyEnvironment().declare(data.getValues());
				else
					newEnv = runtime.getEmptyEnvironment().declare(nonterminal.getParameters(), data.getValues());
				
				for (BodyGrammarSlot<T> s : getFirstSlots(input.charAt(i))) {
					
					runtime.setEnvironment(newEnv);

					if (s.getLabel() != null)
						runtime.getEvaluatorContext().declareVariable(String.format(Expression.LeftExtent.format, s.getLabel()), i);

					if (!s.getConditions().execute(input, gssNode, i, runtime.getEvaluatorContext(), runtime))
						runtime.scheduleDescriptor(s, gssNode, runtime.getResultOps().dummy(), runtime.getEnvironment());
				}
				
			} else {
				gssNode.createGSSEdge(input, returnSlot, u, node, env, runtime);
			}
			return gssNode;
		};
		nodeLookup.get(i, data, creator);
	}
	
	public GSSNode<T> getGSSNode(int i, GSSNodeData<Object> data) {
		return nodeLookup.get(this, i, data);
	}

}
