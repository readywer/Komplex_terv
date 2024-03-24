INSERT INTO client (id, name, username, password, email, role) VALUES
  (1, 'Nagy Sándor', 'nagys', 'ns-secret', 'nagy.sandor@gmail.com', 'ADMIN'),
  (2, 'Horváth Ádám', 'horvatha', 'ha-secret', 'horvath.adam@gmail.com', 'CLIENT'),
  (3, 'Kovács Péter', 'kovacsp', 'kp-secret', 'kovacs.peter@gmail.com', 'CLIENT'),
  (4, 'Kiss István', 'kissi', 'ki-secret', 'kiss.istvan@gmail.com', 'CLIENT');
ALTER TABLE client ALTER COLUMN id RESTART WITH (SELECT MAX(ID) FROM client) + 1;
