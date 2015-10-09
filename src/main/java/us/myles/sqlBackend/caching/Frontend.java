package us.myles.sqlBackend.caching;

import us.myles.sqlBackend.api.backend.RecordData;

public class Frontend {
    private RecordData internalData;
    private Long lastCheck;

    public Frontend(RecordData internalData) {
        this.internalData = internalData;
    }

    public void init() {

    }

    public void load() {

    }

    // if this is false, record is deleted >.>
    public boolean ensureLatest(){
        return internal().verify(this, true);
    }

    protected RecordData internal() {
        return this.internalData;
    }

    public boolean verify() {
        return internal().verify(this, false);
    }

    public Long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Long lastCheck) {
        this.lastCheck = lastCheck;
    }

    public void inject(RecordData recordData) {
        this.internalData = recordData;
    }
}
