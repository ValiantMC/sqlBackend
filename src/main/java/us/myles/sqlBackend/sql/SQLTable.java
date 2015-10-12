package us.myles.sqlBackend.sql;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.PreparedBatchPart;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Update;
import us.myles.sqlBackend.api.backend.RecordData;
import us.myles.sqlBackend.api.backend.RecordProvider;
import us.myles.sqlBackend.caching.DummyRecordData;
import us.myles.sqlBackend.sql.dao.RecordBackend;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

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
        MapData md = new MapData();
        md.directPut("id", id);
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

    @Override
    public void bulkUpdate(int record, Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("update <table> set ");
        for (String key : map.keySet()) {
            if (!sb.toString().equals("update <table> set ")) {
                sb.append(", ");
            }
            sb.append("" + key + " = ? ");

        }
        sb.append("where id = ?;");
        Update s = createUpdate(sb.toString());
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            s.bind(i++, entry.getValue());
        }
        s.bind(i, record);

        s.execute();
    }

    public void queueUpdateRecord(int id, String key, Object value, Long lastUpdated) {
        service.queue(table, id, key, value, lastUpdated);
    }

    public SQLService getService() {
        return this.service;
    }
}
