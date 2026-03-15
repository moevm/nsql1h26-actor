db = db.getSiblingDB('nosql_db');
db.createUser({
  user: 'admin',
  pwd: 'pass',
  roles: [{ role: 'readWrite', db: 'nosql_db' }]
});