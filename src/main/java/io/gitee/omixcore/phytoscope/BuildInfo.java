package io.gitee.omixcore.phytoscope;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build-time version metadata injected by the Spring Boot {@code build-info} goal.
 *
 * <p>Maven generates {@code META-INF/build-info.properties} during the
 * {@code generate-resources} phase, so both {@code mvn test} and
 * {@code mvn package} see the real project version from the pom. When the
 * resource is absent — for example when running against sources that were
 * compiled without the Maven output on the classpath — the value falls back
 * to {@link #FALLBACK_VERSION}.</p>
 */
public final class BuildInfo {

    /** Version reported when no {@code build-info.properties} resource is available. */
    public static final String FALLBACK_VERSION = "0.2.0";

    private static final String RESOURCE = "/META-INF/build-info.properties";

    private static volatile String cached;

    private BuildInfo() {
    }

    /**
     * Returns the application version from build metadata.
     *
     * @return the {@code build.version} value, or {@link #FALLBACK_VERSION} when
     *         the metadata is missing or malformed
     */
    public static String version() {
        String value = cached;
        if (value == null) {
            value = load();
            cached = value;
        }
        return value;
    }

    private static String load() {
        try (InputStream in = BuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return FALLBACK_VERSION;
            }
            Properties properties = new Properties();
            properties.load(in);
            return versionFrom(properties);
        } catch (IOException ex) {
            return FALLBACK_VERSION;
        }
    }

    /** Extracts {@code build.version} from a properties object; package-visible for tests. */
    static String versionFrom(Properties properties) {
        if (properties == null) {
            return FALLBACK_VERSION;
        }
        String version = properties.getProperty("build.version");
        return version == null || version.isBlank() ? FALLBACK_VERSION : version.trim();
    }
}
