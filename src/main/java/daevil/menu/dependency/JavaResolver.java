package daevil.menu.dependency;

import daevil.Daevil;
import daevil.OSType;
import daevil.menu.BatchFileBuilder;
import daevil.menu.Menu;
import daevil.property.Property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static daevil.property.Property.get;

public class JavaResolver extends Resolver {

    private String _urlPrefix = "http://cfmlprojects.org/artifacts/oracle/jre/";
    public final Property<String> urlPrefix = get(() -> _urlPrefix).set(value -> _urlPrefix = value);

    private String _version = "latest";
    public final Property<String> version = get(() -> _version).set(value -> _version = value);

    private String _prompt = "JAVA_HOME not set, and Java not detected in path.  Download a JRE? [Y/n]";
    public final Property<String> prompt = get(() -> _prompt).set(value -> _prompt = value);

    private String _jreZip;
    public final Property<String> jreZip = get(() -> _jreZip).set(value -> _jreZip = value);

    public JavaResolver(Menu menu) {
        super(menu);
    }

    private String url() {
        return urlPrefix.get() + version.get() + '-';
    }


    @Override
    public String generate(OSType osType) {
        if (OSType.NIX.typeOf(osType)) {
            return generateNix();
        } else if (OSType.WINDOWS.typeOf(osType)) {
            return generateWindows();
        } else {
            throw new IllegalArgumentException("Don't know how to handle osType of " + osType);
        }
    }

    public String generateNix() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("urlPrefix", urlPrefix.get());
        map.put("prompt", prompt.get());
        return Daevil.render("daevil/script/nix/bash/JavaResolver", map);
    }

    private String generateWindows() {
        BatchFileBuilder builder = new Daevil().controlScript().batchFileBuilder();
        HashMap<String, Object> map = new HashMap<>();
        map.put("builder", builder);
        map.put("urlPrefix", urlPrefix.get());
        map.put("prompt", prompt.get());
        return Daevil.render("daevil/script/windows/batch/JavaResolver", map);
    }
}
