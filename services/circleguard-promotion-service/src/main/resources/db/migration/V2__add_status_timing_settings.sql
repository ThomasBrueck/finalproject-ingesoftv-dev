CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    unconfirmed_fencing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_threshold_seconds BIGINT NOT NULL DEFAULT 3600
);

ALTER TABLE system_settings 
ADD COLUMN IF NOT EXISTS mandatory_fence_days INTEGER NOT NULL DEFAULT 14,
ADD COLUMN IF NOT EXISTS encounter_window_days INTEGER NOT NULL DEFAULT 14;

-- Seed initial values if not present
INSERT INTO system_settings (unconfirmed_fencing_enabled, auto_threshold_seconds, mandatory_fence_days, encounter_window_days)
SELECT TRUE, 3600, 14, 14
WHERE NOT EXISTS (SELECT 1 FROM system_settings);
