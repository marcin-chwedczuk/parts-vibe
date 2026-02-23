package app.partsvibe.testsupport.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JsonAssertions {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private JsonAssertions() {}

    public static void assertJsonEquals(String expectedJson, String actualJson) {
        try {
            JsonNode expected = OBJECT_MAPPER.readTree(expectedJson);
            JsonNode actual = OBJECT_MAPPER.readTree(actualJson);
            if (!expected.equals(actual)) {
                throw new AssertionError("JSON mismatch.%nExpected:%n%s%nActual:%n%s"
                        .formatted(
                                OBJECT_MAPPER.writeValueAsString(expected), OBJECT_MAPPER.writeValueAsString(actual)));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid JSON used in assertion.", ex);
        }
    }
}
