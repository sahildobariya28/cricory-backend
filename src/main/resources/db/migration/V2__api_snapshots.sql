CREATE TABLE api_snapshots (
    snapshot_key VARCHAR(64) PRIMARY KEY,
    payload TEXT NOT NULL,
    source VARCHAR(64) NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_snapshots_fetched_at ON api_snapshots(fetched_at);
