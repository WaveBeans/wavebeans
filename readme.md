# Mux

A set of project to process audio signals on JVM.

## Mux CLI

Command line interface to edit sound files.

After program start you'll be able to enter commands. The specific list of commands depends on the scope you're in. To know the commands available in the current scope use `help` command, to leave the current scope use `fin` command. At any you want to leave the program type `exit`. There are two types of commands: in-scope and changing scope.

By default, you start in root scope, you can understand it when you see the prompt sign like this ` >`. There are commands that are changing the scope, i.e. you can open the file and it will change the scope and you'll be able to work with specific file, i.e. select the range, save it, down sample, etc.
```sh
> open test.wav
`test.wav`> _
```

## Mux Lib

Kotlin/Java library to work with sound and sound files.

## Intellij Idea instructions

1. Import as gradle project, use gradle wrapper.
2. Add Spek plugin in order to run test in IDE.