package daevil;


import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import daevil.menu.BatchFileBuilder;
import daevil.term.Gobbler;
import daevil.term.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static daevil.term.Console.startReader;
import static daevil.term.Console.startStderrGobbler;
import static daevil.term.Console.startStdoutGobbler;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ExecuteTest extends AbstractWorkTest {


    @Test
    void testTty() throws IOException, InterruptedException {

        PtyProcessBuilder builder = new PtyProcessBuilder(new String[]{"/bin/bash", "-c", "echo Success"})
                .setRedirectErrorStream(true)
                .setConsole(true);
        PtyProcess process = builder.start();
        Gobbler stdout = startReader(process.getInputStream(), null);
        Gobbler stderr = startReader(process.getErrorStream(), null);
        stdout.assertEndsWith("Success");
        stdout.awaitFinish();
        stderr.awaitFinish();
        System.out.println(stdout.getOutput());
        assert ("".equals(stderr.getOutput()));
    }

    @Test
    @EnabledOnOs({OS.WINDOWS})
    void testTtyWinePs() throws IOException, InterruptedException {

        Map<String, String> env = new HashMap<>();
        env.put("TERM", "xterm-mono");
        env.put("NO_COLOR", "1");
        String[] cmd = new String[]{"/bin/sh", "-c", "WINEDEBUG=-all wine cmd /c \"set NO_COLOR=1 TERM=xterm-mono && powershell -Command Write-Host 'Success'\""};
        PtyProcessBuilder builder = new PtyProcessBuilder(cmd)
                .setRedirectErrorStream(false)
//                .setEnvironment(env)
//                .setWindowsAnsiColorEnabled(false)
//                .setUnixOpenTtyToPreserveOutputAfterTermination(true)
                .setConsole(true);
        PtyProcess process = builder.start();
        Gobbler stdout = startStdoutGobbler(process);
        Gobbler stderr = startStderrGobbler(process);
        stdout.awaitFinish();
        stderr.awaitFinish();
        process.destroy();
        assert (stdout.getPlainOutput().equalsIgnoreCase("Success"));
        System.err.println(stderr.getOutput());
//        stdout.assertEndsWith("Success\r\n");
//        assert("".equals(stderr.getOutput()));
    }

    @Test
    @EnabledOnOs({OS.WINDOWS})
    void powershellEcho() {
        Path resourcesTarget = Paths.get(workDir + "/functionTest.bat").toAbsolutePath();
        BatchFileBuilder bat = new BatchFileBuilder(false);
        bat.append("set NO_COLOR=1\n");
        bat.append("set TERM=xterm-mono\n");
        bat.append("echo about from CONSOLE > CON\n");
        bat.append("echo.about to echo \"hello\" from powershell...\n");
        bat.append("powershell -command Write-Host \"hello there from powershell\"\n");
        bat.append("echo.done bruv\n");
        bat.write(resourcesTarget);
        ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
        System.out.println(processResult.outputNoAnsi.get());
        assertTrue(processResult.outputNoAnsi.get().contains("hello there from powershell"));
    }

}
