import java.util.ArrayList;
import java.util.List;

public final class Quoting {

    public static List<String> parse(
            String arguments,
            Redirection redirection
    ) {
        List<String> result = new ArrayList<>();

        // 记录当前字符所处的引号、转义和重定向状态。
        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;
        boolean escapeNextCharacter = false;
        boolean readingOutputFile = false;

        // 暂存当前正在读取的命令参数或输出文件名。
        StringBuilder currentArgument = new StringBuilder();

        for (int i = 0; i < arguments.length(); i++) {
            char currentChar = arguments.charAt(i);

            // 前一个反斜杠会让当前字符失去特殊含义。
            if (escapeNextCharacter) {
                currentArgument.append(currentChar);
                escapeNextCharacter = false;
                continue;
            }

            if (currentChar == '\\') {
                if (insideSingleQuote) {
                    // 单引号内：反斜杠是普通字符
                    currentArgument.append(currentChar);
                    continue;
                } else if (insideDoubleQuote) {
                    // 双引号内：检查下一个字符
                    if (i + 1 < arguments.length()) {
                        char nextCharacter = arguments.charAt(i + 1);
                        if (nextCharacter == '\\'
                                || nextCharacter == '"'
                                || nextCharacter == '$'
                                || nextCharacter == '`') {
                            escapeNextCharacter = true;
                            continue;
                        }
                    }
                } else {
                    // 所有引号外：保护任意下一个字符
                    escapeNextCharacter = true;
                    continue;
                }
            }

            // 只有不在另一种引号中时，引号才会切换对应状态。
            if (currentChar == '\'' && !insideDoubleQuote) {
                insideSingleQuote = !insideSingleQuote;
                continue;
            }

            if (currentChar == '"' && !insideSingleQuote) {
                insideDoubleQuote = !insideDoubleQuote;
                continue;
            }

            // 引号外的空格表示当前参数读取完毕。
            if (currentChar == ' ' && !insideSingleQuote && !insideDoubleQuote) {
                if (!currentArgument.isEmpty()) {
                    if (readingOutputFile) {
                        redirection.setOutputFile(currentArgument.toString());
                        readingOutputFile = false;
                    } else {
                        result.add(currentArgument.toString());
                    }

                    currentArgument.setLength(0);
                }
                continue;
            }

            // 引号外的 > 开始输出重定向；1> 中的 1 不属于命令参数。
            if (currentChar == '>'
                    && !insideDoubleQuote
                    && !insideSingleQuote
                    && !readingOutputFile) {
                if (!currentArgument.isEmpty()) {
                    String currentValue = currentArgument.toString();
                    if (!currentValue.equals("1")) {
                        result.add(currentValue);
                    }
                    currentArgument.setLength(0);
                }
                readingOutputFile = true;
                redirection.setRedirectionRequired(true);
                continue;
            }

            currentArgument.append(currentChar);

        }

        // 输入结束后，保存最后一个尚未被空格提交的值。
        if (!currentArgument.isEmpty()) {
            if (readingOutputFile) {
                redirection.setOutputFile(currentArgument.toString());
            } else {
                result.add(currentArgument.toString());
            }
        }

        return result;
    }
}
