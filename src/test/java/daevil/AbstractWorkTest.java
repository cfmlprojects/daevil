package daevil;


import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;


class AbstractWorkTest {

    public static final Path workDir = Paths.get("src/test/resources/tmp/");

    public static boolean recreateWorkDir() {
        if (Files.exists(workDir)) {
            try {
                Files.walk(workDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return workDir.toFile().mkdir();
    }

    @BeforeAll
    static void beforeAll() throws IOException {
        System.setProperty(Daevil.LOGLEVEL_PROPERTY, "trace");
        recreateWorkDir();
    }

}
