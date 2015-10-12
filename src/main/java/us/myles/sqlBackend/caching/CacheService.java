package us.myles.sqlBackend.caching;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Update;
import us.myles.sqlBackend.api.backend.RecordData;
import us.myles.sqlBackend.api.backend.RecordProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CacheService<T extends Frontend> implements RecordProvider<T> {
    private RecordProvider<RecordData> provider;
    private Constructor<T> constructor;

    Cache<Integer, T> internalCache = (CacheBuilder.newBuilder())
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public CacheService(Class<T> type, RecordProvider<RecordData> provider) {
        this.provider = provider;
        try {
            this.constructor = type.getConstructor(RecordData.class);
        } catch (NoSuchMethodException e) {
            System.out.println("Oops, looks like something went seriously wrong with " + type);
            e.printStackTrace();
        }
    }

    public Optional<T> findRecord(final int id) {
        return obtain(id, Optional.fromNullable((RecordData) null));
    }

    @Override
    public T createRecord() {
        RecordData rawData = provider.createRecord();
        T t;
        try {
            t = constructor.newInstance(rawData);
            t.setLastCheck(System.currentTimeMillis());
            t.init();
            t.load();
            internalCache.put(t.internal().getAsInt("id"), t);
            return t;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public T createRecord(T toCreate) {
        RecordData rawData = provider.createRecord();
        rawData.putAll(toCreate.internal());
        toCreate.setLastCheck(System.currentTimeMillis());
        toCreate.init();
        toCreate.load();
        toCreate.inject(rawData);
        return toCreate;
    }

    private Optional<T> obtain(final int id, final Optional<RecordData> rd) {
        if (internalCache.getIfPresent(id) != null) {
            // ensure it's up to date
            if (!internalCache.getIfPresent(id).verify()) {
                internalCache.invalidate(id);
            }
        }
        try {
            return Optional.fromNullable(internalCache.get(id, new Callable<T>() {
                public T call() throws Exception {
                    // it's not cached.
                    if (!rd.isPresent()) {
                        Optional<RecordData> rawData = provider.findRecord(id);
                        if (!rawData.isPresent())
                            throw new Exception("Not found!!");
                        T t = constructor.newInstance(rawData.get());
                        t.setLastCheck(System.currentTimeMillis());
                        t.load();
                        return t;
                    }
                    T t = constructor.newInstance(rd.get());
                    t.setLastCheck(System.currentTimeMillis());
                    t.load();
                    return t;
                }
            }));
        } catch (ExecutionException e) {
            return Optional.absent();
        }
    }

    @Override
    public void deleteRecord(int id) {
        if (internalCache.getIfPresent(id) != null)
            internalCache.invalidate(id);
        provider.deleteRecord(id);
    }

    @Override
    public void updateRecord(int id, String key, Object value) {
        if (internalCache.getIfPresent(id) != null) {
            internalCache.getIfPresent(id).internal().put(key, value);
        } else {
            provider.updateRecord(id, key, value);
        }
    }

    @Override
    public boolean isRecord(int id) {
        boolean result = provider.isRecord(id);
        if (!result)
            if (internalCache.getIfPresent(id) != null)
                internalCache.invalidate(id);
        return result;
    }

    private List<T> obtainList(List<RecordData> records) {
        List<T> listFromCache = new ArrayList<>();
        for (final RecordData record : records) {
            final int id = record.getAsInt("id");
            listFromCache.add(obtain(id, Optional.fromNullable(record)).get());
        }
        return listFromCache;
    }

    @Override
    public List<T> findRecords(String key, Object value) {
        return obtainList(provider.findRecords(key, value));
    }

    @Override
    public Query createQuery(String initialQuery) {
        return provider.createQuery(initialQuery);
    }

    @Override
    public Update createUpdate(String initialQuery) {
        return provider.createUpdate(initialQuery);
    }

    @Override
    public List<T> findRecords(Query query) {
        return obtainList(provider.findRecords(query));
    }

    @Override
    public Optional<T> findRecord(String key, Object value) {
        Optional<RecordData> rd = provider.findRecord(key, value);
        if (rd.isPresent()) {
            return obtain(rd.get().getAsInt("id"), rd);
        } else {
            return Optional.absent();
        }
    }

    @Override
    public List<T> findAll() {
        return obtainList(provider.findAll());
    }

    public void shutdown() {
        provider.shutdown();
    }

    @Override
    public void forceQueuedUpdates(T t) {
        provider.forceQueuedUpdates(t.internal());
    }

    @Override
    public void bulkUpdate(int record, Map<String, Object> map) {
        provider.bulkUpdate(record, map);
    }
}
