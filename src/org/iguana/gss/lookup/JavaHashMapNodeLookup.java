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

package org.iguana.gss.lookup;

import iguana.utils.collections.CollectionsUtil;
import iguana.utils.input.Input;
import org.iguana.grammar.slot.NonterminalGrammarSlot;
import org.iguana.result.ResultOps;
import org.iguana.gss.GSSNode;

import java.util.HashMap;
import java.util.Map;

public class JavaHashMapNodeLookup<T> extends AbstractNodeLookup<T> {

	private Map<Integer, GSSNode<T>> map = new HashMap<>();

	@Override
	public void get(int i, GSSNodeCreator<T> creator) {
		map.compute(i, (k, v) -> creator.create(v)); 
	}

	@Override
	public GSSNode<T> get(int i) {
		return map.get(i);
	}

	@Override
	public void reset(Input input) {
		super.reset(input);
		map = new HashMap<>();
	}

	@Override
	public Iterable<GSSNode<T>> getNodes() {
		return CollectionsUtil.concat(map.values(), super.map.values());
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public void put(int i, GSSNode<T> gssNode) {
		map.put(i, gssNode);
	}

	@Override
	public GSSNode<T> get(NonterminalGrammarSlot<T> slot, int i) {
		return map.computeIfAbsent(i, k -> new GSSNode<>(slot, i));
	}

}