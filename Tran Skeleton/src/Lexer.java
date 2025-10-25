import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

public class Lexer {
    private TextManager text1; // = new TextManager()

    // line and character positions
    private int line_position = 1;
    private int char_position = 0;

    // to keep track of indentation level.
    private int indent_number = 0;

    private HashMap<String, Token.TokenTypes> punchuation = new HashMap<String, Token.TokenTypes>();
    private HashMap<String,Token.TokenTypes> keywords = new HashMap<String, Token.TokenTypes>();
    //constructor
    public Lexer(String input) {
        this.text1 = new TextManager(input);


       // Key-Value pairs of all the punctuations.
        punchuation.put("+",Token.TokenTypes.PLUS);
        punchuation.put("-",Token.TokenTypes.MINUS);
        punchuation.put("*",Token.TokenTypes.TIMES);
        punchuation.put("/", Token.TokenTypes.DIVIDE);
        punchuation.put("%",Token.TokenTypes.MODULO);
        punchuation.put(",",Token.TokenTypes.COMMA);

        punchuation.put(".",Token.TokenTypes.DOT);
        punchuation.put(":",Token.TokenTypes.COLON);
        punchuation.put("(",Token.TokenTypes.LPAREN);
        punchuation.put(")",Token.TokenTypes.RPAREN);

        punchuation.put("=",Token.TokenTypes.ASSIGN);

        punchuation.put("==",Token.TokenTypes.EQUAL);
        punchuation.put("!=",Token.TokenTypes.NOTEQUAL);

        punchuation.put("<",Token.TokenTypes.LESSTHAN);
        punchuation.put("<=",Token.TokenTypes.LESSTHANEQUAL);

        punchuation.put(">",Token.TokenTypes.GREATERTHAN);
        punchuation.put(">=",Token.TokenTypes.GREATERTHANEQUAL);

        // keywords
        keywords.put("accessor",Token.TokenTypes.ACCESSOR);
        keywords.put("mutator",Token.TokenTypes.MUTATOR);
        keywords.put("implements",Token.TokenTypes.IMPLEMENTS);
        keywords.put("class",Token.TokenTypes.CLASS);
        keywords.put("interface",Token.TokenTypes.INTERFACE);
        keywords.put("loop",Token.TokenTypes.LOOP);
        keywords.put("if",Token.TokenTypes.IF);
        keywords.put("else",Token.TokenTypes.ELSE);


        keywords.put("true",Token.TokenTypes.TRUE);
        keywords.put("false",Token.TokenTypes.FALSE);
        keywords.put("new",Token.TokenTypes.NEW);

        keywords.put("private",Token.TokenTypes.PRIVATE);
        keywords.put("shared",Token.TokenTypes.SHARED);
        keywords.put("construct",Token.TokenTypes.CONSTRUCT);

        keywords.put("&&",Token.TokenTypes.AND);
        keywords.put("||",Token.TokenTypes.OR);
        keywords.put("!",Token.TokenTypes.NOT);

    }

    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();

