package daevil;


import daevil.menu.BatchFileBuilder;
import daevil.menu.dependency.Resolver;
import daevil.script.nix.bash.BashEchoResolver;
import daevil.script.windows.batch.BatEchoResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

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
                if(osType.typeOf(OSType.WINDOWS)){
                    return BatEchoResolver.template(batBuilder,resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?", "Ok dude, downloading...").render().toString();
                } else{
                    return BashEchoResolver.template(resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?", "Ok dude, downloading...").render().toString();
                }
            }
        });
        daeviler.dest.set(Paths.get("."));
        daeviler.controlScript().generate(OSType.WINDOWS,resourcesTarget);
        daeviler.controlScript().generate(OSType.NIX,resourcesTarget);
    }


    @Test
    void echoResolverNix() {
        final String url = "http://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
//        System.out.println(daeviler.controlScript().generate(OSType.NIX));
        OSType.ProcessResult processResult = daeviler.controlScript().execute(resourcesTarget, "y", "0", "\n");
        assertTrue(processResult.output.get().contains("Ok dude"));

        processResult = daeviler.controlScript().execute(resourcesTarget, "n", "0", "\n");
        assertTrue(processResult.output.get().contains("NO"));
    }

    @Test
    void echoResolverWindows() {
        OSType.ProcessResult processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS,"N", "0", "0");
        assertTrue(processResult.output.get().contains("NO download for you"));
        processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS, "Y", "0", "0");
        assertTrue(processResult.output.get().contains("Ok dude"),processResult.output.get());
    }

}
