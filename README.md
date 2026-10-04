# Java Shell

A lightweight, POSIX-inspired command-line shell built with Java 21 and JLine.
It provides an interactive REPL for running built-in commands and external
programs available through the system `PATH`.

## Features

- Built-in commands: `exit`, `echo`, `type`, `pwd`, and `cd`
- External program discovery and execution through `PATH`
- Single quotes, double quotes, and backslash escaping
- Standard output and error redirection with overwrite and append modes
- Command completion for built-ins and executable programs
- File and directory path completion
- Longest common prefix completion for multiple matches
- Alphabetically sorted suggestions after pressing Tab twice

## Requirements

- Java 21
- Maven

## Run Locally

```sh
./your_program.sh
```

The shell displays a prompt where commands can be entered:

```text
$ echo "Hello, world!"
Hello, world!
```

## Redirection Examples

```sh
echo "Hello" > output.txt
echo "Again" >> output.txt
command 2> error.txt
command 2>> error.txt
```

## Project Structure

```text
src/main/java/
├── Main.java                  Application entry point
├── Shell.java                 REPL and command execution
├── Navigation.java            pwd and cd behavior
├── Quoting.java               Argument parsing and redirection detection
├── Redirection.java           Output and error stream redirection
├── CommandCompletion.java     Built-in and executable completion
└── FileCompletion.java        File and directory completion
```
