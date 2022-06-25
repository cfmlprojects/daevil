package daevil;


import com.google.common.collect.ImmutableMap;
import daevil.menu.BatchFileBuilder;
import daevil.term.ProcessResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static daevil.iconexe.IconExe.copyFile;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ResolverFunctionTest extends AbstractWorkTest {


    @Test
    void md5File() throws IOException {
        Path resourcesTarget = Paths.get(workDir + "/functionTest.bat").toAbsolutePath();
        String fileName = "file.txt";
        String fileMd5Name = "file.txt.md5";
        String fileMd5BadName = fileMd5Name + ".bad";
        copyFile("src/test/resources/md5/" + fileName, workDir + "/" + fileName);
        copyFile("src/test/resources/md5/" + fileMd5Name, workDir + "/" + fileMd5Name);
        System.out.println("created " + workDir + fileMd5Name);
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("md5_file", fileName, fileMd5Name));
        bat.write(resourcesTarget);
        ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
        assertTrue(processResult.output.get().contains("MD5 MATCH: True"));

        bat = new BatchFileBuilder();
        copyFile("src/test/resources/md5/" + fileMd5BadName, workDir + "/" + fileMd5BadName);
        bat.append(bat.call("md5_file", fileName, fileMd5Name + ".bad"));
        bat.write(resourcesTarget);
        processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
        assertTrue(processResult.output.get().contains("MD5 MISMATCH: False"));

    }

    @Test
    void unzipFile() throws IOException {
        Path resourcesTarget = Paths.get(workDir + "/functionTest.bat").toAbsolutePath();
        String filepath = Paths.get("src/test/resources/md5/file.zip").toAbsolutePath().toString();
        String filemd5path = Paths.get(workDir + "/unzipped").toAbsolutePath().toString();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("file_unzip", filepath, filemd5path));
        bat.write(resourcesTarget);
        ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
        if (processResult.exitCode.get() > 0) {
            System.out.println(processResult.output.get());
//            Files.lines(capturedOutput).forEach(line -> System.out.println(line.replaceAll("\\p{Cc}", "")));
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
        final String url = "https://repo1.maven.org/maven2/com/fizzed/rocker-runtime/0.24.0/rocker-runtime-0.24.0-sources.jar";
        Path resourcesTarget = Paths.get(workDir + "/functionTest.bat").toAbsolutePath();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("md5url_file", url, "%TEMP%/zip.jar"));
        bat.write(resourcesTarget);
        ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), null, null, 10, false);
        System.out.println(processResult.output.get());
        assertTrue(processResult.output.get().contains("MD5 MATCH: True"));
    }

    @Test
    void readkeys() {
        Path resourcesTarget = Paths.get(workDir + "/functionTest.bat").toAbsolutePath();
        BatchFileBuilder bat = new BatchFileBuilder();
        bat.append(bat.call("readkeys"));
        bat.write(resourcesTarget);
        Map<String, String> input = ImmutableMap.ofEntries(entry("Option number:", "0"));
        ProcessResult processResult = OSType.WINDOWS.execute(resourcesTarget.toString(), input);
    }

}
