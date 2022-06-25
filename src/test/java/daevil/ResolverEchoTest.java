package daevil;


import com.google.common.collect.ImmutableMap;
import daevil.menu.BatchFileBuilder;
import daevil.menu.dependency.Resolver;
import daevil.term.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ResolverEchoTest extends AbstractWorkTest {

    private Daevil daeviler;
    private Path resourcesTarget;

    @BeforeEach
    void beforeEach() {
        resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        daeviler = new Daevil();
        BatchFileBuilder batBuilder = daeviler.controlScript().batchFileBuilder();
        daeviler.controlScript().resolver(new Resolver(daeviler.controlScript()) {
            @Override
            public String generate(OSType osType) {
                HashMap<String, Object> map = new HashMap<>();
                if(osType.typeOf(OSType.WINDOWS)){
                    map.put("builder", batBuilder);
                    map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/dl");
                    map.put("prompt", "Download?");
                    map.put("echo", "Ok lady, downloading...");
                    return Daevil.render("daevil/script/windows/batch/BatEchoResolver",map);
                } else{
                    map.put("toDir", resourcesTarget.toAbsolutePath().toString() + "/dl2");
                    map.put("prompt", "Download2?");
                    map.put("echo", "Ok dude, downloading...");
                    return Daevil.render("daevil/script/nix/bash/BashEchoResolver",map);
                }
            }
        });
        daeviler.dest.set(Paths.get("."));
        daeviler.controlScript().generate(OSType.WINDOWS,resourcesTarget);
        daeviler.controlScript().generate(OSType.NIX,resourcesTarget);
    }


    @Test
    void echoResolverNix() {
        ProcessResult processResult;
        Map<String, String> input;

        input = ImmutableMap.ofEntries( entry("Download2? [y/n]:", "y"), entry("Option number:", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.output.get().contains("Ok dude"));

        input = ImmutableMap.ofEntries( entry("Download2? [y/n]:", "n"), entry("Option number:", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.output.get().contains("NO"));
    }

    @Test
    void echoResolverWindows() {
        ProcessResult processResult;
        Map<String, String> input;

        input = ImmutableMap.ofEntries( entry("Download2? [y/n]:", "n"), entry("Option number:", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.output.get().contains("NO download for you"));

        input = ImmutableMap.ofEntries( entry("Download2? [y/n]:", "y"), entry("Option number:", "0") );
        processResult = daeviler.controlScript().execute(resourcesTarget, input );
        assertTrue(processResult.output.get().contains("Ok dude"),processResult.output.get());
    }

}
