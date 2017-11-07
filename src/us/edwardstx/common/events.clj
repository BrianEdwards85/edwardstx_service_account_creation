(ns us.edwardstx.common.events
  (:require [us.edwardstx.common.rabbitmq :refer [get-channel] :as rabbitmq]
            [us.edwardstx.common.uuid :refer [uuid]]
            [cheshire.core :as json]
            [langohr.basic     :as lb]
            [langohr.core :as rmq]
            [manifold.stream :as s]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn publish-event-handler [service-name rabbitmq {:keys [body key id]}]
  (let [routing-key (format "events.%s.%s" service-name key)
        payload (json/generate-string body)
        channel (get-channel rabbitmq)]
    (try
      (lb/publish channel "events" routing-key payload {:content-type "application/json"
                                                        :message-id id
                                                        :app-id service-name
                                                        :type key})
      (catch Exception ex #(log/error ex "Unable to publis event"))
      (finally (rmq/close channel)))))

(defn publish-event [{:keys [event-stream]} key body]
  (let [mid (uuid)]
    (s/put! event-stream {:key key :body body :id mid})
    mid))

(defrecord Events [conf rabbitmq event-stream]
  component/Lifecycle

  (start [this]
    (let [service-name (-> conf :conf :service-name)
          event-stream (s/stream)]
      (s/consume (partial publish-event-handler service-name rabbitmq) event-stream)
      (assoc this :event-stream event-stream)))

  (stop [this]
    (do
      (s/close! event-stream)
      (assoc this :channel nil :event-stream nil)))
  )

(defn new-events []
  (component/using
   (map->Events {})
   [:conf :rabbitmq]))

