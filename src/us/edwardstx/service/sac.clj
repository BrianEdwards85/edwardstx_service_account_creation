(ns us.edwardstx.service.sac
  (:require [com.stuartsierra.component :as component]
            [config.core :refer [env]]
            [manifold.deferred :as d]
            [us.edwardstx.service.sac.handler :refer [new-handler]]
            [us.edwardstx.service.sac.orchestrator :refer [new-orchestrator]]
            [us.edwardstx.common.conf :refer [new-conf]]
            [us.edwardstx.common.db :refer [new-database]]
            [us.edwardstx.common.events :refer [new-events]]
            [us.edwardstx.common.keys :refer [new-keys]]
            [us.edwardstx.common.logging :refer [new-logging]]
            [us.edwardstx.common.rabbitmq :refer [new-rabbitmq]]
            [us.edwardstx.common.tasks :refer [new-tasks]]
            [us.edwardstx.common.token :refer [new-token]]
            )
  (:gen-class))

(defonce system (atom {}))

(defn init-system [env]
  (component/system-map
   :keys (new-keys env)
   :token (new-token env)
   :conf (new-conf env)
   :logging (new-logging)
   :tasks (new-tasks)
   :db (new-database)
   :rabbitmq (new-rabbitmq)
   :events (new-events)
   :orchestrator (new-orchestrator)
   :handler (new-handler)
   ))


(defn -main [& args]
  (println "Hello world")
  (let [semaphore (d/deferred)]
    (reset! system (init-system env))

    (swap! system component/start)

    (deref semaphore)

    (component/stop @system)

    (shutdown-agents)
    ))


