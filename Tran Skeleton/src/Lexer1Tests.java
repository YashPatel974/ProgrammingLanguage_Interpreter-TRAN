import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

public class Lexer1Tests {

    @Test
    public void SimpleLexerTest() {
        var l = new Lexer("ab cd ef gh");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test
    public void lineLexerTest() {
        var l = new Lexer("interface someName\r\n\tupdateClock()\r\n\tsquare() : number s");
        try {
            var res = l.Lex();
            Assertions.assertEquals(15, res.size());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MultilineLexerTest() {
        var l = new Lexer("ab cd ef gh\nasdjkdsajkl\ndsajkdsa asdjksald dsajhkl \n");
        try {
            var res = l.Lex();
            Assertions.assertEquals(11, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
            Assertions.assertEquals("asdjkdsajkl", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(6).getType());
            Assertions.assertEquals("dsajkdsa", res.get(7).getValue());
            Assertions.assertEquals("asdjksald", res.get(8).getValue());
            Assertions.assertEquals("dsajhkl", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void NotEqualsTest() {
        var l = new Lexer("!=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(0).getType());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }



    @Test
    public void TwoCharacterTest() {
        var l = new Lexer(">= > <= < = == !=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(7, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHAN, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHAN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(6).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MixedTest() {
        var l = new Lexer("word 1.2 : ( )");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("word", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("1.2", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(4).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }





// My test cases to check my code.

    @Test
    public void numberTest() {
        var l = new Lexer("1 2 3 7 88");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(0).getType());
            Assertions.assertEquals("1", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("2", res.get(1).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(2).getType());
            Assertions.assertEquals("3", res.get(2).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("7", res.get(3).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("88", res.get(4).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void decimalnumberTest() {
        var l = new Lexer("1 2 3 7 88 8.8 9.9 1.1 1.2 3.3");
        try {
            var res = l.Lex();
            Assertions.assertEquals(10, res.size());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(0).getType());
            Assertions.assertEquals("1", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("2", res.get(1).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(2).getType());
            Assertions.assertEquals("3", res.get(2).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("7", res.get(3).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("88", res.get(4).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(5).getType());
            Assertions.assertEquals("8.8", res.get(5).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(6).getType());
            Assertions.assertEquals("9.9", res.get(6).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(7).getType());
            Assertions.assertEquals("1.1", res.get(7).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(8).getType());
            Assertions.assertEquals("1.2", res.get(8).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(9).getType());
            Assertions.assertEquals("3.3", res.get(9).getValue());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MixedWordNumberTest() {
        var l = new Lexer("word\ndeci 1.2 2.3\npun\noppo");
        try {
            var res = l.Lex();
            Assertions.assertEquals(9, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("word", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
            //Assertions.assertEquals("word", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("deci", res.get(2).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("1.2", res.get(3).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("2.3", res.get(4).getValue());

            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(5).getType());

            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(6).getType());
            Assertions.assertEquals("pun", res.get(6).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(7).getType());

            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(8).getType());
            Assertions.assertEquals("oppo", res.get(8).getValue());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test
    public void singlePunctuationTest() {
        var l = new Lexer(": ( ) + - * / % , = >= <= == !=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(14, res.size());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.PLUS, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.MINUS, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.TIMES, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.DIVIDE, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.MODULO, res.get(7).getType());
            Assertions.assertEquals(Token.TokenTypes.COMMA, res.get(8).getType());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(9).getType());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(10).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(11).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(12).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(13).getType());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test
    public void DoublePunctuationTest() {
        var l = new Lexer(">= <= == != =");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(3).getType());

            Assertions.assertNotEquals(Token.TokenTypes.NOTEQUAL, res.get(4).getType());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void weirdnumberTest() {
        var l = new Lexer("2.3.5");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(0).getType());
            Assertions.assertEquals("2.3", res.get(0).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals(".5", res.get(1).getValue());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void onlydecimalnumberTest() {
        var l = new Lexer(".5 0.3");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(0).getType());
            Assertions.assertEquals(".5", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("0.3", res.get(1).getValue());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }
}
