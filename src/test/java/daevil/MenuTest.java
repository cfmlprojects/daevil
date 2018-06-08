package daevil;

import daevil.menu.BashMenu;
import daevil.menu.BatchFileMenu;
import daevil.menu.MultiOSMenu;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuTest extends AbstractWorkTest {

    @Test
    void bashMenu() {
        BashMenu menu = new BashMenu("Test Bash Menu");
        menu
                .addOption("start", "Starts the thing", "echo start")
                .addOption("stop", "Stops the thing", "echo stop");
        Path resourcesTarget = Paths.get(workDir.toString() + "/ctl-echo.bat");
        menu.generate(resourcesTarget);
    }


    @Test
    void multiOSMenu() {
        // tag::docExample[]
        MultiOSMenu menu = new MultiOSMenu("Test Menu");

        menu.fileName.set("menu");

        menu.addOption("start", "Starts the thing")
                .command(OSType.WINDOWS, "echo start " + OSType.WINDOWS)
                .command(OSType.NIX_DARWINISH, "echo start " + OSType.NIX_DARWINISH)
                .command(OSType.NIX, "echo start " + OSType.NIX);

        menu.addOption("stop", "Stops the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX_DARWINISH, "echo stop " + OSType.NIX_DARWINISH)
                .command(OSType.NIX, "echo stop " + OSType.NIX);

        Path resourcesTarget = Paths.get(workDir.toString() + "/multiosmenu");

        menu.generate(resourcesTarget).forEach(Daevil.log::info);
        // end::docExample[]
    }

    @Test
    void duplicateOptionNameException() {
        assertThrows(RuntimeException.class,
                () -> {
                    MultiOSMenu menu = new MultiOSMenu("Test Menu");
                    menu
                            .addOption("start", "Starts the thing", "iamAcommandDontExist start")
                            .addOption("start", "Stops the thing", "echo stop");
                });
    }


    @Test
    void multiOSMenuJavaResolver() {
        MultiOSMenu menu = new MultiOSMenu("Test Bash Menu");
        menu.fileName.set("menu");

        menu.javaResolver();
        assertEquals(1, menu.resolvers().size());

        menu.addOption("start", "Starts the thing")
                .command(OSType.WINDOWS, "echo start " + OSType.WINDOWS)
                .command(OSType.NIX_DARWINISH, "echo start " + OSType.NIX_DARWINISH)
                .command(OSType.NIX, "echo start " + OSType.NIX);

        menu.addOption("stop", "Stops the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX_DARWINISH, "echo stop " + OSType.NIX_DARWINISH)
                .command(OSType.NIX, "echo stop " + OSType.NIX);


        Path resourcesTarget = Paths.get(workDir.toString() + "/multiosmenu");
        menu.generate(resourcesTarget);
        System.out.println(menu.generate());

    }

    @Test
    void batchFileMenu() throws IOException {
        BatchFileMenu menu = new BatchFileMenu("Test Batch File Menu");
        menu
                .addOption("start", "Starts the thing", "echo start")
                .addOption("stop", "Stops the thing", "echo stop");
        Path resourcesTarget = Paths.get("src/test/resources/tmp/windows/ctl-echo.bat");

        resourcesTarget.toFile().getParentFile().mkdirs();
        String script = menu.generate();
        try {
            Files.write(resourcesTarget, script.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(script);

        resourcesTarget = Paths.get(workDir.toString() + "/windows");
        Daevil daevil = new Daevil();
        daevil.name.set("echoctl");
        daevil.description.set("Nifty script to echo");
        daevil.dest.set(Paths.get("."));
        daevil.ctlScript.set(workDir.toString() + "/windows/ctl-echo.bat");
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);

    }


}
