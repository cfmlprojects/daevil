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


class ResolverConfirmationTest extends AbstractWorkTest {

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
                if (osType.typeOf(OSType.WINDOWS)) {
                    return BatEchoResolver.template(batBuilder, resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?", "Ok dude, downloading...").render().toString();
                } else {
                    return BashEchoResolver.template(resourcesTarget.toAbsolutePath().toString() + "/dl", "Download?", "Ok dude, downloading...").render().toString();
                }
            }
        });
        daeviler.controlScript().resolver(new Resolver(daeviler.controlScript()) {
            @Override
            public String generate(OSType osType) {
                if (osType.typeOf(OSType.WINDOWS)) {
                    return BatEchoResolver.template(batBuilder, resourcesTarget.toAbsolutePath().toString() + "/dl2", "Download 2?", "Ok lady, downloading...").render().toString();
                } else {
                    return BashEchoResolver.template(resourcesTarget.toAbsolutePath().toString() + "/dl2", "Download 2?", "Ok lady, downloading...").render().toString();
                }
            }
        });
        daeviler.dest.set(Paths.get("."));
        daeviler.controlScript().generate(OSType.WINDOWS, resourcesTarget);
        daeviler.controlScript().generate(OSType.NIX, resourcesTarget);

    }


    @Test
    void echoResolverNix() {
        OSType.ProcessResult processResult = daeviler.controlScript().execute(resourcesTarget,
                "y", "y", "0", "\n"
        );
        assertTrue(processResult.output.get().contains("Ok dude"));
        assertTrue(processResult.output.get().contains("Ok lady"));

        processResult = daeviler.controlScript().execute(resourcesTarget,
                "n", "n", "0", "\n"
        );
        assertTrue(processResult.output.get().contains("NO"));
    }

    @Test
    void echoResolverWindows() {
        OSType.ProcessResult processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS, "N", "N", "0", "0");
        assertTrue(processResult.output.get().contains("NO download for you"));
        processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS, "Y", "Y", "0", "0");
        assertTrue(processResult.output.get().contains("Ok dude"), processResult.output.get());
        assertTrue(processResult.output.get().contains("Ok lady"));
    }

}
