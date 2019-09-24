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

package org.iguana.grammar.precedence;

import org.iguana.grammar.Grammar;
import org.iguana.grammar.patterns.AbstractPattern;
import org.iguana.grammar.patterns.ExceptPattern;
import org.iguana.grammar.patterns.PrecedencePattern;
import org.iguana.grammar.symbol.LayoutStrategy;
import org.iguana.grammar.symbol.Nonterminal;
import org.iguana.grammar.symbol.Rule;
import org.iguana.grammar.symbol.Symbol;
import org.iguana.grammar.transformation.EBNFToBNF;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class OperatorPrecedence {
	
	private Map<Nonterminal, List<List<Symbol>>> definitions;
	
	private Map<String, Integer> newNonterminals;
	
	private Map<Nonterminal, List<PrecedencePattern>> precednecePatterns;
	
	private List<ExceptPattern> exceptPatterns;
	
	private Map<List<List<Symbol>>, Nonterminal> existingAlternates;
	
	public OperatorPrecedence(Iterable<PrecedencePattern> precedencePatterns) {
		this(precedencePatterns, Collections.emptyList());
	}

	public OperatorPrecedence() {
		this.newNonterminals = new HashMap<>();
		this.precednecePatterns = new HashMap<>();
		this.existingAlternates = new HashMap<>();
		this.exceptPatterns = new ArrayList<>();
	}
	
	public OperatorPrecedence(Iterable<PrecedencePattern> precedencePatterns, Iterable<ExceptPattern> exceptPatterns) {
		this();		
		precedencePatterns.forEach(x -> add(x));
		exceptPatterns.forEach(x -> add(x));
	}
	
	public Grammar transform(Grammar grammar) {
		
		Map<Nonterminal, List<Rule>> l = grammar.getDefinitions();
		
		this.definitions = new HashMap<>();
		for (Entry<Nonterminal, List<Rule>> e : l.entrySet()) {
			List<List<Symbol>> listOfList = new ArrayList<>();
			for (List<Symbol> list : e.getValue().stream().map(r -> r.getBody()).collect(Collectors.toList())) {
				List<Symbol> newList = new ArrayList<>(list); 
				listOfList.add(newList);					
			}
			definitions.put(e.getKey(), listOfList);
		}

		rewritePrecedencePatterns();
		rewriteExceptPatterns();
		
		for(Nonterminal nonterminal : this.definitions.keySet()) {
			if(nonterminal.getIndex() > 0) {
				addNewRules(nonterminal, this.definitions.get(nonterminal));
			}
		}
		
		Grammar.Builder builder = new Grammar.Builder();
		for (Entry<Nonterminal, List<List<Symbol>>> e : definitions.entrySet()) {
			Nonterminal head = e.getKey();
			for (int i = 0; i < e.getValue().size(); i++) {
				List<Symbol> list = definitions.get(head).get(i);
				Symbol layout = grammar.getDefinitions().get(plain(head)).get(i).getLayout();
				LayoutStrategy strategy = grammar.getDefinitions().get(plain(head)).get(i).getLayoutStrategy();
				Rule rule = Rule.withHead(head).addSymbols(list).setLayout(layout).setLayoutStrategy(strategy).build();
				if (rule.getBody() != null)
					builder.addRule(rule);
			}
		}

		return builder.setLayout(grammar.getLayout()).build();
	}
	
	private void rewriteExceptPatterns() {
		rewriteExceptPatterns(groupPatterns(exceptPatterns));
	}
	
	private void rewriteExceptPatterns(Map<ExceptPattern, List<List<Symbol>>> patterns) {
		for (Entry<ExceptPattern, List<List<Symbol>>> e : patterns.entrySet()) {
			ExceptPattern pattern = e.getKey();
			
			for (List<Symbol> alt : definitions.get(pattern.getNonterminal())) {
				if (match(plain(alt), pattern.getParent())) {
					Nonterminal newNonterminal = createNewNonterminal(alt, pattern.getPosition(), e.getValue());
					set(alt, pattern.getPosition(), newNonterminal);
				}
			}
			
			if(newNonterminals.containsKey(pattern.getNonterminal().getName())) {
				int index = newNonterminals.get(pattern.getNonterminal().getName());
				for(int i = 1; i <= index; i++) {
					Nonterminal nonterminal = new Nonterminal.Builder(pattern.getNonterminal().getName()).setIndex(i).setEbnfList(pattern.getNonterminal().isEbnfList()).build();
					for(List<Symbol> alt : definitions.get(nonterminal)) {
						
						if(alt != null) {
							if (match(plain(alt), pattern.getParent())) {
								Nonterminal newNonterminal = createNewNonterminal(alt, pattern.getPosition(), e.getValue());
								set(alt, pattern.getPosition(), newNonterminal);
							}
						}						
					}					
				}
			}
		}
	}

	
	public void add(PrecedencePattern pattern) {
		pattern = new PrecedencePattern(pattern.getNonterminal(), 
										EBNFToBNF.rewrite(pattern.getParent(), null), 
										pattern.getPosition(), 
										EBNFToBNF.rewrite(pattern.getChild(), null));
		Nonterminal nonterminal = pattern.getNonterminal();
		if (precednecePatterns.containsKey(nonterminal)) {
			precednecePatterns.get(nonterminal).add(pattern);
		} else {
			List<PrecedencePattern> set = new ArrayList<>();
			set.add(pattern);
			precednecePatterns.put(nonterminal, set);
		}
	}
	
	public void add(ExceptPattern pattern) {
		pattern = new ExceptPattern(pattern.getNonterminal(), 
				EBNFToBNF.rewrite(pattern.getParent(), null), 
				pattern.getPosition(), 
				EBNFToBNF.rewrite(pattern.getChild(), null));
		exceptPatterns.add(pattern);
	}
	
	/**
	 * Groups filters based on their parent and position.
	 * For example, two filters (E, E * .E, E + E) and
	 * (E, E * .E, E * E) will be grouped as:
	 * (E, E * .E, {E * E, E + E}) 
	 * 
	 * @param patterns
	 * @return
	 */
	private <T extends AbstractPattern> Map<T, List<List<Symbol>>> groupPatterns(Iterable<T> patterns) {
		Map<T, Set<List<Symbol>>> group = new LinkedHashMap<>();
		
		for(T pattern : patterns) {
			Set<List<Symbol>> set = group.get(pattern);
			if(set == null) {
				set = new LinkedHashSet<>();
				group.put(pattern, set);
			}
			set.add(pattern.getChild());
		}
		
		Map<T, List<List<Symbol>>> result = new LinkedHashMap<>();
		for(Entry<T, Set<List<Symbol>>> e : group.entrySet()) {
			result.put(e.getKey(), new ArrayList<>(e.getValue()));
		}
		
		return result;
	}
	
	private void rewritePrecedencePatterns() {
		for (Entry<Nonterminal, List<PrecedencePattern>> entry : precednecePatterns.entrySet()) {
//			log.debug("Applying the pattern %s with %d.", entry.getKey(), entry.getValue().size());

			Nonterminal nonterminal = entry.getKey();
			Map<PrecedencePattern, List<List<Symbol>>> patterns = groupPatterns(entry.getValue());
			
			rewriteFirstLevel(nonterminal, patterns);
			// if (nonterminal.getIndex() > 0)
				rewriteDeeperLevels(nonterminal, patterns);
		}
	}

	private void rewriteDeeperLevels(Nonterminal head, Map<PrecedencePattern, List<List<Symbol>>> patterns) {
		
		for(Entry<PrecedencePattern, List<List<Symbol>>> e : patterns.entrySet()) {
			
			PrecedencePattern pattern = e.getKey();
			List<List<Symbol>> children = e.getValue();
			
			for (List<Symbol> alt : definitions.get(head)) {
				if (pattern.isLeftMost() && match(plain(alt), pattern.getParent())) {
					Nonterminal nt = (Nonterminal)alt.get(0);
					rewriteRightEnds(nt, pattern, children, new HashSet<Nonterminal>());
					rewriteIndirectRightEnds(nt, pattern, children, new HashSet<Nonterminal>());
				}

				if (pattern.isRightMost() && match(plain(alt), pattern.getParent())) {
					Nonterminal nt = (Nonterminal)alt.get(alt.size() - 1);
					rewriteLeftEnds(nt, pattern, children, new HashSet<Nonterminal>());
					rewriteIndirectLeftEnds(nt, pattern, children, new HashSet<Nonterminal>());
				}
			}
		}
	}
	
	private void rewriteLeftEnds(Nonterminal nonterminal, PrecedencePattern pattern, List<List<Symbol>> children, Set<Nonterminal> visited) {
		
		if (visited.contains(nonterminal)) {
			return;
		} else {
			visited.add(nonterminal);
		}
		
		if (nonterminal.getIndex() == 0) return;
			
		for(List<Symbol> alternate : definitions.get(nonterminal)) {
			
			if(alternate == null) {
				continue;
			}
			
			if (alternate.size() == 0 || !(alternate.get(0) instanceof Nonterminal)) {
				continue;
			}

			Nonterminal first = (Nonterminal) alternate.get(0);
			
			if (plainEqual(first, pattern.getNonterminal())) {
				if(contains(first, children)) {
					Nonterminal newNonterminal = createNewNonterminal(alternate, 0, children);
					set(alternate, 0, newNonterminal);
					rewriteLeftEnds(newNonterminal, pattern, children, visited);
				}				
			}
		}			
	}
	
	private void rewriteIndirectLeftEnds(Nonterminal nonterminal, PrecedencePattern pattern, List<List<Symbol>> children, Set<Nonterminal> visited) {
		
		if (visited.contains(nonterminal)) {
			return;
		} else {
			visited.add(nonterminal);
		}
		
		if (nonterminal.getIndex() == 0) return;
			
		for(List<Symbol> alternate : definitions.get(nonterminal)) {
			
			if(alternate == null) {
				continue;
			}
			
			if (alternate.size() == 0 || !(alternate.get(0) instanceof Nonterminal)) {
				continue;
			}

			Nonterminal first = (Nonterminal) alternate.get(0);
			
			if (plainEqual(first, pattern.getNonterminal())) {
				if(contains(first, children)) {
					Nonterminal newNonterminal = createNewNonterminal(alternate, 0, children);
					set(alternate, 0, newNonterminal);
					rewriteLeftEnds(newNonterminal, pattern, children, visited);
				}				
			} else {
				assert pattern.isRightMost();
				rewriteIndirectLeftEnds(first, pattern, children, visited);
			}
		}			
	}
	
	private void rewriteRightEnds(Nonterminal nonterminal, PrecedencePattern pattern, List<List<Symbol>> children, Set<Nonterminal> visited) {
		
		if (visited.contains(nonterminal)) {
			return;
		} else {
			visited.add(nonterminal);
		}
		
		if (nonterminal.getIndex() == 0) return;
			
		for(List<Symbol> alternate : definitions.get(nonterminal)) {
			
			if (alternate == null) {
				continue;
			}
			
			if (alternate.size() == 0 || !(alternate.get(alternate.size() - 1) instanceof Nonterminal)) {
				continue;
			}

			Nonterminal last = (Nonterminal) alternate.get(alternate.size() - 1);
			
			if (plainEqual(last, pattern.getNonterminal())) {
				if(contains(last, children)) {
					Nonterminal newNonterminal = createNewNonterminal(alternate, alternate.size() - 1, children);
					set(alternate, alternate.size() - 1, newNonterminal);
					rewriteRightEnds(newNonterminal, pattern, children, visited);
				}				
			}
		}
	}
	
	private void rewriteIndirectRightEnds(Nonterminal nonterminal, PrecedencePattern pattern, List<List<Symbol>> children, Set<Nonterminal> visited) {
		
		if (visited.contains(nonterminal)) {
			return;
		} else {
			visited.add(nonterminal);
		}
		
		if (nonterminal.getIndex() == 0) return;
		
		for(List<Symbol> alternate : definitions.get(nonterminal)) {
			
			if (alternate == null) {
				continue;
			}
			
			if (alternate.size() == 0 || !(alternate.get(alternate.size() - 1) instanceof Nonterminal)) {
				continue;
			}

			Nonterminal last = (Nonterminal) alternate.get(alternate.size() - 1);
			
			if (plainEqual(last, pattern.getNonterminal())) {
				if(contains(last, children)) {
					Nonterminal newNonterminal = createNewNonterminal(alternate, alternate.size() - 1, children);
					set(alternate, alternate.size() - 1, newNonterminal);
					rewriteRightEnds(newNonterminal, pattern, children, visited);
				}				
			} else {
				rewriteIndirectRightEnds(last, pattern, children, visited);
			}
		}
	}
	
	private Nonterminal createNewNonterminal(List<Symbol> alt, int position, List<List<Symbol>> filteredAlternates) {
		
		Nonterminal filteredNonterminal = (Nonterminal) alt.get(position);

		List<List<Symbol>> set = without(filteredNonterminal, filteredAlternates);
		Nonterminal newNonterminal = existingAlternates.get(plain2(set));
		
		if(newNonterminal == null) {
			newNonterminal = createNewNonterminal(filteredNonterminal);
			List<List<Symbol>> copy = copyAlternates(set);
			existingAlternates.put(plain2(copy), newNonterminal);
			definitions.put(newNonterminal, copy);
		}
		
		return newNonterminal;
	}
	
	private void rewriteFirstLevel(Nonterminal head, Map<PrecedencePattern, List<List<Symbol>>> patterns) {
		
		Map<PrecedencePattern, Nonterminal> freshNonterminals = new LinkedHashMap<>();
		
		Map<List<List<Symbol>>, Nonterminal> map = new HashMap<>();
		
		if(!newNonterminals.containsKey(head.getName())) {
			newNonterminals.put(head.getName(), 0);
		}
		
		// Creating fresh nonterminals
		for(Entry<PrecedencePattern, List<List<Symbol>>> e : patterns.entrySet()) {
			
			PrecedencePattern pattern = e.getKey();
			
			Nonterminal freshNonterminal = map.get(e.getValue());
			
			if(freshNonterminal == null) {
				int index = newNonterminals.get(pattern.getNonterminal().getName());
				freshNonterminal = new Nonterminal.Builder(pattern.getNonterminal().getName())
												  .setIndex(index + 1)
												  .setEbnfList(pattern.getNonterminal().isEbnfList())
												  .build();
				newNonterminals.put(freshNonterminal.getName(), index + 1);
				map.put(e.getValue(), freshNonterminal);
			}

			freshNonterminals.put(pattern, freshNonterminal);
		}
		
		// Replacing nonterminals with their fresh ones
		for(Entry<PrecedencePattern, List<List<Symbol>>> e : patterns.entrySet()) {
			
			PrecedencePattern pattern = e.getKey();

			for(List<Symbol> alt : definitions.get(head)) {
				
				if(!match(plain(alt), pattern.getParent())) {
					continue;
				}
				
//				log.trace("Applying the pattern %s on %s.", pattern, alt);
				
				if (!pattern.isDirect()) {
					
					Nonterminal copy;
					
					Set<List<Symbol>> alternates = new LinkedHashSet<>();
					if(pattern.isLeftMost()) {
						copy = copyIndirectAtLeft((Nonterminal) alt.get(pattern.getPosition()), pattern.getNonterminal());
						getLeftEnds(copy, pattern.getNonterminal(), alternates);
						for(List<Symbol> a : alternates) {
							set(a, 0, new Nonterminal.Builder(freshNonterminals.get(pattern)).addConditions(a.get(0)).build());
						}
					} else {
						copy = copyIndirectAtRight((Nonterminal) alt.get(pattern.getPosition()), pattern.getNonterminal());
						getRightEnds(copy, pattern.getNonterminal(), alternates);
						for(List<Symbol> a : alternates) {
							set(a, a.size() - 1, new Nonterminal.Builder(freshNonterminals.get(pattern)).addConditions(a.get(a.size() - 1)).build());
						}
					}
					
					set(alt, pattern.getPosition(), new Nonterminal.Builder(copy).addConditions(alt.get(pattern.getPosition())).build());

				} else {
					set(alt, pattern.getPosition(), new Nonterminal.Builder(freshNonterminals.get(pattern)).addConditions(alt.get(pattern.getPosition())).build());
				}
			}
		}
		
		// creating the body of fresh direct nonterminals
		for(Entry<PrecedencePattern, Nonterminal> e : freshNonterminals.entrySet()) {
			PrecedencePattern pattern = e.getKey();
			Nonterminal freshNonterminal = e.getValue();
			List<List<Symbol>> alternates = deepCopy(without(head, patterns.get(pattern)), pattern.getNonterminal());
			definitions.put(freshNonterminal, alternates);
		}
		
	}
	
	private void set(List<Symbol> alt, int position, Symbol newSymbol) {
		alt.set(position, newSymbol);
	}
	
	/**
	 * 
	 * Returns a list of all nonterminals with the given name which are
	 * reachable from the given head and are on the left-most end.
	 * 
	 */
	private void getLeftEnds(Nonterminal head, Nonterminal nonterminal, Set<List<Symbol>> nonterminals) {
		getLeftEnds(head, nonterminal, nonterminals, new HashSet<Nonterminal>());
	}
	
	private void getLeftEnds(Nonterminal head, Nonterminal nonterminal, Set<List<Symbol>> nonterminals, Set<Nonterminal> visited) {
		
		if (visited.contains(head)) {
			return;
		} else {
		    visited.add(head);
		}
		
		for (List<Symbol> alt : definitions.get(head)) {
			
			if(alt == null || alt.size() == 0) {
				continue;
			}
			
			if (alt.get(0) instanceof Nonterminal) {
				Nonterminal first = (Nonterminal) alt.get(0);
				if (plainEqual(first, nonterminal)) {
					nonterminals.add(alt);
				} else {
					getLeftEnds(first, nonterminal, nonterminals, visited);
				}
			}
		}
	}

	/**
	 * 
	 * Returns a list of all nonterminals with the given name which are
	 * reachable from the given head and are on the right-most end.
	 * 
	 * @param head
	 * @param directNonterminal
	 * @param alternates
	 */
	private void getRightEnds(Nonterminal head, Nonterminal directNonterminal, Set<List<Symbol>> alternates) {
		getRightEnds(head, directNonterminal, alternates, new HashSet<Nonterminal>());
	}
	
	private void getRightEnds(Nonterminal head, Nonterminal directNonterminal, Set<List<Symbol>> alternates, Set<Nonterminal> visited) {
		
		if(visited.contains(head)) {
			return;
		} else {
			visited.add(head);
		}
		
		for (List<Symbol> alt : definitions.get(head)) {
			
			if(alt == null || alt.size() == 0) {
				continue;
			}
			
			if (alt.get(alt.size() - 1) instanceof Nonterminal) {
				Nonterminal last = (Nonterminal) alt.get(alt.size() - 1);
				if (plainEqual(last, directNonterminal)) {
					alternates.add(alt);
				} else {
					getRightEnds(last, directNonterminal, alternates, visited);
				}
			}
		}
	}
	
	
	private Nonterminal copyIndirectAtLeft(Nonterminal head, Nonterminal directNonterminal) {
		return copyIndirectAtLeft(head, directNonterminal, new HashMap<Nonterminal, Nonterminal>());
	}

	private Nonterminal copyIndirectAtRight(Nonterminal head, Nonterminal directNonterminal) {
		return copyIndirectAtRight(head, directNonterminal, new HashMap<Nonterminal, Nonterminal>());
	}
	
	private Nonterminal copyIndirectAtLeft(Nonterminal head, Nonterminal directName, HashMap<Nonterminal, Nonterminal> map) {
		
		Nonterminal copy = map.get(head);
		if(copy != null) {
			return copy;
		}
		
		copy = createNewNonterminal(head);
		map.put(head, copy);

		List<List<Symbol>> copyAlternates = copyAlternates(definitions.get(head));
		definitions.put(copy, copyAlternates);
		
		for(List<Symbol> alt : copyAlternates) {
			if(alt.get(0) instanceof Nonterminal) {
				Nonterminal nonterminal = (Nonterminal) alt.get(0);
				// Leave the direct nonterminal, copy indirect ones
				if(!nonterminal.equals(directName)) {
					set(alt, 0, copyIndirectAtLeft(nonterminal, directName, map));
				}
			}
		}
		
		return copy;
	}
	
	private Nonterminal copyIndirectAtRight(Nonterminal head, Nonterminal directNonterminal, HashMap<Nonterminal, Nonterminal> map) {
		
		Nonterminal copy = map.get(head);
		if(copy != null) {
			return copy;
		}
		
		copy = createNewNonterminal(head);
		map.put(head, copy);
		
		List<List<Symbol>> copyAlternates = copyAlternates(definitions.get(head));
		definitions.put(copy, copyAlternates);
		
		for(List<Symbol> alt : copyAlternates) {
			
			if (alt.size() == 0)
				continue;
			
			if(alt.get(alt.size() - 1) instanceof Nonterminal) {
				Nonterminal nonterminal = (Nonterminal) alt.get(alt.size() - 1);
				
				// Leave the direct nonterminal, copy indirect ones
				if(!nonterminal.equals(directNonterminal)) {
					set(alt, alt.size() - 1, copyIndirectAtRight(nonterminal, directNonterminal, map));
				}
			}
		}
		
		return copy;
	}
	
	private Nonterminal createNewNonterminal(Nonterminal nonterminal) {
		
		if(!newNonterminals.containsKey(nonterminal.getName())) {
			newNonterminals.put(nonterminal.getName(), 0);
		}
		
		int index = newNonterminals.get(nonterminal.getName());
		Nonterminal newNonterminal = new Nonterminal.Builder(nonterminal.getName())
												    .setIndex(index + 1)
												    .setEbnfList(nonterminal.isEbnfList())
												    .addConditions(nonterminal)
												    .build();
		newNonterminals.put(nonterminal.getName(), index + 1);
		
		return newNonterminal;
	}
	
	public boolean match(List<Symbol> list1, List<Symbol> list2) {
		
		if(list1.size() != list2.size()) {
			return false;
		}
		
		for(int i = 0; i < list1.size(); i++) {
			if(!list1.get(i).equals(list2.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	private List<List<Symbol>> deepCopy(List<List<Symbol>> alternates, Nonterminal nonterminal) {
		return deepCopy(alternates, nonterminal, new HashMap<Nonterminal, Nonterminal>());
	}
	
	private List<List<Symbol>> deepCopy(List<List<Symbol>> alternates, Nonterminal nonterminal, Map<Nonterminal, Nonterminal> map) {
		List<List<Symbol>> copyAlts = new ArrayList<>();
		for(List<Symbol> alternate : alternates) {
			if (alternate != null) {
				copyAlts.add(copy(alternate, nonterminal, map));
			} else {
				copyAlts.add(null);
			}
		}
		return copyAlts;
	}
	
	private List<Symbol> copy(List<Symbol> alternate,  Nonterminal nonterminal, Map<Nonterminal, Nonterminal> map) {
		List<Symbol> copyAlt = new ArrayList<>();
		for(Symbol symbol : alternate) {
			if (symbol instanceof Nonterminal) {
				Nonterminal n = (Nonterminal) symbol;
				if (!plainEqual(n, nonterminal) && n.getIndex() > 0) {
					if (map.containsKey(n)) {
						Nonterminal newNonterminal = map.get(n);
						copyAlt.add(newNonterminal);						
					} else {
						Nonterminal newNonterminal = createNewNonterminal(n);
						map.put(n,  newNonterminal);
                        definitions.put(newNonterminal, deepCopy(definitions.get(n), nonterminal, map));
						copyAlt.add(newNonterminal);				
					}
					continue;
				}
			}
			copyAlt.add(symbol);
		}
		return copyAlt;
	}
	
	private List<List<Symbol>> copyAlternates(List<List<Symbol>> alternates) {
		List<List<Symbol>> copy = new ArrayList<>();
		
		for(List<Symbol> alternate : alternates) {
			
			if(alternate == null) {
				copy.add(null);
			} else {
				List<Symbol> copyAlt = new ArrayList<>();
				for(Symbol symbol : alternate) {
					copyAlt.add(symbol);
				}
				copy.add(copyAlt);				
			}
		}
		
		return copy;
	}
	
	private List<List<Symbol>> without(Nonterminal head, List<List<Symbol>> set) {
		List<List<Symbol>> without = new ArrayList<>();
		for(List<Symbol> alt : definitions.get(head)) {
			
			if(alt == null) {
				without.add(null);
			} 
			else if (!set.contains(plain(alt))) {
				without.add(alt);
			} else {
				without.add(null);
			}
		}
		return without;
	}
	
	public static List<List<Symbol>> plain2(List<List<Symbol>> alternates) {
		List<List<Symbol>> list = new ArrayList<>();
		
		for(List<Symbol> l : alternates) {
			if(l == null) {
				list.add(null);
			} else {
				list.add(plain(l));				
			}
		}
		
		return list;
	}
	
	public static Rule plain(Rule rule) {
		Nonterminal plainHead = (Nonterminal) plain(rule.getHead());
		List<Symbol> plainAlternate = plain(rule.getBody());
		return Rule.withHead(plainHead).addSymbols(plainAlternate).build();
	}
	
	public static List<Symbol> plain(List<Symbol> alternate) {
		List<Symbol> plain = new ArrayList<>();
		for(Symbol symbol : alternate) {
			if(symbol instanceof Nonterminal && ((Nonterminal) symbol).getIndex() > 0) {
				Nonterminal nonterminal = new Nonterminal.Builder(symbol.getName()).setEbnfList(((Nonterminal) symbol).isEbnfList()).build();
				plain.add(nonterminal);
			} else {
				plain.add(symbol);
			}
		}
		return plain;
	}
	
	public static Symbol plain(Symbol symbol) {
		if(symbol instanceof Nonterminal && ((Nonterminal) symbol).getIndex() > 0) {
			return new Nonterminal.Builder(symbol.getName()).setEbnfList(((Nonterminal) symbol).isEbnfList()).build();
		} else {
			return symbol;
		}
	}
	
	public static boolean plainEqual(Nonterminal n1, Nonterminal n2) {
		return n1.getName().equals(n2.getName());
	}

	private void addNewRules(Nonterminal nonterminal, List<List<Symbol>> alternates) {
		definitions.put(nonterminal, alternates);
	}
	
	private boolean contains(Nonterminal nonterminal, List<List<Symbol>> alternates) {
		
		List<List<Symbol>> set = definitions.get(nonterminal);
		
		for(List<Symbol> alt1 : set) {
			
			if(alt1 == null) continue;
			
			for(List<Symbol> alt2 : alternates) {
				
				if(alt2 == null) continue;
				
				if(plain(alt1).equals(alt2)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
}
