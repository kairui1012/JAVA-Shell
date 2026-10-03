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

        // Register commands that are handled directly by this shell.
        commands.put("exit", (arguments, outputStream, errorStream) -> exit());
        commands.put("echo", this::echo);
        commands.put("type", (arguments, outputStream, errorStream) -> type(arguments, commands, directories, outputStream));
        commands.put("pwd", (arguments, outputStream, errorStream) -> navigation.pwd(outputStream));
        commands.put("cd", (arguments, outputStream, errorStream) -> navigation.cd(arguments));

        // Start the completion list with all shell built-in command names.
        Set<String> strings = new HashSet<>(commands.keySet());

        // Add executable filenames found in every directory listed in PATH.
        for (String directory : directories) {
            File dir = new File(directory);
            File[] files = dir.listFiles();

            // Ignore PATH entries that cannot be read or are not directories.
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile() && file.canExecute()) {
                    strings.add(file.getName());
                }
            }
        }

        // Build tab-completion candidates from shell built-ins and executable files in PATH.
        StringsCompleter stringsCompleter = new StringsCompleter(strings);

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
            // Use the text currently entered by the user as the completion prefix.
            String currentInput = lineReader.getBuffer().toString();

            List<String> matches = new ArrayList<>();

            // Collect every built-in or executable whose name starts with the current input.
            for (String executable : strings) {
                if (executable.startsWith(currentInput)) {
                    matches.add(executable);
                }
            }

            // No match: ring the terminal bell and restart the TAB sequence.
            if (matches.isEmpty()) {
                terminal.writer().print("\u0007");
                terminal.writer().flush();

                tabCount[0] = 0;
                return true;
            }

            // One match: replace the input with the completed command and append a space.
            if (matches.size() == 1) {
                tabCount[0] = 0;
                lineReader.callWidget(LineReader.COMPLETE_WORD);
                return true;
            }

            // Multiple matches: the first TAB rings the bell as a prompt.
            if (tabCount[0] == 0) {
                terminal.writer().print("\u0007");
                terminal.writer().flush();

                tabCount[0] = 1;
            } else {
                // The second consecutive TAB prints all matches in alphabetical order.
                matches.sort(String::compareTo);

                String combineMatchesResult = String.join("  ", matches);

                terminal.writer().println();
                terminal.writer().println(combineMatchesResult);
                terminal.writer().flush();

                // Restore the prompt and the user's current input after printing the matches.
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
