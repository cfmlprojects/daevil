package daevil.term;

import com.google.common.base.Ascii;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import daevil.Daevil;
import daevil.OSType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static daevil.OSType.NIX;
import static daevil.OSType.WINDOWS;

public class Console {


    public static List<String> setRunWIthWineCmd(PtyProcessBuilder processBuilder, String executeThis) {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-mono");
        env.put("WINEDEBUG", "-all");
        //env.put("WINEDEBUG", "fixme-all");
//            env.put("WINEPREFIX", System.getProperty("user.home") + "/.wine");

        processBuilder.setEnvironment(env);

        String wincmdline = "wine " + String.join(" ", executeThis);

        String[] cmd = {
                "/bin/sh", "-c", wincmdline
                //"/bin/bash", thisDir + "/src/test/docker/docker-wine", wincmdline
        };
        processBuilder.setCommand(cmd);
        return List.of(cmd);
    }

    public static Gobbler startStdoutGobbler(PtyProcess process) {
        return new Gobbler(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8), null, process);
    }

    public static Gobbler startStderrGobbler(PtyProcess process) {
        return new Gobbler(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8), null, process);
    }

    public static Gobbler startReader(InputStream in, CountDownLatch latch) {
        return new Gobbler(new InputStreamReader(in, StandardCharsets.UTF_8), latch, null);
    }

    public static String convertInvisibleChars(String s) {
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\b", "\\b")
                .replace("\u001b", "ESC")
                .replace(String.valueOf((char) Ascii.BEL), "BEL");
    }

    public static void writeToStdinAndFlush(PtyProcess process, String input,
                                            boolean hitEnter) throws IOException {
        String text = hitEnter ? input + (char) process.getEnterKeyCode() : input;
        process.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
    }

    public static String getProcessStatus(PtyProcess process) {
        boolean running = process.isAlive();
        Integer exitCode = getExitCode(process);
        if (running && exitCode == null) {
            return "alive process";
        }
        return "process running:" + running + ", exit code:" + (exitCode != null ? exitCode : "N/A");
    }

    private static Integer getExitCode(PtyProcess process) {
        Integer exitCode = null;
        try {
            exitCode = process.exitValue();
        } catch (IllegalThreadStateException ignored) {
        }
        return exitCode;
    }

    private void interactWithProcess(PtyProcess process, Gobbler stdout, Map<String, String> input) {
        input.forEach((findInOutput, thenReplyWith) -> {
            if (stdout.awaitTextEndsWith(findInOutput, 10000)) {
                try {
                    Console.writeToStdinAndFlush(process, thenReplyWith, true);
                    stdout.readLine(1000);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public ProcessResult execute(OSType host, OSType osType, String executeThis, List<String> arguments, Map<String, String> input, int timeoutSeconds, boolean haltOnFailure) {

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

        if (osType.typeOf(WINDOWS)) {
            // https://stackoverflow.com/questions/13320578/how-to-run-batch-script-without-using-bat-extension
            arguments.add(0, "cmd.exe");
            arguments.add(1, "/c");
        } else {
            arguments.add(0, "sh");
            arguments.add(1, "-c");
        }
        arguments.add(2, executeThis);

        PtyProcessBuilder processBuilder = new PtyProcessBuilder()
                .setDirectory(processDirectory.getAbsolutePath())
                .setRedirectErrorStream(false)
                .setConsole(true)
                .setCommand(arguments.toArray(String[]::new));

        if (host.typeOf(NIX) && osType.typeOf(WINDOWS)) {
            arguments = Console.setRunWIthWineCmd(processBuilder, executeThis);
        }

        final String processCommand = String.join(" ", arguments);
        Daevil.log.debug("Executing" + " (" + processDirectory + "): " + processCommand);

        try {
            final PtyProcess process = processBuilder.start();
            Gobbler stdout = Console.startStdoutGobbler(process);
            Gobbler stderr = Console.startStderrGobbler(process);

            if (input != null) {
                interactWithProcess(process, stdout, input);
            } else {
                stdout.awaitFinish();
            }

            stdout.awaitFinish();
            stderr.awaitFinish();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                Daevil.log.error("Took more than " + timeoutSeconds + " seconds to run command");
                Daevil.log.error(stderr.getPlainOutput());
                process.getOutputStream().flush();
                process.destroy();
            }
            if (!process.isAlive()) {
                exitCode.set(process.exitValue());
            }
            process.destroy();
            String output = stdout.getPlainOutput() + stderr.getPlainOutput();
            result = new ProcessResult(processCommand, output, exitCode.get());
            if (exitCode.get() == 0) {
                Daevil.log.info(output.trim());
            } else {
                Daevil.log.error(output.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exitCode.get() != 0 && haltOnFailure) {
            throw new RuntimeException("Encountered an error (" + exitCode.get() + "), halting further commands.");
        }

        return result;
    }


}
