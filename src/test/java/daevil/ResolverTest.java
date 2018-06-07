package daevil;


import daevil.menu.BatchFileBuilder;
import daevil.menu.dependency.Resolver;
import daevil.script.nix.bash.BashEchoResolver;
import daevil.script.nix.bash.Md5UrlUnzipResolver;
import daevil.script.windows.batch.BatEchoResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static daevil.OSType.NIX;
import static daevil.OSType.WINDOWS;
import static org.junit.jupiter.api.Assertions.*;


class ResolverTest extends AbstractWorkTest {

    @Test
    void echoResolverNoCrossover() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        Daevil daevil = new Daevil();
        BatchFileBuilder batBuilder = daevil.controlScript().batchFileBuilder();
        daevil.controlScript().resolver(new Resolver(daevil.controlScript()) {
            @Override
            public String generate(OSType osType) {
                if (osType.typeOf(OSType.WINDOWS)) {
                    Daevil.log.info(WINDOWS.toString());
                    return BatEchoResolver.template(batBuilder, resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?", "Ok dude, downloading...").render().toString();
                } else {
                    Daevil.log.info(NIX.toString());
                    return BashEchoResolver.template(resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?", "Ok dude, downloading...").render().toString();

                }
            }
        });
        daevil.dest.set(Paths.get("."));
        daevil.controlScript().generate(OSType.WINDOWS, resourcesTarget);
        daevil.controlScript().generate(OSType.NIX, resourcesTarget);
        try (Stream<String> stream = Files.lines(Paths.get(resourcesTarget.toString(), daevil.controlScript().fileName.get()))) {
            assertFalse(stream.anyMatch(line -> line.contains("@echo off")));
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    void md5zipResolver() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        final String url = "http://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        Daevil daevil = new Daevil();
        daevil.controlScript().resolver(new Resolver(daevil.controlScript()) {
            @Override
            public String generate(OSType osType) {
                return Md5UrlUnzipResolver.template(url, resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?").render().toString();
            }
        });
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.NIX, resourcesTarget);
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);
//        System.out.println(daevil.controlScript().generate(OSType.NIX));
        OSType.ProcessResult processResult = daevil.controlScript().execute(resourcesTarget, "y", "0", "\n");
        assertTrue(processResult.output.get().contains("Getting " + url));
        processResult = daevil.controlScript().execute(resourcesTarget, "n", "0", "\n");
        assertFalse(processResult.output.get().contains("Getting " + url));
    }

    @Test
    void md5zipResolverNix() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        final String url = "http://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        Daevil daevil = new Daevil();
        daevil.controlScript().resolver(new Resolver(daevil.controlScript()) {
            @Override
            public String generate(OSType osType) {
                return Md5UrlUnzipResolver.template(url, resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?").render().toString();
            }
        });
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.NIX, resourcesTarget);
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);
//        System.out.println(daevil.controlScript().generate(OSType.NIX));
        OSType.ProcessResult processResult = daevil.controlScript().execute(resourcesTarget, "y", "0", "\n");
        assertTrue(processResult.output.get().contains("Getting " + url));
        processResult = daevil.controlScript().execute(resourcesTarget, "n", "0", "\n");
        assertFalse(processResult.output.get().contains("Getting " + url));
    }

}
