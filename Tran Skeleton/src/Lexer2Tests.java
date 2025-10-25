import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Lexer2Tests {
    @Test
    public void KeyWordLexerTest() {
        var l = new Lexer("class interface something accessor: mutator: if else loop");
        try {
            var res = l.Lex();
            Assertions.assertEquals(10, res.size());
            Assertions.assertEquals(Token.TokenTypes.CLASS, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.INTERFACE, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("something", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.ACCESSOR, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.MUTATOR, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.IF, res.get(7).getType());
            Assertions.assertEquals(Token.TokenTypes.ELSE, res.get(8).getType());
            Assertions.assertEquals(Token.TokenTypes.LOOP, res.get(9).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void QuotedStringLexerTest() {
        var l = new Lexer("test \"hello\" 'c' \"there\" 1.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(3).getType());
            Assertions.assertEquals("there", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("1.2", res.get(4).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }
    @Test
    public void IndentTest() {
        var l = new Lexer(
                "loop keepGoing\n" +
                    "    if n >= 15\n" +
	                "        keepGoing = false\n"
                 );
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    // my test cases that I built for testing my lexer.
    @Test
    public void IndentTest2() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n"+
                        "    else\n"+
                        "        keepGoing = true\n"+
                        "string"

        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(25, res.size());

            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(14).getType());

            Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(17).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(21).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(22).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(23).getType());

        }

        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }



    @Test
    public void IndentTest3() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n" +
                        "    else\n" +
                        "        if\n" +
                        "            keepGoing = true\n" +
                        "string"

        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(29, res.size());

            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(25).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(26).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(27).getType());



        } catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());

        }
    }


    @Test
    public void IndentTest4() {
        var l = new Lexer(
                "class foo {indent level = 0}\n" +
                        "    Bar (number x) {indent level = 1}\n" +

                        "        loop x>0 {indent level = 2}\n" +

                        "            x=x-1 {indent level = 3}"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(25, res.size());





        } catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());

        }
    }

    @Test
    public void QuotedStringLexerTest2() {
        var l = new Lexer("test \"hello \n world \n I'm \n here\" \"th\nere\" 1.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello \n world \n I'm \n here", res.get(1).getValue());

            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(2).getType());
            Assertions.assertEquals("th\nere", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("1.2", res.get(3).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void commentsLexerTest() {
        var l = new Lexer("test \"hello\" {this is \n {inside comment} \n comment} 'c' \"there\" 1.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(3).getType());
            Assertions.assertEquals("there", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("1.2", res.get(4).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void comments2LexerTest() {
        var l = new Lexer("test \"hello\" {this is \n {inside comment \n comment} comment}  'c' \"there\" 1.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(3).getType());
            Assertions.assertEquals("there", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("1.2", res.get(4).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }
}
