(ns us.edwardstx.service.sac.data.user
  (:require [hugsql.core :as hugsql]
            [us.edwardstx.common.db :refer [get-connection] :as db]
            [us.edwardstx.common.spec :as specs]
            [clojure.spec.alpha :as s]
            [manifold.deferred :as d]))

(hugsql/def-db-fns "sql/user.sql")

(defn create-user [db user pass]
  (d/future
    (create-user-sql (get-connection db) {:user [user] :pass [(str "'" pass "'")]})))
