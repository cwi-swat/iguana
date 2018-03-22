package org.iguana.parser.datadependent.precedence;

import iguana.regex.Char;
import iguana.regex.Seq;
import iguana.utils.input.Input;
import org.iguana.grammar.Grammar;
import org.iguana.grammar.GrammarGraph;
import org.iguana.grammar.symbol.*;
import org.iguana.grammar.transformation.DesugarPrecedenceAndAssociativity;
import org.iguana.parser.Iguana;
import org.iguana.parser.ParseResult;
import org.iguana.util.Configuration;
import org.junit.Assert;
import org.junit.Test;

import static org.iguana.grammar.symbol.LayoutStrategy.NO_LAYOUT;

@SuppressWarnings("unused")
public class Test8_4_b {

    @Test
    public void test() {
         Grammar grammar =

Grammar.builder()

// $default$ ::=  {UNDEFINED,-1,NON_REC} PREC(1,1) 
.addRule(Rule.withHead(Nonterminal.builder("$default$").build()).setLayoutStrategy(NO_LAYOUT).setRecursion(Recursion.NON_REC).setAssociativity(Associativity.UNDEFINED).setPrecedence(-1).setPrecedenceLevel(PrecedenceLevel.from(1,1,-1,false,false,false,new Integer[]{},false,new Integer[]{})).build())
// E ::= E (^) E  {LEFT,1,LEFT_RIGHT_REC} LEFT(1,1,1) PREC(1,1) 
.addRule(Rule.withHead(Nonterminal.builder("E").build()).addSymbol(Nonterminal.builder("E").build()).addSymbol(Terminal.builder(Seq.builder(Char.builder(94).build()).build()).build()).addSymbol(Nonterminal.builder("E").build()).setRecursion(Recursion.LEFT_RIGHT_REC).setAssociativity(Associativity.LEFT).setPrecedence(1).setAssociativityGroup(new AssociativityGroup(Associativity.LEFT,PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{}),1,1,1)).setPrecedenceLevel(PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{})).build())
// E ::= (-) E  {UNDEFINED,1,RIGHT_REC} LEFT(1,1,1) PREC(1,1) 
.addRule(Rule.withHead(Nonterminal.builder("E").build()).addSymbol(Terminal.builder(Seq.builder(Char.builder(45).build()).build()).build()).addSymbol(Nonterminal.builder("E").build()).setRecursion(Recursion.RIGHT_REC).setAssociativity(Associativity.UNDEFINED).setPrecedence(1).setAssociativityGroup(new AssociativityGroup(Associativity.LEFT,PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{}),1,1,1)).setPrecedenceLevel(PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{})).build())
// E ::= (+) E  {UNDEFINED,1,RIGHT_REC} LEFT(1,1,1) PREC(1,1) 
.addRule(Rule.withHead(Nonterminal.builder("E").build()).addSymbol(Terminal.builder(Seq.builder(Char.builder(43).build()).build()).build()).addSymbol(Nonterminal.builder("E").build()).setRecursion(Recursion.RIGHT_REC).setAssociativity(Associativity.UNDEFINED).setPrecedence(1).setAssociativityGroup(new AssociativityGroup(Associativity.LEFT,PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{}),1,1,1)).setPrecedenceLevel(PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{})).build())
// E ::= (a)  {UNDEFINED,-1,NON_REC} PREC(1,1) 
.addRule(Rule.withHead(Nonterminal.builder("E").build()).addSymbol(Terminal.builder(Seq.builder(Char.builder(97).build()).build()).build()).setRecursion(Recursion.NON_REC).setAssociativity(Associativity.UNDEFINED).setPrecedence(-1).setPrecedenceLevel(PrecedenceLevel.from(1,1,-1,true,false,false,new Integer[]{},false,new Integer[]{})).build())
// E ::= E (*) E  {UNDEFINED,2,LEFT_RIGHT_REC} PREC(2,2) 
.addRule(Rule.withHead(Nonterminal.builder("E").build()).addSymbol(Nonterminal.builder("E").build()).addSymbol(Terminal.builder(Seq.builder(Char.builder(42).build()).build()).build()).addSymbol(Nonterminal.builder("E").build()).setRecursion(Recursion.LEFT_RIGHT_REC).setAssociativity(Associativity.UNDEFINED).setPrecedence(2).setPrecedenceLevel(PrecedenceLevel.from(2,2,2,false,false,true,new Integer[]{1},false,new Integer[]{})).build())
// S ::= E  {UNDEFINED,-1,NON_REC} PREC(1,1) 
.addRule(Rule.withHead(Nonterminal.builder("S").build()).addSymbol(Nonterminal.builder("E").build()).setRecursion(Recursion.NON_REC).setAssociativity(Associativity.UNDEFINED).setPrecedence(-1).setPrecedenceLevel(PrecedenceLevel.from(1,1,-1,false,false,false,new Integer[]{},false,new Integer[]{})).build())
.build();

         grammar = new DesugarPrecedenceAndAssociativity().transform(grammar);
         System.out.println(grammar.toStringWithOrderByPrecedence());

         Input input = Input.fromString("a^+a");

         ParseResult result = Iguana.parse(input, grammar, Nonterminal.withName("S"));

         Assert.assertTrue(result.isParseSuccess());

         Assert.assertEquals(0, result.asParseSuccess().getStatistics().getAmbiguousNodesCount());
    }
}
