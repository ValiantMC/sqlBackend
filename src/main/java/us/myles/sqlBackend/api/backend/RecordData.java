package us.myles.sqlBackend.api.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import us.myles.sqlBackend.caching.Frontend;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RecordData extends ConcurrentHashMap<String, Object> {

    private static Gson internalGson = new GsonBuilder().create();

    /* Simple utiliy methods */

    public boolean is(String key) {
        return containsKey(key);
    }

    public Integer getAsInt(String key) {
        return (Integer) get(key);
    }

    public String getAsString(String key) {
        return (String) get(key);
    }

    public Double getAsDouble(String key) {
        return (Double) get(key);
    }

    public Long getAsLong(String key) {
        return (Long) get(key);
    }

    public Float getAsFloat(String key) {
        return (Float) get(key);
    }

    public Short getAsShort(String key) {
        return (Short) get(key);
    }

    /* Custom handler for UUIDs */

    public UUID getAsUUID(String key) {
        return UUID.fromString(getAsString(key));
    }

    public Object put(String key, UUID value) {
        return put(key, value.toString());
    }

    /* Custom handler for JSON (Because who doesn't love it ;)) */

    public JsonElement getAsJSON(String key) {
        return internalGson.fromJson(getAsString(key), JsonElement.class);
    }

    public Object put(String key, JsonElement value) {
        return put(key, internalGson.toJson(value));
    }

    /* Handlers for lower */

    public abstract void handlePut(String key, Object value);

    public abstract void pushUpdates();

    /* Dispatcher */

    @Override
    public void putAll(Map<? extends String, ?> m) {
        Iterator<? extends String> iterator = m.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            put(key, m.get(key));
        }
    }

    // allow raw access
    public Object directPut(String key, Object value) {
        return super.put(key, value);
    }

    @Override
    public Object put(String key, Object value) {
        // Put Handler
        handlePut(key, value);
        // Actually Put
        return super.put(key, value);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        if (!is(key))
            put(key, value);
        return value;
    }

    public abstract boolean verify(Frontend frontend, boolean force);
}
