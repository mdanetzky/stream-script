package com.mdanetzky.streamscript.parser;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.ScriptParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ScriptParser {

    static final String TOKEN_START = "{";
    static final String TOKEN_END = "}";
    private static final String TOKEN_END_REGEX = "}";
    private static final String TOKEN_START_REGEX = "\\{";
    private static final String TOKEN_CLOSING = "/";
    private static final String TOKEN_END_WITH_CLOSING = TOKEN_CLOSING + TOKEN_END;
    private static final String TOKEN_START_WITH_CLOSING = TOKEN_START + TOKEN_CLOSING;

    public static void verify(String script) {
        parse(script);
    }

    public static Source<ByteString, NotUsed> run(String script, Map<String, Object> context) {
        return parse(script).toStream(io.vavr.collection.HashMap.ofAll(context));
    }

    static CommandContainer parse(String script) {
        ScriptElement rootElement = parseToElements(script);
        return CommandParser.parseScript(rootElement);
    }

    static ScriptElement parseToElements(String script) {
        Script scriptObject = new Script();
        return scriptObject.parse(script);
    }

    private static class Script {

        private final Stack<ScriptElement> elementsStack = new Stack<>();
        private ScriptElement currentElement = new ScriptElement();

        private static String markAsCommand(String token) {
            return TOKEN_START + token + TOKEN_END;
        }

        private List<String> tokenize(String script) {
            String[] betweenOpenings = script.split(TOKEN_START_REGEX);
            List<String> tokens = new ArrayList<>();
            for (String firstPart : betweenOpenings) {
                String[] betweenClosings = firstPart.split(TOKEN_END_REGEX);
                if (betweenClosings.length == 1) {
                    String token = (firstPart.endsWith(TOKEN_END))
                            ? markAsCommand(betweenClosings[0])
                            : betweenClosings[0];
                    addIfNotEmpty(tokens, token);
                } else {
                    addIfNotEmpty(tokens, markAsCommand(betweenClosings[0]));
                    addIfNotEmpty(tokens, betweenClosings[1]);
                }
            }
            return tokens;
        }

        private void addIfNotEmpty(List<String> list, String string) {
            if (!string.isEmpty()) {
                list.add(string);
            }
        }

        private ScriptElement parse(String script) {
            List<String> tokens = tokenize(script);
            elementsStack.push(currentElement);
            tokens.forEach(this::parseToken);
            return currentElement;
        }

        private void parseToken(String token) {
            if (token.startsWith(TOKEN_START_WITH_CLOSING)) {
                // end of surrounding tag
                String commandOpening = cleanupCommand(token).substring(0, token.length() - TOKEN_END.length() - 1);
                String currentCommand = currentElement.getContent();
                if (!currentCommand.startsWith(commandOpening)) {
                    throw new ScriptParserException(
                            "Closing tag:\"" + token + "\" does not match opening tag:\"" + currentCommand + "\""
                    );
                }
                currentElement = elementsStack.pop();
            } else if (token.startsWith(TOKEN_START) && token.endsWith(TOKEN_END_WITH_CLOSING)) {
                // standalone tag
                ScriptElement element = new ScriptElement();
                element.setContent(cleanupCommand(token));
                currentElement.getElements().add(element);
            } else if (token.startsWith(TOKEN_START)) {
                // beginning of surrounding tag
                ScriptElement element = new ScriptElement();
                element.setContent(cleanupCommand(token));
                currentElement.getElements().add(element);
                elementsStack.push(currentElement);
                currentElement = element;
            } else {
                // string
                if (!token.isEmpty()) {
                    ScriptElement element = new ScriptElement();
                    element.setContent(token);
                    currentElement.getElements().add(element);
                }
            }
        }

        private String cleanupCommand(String token) {
            if (token.startsWith(TOKEN_START_WITH_CLOSING)) {
                return TOKEN_START + token.substring(TOKEN_START_WITH_CLOSING.length());
            }
            if (token.endsWith(TOKEN_END_WITH_CLOSING)) {
                return token.substring(0, token.length() - TOKEN_START_WITH_CLOSING.length()) + TOKEN_END;
            }
            return token;
        }
    }
}
