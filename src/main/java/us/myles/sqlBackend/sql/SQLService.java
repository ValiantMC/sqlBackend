package us.myles.sqlBackend.sql;

import org.skife.jdbi.v2.Binding;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.RewrittenStatement;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.sqlite.SQLiteDataSource;
import us.myles.sqlBackend.api.backend.RecordData;
import us.myles.sqlBackend.api.backend.RecordProvider;
import us.myles.sqlBackend.api.backend.RecordService;
import us.myles.sqlBackend.sql.dao.MapDataMapper;
import us.myles.sqlBackend.sql.dao.RecordBackend;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

public class SQLService implements RecordService {

    private String connectionAddr;
    private DBI dbi;
    private Map<String, SQLTable> tables = new ConcurrentHashMap<>();
    private Queue<SQLValueChange> pushQueue = new ConcurrentLinkedQueue<>();
    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private RecordBackend recordBackend;

    public SQLService(String connectionAddr) {
        this.connectionAddr = connectionAddr;
        ses.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                final Runnable r = pushQueue.poll();
                if (r == null) return;
                r.run();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
        connect();
    }

    private void connect() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(connectionAddr);
        this.dbi = new DBI(ds);
        // rewrite the statements >.>
        final StatementRewriter oldRewriter = dbi.getStatementRewriter();
        // bind <table> to table variable (because it's stupid...)
        dbi.setStatementRewriter(new StatementRewriter() {
            public RewrittenStatement rewrite(String s, Binding binding, StatementContext statementContext) {
                if (s.contains("<table>"))
                    s = s.replace("<table>", binding.forName("table").toString());
                if (s.contains("<key>"))
                    s = s.replace("<key>", binding.forName("key").toString().replace("'", ""));
                RewrittenStatement rs = oldRewriter.rewrite(s, binding, statementContext);
                return rs;
            }
        });
        // finally create the sql
        this.recordBackend = dbi.open(RecordBackend.class);
        this.recordBackend.getHandle().registerMapper(new MapDataMapper());
    }

    public DBI getDBI() {
        return dbi;
    }

    public RecordProvider getTable(String name) {
        if (!tables.containsKey(name))
            tables.put(name, new SQLTable(name, recordBackend, this));
        return tables.get(name);
    }

    @Override
    public void forceQueuedUpdates(String table, Integer id) {
        for (SQLValueChange svc : pushQueue) {
            if (svc.getTable().equals(table) && svc.getRecord() == id) {
                svc.run();
            }
        }
    }

    public void queue(String table, int id, String key, Object value, Long lastUpdated) {
        // look throug queue first
        for (SQLValueChange svc : pushQueue) {
            if (svc.getTable().equals(table) && svc.getRecord() == id && svc.getKey().equals(key)) {
                svc.setValue(value);
                return;
            }
        }
        this.pushQueue.add(new SQLValueChange(this, table, id, key, value, lastUpdated));
    }

    public void syncRecord(String table, RecordData rd) {
        int recordID = rd.getAsInt("id");
        for (SQLValueChange v : pushQueue) {
            if (v.getTable().equals(table) && v.getRecord() == recordID) {
                rd.put(v.getKey(), v.getValue());
            }
        }
    }

    public void shutdown() {
        System.out.println("Attempting to finish SQL Query Queue.");
        Long time = 0L;
        while (pushQueue.size() != 0) {
            if (time >= 10000) {
                System.out.println("Could not finish entire queue in time, attempting to finish current queries.");
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            time += 100;
        }
        System.out.println("Shutting down SQL...");
        ses.shutdownNow();
        try {
            ses.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        recordBackend.close();
        System.out.println("All done, woohoo!");
    }
}
