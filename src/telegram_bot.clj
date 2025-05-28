(ns telegram-bot
  (:require
   [integrant.core :as ig]
   [long-polling-telegram-bot :refer [long-polling]]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::msg-handler [_ {:keys [msg->answer]}]
  (fn [{:keys [message callback_query] :as upd}]
    (if-let [msg (or message (-> callback_query
                                 :message
                                 (assoc :data (:data callback_query))))]
      (do (when (-> msg
                    :chat
                    :id
                    not)
            (log/warn "strange message without chat-id: " (pformat upd)))
          (log/info "Received message")
          (log/info (pformat msg))
          (try (msg->answer msg)
               (catch Exception e
                 (log/error "Catch exception " e))))
      (log/error "unexpected message type" (pformat upd)))))

(defmethod ig/halt-key! ::run-client [_ {:keys [bot thread]}]
  (if thread
    (do (log/info "Stop long-polling telegram-bot")
        (.interrupt ^Thread thread))
    (do (log/info "Stop webhook telegram-bot")
        (tbot/delete-webhook bot))))

(defmethod ig/init-key ::run-client [_ {:keys [bot
                                               url
                                               long-polling-config
                                               msg-handler]}]
  (log/info "Start telegram bot: " (or url long-polling-config))
  (merge
   {:bot bot}
   (if (nil? url)
     {:thread (long-polling bot long-polling-config msg-handler)}
     {:webhook (tbot/set-webhook bot {:url url
                                      :content-type :multipart})})))

(defmethod ig/init-key ::client [_ {:keys [token]}]
  (log/info "Start client-bot")
  (if (nil? token)
    (log/error "No client-bot token")
    (let [bot (tbot/create token)]
      (log/info (tbot/get-me bot))
      bot)))
