# Java Shell

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![JLine](https://img.shields.io/badge/JLine-4.4.6-4B5563)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)

**A POSIX-inspired command-line shell built from scratch in Java.**

This project goes beyond launching processes. It implements its own command
dispatch, quote-aware parsing, stream redirection, PATH resolution, interactive
line editing, and a Bash-inspired programmable completion protocol.

## Highlights

- **Interactive REPL** powered by JLine with a custom Tab widget
- **Built-in command dispatch** for `exit`, `echo`, `type`, `pwd`, `cd`, and `complete`
- **External process execution** through `PATH` lookup and `ProcessBuilder`
- **Quote-aware parsing** for single quotes, double quotes, and backslash escaping
- **Stream redirection** for stdout and stderr using overwrite and append modes
- **Context-aware completion** for commands, executable programs, files, and directories
- **Longest common prefix expansion** when several candidates share a prefix
- **Double-Tab discovery** with alphabetically sorted completion candidates
- **Programmable completion** backed by external processes and shell-style context variables

## How It Works

```text
                         ┌───────────────────────┐
User input ──► JLine ──► │ Custom Tab completion │
                         └───────────┬───────────┘
                                     │
                  ┌──────────────────┼──────────────────┐
                  ▼                  ▼                  ▼
          Command lookup       File completion    Programmable
          built-ins + PATH     files + folders    external completer

User input ──► Quote-aware parser ──► Built-in handler
                         │
                         └──────────► ProcessBuilder ──► External program
                                           │
                                           └── stdout / stderr redirection
```

The shell keeps command parsing, navigation, redirection, and completion in
separate components so each responsibility can evolve independently.

## Programmable Completion

Custom completion commands can be registered at runtime:

```sh
complete -C /path/to/completer deploy
```

After registration, pressing Tab after `deploy` starts the external completer
and passes it contextual information.

### Completer process contract

The completer receives three arguments after its executable name:

```text
$1  Target command
$2  Word currently being completed
$3  Previous word
```

It also receives:

```text
COMP_LINE   Complete input line
COMP_POINT  Current input length measured in UTF-8 bytes
```

Every line written to the completer's standard output becomes a completion
candidate. Those candidates then use the same single-match, longest-common-prefix,
and double-Tab behavior as built-in completion.

Registered completion specifications can be inspected or removed:

```sh
complete -p
complete -p deploy
complete -r deploy
```

If a command has no registered programmable completer, the shell falls back to
ordinary file and directory completion.

## Completion Behavior

```text
No match          → ring the terminal bell
One match         → complete the candidate and append a space
Shared prefix     → extend input to the longest common prefix
First Tab         → ring the terminal bell when input cannot expand
Second Tab        → print all matching candidates in sorted order
Directory match   → append “/” and continue path completion
```

## Built-in Commands

| Command | Description |
|---|---|
| `echo` | Writes text to standard output |
| `type` | Identifies a built-in or resolves an executable through `PATH` |
| `pwd` | Prints the shell's current working directory |
| `cd` | Changes the shell's current working directory |
| `complete` | Registers, prints, or removes programmable completion rules |
| `exit` | Terminates the shell |

## Parsing and Redirection

The parser preserves argument boundaries across quoted and escaped input:

```sh
echo "hello world"
echo 'single quoted value'
echo escaped\ space
```

Output and error streams support overwrite and append modes:

```sh
echo "first line" > output.txt
echo "next line" >> output.txt
command 2> error.txt
command 2>> error.txt
```

Redirection works for both built-in handlers and external child processes.

## Run Locally

### Requirements

- Java 21
- Maven

### Start the shell

```sh
./your_program.sh
```

Example session:

```text
$ echo "Hello from Java"
Hello from Java
$ type java
java is /usr/bin/java
$ pwd
/path/to/project
```

## Project Structure

```text
src/main/java/
├── Main.java                    Application entry point
├── Shell.java                   REPL, dispatch, and process execution
├── Quoting.java                 Quote-aware tokenization and redirect parsing
├── Redirection.java             Built-in and child-process stream routing
├── Navigation.java              pwd and cd behavior
├── CommandCompletion.java       Built-in and PATH command candidates
├── FileCompletion.java          File and directory candidates
└── ProgrammableCompletion.java  Runtime completion registration
```

## Build

```sh
mvn clean package
```

The project uses Maven Assembly to produce an executable JAR containing its
runtime dependencies.
