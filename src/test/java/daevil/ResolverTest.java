package daevil;


import com.google.common.collect.ImmutableMap;
import daevil.menu.BatchFileBuilder;
import daevil.menu.dependency.Resolver;
import daevil.term.ProcessResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static daevil.OSType.NIX;
import static daevil.OSType.WINDOWS;
import static java.util.Map.entry;
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
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("builder", batBuilder);
                    map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/rockerwin");
                    map.put("prompt", "Download?");
                    map.put("echo", "Ok dude, downloading...");
                    return Daevil.render("daevil/script/windows/batch/BatEchoResolver",map);
                } else {
                    Daevil.log.info(NIX.toString());
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/rockernix");
                    map.put("prompt", "Download?");
                    map.put("echo", "Ok dude, downloading...");
                    return Daevil.render("daevil/script/nix/bash/BashEchoResolver",map);

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
        recreateWorkDir();
        Path resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        final String url = "https://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        Daevil daevil = new Daevil();
        daevil.controlScript().resolver(new Resolver(daevil.controlScript()) {
            @Override
            public String generate(OSType osType) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("url", url);
                map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/rockerwin");
                map.put("prompt", "Download?");
                return Daevil.render("daevil/script/nix/bash/Md5UrlUnzipResolver",map);
            }
        });
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.NIX, resourcesTarget);
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);
//        System.out.println(daevil.controlScript().generate(OSType.NIX));
        ProcessResult processResult;
        Map<String, String> input;

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "y"), entry("Option number:", "0") );
        processResult = daevil.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.output.get().contains("Getting " + url));

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "n"), entry("Option number:", "0") );
        processResult = daevil.controlScript().execute(resourcesTarget, input );
        assertFalse(processResult.output.get().contains("Getting " + url));
    }

    @Test
    void md5zipResolverNix() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        final String url = "https://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        Daevil daevil = new Daevil();
        daevil.controlScript().resolver(new Resolver(daevil.controlScript()) {
            @Override
            public String generate(OSType osType) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("url", url);
                map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/rockerwin");
                map.put("prompt", "Download?");
                return Daevil.render("daevil/script/nix/bash/Md5UrlUnzipResolver.jte",map);
            }
        });
        daevil.dest.set(Paths.get("."));
        daevil.generateScripts(OSType.NIX, resourcesTarget);
        daevil.generateScripts(OSType.WINDOWS, resourcesTarget);
//        System.out.println(daevil.controlScript().generate(OSType.NIX));
        ProcessResult processResult;
        Map<String, String> input;

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "y"), entry("Option number:", "0") );
        processResult = daevil.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.output.get().contains("Getting " + url));

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "n"), entry("Option number:", "0") );
        processResult = daevil.controlScript().execute(resourcesTarget, input );
        assertFalse(processResult.output.get().contains("Getting " + url));
    }

}