        // if we are not at the end.
        while(!text1.isAtEnd()){
            char C = text1.getCharacter();
            char_position+=1;

            if(Character.isLetter(C)){
                retVal.add(parseWord(C));
            } else if (C == '\n') {
                //char_position += 1;
                //incrementing line
                line_position+=1;
                //character position back at 0 at the start of every newLine.
                char_position = 0;

                Token t_newline = new Token(Token.TokenTypes.NEWLINE,line_position,char_position);
                retVal.add(t_newline);

                int space_number = 1;

                //If I reach the end of the file
                if (text1.isAtEnd()){
                    while (indent_number>0){
                        retVal.add(new Token(Token.TokenTypes.DEDENT,line_position,char_position));
                        indent_number--;
                    }
                }//if I found indentation after new line instead of it being end of file.
                else if (!text1.isAtEnd() && text1.peekCharacter(0) == ' '){
                    C = text1.peekCharacter(0);
                    retVal.addAll(parseIndentation(C,space_number));
                }//if I found something other than space after new line, that means it's written at indentation level 0.
                else if ((!text1.isAtEnd() && text1.peekCharacter(0) != ' ') && text1.peekCharacter(0)!='\n') {
                    if (text1.peekCharacter(0) == '\t') {
                        space_number = 4;
                        C = text1.peekCharacter(0);
                        retVal.addAll(parseIndentation(C,space_number));
                        //char_position+=4;
                        //retVal.add(new Token(Token.TokenTypes.INDENT,line_position,char_position));
                        //indent_number++;
                    } else {
                        while (indent_number>0){
                            retVal.add(new Token(Token.TokenTypes.DEDENT,line_position,char_position));
                            indent_number--;
                        }
                    }

                }
            }else if(Character.isDigit(C)){
                retVal.add(parseNumber(C));
            }else if(C =='.'){
                char next = text1.peekCharacter(0);
                if (Character.isDigit(next)) {
                    retVal.add(parseNumber(C));
                } else {
                    // for period !!!.
                    retVal.add(parsePunctuation(C));
                }
           } //else if(punchuation.containsKey(Character.toString(C))){
//                retVal.add(parsePunctuation(C));
//
//            } //  && and ||
            else if (Character.toString(C).equals("&")) {
                if (text1.peekCharacter(0)=='&'){
                    String Andpun = Character.toString(C) + Character.toString(text1.getCharacter());
                    char_position++;
                    retVal.add(new Token(Token.TokenTypes.AND,line_position,char_position));
                }
            }

            else if (Character.toString(C).equals("|")) {
                if (text1.peekCharacter(0)=='|'){
                    String Andpun = Character.toString(C) + Character.toString(text1.getCharacter());
                    char_position++;
                    retVal.add(new Token(Token.TokenTypes.OR,line_position,char_position));
                }
            }


            // getting punctuations
            else if (Character.toString(C).equals("!")) {
                if (!text1.isAtEnd()){
                    if (text1.peekCharacter(0) == '='){
                        retVal.add(parsePunctuation(C));
                    }else{
                        retVal.add(new Token(keywords.get(Character.toString(C)), line_position,char_position));
                    }
                }else{
                    retVal.add(new Token(keywords.get(Character.toString(C)), line_position,char_position));
                }
            } else if (Character.toString(C).equals(">")) {
                if (text1.peekCharacter(0) == '='){
                    retVal.add(parsePunctuation(C));
                }else{
                    retVal.add(new Token(punchuation.get(Character.toString(C)), line_position,char_position));
                }
            }

            else if (Character.toString(C).equals("<")) {
                if (text1.peekCharacter(0) == '='){
                    retVal.add(parsePunctuation(C));
                }else{
                    retVal.add(new Token(punchuation.get(Character.toString(C)), line_position,char_position));
                }
            }
            else if (punchuation.containsKey(Character.toString(C))) {
                if(!Character.toString(C).equals("=")){
                    retVal.add(new Token(punchuation.get(Character.toString(C)), line_position,char_position));
                }else {
                    retVal.add(parsePunctuation(C));
                }

            }



            else if (C == '"') {
                //retVal.add(parseWord(C));

                retVal.add(parseQuotedStrings(C));
            } else if (C == '\'') {
                char nextchar = text1.getCharacter();
                char_position += 1;

                if (text1.peekCharacter(0)!= '\''){
                    SyntaxErrorException err = new SyntaxErrorException("Use single quotes to end the single quote.",line_position,char_position);
                    System.out.println(err);
                    throw err;
                }else if(text1.peekCharacter(0) == '\'') {
                    C = text1.getCharacter();
                    char_position += 1;

                    retVal.add(new Token(Token.TokenTypes.QUOTEDCHARACTER,line_position,char_position,Character.toString(nextchar)));
                }
            } else if (C == '{') {
                parseComments(C);
            } else if(C == '}'){
                SyntaxErrorException err = new SyntaxErrorException("You're Closing a comment that you never started !.",line_position,char_position);
                System.out.println(err);
                throw err;
            }

        }
        while (indent_number>0){
            retVal.add(new Token(Token.TokenTypes.DEDENT,line_position,char_position));
            indent_number--;
        }

