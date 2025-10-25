public class TextManager {
    private String input_string;
    private int position;


    public TextManager(String input) {
        this.input_string = input;
    }

    public boolean isAtEnd() {
        if (position >= input_string.length()) {
            return true;
        }
        return false;
    }

    public char peekCharacter() {
        return input_string.charAt(position);
    }

    public char peekCharacter(int distance) {
        return input_string.charAt(position + distance);
    }

    //this returns the character at “position” and
    //increments position.
    public char getCharacter() {

        return input_string.charAt(position++);

    }
}
