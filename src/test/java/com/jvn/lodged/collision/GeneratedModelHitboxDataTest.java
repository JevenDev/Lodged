package com.jvn.lodged.collision;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeneratedModelHitboxDataTest {
    @Test
    void bundledDataCoversTheVanillaModelRoster() throws Exception {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("assets/lodged/model_hitboxes.tsv");
        assertNotNull(stream);

        int cuboids = 0;
        Set<String> models = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                assertTrue(fields.length == 13, "Malformed generated model row");
                cuboids++;
                models.add(fields[0]);
            }
        }

        assertTrue(cuboids > 1_400);
        assertTrue(models.size() > 160);
        assertTrue(models.containsAll(Set.of(
                "allay", "armadillo", "camel", "cow", "ender_dragon",
                "guardian", "horse", "sniffer", "warden", "wither")));
    }
}
