package us.myles.sqlBackend.sql;

import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
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
    Thread ses;
    private RecordBackend recordBackend;

    public SQLService(String connectionAddr) {
        this.connectionAddr = connectionAddr;
        ses = new Thread() {
            public void run() {
                while (true) {
                    final Runnable r = pushQueue.poll();
                    if (r == null) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {}
                    } else {
                        r.run();
                    }
                }
            }
        };
        ses.setDaemon(true);
        ses.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
        connect();
    }

    private void connect() {
        //init sql
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Warning, no SQL found...");
        }
        if (connectionAddr.startsWith("jdbc:sqlite")) {
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl(connectionAddr);
            this.dbi = new DBI(ds);
        } else {
            BasicDataSource bds = new BasicDataSource();
            bds.setUrl(connectionAddr);
            bds.setInitialSize(5);
            this.dbi = new DBI(bds);
        }

        // rewrite the statements >.>
        final StatementRewriter oldRewriter = dbi.getStatementRewriter();
        // bind <table> to table variable (because it's stupid...)
        dbi.setStatementRewriter(new StatementRewriter() {
            public RewrittenStatement rewrite(String s, Binding binding, StatementContext statementContext) {
                if (s.contains("<table>"))
                    s = s.replace("<table>", binding.forName("table").toString().replace("'", ""));
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
            if (svc.getTable().equals(table) && svc.getRecord() == id) {
                svc.getMap().put(key, value);
                return;
            }
        }
        this.pushQueue.add(new SQLValueChange(this, table, id, key, value, lastUpdated));
    }

    public void syncRecord(String table, RecordData rd) {
        int recordID = rd.getAsInt("id");
        for (SQLValueChange v : pushQueue) {
            if (v.getTable().equals(table) && v.getRecord() == recordID) {
                for (Map.Entry<String, Object> entry : v.getMap().entrySet()) {
                    rd.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void shutdown() {
        if (recordBackend != null) {
            ses.interrupt();
            System.out.println("Attempting to finish SQL Query Queue. (" + pushQueue.size() + ")");
            for (SQLValueChange v : pushQueue) {
                v.run();
            }
            System.out.println("Shutting down SQL...");
            recordBackend.close();
            System.out.println("All done, woohoo!");
        }
    }
}
