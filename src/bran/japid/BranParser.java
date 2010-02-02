package bran.japid;

import java.util.Stack;

/**
 * Template parser
 */
public class BranParser {

    private String pageSource;

    public BranParser(String pageSource) {
        this.pageSource = pageSource;
        this.len = pageSource.length();
    }
    // bran keep track of nested state tokens, eg. nested function calls in expressions 
    // what inside is not used for now, we only are interested in the depth
    Stack<BranParser.Token> emthodCallStackInExpr = new Stack<BranParser.Token>();

    //
    public enum Token {

        EOF, //
        PLAIN, //
        SCRIPT, // %{...}% or {%...%} bran: or ~{}~, the open wings directives
        EXPR, // ${...}
        START_TAG, // #{...}
        END_TAG, // #{/...}
        MESSAGE, // &{...}
        ACTION, // @{...}
        ABS_ACTION, // @@{...}
        COMMENT, // *{...}*
        // bran expression without using {}, such as ~_;
        EXPR_WING, // ~{...}
        EXPR_NATURAL, // ~xxx or $xxx
        EXPR_NATURAL_METHOD_CALL, // bran  function call in expression: ~user?.name.format( '###' ) 
        EXPR_NATURAL_ARRAY_OP, // bran : ~myarray[-1].val 
        EXPR_NATURAL_STRING_LITERAL, // bran ~user?.name.format( '#)#' ) or $'hello'.length 
        TEMPLATE_ARGS, // bran ~( )
    }


    // end2/begin2 for mark the current returned token while the begin is the start pos of next token
    private int end, begin, end2, begin2, len;
    private BranParser.Token state = Token.PLAIN;

    private BranParser.Token found(BranParser.Token newState, int skip) {
        begin2 = begin;
        end2 = --end;
        begin = end += skip;
        BranParser.Token lastState = state == Token.EXPR_NATURAL ? Token.EXPR : state;
        state = newState;
        return lastState;
    }

    private void skip(int skip) {
    	end2 = --end;
    	end += skip;
    }

    public Integer getLine() {
        String token = pageSource.substring(0, begin2);
        if (token.indexOf("\n") == -1) {
            return 1;
        } else {
            return token.split("\n").length;
        }
    }

    public String getToken() {
        return pageSource.substring(begin2, end2);
    }

    public String checkNext() {
        if (end2 < pageSource.length()) {
            return pageSource.charAt(end2) + "";
        }
        return "";
    }

