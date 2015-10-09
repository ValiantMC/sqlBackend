package us.myles.sqlBackend.sql;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Update;
import us.myles.sqlBackend.api.backend.RecordData;
import us.myles.sqlBackend.api.backend.RecordProvider;
import us.myles.sqlBackend.sql.dao.RecordBackend;

import java.util.List;

public class SQLTable implements RecordProvider<RecordData> {
    private final String table;
    private final RecordBackend backend;
    private final SQLService service;
    private final Object lock = new Object();

    public SQLTable(String name, RecordBackend recordBackend, SQLService sqlService) {
        this.table = name;
        this.backend = recordBackend;
        this.service = sqlService;
    }

    @Override
    public Optional<RecordData> findRecord(int id) {
        synchronized (lock) {
            MapData rd = backend.findRecord(table, id);
            if (rd != null) {
                rd.attach(this);
                service.syncRecord(table, rd);
            }
            return Optional.fromNullable((RecordData) rd);
        }
    }

    @Override
    public RecordData createRecord() {
        int id = backend.createRecord(table);
        MapData md = backend.findRecord(table, id);
        md.attach(this);
        return md;
    }

    @Override
    public void deleteRecord(int id) {
        synchronized (lock) {
            backend.deleteRecord(table, id);
        }
    }

    @Override
    public void updateRecord(int id, String key, Object value) {
        synchronized (lock) {
            backend.updateRecord(table, id, key, value);
        }
    }

    @Override
    public boolean isRecord(int id) {
        synchronized (lock) {
            return backend.isRecord(table, id);
        }
    }

    @Override
    public List<RecordData> findRecords(String key, Object value) {
        synchronized (lock) {
            List<MapData> data = backend.findRecords(table, key, value);
            for (MapData md : data) {
                md.attach(this);
                service.syncRecord(table, md);
            }
            return (List<RecordData>) (List<?>) data;
        }
    }

    @Override
    public Query<RecordData> createQuery(String initialQuery) {
        return (Query<RecordData>) (Query<?>) backend.getHandle().createQuery(initialQuery).mapTo(MapData.class).bind("table", table);
    }

    @Override
    public Update createUpdate(String initialQuery) {
        return backend.getHandle().createStatement(initialQuery).bind("table", table);
    }

    @Override
    public List<RecordData> findRecords(Query query) {
        return (List<RecordData>) (List<?>) query.list();
    }

    @Override
    public Optional<RecordData> findRecord(String key, Object value) {
        synchronized (lock) {
            MapData rd = backend.findRecord(table, key, value);
            if (rd != null) {
                rd.attach(this);
                service.syncRecord(table, rd);
            }
            return Optional.fromNullable((RecordData) rd);
        }
    }

    @Override
    public List<RecordData> findAll() {
        synchronized (lock) {
            List<MapData> data = backend.findAll(table);
            for (MapData md : data) {
                md.attach(this);
                service.syncRecord(table, md);
            }
            return (List<RecordData>) (List<?>) data;
        }
    }

    @Override
    public void shutdown() {
        service.shutdown();
    }

    @Override
    public void forceQueuedUpdates(RecordData rd) {
        service.forceQueuedUpdates(table, rd.getAsInt("id"));
    }

    public void queueUpdateRecord(int id, String key, Object value, Long lastUpdated) {
        service.queue(table, id, key, value, lastUpdated);
    }

    public SQLService getService() {
        return this.service;
    }
}
