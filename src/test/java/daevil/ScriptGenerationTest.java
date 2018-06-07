package daevil;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ScriptGenerationTest extends AbstractWorkTest {

    @Test
    void copyFileReplaceTokens() {
        String installServiceBat = "/script/nix/";
        Path installServiceTarget = Paths.get(workDir.toString() + "/nix").toAbsolutePath();
        installServiceTarget.getParent().toFile().mkdirs();
        HashMap<String, String> tokens = new HashMap<>();
        tokens.put("name", "SomeName");
        tokens.put("title", "SomeTitle");
        ResourceUtil.copyResourcesReplaceTokens(installServiceBat, installServiceTarget, tokens, false, "install.*", "");
        try (Stream<String> stream = Files.lines(Paths.get(installServiceTarget.toString() + "/install.sh"))) {
            stream.forEach(line -> {
                assertFalse(line.contains("@name@"));
            });
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    void windowsDaevil() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/windows");
        Daevil daevil = new Daevil();
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);
    }

    @Test
    void daevilNix() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/nix");
        Daevil daevil = new Daevil();
        daevil.javaResolver();
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.NIX, resourcesTarget);
        try (Stream<String> stream = Files.lines(Paths.get(resourcesTarget.toString() + "/service-ctl"))) {
            assertTrue(stream.anyMatch(line -> line.contains("JRE")));
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    void daevilHost() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/host");
        Daevil daevil = new Daevil();
        daevil.generateScriptsForHostOS(resourcesTarget);
    }

    @Test
    void daevilAll() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/all");
        Daevil daevil = new Daevil();
        daevil.javaResolver();
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.NIX, resourcesTarget);
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);
        try (Stream<String> stream = Files.lines(Paths.get(resourcesTarget.toString() + "/service-ctl"))) {
            assertTrue(stream.anyMatch(line -> line.contains("JRE")));
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
