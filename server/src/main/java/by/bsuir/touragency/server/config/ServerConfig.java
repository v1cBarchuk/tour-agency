package by.bsuir.touragency.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Конфигурация сервера. Источники по возрастанию приоритета:
 * server.properties внутри jar → внешний файл → аргументы командной строки.
 */
public final class ServerConfig {

    private static final Logger LOG = Logger.getLogger(ServerConfig.class.getName());
    private static final String DEFAULT_FILE = "server.properties";

    private final Properties props;

    private ServerConfig(Properties props) {
        this.props = props;
    }

    public static ServerConfig load(String[] args) throws IOException {
        Properties props = new Properties();

        try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(DEFAULT_FILE)) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }

        Path external = Path.of(findArg(args, "config", DEFAULT_FILE));
        if (Files.exists(external)) {
            try (Reader reader = Files.newBufferedReader(external, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
            LOG.info("Загружен файл настроек: " + external.toAbsolutePath());
        }

        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] pair = arg.substring(2).split("=", 2);
                props.setProperty(pair[0], pair[1]);
            }
        }
        return new ServerConfig(props);
    }

    private static String findArg(String[] args, String name, String defaultValue) {
        String prefix = "--" + name + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return defaultValue;
    }

    public String get(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Не задан параметр конфигурации: " + key);
        }
        return value.trim();
    }

    // Перегрузка: числовой параметр со значением по умолчанию
    public int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        return value == null ? defaultValue : Integer.parseInt(value.trim());
    }

    public int getPort() { return getInt("server.port", 5555); }
    public int getThreads() { return getInt("server.threads", 20); }
    public String getDbUrl() { return get("db.url"); }
    public String getDbUser() { return get("db.user"); }
    public String getDbPassword() { return props.getProperty("db.password", ""); }
    public int getDbPoolSize() { return getInt("db.pool.size", 10); }
}