        return retVal;
    }

    private Token parseWord(char C) throws Exception{

        String CurrentWord = Character.toString(C);

        // while not at the end of document.
        while (!text1.isAtEnd()){
            C = text1.peekCharacter();
            // if C is NOT a letter
            if(!Character.isLetter(C)){

                // if CurrentWord is not empty.
                if(!CurrentWord.isEmpty()){

                    //Create a new Token using CurrentWord
                    //Add Token to ListOfTokens
                    Token t1;

                    // checking if we have keyword or a word.
                    if (keywords.containsKey(CurrentWord)){
                        t1 = new Token(keywords.get(CurrentWord),line_position,char_position);
                    }else {
                        t1 = new Token(Token.TokenTypes.WORD,line_position,char_position,CurrentWord);
                    }

                    return t1;
                }
                CurrentWord = "";

            }else{
                    CurrentWord += C;
                    C = text1.getCharacter();
                    char_position+=1;
            }
        }
        Token t;
        if(C == '"') {
            t = new Token(Token.TokenTypes.QUOTEDSTRING,line_position,char_position,CurrentWord);
        }else if (keywords.containsKey(CurrentWord)){
            t = new Token(keywords.get(CurrentWord),line_position,char_position);
        }else {
            t = new Token(Token.TokenTypes.WORD,line_position,char_position,CurrentWord);
        }
        return t;
    }

    private Token parseQuotedStrings(char C){

        String CurrentWord = "";
//String.valueOf(text1.peekCharacter())

        // while not at the end of document.
        while (!text1.isAtEnd()){
            C = text1.peekCharacter();


                // if CurrentWord is not double quotes
                if(C == '"'){

                    //Create a new Token using CurrentWord
                    //Add Token to ListOfTokens
                    Token t = null;

                        // checking if the quoted part is a char or a string.
                        if (CurrentWord.length() == 1){
                            t = new Token(Token.TokenTypes.QUOTEDCHARACTER,line_position,char_position,CurrentWord);
                            C = text1.getCharacter();
                            char_position+=1;
                        }else{
                            t = new Token(Token.TokenTypes.QUOTEDSTRING,line_position,char_position,CurrentWord);
                            C = text1.getCharacter();
                            char_position+=1;
                        }

                    return t;
                }//CurrentWord = "";
                else{

                    //handling anything other than letter between quotes
                    if(C == '\n'){
                        CurrentWord += C;
                        C = text1.getCharacter();
                        line_position+=1;
                        char_position = 0;
                    }else{
                        CurrentWord += C;
                        C = text1.getCharacter();
                        char_position+=1;
                    }
                }
            } //end of function

        Token t2=null;
        if(C == '"') {
            t2 = new Token(Token.TokenTypes.QUOTEDSTRING,line_position,char_position,CurrentWord);
        }
        return t2;
    }

    private Token parseNumber(char C){
        String CurrentNumber = Character.toString(C);
        boolean foundDot = false;

        while(!text1.isAtEnd()){
            C = text1.peekCharacter();

            if(!Character.isDigit(C) && C != '.'){

                if(!CurrentNumber.isEmpty()){
                    //Create a new Token using CurrentWord
                    //Add Token to ListOfTokens
                    return new Token(Token.TokenTypes.NUMBER,line_position,char_position,CurrentNumber);
                }
                CurrentNumber = "";

            } else{
                if(C == '.' && !foundDot){
                    foundDot = true;
                    CurrentNumber += C;

                    //getting next number and incrementing the character position.
                    C = text1.getCharacter();
                    char_position += 1;

                } else if (Character.isDigit(C)) {
                    CurrentNumber += C;
                    C = text1.getCharacter();
                    char_position += 1;

                }else if(C == '.' && foundDot){
                     return new Token(Token.TokenTypes.NUMBER,line_position,char_position,CurrentNumber);
                }

            }
        }
        return new Token(Token.TokenTypes.NUMBER,line_position,char_position,CurrentNumber);
    }


    private Token parsePunctuation(char C){
        String CurrentWord = Character.toString(C);
        // specifically for ++ and --
        if(Character.toString(C).equals("+")){
            return new Token(punchuation.get(CurrentWord),line_position,char_position);
        }else if(Character.toString(C).equals("-")){
            return new Token(punchuation.get(CurrentWord),line_position,char_position);
        }

        while(!text1.isAtEnd()){
            C = text1.peekCharacter();

            if(!punchuation.containsKey(Character.toString(C))){
                if(!CurrentWord.isEmpty()){
                    return new Token(punchuation.get(CurrentWord),line_position,char_position);
                }
            }else {
                CurrentWord += C;
                C = text1.getCharacter();
                char_position += 1;
            }
        }

        return new Token(punchuation.get(CurrentWord),line_position,char_position);
    }

    // Parse Indentation to check the indentation levels.
    private List<Token> parseIndentation(char C, int space_number) throws Exception{
        var indentations = new LinkedList<Token>();
        // to count total indents in current line.
        int number_of_indent = 0;
        // for peeking positions ahead.
        int position_ahead = 1;
        // check for /t here
//        if (C == '\t'){
//            number_of_indent += 1;
//            space_number+=4;
//            //C = text1.peekCharacter(position_ahead);
//
//        }
        while (C == ' '){
            // 4 white spaces = 1 tab.
            if (space_number%4==0){
                number_of_indent += 1;
            }

            C = text1.peekCharacter(position_ahead);
            if(C == ' '){
                space_number+=1;
                position_ahead+=1;
            }

        }

        while (C == '\t'){

            // one \t is 4 spaces anyway.
            number_of_indent += 1;


            C = text1.peekCharacter(position_ahead);
            if(C == '\t'){
                space_number+=4;
                position_ahead+=1;
            }

        }


        //Token t = null;

        // if the number of spaces are 4,8,12 then add them, otherwise throw error.
        if(space_number%4!=0){
            SyntaxErrorException error =  new SyntaxErrorException("Incorrect number of spaces in indentation",line_position,space_number);
            System.out.print(error);
            throw error;

        }else{
            if(number_of_indent > indent_number){
                indent_number = number_of_indent;
                // character position at the number of spaces.
                char_position = space_number;

                indentations.add(new Token(Token.TokenTypes.INDENT, line_position, char_position));
            } else if (number_of_indent < indent_number) {
                int ind_diff = indent_number - number_of_indent;

                // adding all possible number of dedentations
                while (ind_diff>0){
                    // character position at the number of spaces.
                    char_position = space_number;

                    indentations.add(new Token(Token.TokenTypes.DEDENT, line_position, char_position));
                    ind_diff-=1;
                }
                //setting new level of the indentation.
                indent_number = number_of_indent;

            }
        }


        return indentations;
    }

    // parsing comments to ignore them.
    private void parseComments(char C) throws Exception{
        int left_braces = 1;
        int right_braces = 0;

        while (left_braces > right_braces){
           C = text1.getCharacter();
           char_position += 1;
           if (C == '{'){
               left_braces+=1;
           } else if (C == '}'){
               right_braces+=1;
           } else if (C == '\n'){

               //every new line, line number increases but character position should be back to 0.
               line_position+=1;
               char_position = 0;
           } else if (text1.isAtEnd()) {
               SyntaxErrorException error = new SyntaxErrorException("You Forgot to end your comment",line_position,char_position);
               System.out.print(error);
               throw error;
           }
        }
    }

}