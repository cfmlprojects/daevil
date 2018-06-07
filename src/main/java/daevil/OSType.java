package daevil;

import daevil.menu.MenuOption;
import daevil.menu.MultiOSMenu;
import daevil.property.Property;
import org.jsoftbiz.utils.OS;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static daevil.property.Property.get;

public enum OSType {

    ANY("ANY"),
    NIX("NIX"),
    NIX_DEBIANISH("NIX"),
    NIX_RHELISH("NIX"),
    NIX_DARWINISH("NIX"),
    WINDOWS("WINDOWS");

    private String type;
    private static String ttyConfig;

    OSType(String type) {
        this.type = type;
    }

    public static OSType host() {
        OS os = OS.getOs();

        if (os.getName().contains("Windows"))
            return OSType.WINDOWS;

        if (Files.exists(Paths.get("/Library/LaunchDaemons/")))
            return OSType.NIX_DARWINISH;

        if (Files.exists(Paths.get("/lib/lsb/init-functions")))
            return OSType.NIX_DEBIANISH;

        if (Files.exists(Paths.get("/etc/init.d/functions")))
            return OSType.NIX_RHELISH;

        if (os.getName().contains("nix"))
            return OSType.NIX;

        throw new IllegalArgumentException("Unknown os type: " + os.getPlatformName());
    }

    public String type() {
        return type;
    }

    public boolean typeOf(OSType osType) {
        switch (osType) {
            case ANY:
                return true;
            case NIX:
                return type.equals("NIX") || this == ANY;
            case NIX_DARWINISH:
                return this == NIX_DARWINISH || this == ANY;
            case NIX_DEBIANISH:
                return this == NIX_DEBIANISH || this == ANY;
            case NIX_RHELISH:
                return this == NIX_RHELISH || this == ANY;
            case WINDOWS:
                return this == WINDOWS || this == ANY;
        }
        return false;
    }

    public ProcessResult execute(Path executable) {
        File scriptFile = executable.toFile();
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Executable not found for osType: " + executable + " : " +this);
        }
        return execute(executable.toAbsolutePath().toString(), null, null, 10, false);
    }

    public ProcessResult execute(String executeThis, String... input) {
        return execute(executeThis,null,Arrays.asList(input),10,false);
    }

    public ProcessResult execute(String executeThis, List<String> arguments, List<String> input, int timeoutSeconds, boolean haltOnFailure) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final AtomicInteger exitCode = new AtomicInteger();
        ProcessResult result = new ProcessResult(executeThis, "", -1);
        File scriptFile = Paths.get(executeThis).toFile();
        File processDirectory;
        if (scriptFile.exists()) {
            processDirectory = scriptFile.getParentFile();
        } else {
            processDirectory = Paths.get(".").toFile();
        }
        arguments = arguments == null ? new ArrayList<>() : arguments;
        input = input == null ? new ArrayList<>() : input;
        if (this.typeOf(WINDOWS)) {
            // https://stackoverflow.com/questions/13320578/how-to-run-batch-script-without-using-bat-extension
            arguments.add(0, "cmd.exe");
            arguments.add(1, "/c");
            arguments.add(2, executeThis);
            processBuilder.command(arguments);
        } else {
            arguments.add(0, "sh");
            arguments.add(1, "-c");
            arguments.add(2, executeThis);
            processBuilder.command(arguments);
        }
        if(host().typeOf(NIX) && this.typeOf(WINDOWS)){

//            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
//            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
//            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            //            String wincmdline = "wineconsole --backend=curses cmd.exe /c " + String.join(" ", executeThis);
            String wincmdline = "wine cmd.exe /c " + String.join(" ", executeThis);
            arguments.clear();
            arguments.add(0, "script");
            arguments.add(1, "--return");
            arguments.add(2, "-c");
            arguments.add(3, wincmdline);
            processBuilder.command(arguments);
//            arguments.add(1, "-c");
//            arguments.set(0,"sh -c \"wine " + String.join(" ", processBuilder.command()) + "\"");
            processBuilder.environment().put("WINEPATH","C:\\windows\\system32\\windowspowershell\\v1.0\\");
            processBuilder.environment().put("WINEDEBUG","fixme-all");
            processBuilder.environment().put("WINEPRIFIX","~/.wine");
            processBuilder.environment().put("WINEARCH","win32");
        }
        final String processCommand = String.join(" ", processBuilder.command());
        Daevil.log.debug("Executing" + " (" + processDirectory + "): " + processCommand);
        processBuilder.directory(processDirectory).redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream outPrintStream = new PrintStream(outputStream);
            Thread outputGobbler = new Thread(new StreamGobbler(process.getInputStream(), outPrintStream));
            outputGobbler.start();
            try {
                OutputStream os = process.getOutputStream();
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
                outPrintStream.flush();
                for (String arg : input) {
                    Thread.sleep(600);
                    System.out.println("Writing to process:" + arg);
                    pw.println(arg);
                    pw.flush();
                }
                outPrintStream.flush();
//                String output = ResourceUtil.getInputAsString(process.getInputStream());
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    Daevil.log.error("Took more than " + timeoutSeconds + " seconds to run command");
                    outPrintStream.flush();
                    process.destroy();
                }
                outPrintStream.flush();
                pw.close();
                outputGobbler.join();  // in case process ends before the threads finish
                if(!process.isAlive()){
                    exitCode.set(process.exitValue());
                }
                String output = outputStream.toString().replaceAll("[\r|\n]+","\n");
                if (exitCode.get() == 0) {
                    Daevil.log.info(output.trim());
                } else {
                    Daevil.log.error(output.trim());
                }
                result = new ProcessResult(processCommand, output, exitCode.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!process.isAlive()){
                exitCode.set(process.exitValue());
            }
            process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exitCode.get() != 0 && haltOnFailure) {
            throw new RuntimeException("Encountered an error (" + exitCode.get() + "), halting further commands.");
        }

        return result;
    }


    public class ProcessResult {
        private String _command;
        public final Property<String> command = get(() -> _command)
                .set(value -> _command = value);

        private String _output;
        public final Property<String> output = get(() -> _output)
                .set(value -> _output = value);

        private Integer _exitCode;
        public final Property<Integer> exitCode = get(() -> _exitCode)
                .set(value -> _exitCode = value);

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


    class StreamGobbler implements Runnable {
        private final InputStream is;
        private final PrintStream os;

        StreamGobbler(InputStream is, PrintStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    os.print((char) c);
            } catch (IOException x) {
                // Handle error
            }
        }
    }


}