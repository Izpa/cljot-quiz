(ns quiz
  (:require
   [clojure.string :as str]
   [honey.sql :as sql]
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defmethod ig/init-key ::subscribed? [_ {:keys [bot channel-id]}]
  #(let [{:keys [ok error_code description]
          {:keys [status]} :result
          :as response} (tbot/get-chat-member bot channel-id %)]
     (when (and (not ok)
                (= error_code 400)
                (= description "Bad Request: PARTICIPANT_ID_INVALID"))
       (log/error "Unexpected get-chat-memeber response" response))
     (not= status "left")))

(defmethod ig/init-key ::admin? [_ {:keys [admin-chat-ids]}]
  #(contains? admin-chat-ids %))

(defn telegram-send
  ([bot to-id main-content] (telegram-send bot to-id main-content {}))
  ([bot to-id main-content additional-content]
   (let [sent_message (tbot/send-message bot
                                         to-id
                                         main-content
                                         (merge {:parse_mode "HTML"}
                                                additional-content))]
     (log/info "Send message: "
               (pformat sent_message))
     sent_message)))

(defmethod ig/init-key ::telegram-send [_ {:keys [bot]}]
  (partial telegram-send bot))

(def subscribed-callback-data "subscribed")

(def subscribed-additional-content
  {:reply_markup {:inline_keyboard [[{:text "Я подписался"
                                      :callback_data subscribed-callback-data}]]}})

(defmethod ig/init-key ::user-welcome [_ {:keys [db-execute!]}]
  (fn [answer chat]
    (db-execute! {:insert-into :users
                  :values [(select-keys chat
                                        [:id
                                         :username
                                         :last_name
                                         :first_name])]}
                 true)
    (answer (str "Давай знакомиться?\n\n"
                 "\u270CМы - Центр инноваций и Клуб LANIT Product manager.\n\n"
                 "Топим за продуктовый подход и развиваем продуктовую культуру в корпорации.\n\n"
                 "🔥Сегодня разыгрываем футболки, которые мы сделали совместно с SlovoDna. "
                 "Да, на кону те самые футболки - классные и стильные. "
                 "В такой можно ходить не только на даче:) "
                 "Условия простые:\n\n"
                 "⚡️подписаться на наш <a href='https://t.me/+C-XaEZ28W5szZTUy'>канал</a>\n"
                 "⚡️пройти квиз из 5 вопросов.\n\n"
                 "После подписки нажми кнопку “Я подписался”")
            subscribed-additional-content)))

(defn user-id->next-question
  [db-execute! user-id]
  (let [db-question (db-execute! {:with [[:next-unanswered-question
                                          {:select [[:q.id :question-id]
                                                    [:q.text :question-text]
                                                    [:a.question-message-id :question-message-id]]
                                           :from [[:questions :q]]
                                           :left-join [[:user-answers :a]
                                                       [:and
                                                        [:= :q.id :a.question-id]
                                                        [:= :a.user-id user-id]]]
                                           :where [:and
                                                   [:is :a.answer-text nil]
                                                   [:is :a.option-id nil]]
                                           :order-by [[:q.sort-order]]
                                           :limit 1}]]
                                  :select [[:nuq.question-id :question-id]
                                           [:nuq.question-text :question-text]
                                           [:nuq.question-message-id :question-message-id]
                                           [:o.id :option-id]
                                           [:o.text :option-text]
                                           [:o.is-correct :option-is-correct]]
                                  :from [[:next-unanswered-question :nuq]]
                                  :left-join [[:question-options :o]
                                              [:= :nuq.question-id :o.question-id]]
                                  :order-by [[:o.sort-order]]}
                                 false)]
    (reduce (fn [q {:keys [option-id option-text option-is-correct]}]
              (if option-id
                (cond-> q
                  :always (update :options assoc option-id option-text)
                  :always (update :option-ids conj option-id)
                  option-is-correct (assoc :correct-option-id option-id))
                q))
            (-> db-question
                first
                (select-keys [:question-id
                              :question-text
                              :question-message-id])
                (assoc :options {})
                (assoc :option-ids #{}))
            db-question)))

(defn after-questions
  [answer]
  (answer (str "Это было огненно!\n"
               "Лови наш фирменный <a href='https://t.me/addstickers/LANIT3'>стикерпак</a>, "
               "наклейки можешь взять на стенде :)\n\n"
               "⭐️Чтобы и дальше быть на продуктовой волне, "
               "присоединяйся к <a href='https://t.me/+K8YGduhn8NxiYjg6'>сообществу</a> продактов ЛАНИТ :)"))
  (answer (str "🎁Это еще не все! Совсем скоро будем разыгрывать футболки от SlovoDna! "
               "Победителей выберет великий рандомайзер:)\n\n"
               "💬Жди сообщение в боте.\n\n"
               "Не уходи далеко, у нас еще есть игры, "
               "поп-корн и пара коробок с нашими шоколадками, "
               "которые сами себя не съедят:)")))

(defn ask-question
  [question-id question-text  options id answer db-execute!]
  (->> (if (not-empty options)
         {:reply_markup {:inline_keyboard (mapv (fn [[option-id option-text]]
                                                  [{:text option-text
                                                    :callback_data option-id}])
                                                options)}}
         {})
       (answer (str/replace question-text #"\\n" "\n"))
       :result
       :message_id
       (assoc {:user-id id :question-id question-id} :question-message-id)
       vector
       (assoc {:insert-into :user-answers} :values)
       (db-execute!)))

(defn questions
  [db-execute!
   subscribed?
   answer
   {{:keys [id]} :chat
    :keys [data
           text]
    :as msg}]
  (let [data (try (Integer/parseInt data)
                  (catch Exception _ data))
        any-answers? (-> {:select [[(sql/call :count :*)]]
                          :from [:user-answers]
                          :where [:= :user-id id]}
                         (db-execute! true)
                         :count
                         (not= 0))]
    (if (or (subscribed? id)
            any-answers?)
      (let [{:keys [question-id
                    question-text
                    question-message-id
                    options
                    option-ids
                    correct-option-id]} (user-id->next-question db-execute! id)]
        (if question-id
          (if question-message-id
            (if (not-empty options)
              (if (and data
                       (contains? option-ids data))
                (do
                  (db-execute! {:update :user-answers
                                :set {:option-id data}
                                :where [:and
                                        [:= :user-id id]
                                        [:= :question-id question-id]]})
                  (when correct-option-id
                    (if (= data
                           correct-option-id)
                      (answer "Верно!")
                      (answer (str "Неправильно, правильный ответ: " (get options correct-option-id)))))
                  (questions db-execute! subscribed? answer msg))
                (answer "Пожалуйста, используйте кнопки для ответа"))
              (if text
                (do (db-execute! {:update :user-answers
                                  :set {:answer-text text}
                                  :where [:and
                                          [:= :user-id id]
                                          [:= :question-id question-id]]})
                    (questions db-execute! subscribed? answer msg))
                (answer "Пожалуйста, используйте текст для ответа")))
            (ask-question question-id question-text  options id answer db-execute!))
          (after-questions answer)))
      (answer "Не видим твою подписку :) Попробуй ещё раз :)" subscribed-additional-content))))

(defmethod ig/init-key ::user-main-chain [_ {:keys [db-execute! subscribed?]}]
  (partial questions db-execute! subscribed?))

(defmethod ig/init-key ::user-answer [_ {:keys [db-execute! user-welcome user-main-chain]}]
  (fn [msg answer]
    (let [{{:keys [id]
            :as chat} :chat} msg
          user (db-execute! {:select :*
                             :from :users
                             :where [:= :id id]}
                            true)]
      (if user
        (user-main-chain answer msg)
        (user-welcome answer chat)))))

(defn command?
  [text]
  (when text (str/starts-with? text "/")))

(defn select-random-users
  [users-count]
  {:select [:*]
   :from :users
   :where [[:= :is-winner nil]]
   :order-by [(sql/call [:random])]
   :limit users-count})

(defn select-winners
  [db-execute! subscribed? winners-count]
  (loop [winners []]
    (if (< (count winners) winners-count)
      (if-let [users (-> (- winners-count (count winners))
                         select-random-users
                         db-execute!
                         not-empty)]
        (let [{:keys [subscribed
                      not-subscribed-ids]}
              (reduce (fn [r {:keys [id] :as user}]
                        (if (subscribed? id)
                          (update r :subscribed conj user)
                          (update r :not-subscribed-ids conj id)))
                      {:subscribed []
                       :not-subscribed-ids []}
                      users)]
          (when (not-empty not-subscribed-ids)
            (db-execute! {:update :users
                          :set {:is-winner false}
                          :where [:in :id not-subscribed-ids]} true))
          (when (not-empty subscribed)
            (db-execute! {:update :users
                          :set {:is-winner true}
                          :where [:in :id (map :id subscribed)]}))
          (recur (concat winners subscribed)))
        winners)
      winners)))

(defmethod ig/init-key ::admin-commands [_ {:keys [db-execute! bot admin? subscribed?]}]
  {:start (fn [_ _ answer]
            (answer (str "Вы - администратор! Вам доступны команды:\n"
                         "/winner N - сгенерировать N победителей (N - число)\n"
                         "/stat - получить число текущих пользователей\n\n"
                         "Так же любое сообщение без команды будет предложено переслать всем пользователям бота")))
   :stat (fn [_ _ answer]
           (->> {:select [[:%count.*]]
                 :from :users}
                db-execute!
                first
                :count
                (str "Кол-во пользователей: ")
                answer))
   :winner (fn [_msg winner-count answer]
             (let [winner-count (Integer/parseInt winner-count)
                   winners (select-winners db-execute! subscribed? winner-count)]
               (when (not-empty winners)
                 (doseq [{:keys [id]} (db-execute! {:select [:id] :from :users})]
                   (try (tbot/send-message bot
                                           id
                                           (str (count winners)
                                                " победителей выбраны рандомайзером!\n\n"
                                                (str/join "\n"
                                                          (mapv #(if-let [username (:username %)]
                                                                   username
                                                                   (str/join " " [(:first-name %)
                                                                                  (:last-name %)]))
                                                                winners))))
                        (catch Exception _))))
               (doseq [{:keys [id]} winners]
                 (tbot/send-message bot
                                    id
                                    (str "ТЫ ВЫИГРАЛ ФУТБОЛКУ от "
                                         "<a href='https://t.me/slovodna'>SlovoDna</a>, "
                                         "ЖДЕМ ТЕБЯ НА СТЕНДЕ! "
                                         "У тебя есть 30 минут, чтобы получить свой приз. "
                                         "Если не успеешь, футболка <s>превратится в тыкву</s> "
                                         "перейдет к следующему победителю 😭 (winner-id: " id ")")
                                    {:parse_mode "HTML"}))
               (answer (if (not-empty winners)
                         (str/join "\n"
                                   (mapv (fn [{:keys [username
                                                      first-name
                                                      last-name
                                                      id]}]
                                           (str username " (" first-name " " last-name ", winner-id: " id ")"))
                                         winners))
                         "Нет участников, удовлетворяющих условиям("))))
   :publish (fn [{{:keys [id]} :chat} message-id answer]
              (->> {:select [:id]
                    :from :users}
                   db-execute!
                   (map :id)
                   (filter #(not (admin? %)))
                   (map #(do (log/debug "user: " %)
                             (tbot/copy-message bot
                                                %
                                                id
                                                message-id)))
                   count
                   (str "Сообщение отправлено пользователям (кол-во): ")
                   answer))})

(defmethod ig/init-key ::admin-answer [_ {:keys [admin-commands]}]
  (fn [{:keys [text
               message_id
               data]
        :as msg}
       answer]
    (let [command (or data text)]
      (cond
        (command? command)
        (if-let [command-fn (get admin-commands (-> command
                                                    (str/split #"\s+")
                                                    first
                                                    (subs 1)
                                                    keyword))]
          (command-fn msg
                      (->> (str/split command #"\s+")
                           (rest)
                           (str/join " "))
                      answer)
          (answer "Unknown command"))

        :else
        (answer "Опубликовать это сообщение?"
                {:reply_to_message_id message_id
                 :reply_markup {:inline_keyboard [[{:text "Опубликовать"
                                                    :callback_data (str "/publish " message_id)}]]}})))))

(defmethod ig/init-key ::msg->answer [_ {:keys [telegram-send admin? user-answer admin-answer]}]
  (fn [msg]
    (let [{{:keys [id]} :chat} msg
          answer (partial telegram-send id)]
      (if id
        (if (admin? id)
          (admin-answer msg answer)
          (user-answer msg answer))
        (log/warn "strange message without chat-id: " (pformat msg))))))
