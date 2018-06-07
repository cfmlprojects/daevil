package daevil;


import daevil.menu.BatchFileBuilder;
import daevil.menu.dependency.Resolver;
import daevil.script.nix.bash.Md5UrlUnzipResolver;
import daevil.script.windows.batch.BatMd5UrlUnzipResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ResolverMd5ConfirmationTest extends AbstractWorkTest {

    private Daevil daeviler;
    private Path resourcesTarget;


    @BeforeEach
    void beforeEach() {
        final String url = "http://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        resourcesTarget = Paths.get(workDir.toString() + "/resolver");
        daeviler = new Daevil();
        BatchFileBuilder batBuilder = daeviler.controlScript().batchFileBuilder();
        daeviler.controlScript().resolver(new Resolver(daeviler.controlScript()) {
            @Override
            public String generate(OSType osType) {
                if (osType.typeOf(OSType.WINDOWS)) {
                    return BatMd5UrlUnzipResolver.template(batBuilder, url,
                            resourcesTarget.toAbsolutePath().toString() + "/rockerwin",
                            "Download?").render().toString();
                } else {
                    return Md5UrlUnzipResolver.template(url,
                            resourcesTarget.toAbsolutePath().toString() + "/rockernix",
                            "Download?").render().toString();
                }
            }
        });
        daeviler.dest.set(Paths.get("."));
        daeviler.controlScript().generate(OSType.WINDOWS, resourcesTarget);
        daeviler.controlScript().generate(OSType.NIX, resourcesTarget);

    }


    @Test
    void md5ResolverNix() {
        OSType.ProcessResult processResult = daeviler.controlScript().execute(resourcesTarget,
                "y", "n", "0", "\n"
        );
        assertTrue(processResult.output.get().contains("Getting"));

        processResult = daeviler.controlScript().execute(resourcesTarget,
                "n", "n", "0", "\n"
        );
        assertFalse(processResult.output.get().contains("Getting"));
    }

    @Test
    void md5ResolverWindows() {
        OSType.ProcessResult processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS,
                "y", "0", "\n", "0");
        assertFalse(processResult.output.get().contains("Failure"));
        assertTrue(processResult.output.get().contains("Finished Download"));
//        processResult = daeviler.controlScript().execute(resourcesTarget, OSType.WINDOWS, "Y", "Y","0", "0");
//        assertTrue(processResult.output.get().contains("Ok dude"),processResult.output.get());
//        assertTrue(processResult.output.get().contains("Ok lady"));
    }

}
