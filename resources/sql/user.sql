-- :name create-user-sql
-- :command :execute
-- :result :raw
CREATE USER :i*:user WITH
       LOGIN
       NOSUPERUSER
       NOCREATEDB
       NOCREATEROLE
       INHERIT
       NOREPLICATION
       CONNECTION LIMIT -1
       PASSWORD :i*:pass
