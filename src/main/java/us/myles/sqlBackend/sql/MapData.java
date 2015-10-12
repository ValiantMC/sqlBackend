package us.myles.sqlBackend.sql;

import com.google.common.base.Optional;
import us.myles.sqlBackend.api.backend.RecordData;
import us.myles.sqlBackend.caching.Frontend;

import java.sql.Timestamp;

public class MapData extends RecordData {
    // just incase of serial stuf
    private transient SQLTable table;

    @Override
    public void handlePut(String key, Object value) {
        Long time = System.currentTimeMillis();
        directPut("lastUpdated", new Timestamp(time));
        getBackendTable().queueUpdateRecord(getAsInt("id"), key, value, time);
    }

    @Override
    public void pushUpdates() {
        getBackendTable().forceQueuedUpdates(this);
    }

    @Override
    public boolean verify(Frontend frontend, boolean force) {
        // checks internal timestamp to make sure it's the latest.
        if (frontend.getLastCheck() != null && !force) {
            if ((System.currentTimeMillis() - frontend.getLastCheck()) < 500) {
                return true;
            }
        }
        frontend.setLastCheck(System.currentTimeMillis());
        // verify it :))
        Optional<RecordData> rd = getBackendTable().findRecord(getAsInt("id"));
        if (rd.isPresent()) {
            // should be present >.> otherwise it was deleted #awkward
            if (rd.get().is("lastUpdated") && is("lastUpdated") || force) {
                if (((Timestamp)rd.get().get("lastUpdated")).getTime() > ((Timestamp)get("lastUpdated")).getTime() || force) {
                    // replace internal
                    frontend.inject(rd.get());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public MapData attach(SQLTable SQLTable) {
        this.table = SQLTable;
        return this;
    }

    public SQLTable getBackendTable() {
        return table;
    }
}
