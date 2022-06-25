package daevil;


import com.google.common.collect.ImmutableMap;
import daevil.menu.BatchFileBuilder;
import daevil.menu.dependency.Resolver;
import daevil.term.ProcessResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResolverMd5ConfirmationTest extends AbstractWorkTest {

    private Daevil daeviler;
    private Path resourcesTarget;


    @BeforeAll
    void generateScripts() {
        final String url = "https://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        daeviler = new Daevil();
        BatchFileBuilder batBuilder = daeviler.controlScript().batchFileBuilder();
        daeviler.controlScript().resolver(new Resolver(daeviler.controlScript()) {
            @Override
            public String generate(OSType osType) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("builder", batBuilder);
                map.put("url", url);
                map.put("prompt", "Download?");
                map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/" + osType.toString());
                if (osType.typeOf(OSType.WINDOWS)) {
                    map.put("toDir", "%TEMP%\\" + osType.toString());
                    return Daevil.render("daevil/script/windows/batch/BatMd5UrlUnzipResolver",map);
                } else {
                    return Daevil.render("daevil/script/nix/bash/Md5UrlUnzipResolver",map);
                }
            }
        });
        daeviler.dest.set(Paths.get("."));
//        daeviler.controlScript().generate(OSType.WINDOWS, resourcesTarget);
//        daeviler.controlScript().generate(OSType.NIX, resourcesTarget);


    }


    @Test
    void md5ResolverNix() {
        ProcessResult processResult;
        Map<String, String> input;

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "y"), entry("Option number", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.outputNoAnsi.get().contains("Getting"));

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "n"), entry("Option number", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertFalse(processResult.outputNoAnsi.get().contains("Getting"));
    }

    @Test
    void md5ResolverWindows() {
        ProcessResult processResult;
        Map<String, String> input;

        input = ImmutableMap.ofEntries( entry("Download? [y/n]:", "y"), entry("Option number:", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertFalse(processResult.output.get().contains("Failure"));
//        assertTrue(processResult.output.get().contains("Successfully unzipped"));
//        processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS, "Y", "Y","0", "0");
//        assertTrue(processResult.output.get().contains("Ok dude"),processResult.output.get());
//        assertTrue(processResult.output.get().contains("Ok lady"));
    }

}
