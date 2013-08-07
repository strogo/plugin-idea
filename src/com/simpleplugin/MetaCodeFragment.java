package com.simpleplugin;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MetaCodeFragment {
    public List<String> parameters;
    public List<String> tokens;

    private char QUOTE = '\'';

    public MetaCodeFragment(List<String> params, List<String> tokens) {
        this.parameters = params;
        this.tokens = tokens;
    }

    public String getCode(List<String> params) {
        assert params.size() == parameters.size();
        ArrayList<String> newTokens = new ArrayList<String>();
        ArrayList<Integer> oldTokensCnt = new ArrayList<Integer>();

        for (int i = 0; i < tokens.size(); i++) {
            String tokenStr = transformedToken(params, tokens.get(i));
            if (tokenStr.equals("##") || tokenStr.equals("###")) {
                if (!newTokens.isEmpty() && i+1 < tokens.size()) {
                    String lastToken = newTokens.get(newTokens.size()-1);
                    String nextToken = transformedToken(params, tokens.get(i+1));
                    newTokens.set(newTokens.size()-1, concatTokens(lastToken, nextToken, tokenStr.equals("###")));
                    oldTokensCnt.set(oldTokensCnt.size()-1, oldTokensCnt.get(oldTokensCnt.size()-1) + 2);
                    ++i;
                }
            } else {
                newTokens.add(tokenStr);
                oldTokensCnt.add(1);
            }
        }

        return getTransformedCode(newTokens, tokens, oldTokensCnt);
    }

    private String getTransformedCode(ArrayList<String> newTokens, List<String> oldTokens, ArrayList<Integer> oldTokensCnt) {
        StringBuilder transformedCode = new StringBuilder();
        for (int i = 0; i < newTokens.size(); i++)
            transformedCode.append(newTokens.get(i));
        return transformedCode.toString();
    }

    private String transformedToken(List<String> actualParams, String token) {
        int index = parameters.indexOf(token);
        return index >= 0 ? actualParams.get(index) : token;
    }

    private String concatTokens(String t1, String t2, boolean toCapitalize) {
        if (t1.isEmpty() || t2.isEmpty()) {
            return t1 + capitalize(t2, toCapitalize && !t1.isEmpty());
        } else if (t1.charAt(0) == QUOTE || t2.charAt(0) == QUOTE) {
            return QUOTE + unquote(t1) + capitalize(unquote(t2), toCapitalize) + QUOTE;
        } else {
            return t1 + capitalize(t2, toCapitalize);
        }
    }

    private String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == QUOTE && s.charAt(s.length()-1) == QUOTE) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }

    private String capitalize(String s, boolean toCapitalize) {
        if (toCapitalize && s.length() > 0) {
            s = StringUtils.capitalize(s);
        }
        return s;
    }
}
