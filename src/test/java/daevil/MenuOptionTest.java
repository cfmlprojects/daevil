package daevil;

import daevil.menu.MenuOption;
import daevil.menu.MultiOSMenu;
import daevil.term.ProcessResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuOptionTest extends AbstractWorkTest {

    @Test
    void executeOption() {
        MultiOSMenu menu = new MultiOSMenu("Test Menu");
        menu
                .addOption("start", "Starts the thing", "echo start")
                .addOption("stop", "Stops the thing", "echo stop");

        menu.options().forEach(menuOption -> {
            menuOption.execute(OSType.host(), true);
        });


    }


    @Test
    void executeOptionFailure() {
        final AtomicInteger executions = new AtomicInteger();
        assertThrows(RuntimeException.class,
                () -> {
                    MultiOSMenu menu = new MultiOSMenu("Test Menu");
                    menu
                            .addOption("start", "Starts the thing", "iamAcommandDontExist start")
                            .addOption("stop", "Stops the thing", "echo stop");

                    menu.options().forEach(menuOption -> {
                        executions.incrementAndGet();
                        menuOption.execute(OSType.host(), true);
                    });
                });
        assertEquals(1, executions.get());

        executions.set(0);
        MultiOSMenu menu = new MultiOSMenu("Test Menu");
        MenuOption menuOption = menu
                .addOption("start", "Starts the thing")
                .command("echo hello")
                .command("ishouldFailAsIdontExist hello")
                .command("echo bye");

            executions.incrementAndGet();
            System.out.println("INCRMENTING" + executions.get());
        List<ProcessResult> results = menuOption.execute(OSType.host(), false);
        results.forEach(stringMapMap -> System.out.println(stringMapMap.toString()));
        assertEquals(3, results.size());
        assertNotEquals(0, results.get(1).exitCode);
    }

    @Test
    void executeOptionFailureIgnored() {
        MultiOSMenu menu = new MultiOSMenu("Test Menu");
        menu
                .addOption("start", "Starts the thing", "iamAcommandDontExist start")
                .addOption("stop", "Stops the thing", "echo stop");

        menu.options().forEach(menuOption -> {
            menuOption.execute(OSType.host(), false);
        });
    }

    @Test
    void multiOSMenuOptions() {
        MultiOSMenu menu = new MultiOSMenu("Test Bash Menu");
        menu.fileName.set("menu");

        menu.addOption("start", "Starts the thing")
                .command(OSType.WINDOWS, "echo start " + OSType.WINDOWS)
                .command(OSType.NIX_DARWINISH, "echo start " + OSType.NIX_DARWINISH)
                .command(OSType.NIX, "echo start " + OSType.NIX);

        menu.addOption("stop", "Stops the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX_DARWINISH, "echo stop " + OSType.NIX_DARWINISH)
                .command(OSType.NIX, "echo stop " + OSType.NIX);


        menu.options().forEach(menuOption -> {
            System.out.println(menuOption.name.get());
            assertEquals(1, menuOption.commandsish(OSType.WINDOWS).size());
            assertEquals(1, menuOption.commandsish(OSType.NIX_DARWINISH).size());
            assertEquals(2, menuOption.commandsish(OSType.NIX).size());
            assertEquals(3, menuOption.commandsish(OSType.ANY).size());
        });

        Path resourcesTarget = Paths.get(workDir.toString() + "/multiosmenu");
        menu.generate(resourcesTarget).forEach(Daevil.log::info);

    }

    @Test
    void commandish() {
        MultiOSMenu menu = new MultiOSMenu("Test Menu");
        menu.fileName.set("menu");

        // if only one nix darwin should get it, otherwise it gets the specific one
        MenuOption menuOption = menu.addOption("stop", "does the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX, "echo stop " + OSType.NIX);
        assertEquals(1, menuOption.commandsish(OSType.NIX_DARWINISH).size());

        menuOption = menu.addOption("stopagain", "does the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX, "echo stop " + OSType.NIX)
                .command(OSType.NIX_DARWINISH, "echo stop " + OSType.NIX_DARWINISH);
        assertEquals(1, menuOption.commandsish(OSType.NIX_DARWINISH).size());
        assertEquals("echo stop " + OSType.NIX_DARWINISH, menuOption.commandsish(OSType.NIX_DARWINISH).get(0));

        menuOption = menu.addOption("somethingelse", "does the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX, "echo stop " + OSType.NIX)
                .command(OSType.NIX_DEBIANISH, "echo stop " + OSType.NIX_DEBIANISH);
        assertEquals(1, menuOption.commandsish(OSType.NIX_DEBIANISH).size());
        assertEquals("echo stop " + OSType.NIX_DEBIANISH, menuOption.commandsish(OSType.NIX_DEBIANISH).get(0));

        menuOption = menu.addOption("somethingelsetoo", "does the thing")
                .command(OSType.WINDOWS, "echo stop " + OSType.WINDOWS)
                .command(OSType.NIX, "echo stop " + OSType.NIX)
                .command(OSType.NIX_RHELISH, "echo stop " + OSType.NIX_RHELISH);
        assertEquals(1, menuOption.commandsish(OSType.NIX_RHELISH).size());
        assertEquals("echo stop " + OSType.NIX_RHELISH, menuOption.commandsish(OSType.NIX_RHELISH).get(0));

    }


}
