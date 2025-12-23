USE bizflow_db;
DELETE FROM users WHERE username IN ('admin', 'test', 'owner');
