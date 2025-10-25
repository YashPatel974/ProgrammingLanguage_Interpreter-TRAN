import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TokenManager {
    List<Token> tokens_in = new LinkedList<>();
    int token_position = 0;

    public TokenManager(List<Token> tokens) {
        this.tokens_in = tokens;
    }

    public boolean done() {
        if (token_position >= tokens_in.size()) {
            return true;
        }
        return false;
    }

    //incrementing the token position in this function.
    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        //just checking if we're not at the end of list.
        if(token_position < tokens_in.size()) {

            if(tokens_in.get(token_position).getType() == t) {
                return Optional.of(tokens_in.get(token_position++));
            }
        }

        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        return Optional.of(tokens_in.get(token_position+i));
    }

    public Optional<Token> peek() {
        return Optional.of(tokens_in.get(token_position));
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){
        //if the token type of the next two position matches the token types in our list.
        if((tokens_in.get(token_position).getType() == first)  && (tokens_in.get(token_position+1).getType() == second)) {
            return true;
        }
        return false;
    }

    public int getCurrentLine(){
        return tokens_in.get(token_position).getLineNumber();
    }

    public int getCurrentColumnNumber(){
        return tokens_in.get(token_position).getColumnNumber();
    }
}