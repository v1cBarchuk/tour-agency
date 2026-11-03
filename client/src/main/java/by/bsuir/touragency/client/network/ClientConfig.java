package by.bsuir.touragency.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Настройки клиента: client.properties в jar → внешний файл → аргументы.
 */
public final class ClientConfig {

    private static final String FILE = "client.properties";

    private final Properties props;

    private ClientConfig(Properties props) {
        this.props = props;
    }

    public static ClientConfig load(List<String> args) throws IOException {
        Properties props = new Properties();
        try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        Path external = Path.of(FILE);
        if (Files.exists(external)) {
            try (Reader reader = Files.newBufferedReader(external, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        }
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] pair = arg.substring(2).split("=", 2);
                props.setProperty(pair[0], pair[1]);
            }
        }
        return new ClientConfig(props);
    }

    public String getHost() {
        return props.getProperty("server.host", "localhost").trim();
    }

    public int getPort() {
        return Integer.parseInt(props.getProperty("server.port", "5555").trim());
    }
}
