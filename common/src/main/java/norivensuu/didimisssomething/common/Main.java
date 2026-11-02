package norivensuu.didimisssomething.common;

import java.nio.file.Path;

public class Main {
    public static final String MOD_ID = "didimisssomething";

    public static final String projectPath = !Path.of(System.getProperty("user.dir")).getFileName().toString().equals("didimisssomething") ? System.getProperty("user.dir") : Path.of(System.getProperty("user.dir")).getParent().toString();

    public static final int timeoutMinutes = 5;
}
