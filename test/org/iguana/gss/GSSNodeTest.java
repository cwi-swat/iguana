package org.iguana.gss;

import iguana.regex.Seq;
import iguana.utils.input.Input;
import org.iguana.grammar.Grammar;
import org.iguana.grammar.GrammarGraph;
import org.iguana.grammar.slot.EndGrammarSlot;
import org.iguana.grammar.slot.NonterminalGrammarSlot;
import org.iguana.grammar.symbol.Nonterminal;
import org.iguana.grammar.symbol.Rule;
import org.iguana.grammar.symbol.Terminal;
import org.iguana.parser.IguanaRuntime;
import org.iguana.result.ParserResultOps;
import org.iguana.sppf.NonPackedNode;
import org.iguana.sppf.NonterminalNode;
import org.iguana.sppf.SPPFNodeFactory;
import org.iguana.sppf.TerminalNode;
import org.iguana.util.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GSSNodeTest {

    private Input input;
    private SPPFNodeFactory sppfFactory;
    private GrammarGraph grammarGraph;

    @Before
    public void init() {
        Rule rule = Rule.withHead(Nonterminal.withName("A")).addSymbol(Terminal.from(Seq.from("a"))).build();
        Grammar grammar = Grammar.builder().addRule(rule).build();

        grammarGraph = GrammarGraph.from(grammar);
        sppfFactory = new SPPFNodeFactory(grammarGraph);
        input = Input.fromString("Test");
    }

    @Test
    public void test() {
        NonterminalGrammarSlot nonterminalGrammarSlot = grammarGraph.getNonterminalGrammarSlot("A");
        GSSNode gssNode = new GSSNode(nonterminalGrammarSlot, 0);

        EndGrammarSlot endGrammarSlot = grammarGraph.getEndGrammarSlot("A ::= a .");

        TerminalNode terminalNode01 = sppfFactory.createTerminalNode("a", 0, 1);
        TerminalNode terminalNode02 = sppfFactory.createTerminalNode("a", 0, 2);
        TerminalNode terminalNode03 = sppfFactory.createTerminalNode("a", 0, 3);

        IguanaRuntime<NonPackedNode> runtime = new IguanaRuntime<>(Configuration.load(), new ParserResultOps());

        // Pop ("a", 0, 1)
        assertTrue(gssNode.pop(input, endGrammarSlot, terminalNode01, runtime));

        NonterminalNode expected = sppfFactory.createNonterminalNode("A ::= a .", terminalNode01);
        assertEquals(expected, gssNode.getPoppedElements().get(0));

        // Pop ("a", 0, 1)
        assertFalse(gssNode.pop(input, endGrammarSlot, terminalNode01, runtime));
        assertEquals(1, gssNode.countPoppedElements());
        assertEquals(2, ((NonPackedNode) gssNode.getPoppedElements().get(0)).childrenCount());

        // Pop ("a", 0, 2)
        assertTrue(gssNode.pop(input, endGrammarSlot, terminalNode02, runtime));
        assertEquals(2, gssNode.countPoppedElements());
        assertEquals(2, ((NonPackedNode) gssNode.getPoppedElements().get(0)).childrenCount());
        assertEquals(1, ((NonPackedNode) gssNode.getPoppedElements().get(1)).childrenCount());

        // Pop ("a", 0, 3)
        assertTrue(gssNode.pop(input, endGrammarSlot, terminalNode03, runtime));
        assertEquals(3, gssNode.countPoppedElements());
        assertEquals(2, ((NonPackedNode) gssNode.getPoppedElements().get(0)).childrenCount());
        assertEquals(1, ((NonPackedNode) gssNode.getPoppedElements().get(1)).childrenCount());
        assertEquals(1, ((NonPackedNode) gssNode.getPoppedElements().get(2)).childrenCount());

        // Pop ("a", 0, 2)
        assertFalse(gssNode.pop(input, endGrammarSlot, terminalNode02, runtime));
        assertEquals(3, gssNode.countPoppedElements());
        assertEquals(2, ((NonPackedNode) gssNode.getPoppedElements().get(0)).childrenCount());
        assertEquals(2, ((NonPackedNode) gssNode.getPoppedElements().get(1)).childrenCount());
        assertEquals(1, ((NonPackedNode) gssNode.getPoppedElements().get(2)).childrenCount());
    }
}
