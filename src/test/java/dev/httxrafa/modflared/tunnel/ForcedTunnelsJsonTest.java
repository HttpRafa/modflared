package dev.httxrafa.modflared.tunnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class ForcedTunnelsJsonTest {

    @Test
    public void testForcedTunnelsJsonIsValidArrayOfStrings() {
        JsonElement element = new JsonParser().parse(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("forced_tunnels.json")));
        assertTrue("forced_tunnels.json must be a JSON array", element.isJsonArray());
        JsonArray array = element.getAsJsonArray();
        for (JsonElement entry : array) {
            assertTrue("Each entry must be a string", entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString());
        }
    }
}
