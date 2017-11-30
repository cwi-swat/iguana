package org.iguana.parser.ebnf;

import iguana.regex.Character;
import iguana.utils.input.Input;
import org.iguana.grammar.Grammar;
import org.iguana.grammar.GrammarGraph;
import org.iguana.grammar.symbol.Nonterminal;
import org.iguana.grammar.symbol.Plus;
import org.iguana.grammar.symbol.Rule;
import org.iguana.grammar.symbol.Terminal;
import org.iguana.grammar.transformation.EBNFToBNF;
import org.iguana.parser.Iguana;
import org.iguana.parser.ParseResult;
import org.iguana.parser.ParseSuccess;
import org.iguana.sppf.IntermediateNode;
import org.iguana.sppf.NonterminalNode;
import org.iguana.sppf.SPPFNodeFactory;
import org.iguana.sppf.TerminalNode;
import org.iguana.util.Configuration;
import org.iguana.util.ParseStatistics;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * S ::= A+
 *
 * A ::= 'a'
 *
 */
public class Test4 {

    static Nonterminal S = Nonterminal.withName("S");
    static Nonterminal A = Nonterminal.withName("A");
    static Terminal a = Terminal.from(Character.from('a'));
    static Rule r1 = Rule.withHead(S).addSymbols(Plus.from(A)).build();
    static Rule r2 = Rule.withHead(A).addSymbols(a).build();
    private static Grammar grammar = Grammar.builder().addRules(r1, r2).build();

    private static Input input1 = Input.fromString("a");
    private static Input input2 = Input.fromString("aa");
    private static Input input3 = Input.fromString("aaa");


    @Test
    public void testParser1() {
        grammar = EBNFToBNF.convert(grammar);
        GrammarGraph graph = GrammarGraph.from(grammar, input1, Configuration.DEFAULT);
        ParseResult result = Iguana.parse(input1, graph, S);
        assertTrue(result.isParseSuccess());
        assertEquals(getParseResult1(graph), result);
    }

    @Test
    public void testParser2() {
        grammar = EBNFToBNF.convert(grammar);
        GrammarGraph graph = GrammarGraph.from(grammar, input2, Configuration.DEFAULT);
        ParseResult result = Iguana.parse(input2, graph, S);
        assertTrue(result.isParseSuccess());
        assertEquals(getParseResult2(graph), result);
    }

    @Test
    public void testParser3() {
        grammar = EBNFToBNF.convert(grammar);
        GrammarGraph graph = GrammarGraph.from(grammar, input3, Configuration.DEFAULT);
        ParseResult result = Iguana.parse(input3, graph, S);
        assertTrue(result.isParseSuccess());
        assertEquals(getParseResult3(graph), result);
    }

    private static ParseResult getParseResult1(GrammarGraph graph) {
        ParseStatistics statistics = ParseStatistics.builder()
                .setDescriptorsCount(6)
                .setGSSNodesCount(3)
                .setGSSEdgesCount(3)
                .setNonterminalNodesCount(3)
                .setTerminalNodesCount(1)
                .setIntermediateNodesCount(0)
                .setPackedNodesCount(3)
                .setAmbiguousNodesCount(0).build();
        return new ParseSuccess(expectedSPPF1(new SPPFNodeFactory(graph)), statistics, input1);
    }

    private static NonterminalNode expectedSPPF1(SPPFNodeFactory factory) {
        TerminalNode node0 = factory.createTerminalNode("a", 0, 1, input1);
        NonterminalNode node1 = factory.createNonterminalNode("A", "A ::= a .", node0, input1);
        NonterminalNode node2 = factory.createNonterminalNode("A+", "A+ ::= A .", node1, input1);
        NonterminalNode node3 = factory.createNonterminalNode("S", "S ::= A+ .", node2, input1);
        return node3;
    }

    private static ParseResult getParseResult2(GrammarGraph graph) {
        ParseStatistics statistics = ParseStatistics.builder()
                .setDescriptorsCount(9)
                .setGSSNodesCount(4)
                .setGSSEdgesCount(4)
                .setNonterminalNodesCount(5)
                .setTerminalNodesCount(2)
                .setIntermediateNodesCount(1)
                .setPackedNodesCount(6)
                .setAmbiguousNodesCount(0).build();
        return new ParseSuccess(expectedSPPF2(new SPPFNodeFactory(graph)), statistics, input2);
    }

    private static NonterminalNode expectedSPPF2(SPPFNodeFactory factory) {
        TerminalNode node0 = factory.createTerminalNode("a", 0, 1, input2);
        NonterminalNode node1 = factory.createNonterminalNode("A", "A ::= a .", node0, input2);
        NonterminalNode node2 = factory.createNonterminalNode("A+", "A+ ::= A .", node1, input2);
        TerminalNode node3 = factory.createTerminalNode("a", 1, 2, input2);
        NonterminalNode node4 = factory.createNonterminalNode("A", "A ::= a .", node3, input2);
        IntermediateNode node5 = factory.createIntermediateNode("A+ ::= A+ A .", node2, node4);
        NonterminalNode node6 = factory.createNonterminalNode("A+", "A+ ::= A+ A .", node5, input2);
        NonterminalNode node7 = factory.createNonterminalNode("S", "S ::= A+ .", node6, input2);
        return node7;
    }

    private static ParseResult getParseResult3(GrammarGraph graph) {
        ParseStatistics statistics = ParseStatistics.builder()
                .setDescriptorsCount(12)
                .setGSSNodesCount(5)
                .setGSSEdgesCount(5)
                .setNonterminalNodesCount(7)
                .setTerminalNodesCount(3)
                .setIntermediateNodesCount(2)
                .setPackedNodesCount(9)
                .setAmbiguousNodesCount(0).build();
        return new ParseSuccess(expectedSPPF3(new SPPFNodeFactory(graph)), statistics, input2);
    }

    private static NonterminalNode expectedSPPF3(SPPFNodeFactory factory) {
        TerminalNode node0 = factory.createTerminalNode("a", 0, 1, input3);
        NonterminalNode node1 = factory.createNonterminalNode("A", "A ::= a .", node0, input3);
        NonterminalNode node2 = factory.createNonterminalNode("A+", "A+ ::= A .", node1, input3);
        TerminalNode node3 = factory.createTerminalNode("a", 1, 2, input3);
        NonterminalNode node4 = factory.createNonterminalNode("A", "A ::= a .", node3, input3);
        IntermediateNode node5 = factory.createIntermediateNode("A+ ::= A+ A .", node2, node4);
        NonterminalNode node6 = factory.createNonterminalNode("A+", "A+ ::= A+ A .", node5, input3);
        TerminalNode node7 = factory.createTerminalNode("a", 2, 3, input3);
        NonterminalNode node8 = factory.createNonterminalNode("A", "A ::= a .", node7, input3);
        IntermediateNode node9 = factory.createIntermediateNode("A+ ::= A+ A .", node6, node8);
        NonterminalNode node10 = factory.createNonterminalNode("A+", "A+ ::= A+ A .", node9, input3);
        NonterminalNode node11 = factory.createNonterminalNode("S", "S ::= A+ .", node10, input3);
        return node11;
    }

}
