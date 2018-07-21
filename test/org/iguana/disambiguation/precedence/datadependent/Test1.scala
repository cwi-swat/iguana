package org.iguana.disambiguation.precedence.datadependent

import iguana.utils.input.Input
import org.iguana.IGGY
import org.iguana.grammar.symbol.Nonterminal
import org.iguana.iggy.IggyParser
import org.iguana.parser.IguanaParser

/**
  * Created by Anastasia Izmaylova
  */
class Test1 extends FunSuite {

  val originalGrammar = {
    @IGGY
    val s =
      """
        | S ::= E
        | E ::= E '^' E right
        |     > E '-'
        |     | 'a'
      """.stripMargin
    IggyParser.getGrammar(s)
  }

  val start = Nonterminal.withName("S")

  val desugaredGrammar = {
    @IGGY
    val s =
      """
        | S ::= E(0)
        | E(p) ::= [2>=p] l=E(p) [l<=0||l>=3] '^' E(2) {2}
        |        | [1>=p] l=E(p) [l<=0||l>=1] '-' {0}
        |        | 'a' {0}
      """.stripMargin
    IggyParser.getGrammar(s)
  }

  test("Parser1") { runOriginal(Input.fromString("a^a^a")) }
  test("Parser2") { runOriginal(Input.fromString("a^a-")) }
  test("Parser3") { runOriginal(Input.fromString("a-^a")) }
  test("Parser4") { runOriginal(Input.fromString("a^a-^a")) }

  test("DDParser1") { runDesugared(Input.fromString("a^a^a")) }
  test("DDParser2") { runDesugared(Input.fromString("a^a-")) }
  test("DDParser3") { runDesugared(Input.fromString("a-^a")) }
  test("DDParser4") { runDesugared(Input.fromString("a^a-^a")) }

  private def runOriginal(input: Input) = {
    val result = IguanaParser.parse(input, originalGrammar, start)
    assert(result.isParseSuccess)
    assertResult(0)(result.asParseSuccess.getStatistics.getCountAmbiguousNodes)
  }

  private def runDesugared(input: Input) = {
    val result = IguanaParser.parse(input, desugaredGrammar, start)
    assert(result.isParseSuccess)
    assertResult(0)(result.asParseSuccess.getStatistics.getCountAmbiguousNodes)
  }

}
