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
        boolean readingRedirectFile = false;
        boolean readingErrorFile = false;

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
                    if (readingErrorFile) {
                        redirection.setErrorFile(currentArgument.toString());
                        readingErrorFile = false;
                    } else if (readingRedirectFile) {
                        redirection.setOutputFile(currentArgument.toString());
                        readingRedirectFile = false;
                    } else {
                        result.add(currentArgument.toString());
                    }

                    currentArgument.setLength(0);
                }
                continue;
            }

            // Detect the redirection operator '>'.
            // It is only an operator outside quotes and while no file name is being read.

            if (currentChar == '>'
                    && !insideDoubleQuote
                    && !insideSingleQuote
                    && !readingRedirectFile
                    && !readingErrorFile) {

                // currentArgument contains the characters immediately before '>'.
                // "sam > output.txt"  -> it is empty because "sam" was already saved.
                // "sam 1> output.txt" -> it contains "1" for stdout.
                // "sam 2> error.txt"  -> it contains "2" for stderr.
                String currentValue = currentArgument.toString();

                boolean append = i + 1 < arguments.length()
                        && arguments.charAt(i + 1) == '>';

                if (append) {
                    i++; // skip second '>'
                }

                if (currentValue.equals("2")) {
                    // "2>" redirects stderr. The "2" is a file descriptor,
                    // so it must not be added to the command arguments.
                    readingErrorFile = true;
                    redirection.startErrorRedirection(append);
                } else {
                    // "1>" and plain ">" redirect stdout. Only a value other
                    // than the stdout file descriptor is a normal argument.
                    if (!currentValue.isEmpty() && !currentValue.equals("1")) {
                        result.add(currentValue);
                    }

                    readingRedirectFile = true;
                    redirection.startOutputRedirection(append);
                }

                // Clear the argument buffer before reading the file name.
                currentArgument.setLength(0);

                // Skip the remaining parsing logic for the '>' character.
                continue;
            }

            currentArgument.append(currentChar);

        }

        // 输入结束后，保存最后一个尚未被空格提交的值。
        if (!currentArgument.isEmpty()) {
            if (readingErrorFile) {
                // Store the actual file name that followed "2>".
                redirection.setErrorFile(currentArgument.toString());
            } else if (readingRedirectFile) {
                redirection.setOutputFile(currentArgument.toString());
            } else {
                result.add(currentArgument.toString());
            }
        }

        return result;
    }
}
