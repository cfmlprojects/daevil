package daevil.term;

import daevil.property.Property;

import static daevil.property.Property.get;
import static daevil.term.Gobbler.cleanWinTextAnsi;

public class ProcessResult {
    private String _command;
    public final Property<String> command = get(() -> _command).set(value -> _command = value);

    private String _output;
    public final Property<String> output = get(() -> _output).set(value -> _output = value);

    public final Property<String> outputNoAnsi = get(() -> cleanWinTextAnsi(_output)).set(value -> null);

    private Integer _exitCode;
    public final Property<Integer> exitCode = get(() -> _exitCode).set(value -> _exitCode = value);

    public ProcessResult(String command) {
        this.command.set(command);
    }

    public ProcessResult(String command, String output, int exitCode) {
        this(command);
        this.output.set(output);
        this.exitCode.set(exitCode);

    }

    public String toString() {
        return String.format("%s ... \"%s\" (%s)", _command, _output.trim(), _exitCode);
    }

}
