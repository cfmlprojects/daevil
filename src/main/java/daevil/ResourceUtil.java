package daevil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceUtil {
    static void copyResourcesReplaceTokens(String path, Path dest, Map<String, String> tokens, boolean recurse, String include, String replaceAll) {
        if (!dest.toFile().exists() && !dest.toFile().mkdirs()) {
            Daevil.log.error("Could not create directory: " + dest.toAbsolutePath());
        }
        try {
            Consumer<Path> pathConsumer = (resourcePath) -> {
                String fileName = resourcePath.toString().substring(resourcePath.toString().lastIndexOf('/') + 1);
                if (!Files.isDirectory(resourcePath) && fileName.matches(include)) {
                    copyPathReplaceTokens(resourcePath, Paths.get(dest.toString(), fileName), tokens);
                } else if (Files.isDirectory(resourcePath) && recurse) {
                    Path childPath = Paths.get(dest.toString(), fileName);
                    if (!Files.exists(childPath) && !childPath.toFile().mkdirs()) {
                        Daevil.log.error("Could not create directory: " + childPath.toAbsolutePath());
                    }
                    copyResourcesReplaceTokens(path + '/' + fileName, childPath, tokens, recurse, include, replaceAll);
                }
            };
            processResource(Daevil.class.getResource(path).toURI(), patth -> {
                try (Stream<Path> stream = Files.list(patth)) {
                    stream.forEach(pathConsumer);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void copyResources(String path, Path dest, String include, String replaceAll) {
        if (!dest.toFile().exists() && !dest.toFile().mkdirs()) {
            Daevil.log.error("Could not create directory: " + dest.toAbsolutePath());
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        URL pathResource = Daevil.class.getResource(path);
        if (pathResource == null) {
            throw new IllegalArgumentException("path not found: " + path);
        }
        URI uri;
        try {
            uri = pathResource.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("unable to get URI: " + e.getMessage());
        }
        Consumer<Path> pathConsumer = (resourcePath) -> {
            String fileName = resourcePath.toString().substring(resourcePath.toString().lastIndexOf('/') + 1);
            if (!Files.isDirectory(resourcePath) && fileName.matches(include)) {
                if (replaceAll != null && !replaceAll.trim().isEmpty()) {
                    fileName = fileName.replaceAll(replaceAll.split(",")[0], replaceAll.split(",")[1]);
                }
                Path destPath = Paths.get(dest.toString(), fileName);
                if (Files.exists(destPath)) {
                    Daevil.log.error("File exists: " + destPath.toAbsolutePath());
                } else {
                    Daevil.log.debug("Copying " + resourcePath + " to " + destPath.toAbsolutePath());
                    try {
                        Files.copy(resourcePath, destPath);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (fileName.endsWith(".sh")) {
                        ResourceUtil.markExecutable(destPath);
                    }
                }
            }
        };
        try {
            processResource(uri, resourcePath -> {
                try (Stream<Path> stream = Files.list(resourcePath)) {
                    stream.forEach(pathConsumer);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void copyPathReplaceTokens(Path source, Path dest, Map<String, String> tokens) {
        Daevil.log.debug("Copy/Replacing " + source + " to " + dest.toAbsolutePath());
        try {
            try (Stream<String> lines = Files.lines(source)) {
                List<String> replaced = lines
                        .map(line -> replaceTokens(line, tokens))
                        .collect(Collectors.toList());
                Files.write(dest, replaced);
                if (dest.toString().endsWith(".sh")) {
                    ResourceUtil.markExecutable(dest);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String replaceTokens(String string, Map<String, String> tokens) {
        final List<String> msg = Arrays.asList(string);
        "@#".chars().forEach(delimiter -> {
            tokens.forEach((key, value) ->
                    msg.set(0, msg.get(0).replace((char) delimiter + key + (char) delimiter, value = value == null ? "" : value))
            );
        });
        return msg.get(0);

    }

    private static void processResource(URI uri, Daevil.IOConsumer<Path> action) throws IOException {
        try {
            Path p = Paths.get(uri);
            action.accept(p);
        } catch (FileSystemNotFoundException ex) {
            try (FileSystem fs = FileSystems.newFileSystem(
                    uri, Collections.emptyMap())) {
                Path p = fs.provider().getPath(uri);
                action.accept(p);
            }
        }
    }

    public static String getInputAsString(InputStream is) {
        try (java.util.Scanner s = new java.util.Scanner(is)) {
            try (Scanner useDelimiter = s.useDelimiter("\\A")) {
                return useDelimiter.hasNext() ? s.next() : "";
            }
        }
    }

    public static void markExecutable(Path path) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        try {
            Daevil.log.debug("Marking executable: " + path);
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