    public BranParser.Token nextToken() {
        for (;;) {

            int left = len - end;
            if (left == 0) {
                end++;
                return found(Token.EOF, 0);
            }

            char c = pageSource.charAt(end++);
            char c1 = left > 1 ? pageSource.charAt(end) : 0;
            char c2 = left > 2 ? pageSource.charAt(end + 1) : 0;

            switch (state) {
                case PLAIN:
                    if (c == '%' && c1 == '{') {
                        return found(Token.SCRIPT, 2);
                    }
                    if (c == '{' && c1 == '%') {
                        return found(Token.SCRIPT, 2);
                    }
                    // bran open wings
                    if (c == '~' && c1 == '{') {
                    	return found(Token.SCRIPT, 2);
                    }
                    if (c == '$' && c1 == '{') {
                        return found(Token.EXPR, 2);
                    }
                    if (c == '~' && c1 == '{') {
                    	return found(Token.EXPR, 2);
                    }
                    if (c == '~' && c1 == '(') {
                    	return found(Token.TEMPLATE_ARGS, 2);
                    }
                    // bran: shell like expression: ~_, ~user.name (this one is diff from sh, which requires  ${user.name}
                    // 
                    if (c == '~' && c1 != '~'  && (Character.isJavaIdentifierStart(c1) || '\'' == c1)) {
                    	return found(Token.EXPR_NATURAL, 1);
                    }
                    if (c == '$' && c1 != '$'  && (Character.isJavaIdentifierStart(c1) || '\'' == c1)) {
                    	return found(Token.EXPR_NATURAL, 1);
                    }
                    if (c == '#' && c1 == '{' && c2 == '/') {
                        return found(Token.END_TAG, 3);
                    }
                    if (c == '#' && c1 == '{') {
                        return found(Token.START_TAG, 2);
                    }
                    if (c == '&' && c1 == '{') {
                        return found(Token.MESSAGE, 2);
                    }
                    if (c == '@' && c1 == '@' && c2 == '{') {
                        return found(Token.ABS_ACTION, 3);
                    }
                    if (c == '@' && c1 == '{') {
                        return found(Token.ACTION, 2);
                    }
                    if (c == '*' && c1 == '{') {
                        return found(Token.COMMENT, 2);
                    }
                    break;
                case SCRIPT:
                    if (c == '}' && c1 == '%') {
                        return found(Token.PLAIN, 2);
                    }
                    if (c == '%' && c1 == '}') {
                        return found(Token.PLAIN, 2);
                    }
                    // bran
                    if (c == '}' && c1 == '~') {
                    	return found(Token.PLAIN, 2);
                    }
                    break;
                case COMMENT:
                    if (c == '}' && c1 == '*') {
                        return found(Token.PLAIN, 2);
                    }
                    break;
                case START_TAG:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    if (c == '/' && c1 == '}') {
                        return found(Token.END_TAG, 1);
                    }
                    break;
                case END_TAG:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    break;
                case EXPR:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    break;
                case TEMPLATE_ARGS:
                	if (c == ')') {
                		return found(Token.PLAIN, 1);
                	}
                	break;
                // bran
                // special characters considered an expression: '?.()
                // break characters: space, other punctuations, new lines, returns
                case EXPR_NATURAL:
            		if ('(' == c) {
            			skipAhread(Token.EXPR_NATURAL_METHOD_CALL, 1);
            		}
            		else if ('[' == c) {
            			skipAhread(Token.EXPR_NATURAL_ARRAY_OP, 1);
            		}
            		else if ('\'' == c) {
            			// start of literal
            			skipAhread(Token.EXPR_NATURAL_STRING_LITERAL, 1);
            		}
            		else if (Character.isWhitespace(c)) {
            			state = Token.EXPR;
            			return found(Token.PLAIN, 0); // it ea
            		}
            		else if (!Character.isJavaIdentifierPart(c) && c != '?' && c != '.' && c != ':' && c != '=') {
            			state = Token.EXPR;
            			return found(Token.PLAIN, 0); // it ea
            		}
                	break;
                case EXPR_NATURAL_METHOD_CALL:
            		if ('(' == c) {
            			// nested call
            			skipAhread(Token.EXPR_NATURAL_METHOD_CALL, 1);
            		}
            		else if (')' == c) {
            			state = this.emthodCallStackInExpr.pop();
            			skip(1);
            		}
                	break;
                case EXPR_NATURAL_ARRAY_OP:
                	if ('[' == c) {
                		// nested call
                		skipAhread(Token.EXPR_NATURAL_ARRAY_OP, 1);
                	}
                	else if (']' == c) {
                		state = this.emthodCallStackInExpr.pop();
                		skip(1);
                	}
                	break;
                case EXPR_NATURAL_STRING_LITERAL:
                	if ('\\' == c && '\'' == c1) {
                		// the escaped ' in a literal string
                		skip(2);
                	}
                	if ('\'' == c) {
                		// end of literal
                		state = this.emthodCallStackInExpr.pop();
                		skip(1);
                	}
                	break;
                case ACTION:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    break;
                case ABS_ACTION:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    break;
                case MESSAGE:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    break;
            }
        }
    }

    private void skipAhread(BranParser.Token exprNatural, int i) {
    	this.emthodCallStackInExpr.push(state);
    	state = exprNatural;
    	skip(i);
    }

	void reset() {
        end = begin = end2 = begin2 = 0;
        state = Token.PLAIN;
    }
}