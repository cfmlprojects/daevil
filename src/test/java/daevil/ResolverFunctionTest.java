package daevil;


import daevil.menu.BatchFileBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;


class ResolverFunctionTest extends AbstractWorkTest {


    @Test
    void md5File() throws IOException {
        Path resourcesTarget = Paths.get(workDir.toString() + "/functionTest.bat").toAbsolutePath();
        Path capturedOutput = Paths.get(workDir.toString() + "/typescript").toAbsolutePath();
        String filepath = Paths.get("src/test/resources/md5/file.txt").toAbsolutePath().toString();
        String filemd5path = Paths.get("src/test/resources/md5/file.txt.md5").toAbsolutePath().toString();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("md5_file", filepath, filemd5path));
        bat.write(resourcesTarget);
        OSType.ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
//        System.out.println(processResult.output.get());
        if (processResult.exitCode.get() > 0) {
            Files.lines(capturedOutput).forEach(line -> System.out.println(line.replaceAll("\\p{Cc}", "")));
        }
        assertTrue(processResult.output.get().contains("MD5 MATCH: True"));
        bat = new BatchFileBuilder();
        bat.append(bat.call("md5_file", filepath, filemd5path + ".bad"));
        bat.write(resourcesTarget);
        processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
        assertTrue(processResult.output.get().contains("MD5 MISMATCH: False"));

    }

    @Test
    void unzipFile() throws IOException {
        Path resourcesTarget = Paths.get(workDir.toString() + "/functionTest.bat").toAbsolutePath();
        Path capturedOutput = Paths.get(workDir.toString() + "/typescript").toAbsolutePath();
        String filepath = Paths.get("src/test/resources/md5/file.zip").toAbsolutePath().toString();
        String filemd5path = Paths.get(workDir.toString() + "/unzipped").toAbsolutePath().toString();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("file_unzip", filepath, filemd5path));
        bat.write(resourcesTarget);
        OSType.ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
//        System.out.println(processResult.output.get());
        if (processResult.exitCode.get() > 0) {
            Files.lines(capturedOutput).forEach(line -> System.out.println(line.replaceAll("\\p{Cc}", "")));
        }
        assertTrue(processResult.output.get().contains("Unzipped"));
//        bat = new BatchFileBuilder();
//        bat.append(bat.call("file_unzip",filepath, filemd5path+".bad"));
//        bat.write(resourcesTarget);
//        processResult = OSType.WINDOWS.execute(resourcesTarget.toString(),null,null,10,false);
//        assertTrue(processResult.output.get().contains("MD5 MISMATCH: False"));

    }

    @Test
    void md5url_File() {
        final String url = "http://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        Path resourcesTarget = Paths.get(workDir.toString() + "/functionTest.bat").toAbsolutePath();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("md5url_file", url, resourcesTarget.getParent().toString() + "/zip.jar"));
        bat.write(resourcesTarget);
        OSType.ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
//        System.out.println(processResult.output.get());
        assertTrue(processResult.output.get().contains("MD5 MATCH: True"));
    }

    @Test
    void readkeys() {
        Path resourcesTarget = Paths.get(workDir.toString() + "/functionTest.bat").toAbsolutePath();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("readkeys"));
        bat.write(resourcesTarget);
        OSType.ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), "0");
    }

}
