-- Safe to re-run: adds role column if an older schema exists without it
USE banking_system;

SET @has_role := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'banking_system'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'role'
);

SET @sql := IF(
    @has_role = 0,
    'ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''USER''',
    'SELECT ''role column already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
