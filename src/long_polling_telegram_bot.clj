(ns long-polling-telegram-bot
  (:require
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  ([bot config]
   (poll-updates bot config nil))

  ([bot config offset]
   (let [resp (tbot/get-updates bot {:offset offset
                                     :timeout (:timeout config)})]
     (if (contains? resp :error)
       (log/error "tbot/get-updates error:" (:error resp))
       resp))))

(defn long-polling
  [bot config msg-handler]
  (log/info "Long polling with timeout " config)
  (let [update-id (atom nil)
        set-id! #(reset! update-id %)
        thread (Thread. #(loop []
                           (log/debug "checking for chat updates.")
                           (let [updates (poll-updates bot config @update-id)
                                 messages (:result updates)]
                             (doseq [msg messages]
                               (msg-handler msg)
                               (-> msg
                                   :update_id
                                   inc
                                   set-id!))
                             (Thread/sleep (long (:sleep config))))
                           (recur)))]
    (.start thread)
    thread))
