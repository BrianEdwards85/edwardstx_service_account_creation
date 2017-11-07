(ns us.edwardstx.service.sac.rabbit-api
  (:require [cheshire.core :as json]
            [clj-crypto.core :as crypto]
            [aleph.http :as http]
            [manifold.deferred :as d]
            [byte-streams :as bs]))

(defn create-url [conf]
  (let [rabbit-conf (-> conf :conf :rabbit)
        username (:username rabbit-conf)
        password (:password rabbit-conf)
        host (:host rabbit-conf)]
    (str "https://" username ":" password "@" host)))

(defn create-user [conf user pass]
  (let [body {:password pass :tags "" :username user}
        url (str (create-url conf) "/rabbitmq/api/users/" user)]
    (d/chain
     (http/put url {:body (json/generate-string body)})
     #(if (= 201 (:status %1))
        user
        (throw (Exception. (format "Unable to create %s account: %s" user (-> %1 :body bs/to-string))))))))

(defn set-acl [conf user]
  (let [vhost (-> conf :conf :rabbit :vhost)
        body {:configure ".*" :read ".*" :username user :vhost vhost :write ".*"}
        url (str  (create-url conf) "/rabbitmq/api/permissions/" vhost "/" user)]
    (d/chain
     (http/put url {:body (json/generate-string body)})
     #(if (= 201 (:status %1))
        user
        (throw (Exception. (format "Unable to set ACL for %s account: %s" user (-> %1 :body bs/to-string))))))))


