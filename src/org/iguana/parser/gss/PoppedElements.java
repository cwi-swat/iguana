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

package org.iguana.parser.gss;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.iguana.datadependent.util.collections.IntKey1PlusObject;
import org.iguana.grammar.slot.EndGrammarSlot;
import org.iguana.parser.GLLParser;
import org.iguana.sppf.NonPackedNode;
import org.iguana.sppf.NonterminalNode;
import org.iguana.sppf.PackedNode;
import org.iguana.util.Holder;
import org.iguana.util.collections.Key;

public class PoppedElements {
	
	private NonterminalNode first;
	
	private Map<Object, NonterminalNode> poppedElements;
	
	public NonterminalNode add(GLLParser parser, int inputIndex, int j, EndGrammarSlot slot, NonPackedNode child) {
		// No node added yet
		if (first == null) {
			first = new NonterminalNode(slot.getNonterminal(), inputIndex, j);
			first.addPackedNode(parser, new PackedNode(slot, first), child);
			parser.nonterminalNodeAdded(first);
			return first;
		// Only one node is added and there is an ambiguity
		} else if (poppedElements == null && first.getRightExtent() == j) {
			first.addPackedNode(parser, new PackedNode(slot, first), child);
			return first;
		} 
		else {
			// Initialize the map and put the first element there
			if (poppedElements == null) {
				poppedElements = new HashMap<>();
				poppedElements.put(first.getRightExtent(), first);
			}
			
			Holder<NonterminalNode> holder = new Holder<>();
			poppedElements.compute(j, (k, v) -> { 
				if (v == null) {
					NonterminalNode node = new NonterminalNode(slot.getNonterminal(), inputIndex, j);
					node.addPackedNode(parser, new PackedNode(slot, node), child);
					parser.nonterminalNodeAdded(node);
					holder.set(node);
					return node;
				}
				else {
					v.addPackedNode(parser, new PackedNode(slot, v), child);
					return v;
				}
			});
			
	 		return holder.get();
		}
	}
	
	public NonterminalNode add(GLLParser parser, int inputIndex, Key key, EndGrammarSlot slot, NonPackedNode child, Object value) {
		// No node added yet
		if (first == null) {
			first = new NonterminalNode(slot.getNonterminal(), inputIndex, child.getRightExtent(), value);
			first.addPackedNode(parser, new PackedNode(slot, first), child);
			return first;
		// Only one node is added and there is an ambiguity
		} else if (poppedElements == null && IntKey1PlusObject.from(first.getRightExtent(), first.getValue(), parser.getInput().length()).equals(key)) {
			first.addPackedNode(parser, new PackedNode(slot, first), child);
			return first;
		} else {
			// Initialize the map and put the first element there
			if (poppedElements == null) {
				poppedElements = new HashMap<>();
				poppedElements.put(IntKey1PlusObject.from(first.getRightExtent(), first.getValue(), parser.getInput().length()), first);
			}
			
			Holder<NonterminalNode> holder = new Holder<>();
			poppedElements.compute(key, (k, v) -> { 
				if (v == null) {
					NonterminalNode node = new NonterminalNode(slot.getNonterminal(), inputIndex, child.getRightExtent(), value);
					node.addPackedNode(parser, new PackedNode(slot, node), child);
					holder.set(node);
					return node;
				}
				else {
					v.addPackedNode(parser, new PackedNode(slot, v), child);
					return v;
				}
			});
			return holder.get();
		}
	}
	
	public void forEach(Consumer<NonterminalNode> c) {
		if (poppedElements == null) {
			if (first != null) c.accept(first);
		} else {
			poppedElements.values().forEach(c);
		}
	}
	
	public NonterminalNode getNonterminalNode(int j) {
		if (poppedElements == null) {
			if (first != null && first.getRightExtent() == j)
				return first;
			else 
				return null;
		}
		return poppedElements.get(j);
	}
	
	public int size() {
		if (poppedElements == null) {
			return first != null ? 1 : 0;
		} else {
			return poppedElements.size();
		}
	}
}