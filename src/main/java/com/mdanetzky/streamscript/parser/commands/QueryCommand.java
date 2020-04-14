package com.mdanetzky.streamscript.parser.commands;

import akka.NotUsed;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickRow;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.ScriptExecutorException;
import com.mdanetzky.streamscript.ScriptParserException;
import com.mdanetzky.streamscript.parser.CommandUtil;
import com.mdanetzky.streamscript.parser.Context;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;
import com.mdanetzky.streamscript.parser.annotations.StreamSourceFromChildren;
import io.vavr.collection.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Command("query")
public class QueryCommand {

    public static final String SLICK_SESSION = "slick.session";
    private static final String VARIABLE_DELIMITER = ":";
    private static final String VARIABLE_TYPE_DELIMITER_REGEX = "\\|";
    private static final String QUERY_END_MARKER = ";";
    private static final String QUERY_PARAM_START_MARKER = "(:";
    private static final String QUERY_PARAM_END_MARKER = ")";
    private static final Pattern PARAMETER_REGEX = Pattern.compile("\\(:([A-Za-z0-9]*)\\)");
    private final List<String> queryParameters = new ArrayList<>();
    private String query;
    private List<Pair<String, DataType>> variableMapping;
    private Function<Map<String, Object>, Source<ByteString, NotUsed>> childrenSource;

    @CommandParameters
    public void setCode(String code) {
        String[] commandTokens = code.split(QUERY_END_MARKER);
        query = commandTokens[0].trim();
        try {
            variableMapping = parseVariableMapping(commandTokens[1]);
            findQueryParameters();
        } catch (ScriptParserException e) {
            String message = "Error parsing query :'" + code + "'\n";
            throw new ScriptParserException(message + e.getMessage());
        }
    }

    private void findQueryParameters() {
        Matcher m = PARAMETER_REGEX.matcher(query);
        while (m.find()) {
            queryParameters.add(m.group(1));
        }
    }

    private List<Pair<String, DataType>> parseVariableMapping(String variableMapping) {
        List<Pair<String, DataType>> parsedVariableMapping = new ArrayList<>();
        String[] variables = variableMapping.split(VARIABLE_DELIMITER);
        for (String variableDef : variables) {
            String[] varDef = variableDef.split(VARIABLE_TYPE_DELIMITER_REGEX);
            if (varDef.length != 2) {
                throw new ScriptParserException("Parsing script: wrong variable mapping: " + variableMapping);
            }
            Pair<String, DataType> mapping = new Pair<>(varDef[0].trim(),
                    parseDataType(varDef[1].toLowerCase().trim()));
            parsedVariableMapping.add(mapping);
        }
        return parsedVariableMapping;
    }

    private DataType parseDataType(String value) {
        try {
            return DataType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ScriptParserException("Parsing script: Unknown query result data type: '" + value + "'");
        }
    }

    private Map<String, Object> mapResult(SlickRow row, Map<String, Object> context) {
        for (Pair<String, DataType> mapping : variableMapping) {
            switch (mapping.getValue()) {
                case DATE:
                    context = context.put(mapping.getKey(), row.nextDate());
                    break;
                case STRING:
                    context = context.put(mapping.getKey(), row.nextString());
                    break;
                case INT:
                    context = context.put(mapping.getKey(), row.nextInt());
                    break;
                default:
                    throw new ScriptExecutorException("Mapping query row: Unknown datatype:'"
                            + mapping.getValue().type
                            + "' defined for query parameter:'"
                            + mapping.getKey() + "' (" + mapping.getKey() + "|" + mapping.getValue().type + ")");
            }
        }
        return context;
    }

    @StreamSourceFromChildren
    public void setChildrenSource(Function<Map<String, Object>, Source<ByteString, NotUsed>> childrenSource) {
        this.childrenSource = childrenSource;
    }

    @StreamSource
    public Source<ByteString, NotUsed> toStream(Map<String, Object> context) {
        SlickSession slickSession = (SlickSession) context.get(SLICK_SESSION).get();
        String query = createSql(context);
        return Slick.source(slickSession, query,
                (SlickRow row) -> childrenSource.apply(mapResult(row, context))
        ).flatMapConcat((source) -> source);
    }

    private String createSql(Map<String, Object> context) {
        String queryWithParamsFilled = query;
        for (String paramName : queryParameters) {
            String paramValue = Context.get(context, paramName);
            if (paramValue == null) {
                throw new ScriptExecutorException("For query: " + query
                        + "\n\nParameter: '" + paramName
                        + "' Not found in context:\n" + CommandUtil.mapToString(context)
                );
            }
            String paramField = QUERY_PARAM_START_MARKER + paramName + QUERY_PARAM_END_MARKER;
            queryWithParamsFilled = queryWithParamsFilled.replace(paramField, paramValue);
        }
        return queryWithParamsFilled;
    }

    @SuppressWarnings("unused")
    private enum DataType {
        STRING("string"),
        INT("int"),
        DATE("date");
        private final String type;

        DataType(String type) {
            this.type = type;
        }
    }

    private static class Pair<K, V> {

        private K key;
        private V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
