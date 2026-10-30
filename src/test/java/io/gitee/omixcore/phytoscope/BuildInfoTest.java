package io.gitee.omixcore.phytoscope;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildInfoTest {

    @Test
    void versionMatchesPomProjectVersion() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        Matcher matcher = Pattern
                .compile("<artifactId>phytoscope</artifactId>\\s*<version>([^<]+)</version>")
                .matcher(pom);
        assertTrue(matcher.find(), "pom.xml must declare the phytoscope artifact version");
        assertEquals(matcher.group(1), BuildInfo.version());
    }

    @Test
    void fallsBackWhenBuildMetadataIsMissing() {
        assertEquals(BuildInfo.FALLBACK_VERSION, BuildInfo.versionFrom(new Properties()));

        Properties blank = new Properties();
        blank.setProperty("build.version", "   ");
        assertEquals(BuildInfo.FALLBACK_VERSION, BuildInfo.versionFrom(blank));
    }
}
