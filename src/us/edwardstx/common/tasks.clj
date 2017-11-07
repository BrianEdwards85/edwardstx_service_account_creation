(ns us.edwardstx.common.tasks
  (:require [us.edwardstx.common.rabbitmq :refer [get-channel] :as rabbitmq]
            [us.edwardstx.common.uuid :refer [uuid]]
            [cheshire.core              :as json]
            [langohr.basic              :as lb]
            [langohr.core               :as rmq]
            [langohr.queue              :as lq]
            [langohr.consumers          :as lc]
            [manifold.deferred          :as d]
            [manifold.stream            :as s]
            [clojure.tools.logging      :as log]
            [com.stuartsierra.component :as component]))

(defn convert-payload [payload content-type]
  (let [payload (String. payload)]
    (if (= "application/json" content-type)
      (json/parse-string payload true)
      payload)))

(defn rcv-msg [stream ch {:keys [delivery-tag message-id content-type reply-to correlation-id] :as meta} ^bytes payload]
  (let [body (convert-payload payload content-type)
        p (d/deferred)]
    (if reply-to
      (d/on-realized p
                     (fn [response]
                       (lb/publish ch "" reply-to (json/generate-string response) {:correlation-id correlation-id :content-type "application/json"})
                       (lb/ack ch delivery-tag))
                     (fn [_] (lb/nack ch delivery-tag false true)))
      (d/on-realized p
                     (fn [_] (lb/ack ch delivery-tag))
                     (fn [_] (lb/nack ch delivery-tag false true))))
    (->
     (s/put! stream (assoc meta :body body :response p))
     (d/chain #(if (not %) (lb/nack ch delivery-tag false true))))))

(defn task-subscription [{:keys [rabbitmq conf]} key]
  (let [channel (get-channel rabbitmq)
        service-name (-> conf :conf :service-name)
        task-stream (s/stream)
        queue-name (format "tasks.%s.%s" service-name key)]
    (s/on-closed task-stream #(rmq/close channel))
    (lq/declare channel queue-name {:exclusive false :auto-delete false})
    (lq/bind channel queue-name "tasks" {:routing-key queue-name})
    (lc/subscribe channel queue-name (partial rcv-msg task-stream) {:auto-ack false})
    task-stream
    ))

(defn send-msg [channel service-name key body opt]
  (let [mid (uuid)
        cid (uuid)
        payload (json/generate-string body)]
    (lb/publish channel
                "tasks"
                (format "tasks.%s" key)
                payload
                (merge opt
                       {:content-type "application/json"
                        :correlation-id cid
                        :app-id service-name
                        :type key
                        :message-id mid}))
    mid))


(defn send-task [{:keys [rabbitmq conf]} key body]
  (let [channel (get-channel rabbitmq)
        service-name (-> conf :conf :service-name)]
    (try
      (send-msg channel service-name key body {})
      (catch Exception ex #(log/error ex (format "Unable to publish task: %s" key)))
      (finally (rmq/close channel)))))

(defn task-response-handler [p ch {:keys [content-type]} ^bytes payload]
  (let [body (convert-payload payload content-type)]
    (d/success! p body)
    (rmq/close ch)))

(defn send-task-with-response [{:keys [rabbitmq conf]} key body]
  (let [channel (get-channel rabbitmq)
        service-name (-> conf :conf :service-name)
        p (d/deferred)]
    (try
      (lc/subscribe channel "amq.rabbitmq.reply-to" (partial task-response-handler p) {:auto-ack true})
      (send-msg channel service-name key body {:reply-to "amq.rabbitmq.reply-to"})
      (catch Exception ex #(do
                             (d/error! p ex)
                             (log/error ex (format "Unable to publish task: %s" key)))))
    p))


(defrecord Tasks [conf rabbitmq event-stream]
  component/Lifecycle

  (start [this]
    (let [event-stream (s/stream)]
      (assoc this :event-stream event-stream)))

  (stop [this]
    (do
      (s/close! event-stream)
      (assoc this :event-stream nil)))
  )

(defn new-tasks []
  (component/using
   (map->Tasks {})
   [:conf :rabbitmq]))


(comment

  (def st (us.edwardstx.common.tasks/task-subscription @system "account.create"))

  (defn rcxv [{:keys [body response message-id]}]
    (println (str body "," message-id))
    (manifold.deferred/success! response (assoc body :mid message-id :ts 3456) ))

  (manifold.stream/consume rcxv st)



  )
