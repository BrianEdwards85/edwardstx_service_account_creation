(ns us.edwardstx.service.sac.handler
  (:require [us.edwardstx.service.sac.orchestrator :as orchestrator]
            [us.edwardstx.common.tasks :as tasks]
            [clojure.tools.logging      :as log]
            [com.stuartsierra.component :as component]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

(defn account-create-handlers [orchestrator {:keys [body response] :as m}]
  (let [account (:account body)
        types (:types body)]
     (->
      (orchestrator/create-account orchestrator account types)
      (d/chain #(hash-map :sucess true :passwd % :account account))
      (d/catch #(hash-map :sucess false :error (.getMessage %)))
      (d/connect response))))

(defn create-handlers [orchestrator tasks]
  (let [create-account-stream (tasks/task-subscription tasks "account.create")]
    (s/consume (partial account-create-handlers orchestrator) create-account-stream)
    (list create-account-stream)))

(defrecord Handler [orchestrator tasks streams]
  component/Lifecycle

  (start [this]
    (assoc this :streams (create-handlers orchestrator tasks)))

  (stop [this]
    (->> this
        :streams
        (map s/close!)
        doall)
    (assoc this :streams nil)))

(defn new-handler []
  (component/using
   (map->Handler {})
   [:orchestrator :tasks]))

