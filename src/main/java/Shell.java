import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class Shell {

    public void run() throws Exception {
        // Read PATH once so external commands and completion candidates use the same search order.
        String systemPath = System.getenv("PATH");
        String[] directories = systemPath.split(Pattern.quote(File.pathSeparator));

        // Create the services that implement the shell's built-in features.
        Navigation navigation = new Navigation();
        ProgrammableCompletion programmableCompletion = new ProgrammableCompletion();
        BackgroundJobs backgroundJobs = new BackgroundJobs();
        Pipelines pipelines = new Pipelines();
        History history = new History();
        org.jline.reader.History lineHistory = new DefaultHistory();

        // Keep the command registry and the shared state used by completion and background jobs.
        HashMap<String, CommandHandler> commands = new HashMap<>();
        HashMap<String, String> completerCommandsByTarget = new HashMap<>();
        HashMap<Integer, Process> backgroundJobsMap = new HashMap<>();
        HashMap<Integer, String> backgroundCommandsMap = new HashMap<>();
        HashMap<String, String> variablesMap = new HashMap<>();


        // Register commands that run inside this shell instead of starting an external process.
        commands.put("exit", (arguments, inputStream, outputStream, errorStream) ->
                exit(lineHistory)
        );
        commands.put("echo", this::echo);
        commands.put(
                "type",
                (arguments, inputStream, outputStream, errorStream) ->
                        type(arguments, commands, directories, outputStream)
        );
        commands.put(
                "pwd",
                (arguments, inputStream, outputStream, errorStream) ->
                        navigation.pwd(outputStream)
        );
        commands.put(
                "cd",
                (arguments, inputStream, outputStream, errorStream) ->
                        navigation.cd(arguments)
        );
        commands.put(
                "complete",
                (arguments, inputStream, outputStream, errorStream) ->
                        programmableCompletion.complete(
                                arguments,
                                outputStream,
                                errorStream,
                                completerCommandsByTarget
                        )
        );
        commands.put(
                "jobs",
                (arguments, inputStream, outputStream, errorStream) ->
                        backgroundJobs.jobs(
                                outputStream,
                                backgroundJobsMap,
                                backgroundCommandsMap
                        )
        );
        commands.put(
                "history",
                (arguments, inputStream, outputStream, errorStream) ->
                        history.history(
                                arguments,
                                outputStream,
                                errorStream,
                                lineHistory
                        )
        );
        commands.put(
                "declare",
                (arguments, inputStream, outputStream, errorStream) ->
                        ParameterExpansion.declare(
                                arguments,
                                outputStream,
                                errorStream,
                                variablesMap
                        )
        );


        CommandCompletion commandCompletion = new CommandCompletion(commands.keySet(), directories);
        FileCompletion fileCompletion = new FileCompletion();

        // Build completion helpers from the registered built-ins and executable files in PATH.
        StringsCompleter stringsCompleter = commandCompletion.createCompleter();

        // Preserve backslashes so Quoting can apply the shell's own escaping rules.
        DefaultParser parser = new DefaultParser();
        parser.setEscapeChars(null);

        // Configure JLine with parsing, completion, and the shared command history.
        LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
                .parser(parser)
                .completer(stringsCompleter)
                .history(lineHistory);
        Terminal terminal = TerminalBuilder.terminal();
        LineReader lineReader = lineReaderBuilder.terminal(terminal).build();

        lineReader.option(
                LineReader.Option.HISTORY_IGNORE_DUPS,
                false
        );

        String histFile = System.getenv("HISTFILE");

        if (histFile != null && !histFile.isBlank()) {
            Path path = Path.of(histFile);
            if (Files.isRegularFile(path)){
                try {
                    List<String> lines = Files.readAllLines(path);

                    for (String line : lines) {
                        lineHistory.add(line);
                    }

                }catch (IOException e) {
                    System.err.println("history: " + e.getMessage());
                }
            }
        }

        // Use an array so the widget lambda can update the consecutive TAB count.
        int[] tabCount = {0};

        // Replace JLine's default TAB action with the shell's custom completion widget.
        Map<String, Widget> widgets = lineReader.getWidgets();
        Map<String, KeyMap<Binding>> keyMaps = lineReader.getKeyMaps();

        widgets.put("my-tab", () -> {
            String currentInput = lineReader.getBuffer().toString();
            List<String> matches = new ArrayList<>();
            List<String> commandsWithCustomCompleters =
                    new ArrayList<>(completerCommandsByTarget.keySet());

            boolean isPathCompletion = currentInput.contains(" ");

            String commandPart = "";
            String pathInput = "";

            if (!isPathCompletion) {
                // Before the first space, complete built-ins and executable command names.
                matches.addAll(commandsWithCustomCompleters);
                matches.addAll(commandCompletion.findMatches(currentInput));
            } else {
                // After the first space, complete command arguments.
                if (completerCommandsByTarget.keySet().stream()
                        .anyMatch(targetCommand -> currentInput.startsWith(targetCommand + " "))) {

                    // Use the programmable completer registered for the target command.
                    for (String targetCommand : completerCommandsByTarget.keySet()) {
                        if (currentInput.startsWith(targetCommand + " ")) {
                            // Resolve the external command that provides completion candidates.
                            String completerCommand = completerCommandsByTarget.get(targetCommand);

                            // Derive the command, current word, and previous word for the completer.
                            String[] strings = currentInput.split(" ");
                            String commandName = strings[0];
                            String currentWord = "";
                            String previousWord = "";

                            if (currentInput.endsWith(" ")) {
                                if (strings.length > 1) {
                                    previousWord = strings[strings.length - 1];
                                }

                                commandPart = currentInput;
                            } else {
                                if (strings.length > 2) {
                                    previousWord = strings[strings.length - 2];
                                    currentWord = strings[strings.length - 1];
                                } else {
                                    currentWord = strings[1];
                                    previousWord = strings[0];
                                }

                                commandPart = currentInput.substring(
                                        0,
                                        currentInput.length() - currentWord.length()
                                );
                            }

                            ProcessBuilder completerProcessBuilder = new ProcessBuilder(
                                    completerCommand,
                                    commandName,
                                    currentWord,
                                    previousWord
                            );

                            // Provide the complete input line and byte cursor position to the completer.
                            Map<String, String> environment = completerProcessBuilder.environment();
                            environment.put("COMP_LINE", currentInput);
                            environment.put(
                                    "COMP_POINT",
                                    String.valueOf(
                                            currentInput.getBytes(StandardCharsets.UTF_8).length
                                    )
                            );

                            // Run the completer as a separate process.
                            Process completerProcess;
                            try {
                                completerProcess = completerProcessBuilder.start();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            // Wait until all completion candidates have been produced.
                            try {
                                completerProcess.waitFor();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

                            // Treat each line from standard output as one completion candidate.
                            BufferedReader completerOutputReader = new BufferedReader(
                                    new InputStreamReader(completerProcess.getInputStream())
                            );

                            try {
                                String completionCandidate;
                                while (
                                        (completionCandidate = completerOutputReader.readLine())
                                                != null
                                ) {
                                    matches.add(completionCandidate);
                                }
                            } catch (IOException e) {
                                terminal.writer().print("\u0007");
                                terminal.writer().flush();
                            }

                            pathInput = currentWord;
                        }
                    }
                } else {
                    // Commands without a programmable completer use file and directory completion.
                    FileCompletion.Result result = fileCompletion.findMatches(currentInput);
                    commandPart = result.commandPart();
                    pathInput = result.pathInput();
                    matches.addAll(result.matches());
                }
            }

            // Ring the terminal bell when no candidate matches the current input.
            if (matches.isEmpty()) {
                terminal.writer().print("\u0007");
                terminal.writer().flush();

                tabCount[0] = 0;
                return true;
            }

            // Insert a unique match immediately and add a trailing space for completed values.
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

            // Find the longest prefix shared by every remaining candidate.
            String lcp = matches.getFirst();

            for (String match : matches) {
                int i = 0;

                while (i < lcp.length()
                        && i < match.length()
                        && lcp.charAt(i) == match.charAt(i)) {
                    i++;
                }

                lcp = lcp.substring(0, i);
            }

            // Extend the current word when the shared prefix contains more characters.
            String currentCompletionInput;

            if (isPathCompletion) {
                currentCompletionInput = pathInput;
            } else {
                currentCompletionInput = currentInput;
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

            // If no extension is possible, ring once and show all candidates on the next TAB.
            if (tabCount[0] == 0) {
                terminal.writer().print("\u0007");
                terminal.writer().flush();

                tabCount[0] = 1;
            } else {
                matches.sort(String::compareTo);

                String combineMatchesResult = String.join("  ", matches);

                terminal.writer().println();
                terminal.writer().println(combineMatchesResult);
                terminal.writer().flush();

                lineReader.callWidget(LineReader.REDRAW_LINE);
                lineReader.callWidget(LineReader.REDISPLAY);

                tabCount[0] = 0;
            }

            return true;
        });

        // Bind the custom completion widget to TAB in JLine's main key map.
        KeyMap<Binding> mainKeyMap = keyMaps.get(LineReader.MAIN);
        Binding binding = new Reference("my-tab");
        mainKeyMap.bind(binding, "\t");

        // Read and execute commands until a built-in handler requests termination.
        while (true) {
            // Report completed background processes before displaying the next prompt.
            backgroundJobs.reapFinishedJobs(
                    System.out,
                    backgroundJobsMap,
                    backgroundCommandsMap
            );

            Redirection redirection = new Redirection();

            String command = lineReader.readLine("$ ");

            // A trailing ampersand requests background execution for an external command.
            boolean isBackground = command.trim().endsWith("&");

            // Pipelines manage their own parsing, processes, and stream connections.
            if (pipelines.isPipeline(command)) {
                pipelines.execute(
                        command,
                        commands,
                        System.out,
                        System.err
                );

                continue;
            }

            // Remove the background marker before tokenizing the command.
            if (isBackground) {
                command = command.trim();
                command = command.substring(0, command.length() - 1).trim();
            }

            // Tokenize the input while preserving quoted arguments and recording redirections.
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

            // Built-ins run in the shell process and receive redirected output streams directly.
            CommandHandler handler = commands.get(commandName);

            if (handler != null) {
                PrintStream outputStream = System.out;
                PrintStream errorStream = System.err;

                try {
                    outputStream = redirection.openOutputStream(System.out);
                    errorStream = redirection.openErrorStream(System.err);

                    if (!handler.execute(
                            parsedArgumentLine,
                            System.in,
                            outputStream,
                            errorStream
                    )) {
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

            // Resolve external commands by searching executable files in PATH order.
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
                // External commands inherit the terminal unless redirection overrides a stream.
                ProcessBuilder pb = new ProcessBuilder(processCommand);

                pb.inheritIO();
                redirection.applyTo(pb);

                Process process = pb.start();

                if (isBackground) {
                    // Track background processes under the lowest available positive job ID.
                    int jobId = 1;

                    while (backgroundJobsMap.containsKey(jobId)) {
                        jobId++;
                    }

                    backgroundJobsMap.put(jobId, process);
                    backgroundCommandsMap.put(jobId, command);

                    System.out.println("[" + jobId + "] " + process.pid());
                } else {
                    // Keep the prompt blocked until a foreground process exits.
                    process.waitFor();
                }
            }
        }
    }

    // Tell the main loop to terminate after the exit built-in runs.
    private boolean exit(org.jline.reader.History lineHistory) {

        String histFile = System.getenv("HISTFILE");

        if (histFile != null && !histFile.isBlank()) {

            StringBuilder content = new StringBuilder();

            for (org.jline.reader.History.Entry entry : lineHistory) {
                content.append(entry.line()).append("\n");
            }

            try {
                Files.writeString(Path.of(histFile), content.toString());
            } catch (IOException e) {
                System.err.println("history: " + e.getMessage());
            }
        }

        return false;
    }

    // Write the supplied arguments exactly once to the selected output stream.
    private boolean echo(
            String arguments,
            InputStream inputStream,
            PrintStream outputStream,
            PrintStream errorStream
    ) {
        outputStream.println(arguments);
        return true;
    }

    // Report whether a command is a shell built-in or an executable found through PATH.
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

                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
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
