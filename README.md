# Java Shell

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![JLine](https://img.shields.io/badge/JLine-4.4.6-4B5563)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)

**A POSIX-inspired interactive shell built from scratch in Java.**

This project implements command dispatch, quote-aware parsing, PATH lookup,
stream redirection, pipelines, background jobs, command history, and a
Bash-inspired programmable completion protocol. JLine provides terminal input
and line editing, while the shell owns the parsing and execution behavior.

## Highlights

- **Interactive REPL** powered by JLine with editable input and command history
- **Built-in commands** for navigation, discovery, history, jobs, and completion
- **External command execution** through PATH lookup and `ProcessBuilder`
- **Quote-aware parsing** for single quotes, double quotes, and backslash escaping
- **Standard stream redirection** with overwrite and append modes
- **External and mixed pipelines** containing child processes and built-ins
- **Background job tracking** with job IDs, running states, and completion notices
- **JLine-backed history** with optional file loading, reading, writing, and appending
- **Context-aware Tab completion** for commands, executables, files, and directories
- **Programmable completion** through external completer processes

## How It Works

```text
                                 ┌──────────────────────┐
User input ──► JLine LineReader ─► quote-aware parsing │
         │                       └──────────┬───────────┘
         │                                  │
         │                   ┌──────────────┼──────────────┐
         │                   ▼              ▼              ▼
         │               built-in       external       pipeline
         │                handler        process        executor
         │                                  │              │
         │                                  ├─ foreground  ├─ external stages
         │                                  └─ background  └─ mixed stages
         │
         └──► custom Tab widget ──► command, path, or programmable completion
```

The implementation separates parsing, navigation, redirection, pipelines,
background jobs, history, and completion into focused classes.

## Built-in Commands

| Command | Description |
|---|---|
| `echo [text]` | Writes text to standard output |
| `type <command>` | Identifies a built-in or resolves an executable through `PATH` |
| `pwd` | Prints the shell's logical current directory |
| `cd [directory]` | Changes the shell's logical current directory; supports `~` and `~/...` |
| `complete ...` | Registers, prints, or removes programmable completion rules |
| `jobs` | Lists tracked background jobs and removes completed entries after reporting them |
| `history [n]` | Prints all history entries or only the latest `n` entries |
| `history -r <file>` | Reads commands from a file into the current JLine history |
| `history -w <file>` | Writes the current history to a file |
| `history -a <file>` | Appends history entries not previously appended during this session |
| `exit` | Terminates the shell |

## Parsing and Redirection

The parser preserves argument boundaries across quoted and escaped input:

```sh
echo "hello world"
echo 'single quoted value'
echo escaped\ space
```

Standard output and standard error support overwrite and append modes:

```sh
echo "first line" > output.txt
echo "next line" >> output.txt
command 2> error.txt
command 2>> error.txt
```

Redirection is applied directly to built-in output streams and through
`ProcessBuilder` for external commands.

## Pipelines

Pipeline stages are separated with a whitespace-delimited `|`:

```sh
printf "one\ntwo\n" | wc -l
echo "hello" | cat
echo "hello" | cat | wc -c
```

Pure external pipelines use `ProcessBuilder.startPipeline()`, allowing the
operating system to connect process streams. Pipelines containing built-ins use
the shell's mixed-stage executor, which passes each stage's captured output to
the next stage.

## Background Jobs

Add `&` to run a single external command in the background:

```sh
sleep 5 &
jobs
```

The shell assigns the lowest available positive job ID and prints the child
process ID immediately. `jobs` displays running and completed jobs, while the
main loop reports completed jobs before the next prompt and then removes them
from the active job table.

## Command History

Interactive commands are stored in JLine's in-memory history, including
consecutive duplicate commands. Use the arrow keys to navigate previous input,
or inspect entries with the `history` built-in:

```sh
history
history 10
```

History can also be transferred to and from files:

```sh
history -r commands.txt
history -w commands.txt
history -a commands.txt
```

If `HISTFILE` points to an existing regular file when the shell starts, its
lines are loaded into the current history:

```sh
HISTFILE="$HOME/.java_shell_history" ./your_program.sh
```

The shell does not automatically write history back to `HISTFILE`; use
`history -w` or `history -a` when persistence is required.

## Completion

The custom Tab widget combines built-in names, executables found in `PATH`, and
file-system entries.

```text
No match          → ring the terminal bell
One match         → insert the candidate; append a space unless it is a directory
Shared prefix     → extend the current input to the longest common prefix
First Tab         → ring the bell when the input cannot be extended
Second Tab        → print all matching candidates in sorted order
Directory match   → append "/" and continue path completion
```

### Programmable completion

Register an external command as the completer for a target command:

```sh
complete -C /path/to/completer deploy
```

When Tab is pressed after `deploy`, the completer process receives:

```text
$1  Target command
$2  Word currently being completed
$3  Previous word
```

The process also receives these environment variables:

```text
COMP_LINE   Complete input line
COMP_POINT  Input length measured in UTF-8 bytes
```

Each line written to the completer's standard output becomes one candidate.
Registered rules can be inspected or removed at runtime:

```sh
complete -p
complete -p deploy
complete -r deploy
```

Commands without a registered programmable completer fall back to file and
directory completion.

## Run Locally

### Requirements

- Java 21
- Maven
- A POSIX-compatible environment for `your_program.sh`

### Start the shell

```sh
./your_program.sh
```

The launcher builds an executable JAR with its runtime dependencies and starts
the shell.

Example session:

```text
$ echo "Hello from Java"
Hello from Java
$ type java
java is /usr/bin/java
$ sleep 1 &
[1] 12345
$ jobs
[1]+  Running                 sleep 1 &
$ history 3
    3  sleep 1 &
    4  jobs
    5  history 3
```

## Build

To create the executable JAR in `target/`:

```sh
mvn package -Ddir=target
java --enable-native-access=ALL-UNNAMED --enable-preview \
  -jar target/codecrafters-shell.jar
```

Maven Assembly packages JLine and the other runtime classes into the generated
JAR.

## Project Structure

```text
src/main/java/
├── Main.java                    Application entry point
├── Shell.java                   REPL, dispatch, completion, and process execution
├── CommandHandler.java          Common interface for built-in commands
├── Quoting.java                 Quote-aware tokenization and redirect parsing
├── Redirection.java             Built-in and child-process stream routing
├── Navigation.java              pwd and cd behavior
├── Pipelines.java               External and mixed pipeline execution
├── BackgroundJobs.java          Background job status and cleanup
├── History.java                 JLine history display and file operations
├── CommandCompletion.java       Built-in and PATH command candidates
├── FileCompletion.java          File and directory candidates
└── ProgrammableCompletion.java  Runtime completion registration
```

## Current Scope

This is a learning-focused, POSIX-inspired shell rather than a complete POSIX
implementation. Pipeline separators currently require whitespace on both sides,
background execution is implemented for single external commands, and features
such as variable expansion, globbing, command substitution, and job-control
signals are outside the current scope.
