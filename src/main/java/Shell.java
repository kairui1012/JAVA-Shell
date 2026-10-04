import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class Shell {

    @FunctionalInterface
    private interface CommandHandler {
        // Returns true to continue the shell or false to exit.
        boolean execute(String arguments, PrintStream outputStream, PrintStream errorStream);
    }

    public void run() throws Exception {

        // Split PATH into directories used to locate external programs.
        String systemPath = System.getenv("PATH");
        String[] directories = systemPath.split(
                Pattern.quote(File.pathSeparator)
        );

        HashMap<String, CommandHandler> commands = new HashMap<>();
        Navigation navigation = new Navigation();
        ProgrammableCompletion completion = new ProgrammableCompletion();

        // Register commands that are handled directly by this shell.
        commands.put("exit", (arguments, outputStream, errorStream) -> exit());
        commands.put("echo", this::echo);
        commands.put("type", (arguments, outputStream, errorStream) -> type(arguments, commands, directories, outputStream));
        commands.put("pwd", (arguments, outputStream, errorStream) -> navigation.pwd(outputStream));
        commands.put("cd", (arguments, outputStream, errorStream) -> navigation.cd(arguments));
        commands.put("complete", (arguments, outputStream, errorStream) -> completion.complete(arguments));


        CommandCompletion commandCompletion = new CommandCompletion(commands.keySet(), directories);
        FileCompletion fileCompletion = new FileCompletion();

        // Build tab-completion candidates from shell built-ins and executable files in PATH.
        StringsCompleter stringsCompleter = commandCompletion.createCompleter();

        // Keep backslashes in the input so the shell can apply its own escaping rules later.
        DefaultParser parser = new DefaultParser();
        parser.setEscapeChars(null);

        // Combine parsing and completion behavior, then attach the reader to the terminal.
        LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder().parser(parser).completer(stringsCompleter);
        Terminal terminal = TerminalBuilder.terminal();
        LineReader lineReader = lineReaderBuilder.terminal(terminal).build();

        // Store the consecutive TAB count in an array so it can be updated inside the widget lambda.
        int[] tabCount = {0};

        // Access JLine's widgets and key maps to register custom TAB behavior.
        Map<String, Widget> widgets = lineReader.getWidgets();
        Map<String, KeyMap<Binding>> keyMaps = lineReader.getKeyMaps();

        widgets.put("my-tab", () -> {

            String buffer = lineReader.getBuffer().toString();
            List<String> matches = new ArrayList<>();

            boolean isPathCompletion = buffer.contains(" ");

            String commandPart = "";
            String pathInput = "";

            if (!isPathCompletion) {

                // =====================
                // Command completion
                // =====================

                matches.addAll(commandCompletion.findMatches(buffer));

            } else {

                // =====================
                // File/path completion
                // =====================

                FileCompletion.Result result = fileCompletion.findMatches(buffer);
                commandPart = result.commandPart();
                pathInput = result.pathInput();
                matches.addAll(result.matches());
            }

            // =====================
            // No match
            // =====================

            if (matches.isEmpty()) {
                terminal.writer().print("\u0007");
                terminal.writer().flush();

                tabCount[0] = 0;
                return true;
            }

            // =====================
            // One match
            // =====================

            if (matches.size() == 1) {

                String match = matches.getFirst();

                lineReader.getBuffer().clear();

                if (isPathCompletion) {

                    if (match.endsWith("/")) {
                        lineReader.getBuffer().write(commandPart + match);
                    } else {
                        lineReader.getBuffer().write(commandPart + match + " ");
                    }

                } else {
                    lineReader.getBuffer().write(match + " ");
                }

                lineReader.callWidget(LineReader.REDRAW_LINE);
                lineReader.callWidget(LineReader.REDISPLAY);

                tabCount[0] = 0;
                return true;
            }

            // =====================
            // Multiple matches
            // Calculate Longest Common Prefix
            // =====================

            String lcp = matches.getFirst();

            for (String match : matches) {

                int i = 0;

                while (
                        i < lcp.length()
                                && i < match.length()
                                && lcp.charAt(i) == match.charAt(i)
                ) {
                    i++;
                }

                lcp = lcp.substring(0, i);
            }

            // =====================
            // Extend to LCP
            // =====================

            String currentCompletionInput;

            if (isPathCompletion) {
                currentCompletionInput = pathInput;
            } else {
                currentCompletionInput = buffer;
            }

            if (lcp.length() > currentCompletionInput.length()) {

                lineReader.getBuffer().clear();

                if (isPathCompletion) {
                    lineReader.getBuffer().write(commandPart + lcp);
                } else {
                    lineReader.getBuffer().write(lcp);
                }

                lineReader.callWidget(LineReader.REDRAW_LINE);
                lineReader.callWidget(LineReader.REDISPLAY);

                tabCount[0] = 0;
                return true;
            }

            // =====================
            // First TAB: bell
            // Second TAB: show all matches
            // =====================

            if (tabCount[0] == 0) {

                terminal.writer().print("\u0007");
                terminal.writer().flush();

                tabCount[0] = 1;

            } else {

                matches.sort(String::compareTo);

                String combineMatchesResult =
                        String.join("  ", matches);

                terminal.writer().println();
                terminal.writer().println(combineMatchesResult);
                terminal.writer().flush();

                lineReader.callWidget(LineReader.REDRAW_LINE);
                lineReader.callWidget(LineReader.REDISPLAY);

                tabCount[0] = 0;
            }

            return true;
        });

        // Bind the custom widget to the TAB key in JLine's main key map.
        KeyMap<Binding> mainKeyMap = keyMaps.get(LineReader.MAIN);
        Binding binding = new Reference("my-tab");
        mainKeyMap.bind(binding, "\t");


        //        lineReader.printAbove
        //        ("""
        //           \s
        //            ╦╔═╗╦  ╦╔═╗  ╔═╗╦ ╦╔═╗╦  ╦
        //            ║╠═╣╚╗╔╝╠═╣  ╚═╗╠═╣║╣ ║  ║
        //           ╚╝╩ ╩ ╚╝ ╩ ╩  ╚═╝╩ ╩╚═╝╩═╝╩═╝
        //
        //           Java Shell
        //           Built from scratch by Sam
        //
        //           GitHub: github.com/kairui1012
        //          \s
        //       \s""");


        while (true) {

            Redirection redirection = new Redirection();

            String command = lineReader.readLine("$ ");

            // Split the input while preserving quoted arguments.
            List<String> parsedCommand = Quoting.parse(command, redirection);

            if (parsedCommand.isEmpty()) {
                continue;
            }

            String commandName = parsedCommand.getFirst();

            List<String> parsedArguments = parsedCommand.subList(
                    1,
                    parsedCommand.size()
            );

            String parsedArgumentLine = String.join(" ", parsedArguments);

            List<String> processCommand = new ArrayList<>();


            // A handler returns true to continue the shell and false to exit.
            CommandHandler handler = commands.get(commandName);


            if (handler != null) {
                PrintStream outputStream = System.out;
                PrintStream errorStream = System.err;
                try {
                    outputStream = redirection.openOutputStream(System.out);
                    errorStream = redirection.openErrorStream(System.err);

                    if (!handler.execute(parsedArgumentLine, outputStream, errorStream)) {
                        break;
                    }

                } finally {

                    if (outputStream != System.out) {
                        outputStream.close();
                    }

                    if (errorStream != System.err) {
                        errorStream.close();
                    }
                }

                continue;
            }
            // Search each PATH directory for an executable with this name.
            for (String directory : directories) {
                Path candidate = Path.of(directory, commandName);

                if (Files.isRegularFile(candidate)
                        && Files.isExecutable(candidate)) {
                    processCommand.add(commandName);
                    processCommand.addAll(parsedArguments);
                    break;
                }
            }

            if (processCommand.isEmpty()) {
                System.out.println(commandName + ": command not found");
            } else {
                ProcessBuilder pb = new ProcessBuilder(processCommand);
                // Connect the child process to this shell's input and output.
                pb.inheritIO();
                redirection.applyTo(pb);

                Process process = pb.start();
                process.waitFor();
            }
        }
    }



    private boolean exit() {
        return false;
    }

    private boolean echo(String arguments, PrintStream outputStream,PrintStream errorStream ) {
        outputStream.println(arguments);
        return true;
    }

    private boolean type(
            String arguments,
            HashMap<String, CommandHandler> commands,
            String[] directories,
            PrintStream outputStream
    ) {
        String target = arguments.trim();

        // A command can either be a shell built-in or an external executable.
        if (commands.containsKey(target)) {
            outputStream.println(target + " is a shell builtin");
        } else {
            boolean found = false;

            for (String directory : directories) {
                Path candidate = Path.of(directory, target);

                if (Files.isRegularFile(candidate)
                        && Files.isExecutable(candidate)) {
                    outputStream.println(target + " is " + candidate);
                    found = true;
                    break;
                }
            }

            if (!found) {
                outputStream.println(target + ": not found");
            }
        }

        return true;
    }

}
