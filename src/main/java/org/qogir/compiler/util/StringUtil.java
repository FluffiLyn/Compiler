package org.qogir.compiler.util;

/**
 * A String object tool
 * @author xuyang
 */

public class StringUtil {

    public StringUtil(){

    }

    /**
     * Remove all space in strings.
     * @param string to be trimmed
     * @return trimmed string
     * @author xuyang
     */
    public String trim(String string){
        if(string == null)
            return null;
        StringBuilder trimmedString = new StringBuilder();
        for(char ch : string.toCharArray()){
            if(ch != ' '){
                trimmedString.append(ch);
            }
        }
        return trimmedString.toString();
    }

    /**
     * Check if the char is an English letter.
     *
     * @param c a char
     * @return true if c is a letter, false otherwise
     */
    public static Boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
