(ns us.edwardstx.service.sac.orchestrator
  (:require [com.stuartsierra.component :as component]
            [us.edwardstx.service.sac.data.user :refer [create-user]]
            [us.edwardstx.service.sac.rabbit-api :as rabbit]
            [us.edwardstx.common.events :refer [publish-event]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [replace]]
            [crypto.random :as rdm]
            [manifold.deferred :as d]
            ))

(defn generate-passwd []
  (replace
   (rdm/base64 40)
   #"[+/=]"
   (fn [_] "")))

(defn create-db-account [db account passwd types]
  (if (types "db")
    (create-user db account passwd)
    (d/success-deferred true)))

(defn create-rabbit-account [conf account passwd types]
  (if (types "rabbit")
    (d/chain
     (rabbit/create-user conf account passwd)
     #(rabbit/set-acl conf %))
    (d/success-deferred true)))

(defn create-account [{:keys [conf db events]} account types]
  (let [passwd (str account "+" (generate-passwd))
        types (set types)]

    (->
     (create-db-account db account passwd types)
     (d/zip (create-rabbit-account conf account passwd types))
     (d/chain (fn [_] (log/info (format "Created service accounts for %s" account))
                (publish-event events "account.created" {:account account :types types})
                passwd))
     (d/catch #(let [msg (format "Uable to create service %s accounts for %s: %s"
                                   (reduce (fn [l r] (str l ", " r)) types)
                                   account (.getMessage %1))]
                   (log/error %1 msg)
                   (throw (Exception. msg %1)))))))

(defrecord Orchestrator [conf db events]
  component/Lifecycle

  (start [this]
    this)

  (stop [this]
    this)
)

(defn new-orchestrator []
  (component/using
   (map->Orchestrator {})
   [:db :conf :events]))

