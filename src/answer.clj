(ns answer
  (:require
   [cheshire.core :as cheshire]
   [clj-http.client :as client]
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [telegrambot-lib.core :as tbot]
   [utils :refer [pformat]]))

(defrecord Command
  [command-id
   button-text
   answer-fn
   answer-main-content
   answer-additional-contnent
   button-ids])

(defn ->command
  [{:keys [command-id
           button-text
           answer-fn
           answer-main-content
           answer-additional-contnent
           button-ids]
    :or {button-text "TODO"
         answer-fn tbot/send-message
         answer-main-content "TODO"
         answer-additional-contnent {}
         button-ids []}}]
  (->Command command-id
             button-text
             answer-fn
             answer-main-content
             answer-additional-contnent
             button-ids))

(defonce orders (atom {}))

(defn command->dialogue
  [bot chat-id content options]
  (log/info "Start new order" {:chat-id chat-id})
  (swap! orders assoc chat-id {})
  (tbot/send-message bot chat-id content options))

(def cmds
  [{:command-id :default
    :button-text ""
    :answer-main-content
    "Извинте, бот не поддерживает текстовый ввод, для навигации, пожалуйста, используйте кнопки меню"
    :button-ids [:main]}
   {:command-id :main
    :button-text "Вернуться на главную"
    :answer-main-content
    (str "Здравствуйте! Я — чат-бот мебельной фабрики «Мария», "
         "ваш персональный помощник в мире кухонь и мебели для всего дома.\n"
         "<b>Чем я могу вам помочь сегодня?</b>")
    :button-ids [:examples
                 :order
                 :promotions]}
   {:command-id :examples
    :button-text "Посмотреть примеры реализованных проектов"
    :answer-main-content
    (str "С удовольствием покажу вам примеры кухонь.\n\n"
         "<b>Наши кухни — это:</b>\n"
         "♦Стильные и современные решения\n"
         "♦Функциональность и комфорт\n"
         "♦Материалы высокого качества\n"
         "♦Но также с выгодными акциями (например, на встроенную технику)\n\n"
         "<b>Какой стиль вас интересует?</b>")
    :button-ids [:modern-example
                 :neoclassic-example
                 :classic-example
                 :main]}
   {:command-id :modern-example
    :button-text "Современный стиль"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihY0hanwjKQ2REOYtP_5PhMRnoW9YhhdhHvMqhsfztKJ_LqjqgAi-tlAt5zV7iI-FHrw8l8wVj_nIF-TiGzXJ1FBHK_xzWekHMk=s1600-rw-v1"
    :button-ids [:neoclassic-example
                 :classic-example
                 :order
                 :main]}
   {:command-id :neoclassic-example
    :button-text "Неоклассический стиль"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihYy2ILzoLP8vXxK5hvEcjHTboFDKZKRN-OV9VzmieK_8IdwhROvkXI6EgxKzg6Xz_mRGnruwQ8jvk9_gXQltj6pGy_G0l_Erw=s1600-rw-v1"
    :button-ids [:modern-example
                 :classic-example
                 :order
                 :main]}
   {:command-id :classic-example
    :button-text "Классический стиль"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihanGxKQx06YHHXpyNPt5fq19a6O1uSXH1pB_ELG0EfVYTLuK3lqvmRvXI0sF_p582IBcQK15_MvLSFnt5xr1DgIop1sRnHkeZI=s1600-rw-v1"
    :button-ids [:modern-example
                 :neoclassic-example
                 :order
                 :main]}
   {:command-id :promotions
    :button-text "Узнать о скидках и акциях"
    :answer-main-content
    (str "<b>Покупать кухни в «Мария» выгодно!</b>\n\n"
         "Каждый месяц мы предлагаем нашим клиентам выгодные акции: скидки и подарки. Причем все акции суммируются.\n\n"
         "<b>Узнайте больше о наших предложениях:</b>")
    :button-ids [:table
                 :technic
                 :installment
                 :order
                 :main]}
   {:command-id :table
    :button-text "Скидка на столешницы до 80%"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihamzHQZHBu0XVqtNRNVSsw7N6sHMnYzTG689ZXqshd-uLMXspNQ-J8JplBLHFsZONW3oWyKRTNKetO48OBCDkqOb8kF2SpycUU=s1600-rw-v1"
    :answer-additional-contnent
    {:caption
     (str "<b>Скидка на столешницы до 80%</b>\n"
          "Получайте удовольствие от готовки на новой кухне «Мария»! "
          "А мы создадим невероятно стильное и удобное рабочее пространство со столешницей из искусственного камня со скидкой до 80 %.")}
    :button-ids [:technic
                 :installment
                 :order
                 :main]}
   {:command-id :installment
    :button-text "Рассрочка 0% на 12 месяцев"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpiha9jDmaBF_viiqc4kk2s6dnh-Ow6RhBSpwK3pS7-bsTB6pvxi6sClnnOAWJTg6Kr8GUNGk4AjqlLNRgVBVc6fetNcSmSHdUHQ=s1600-rw-v1"
    :answer-additional-contnent
    {:caption
     (str "<b>Рассрочка 0% на 12 месяцев</b>\n"
          "без первоначального взноса и переплаты. "
          "А также предложим выгодные условия по субсидированной рассрочке до 36 месяцев.")}
    :button-ids [:table
                 :technic
                 :order
                 :main]}
   {:command-id :technic
    :button-text "Техника в подарок"
    :answer-fn tbot/send-photo
    :answer-main-content "https://lh3.googleusercontent.com/drive-viewer/AKGpihZqNc8Dvy-RDZ8cv-0oGYs1jpYJ_JBLGDJi3VYRz1linIEAjebz4-9PpiwMooREokZT-1RcKB-ocDLBqKCLClFLCvERK1XWNMA=s2560"
    :answer-additional-contnent
    {:caption
     (str "<b>Техника в подарок</b>\n"
          "Только по 31 июля дарим посудомоечную машину при покупке кухни «Мария» и двух единиц встраиваемой техники брендов Korting, Kuppersberg, Krona, Haier, Graude, Smeg или Hotpoint. "
          "Количество подарков ограниченное – успейте забрать свой!")}
    :button-ids [:table
                 :installment
                 :order
                 :main]}
   {:command-id :order
    :button-text "Получить бесплатный дизайн-проект"
    :answer-main-content
    (str "Создайте кухню своей мечты вместе с нашими дизайнерами! \n‍\n"
         "Мы предлагаем вам бесплатный дизайн-проект кухни, который поможет определиться с выбором. "
         "Наш дизайнер учтёт все ваши пожелания и предложит оптимальный вариант.\n\n"
         "<b>Бесплатный дизайн-проект включает:</b>\n"
         "♦Профессиональную  визуализацию мебели\n"
         "♦Индивидуальный подбор материалов и техники\n"
         "♦Расчет стоимости кухни\n"
         "♦Полезные советы и рекомендации\n\n"
         "<b>Для получения бесплатного дизайн-проекта, необходимо ответить на 3 вопроса.</b>")
    :button-ids [:start-order
                 :main]}
   {:command-id :start-order
    :button-text "Оставить заявку"
    :answer-main-content "Укажите, пожалуйста, ваше имя"
    :answer-fn command->dialogue}])

(defn command->key-val
  [command]
  [(:command-id command) command])

(def commands
  (into {}
        (mapv (comp command->key-val ->command) cmds)))

(defmethod ig/init-key ::->menu [_ {:keys [bot]}]
  (fn [command-id chat-id]
    (let [{:keys [answer-fn
                  answer-main-content
                  answer-additional-contnent
                  button-ids]} (get commands command-id)]
      (if answer-fn
        (answer-fn bot
                   chat-id
                   answer-main-content
                   (merge {:reply_markup {:inline_keyboard (mapv (fn [button-id]
                                                                   [{:text (get-in commands [button-id :button-text])
                                                                     :callback_data (name button-id)}])
                                                                 button-ids)
                                          :remove_keyboard true}
                           :parse_mode "HTML"}
                          answer-additional-contnent))
        (throw (ex-info "Unexisted command-id"
                        {:command-id command-id}))))))

(defn ->email-html
  [username first_name last_name id nam city phone]
  (str "telegram-username: " username "<br>"
       "telegram-firstname: " first_name "<br>"
       "telegram-lastname: " last_name "<br>"
       "telegram-chat-id: " id "<br>"
       "received-name: " nam "<br>"
       "received-city: " city "<br>"
       "received-phone: " phone))

(defmethod ig/init-key ::send-email [_ {:keys [api-key]}]
  (fn [to-emails email-html]
    (doseq [to-email to-emails]
      (log/info "Send email "
                (pformat (client/post "https://api.mailopost.ru/v1/email/messages"
                                      {:headers {"Authorization" (str "Bearer " api-key)
                                                 "Content-Type" "application/json"}
                                       :content-type :json
                                       :form-params {:from_email "notification@izpa.xyz"
                                                     :from_name "Бот Кухни Мария"
                                                     :to to-email
                                                     :subject "Новая заявка в телеграм-боте"
                                                     :html email-html}}))))))

(defmethod ig/init-key ::push-request [_ {:keys [sign api-url]}]
  (fn [body]
    (log/info "Push request "
              (pformat (client/post api-url
                                    {:headers {"Authorization" (str "Basic " sign)
                                               "Content-Type" "application/json"}
                                     :content-type :json
                                     :body (cheshire/generate-string body)})))))

(defmethod ig/init-key ::->dialogue [_ {:keys [bot
                                               send-email
                                               push-request]}]
  (fn [{{:keys [id
                first_name
                last_name
                username]
         :as chat} :chat
        :keys [text]}]
    (let [{:keys [nam
                  city
                  phone]} (get @orders id)
          answer (partial tbot/send-message bot id)]
      (cond
        (nil? nam) (if text
                     (do
                       (swap! orders assoc-in [id :nam] text)
                       (answer "Укажите, пожалуйста, ваш город"))
                     (answer "Укажите, пожалуйста, ваше имя"))
        (nil? city) (if text
                      (do
                        (swap! orders assoc-in [id :city] text)
                        (answer "Укажите, пожалуйста, ваш телефон в формате +7XXXXXXXXXX"))
                      (answer "Укажите, пожалуйста, ваш город"))
        (nil? phone) (if (and text
                              (re-matches #"\+7\d\d\d\d\d\d\d\d\d\d" text))
                       (let [order (-> @orders
                                       (get id)
                                       (assoc :phone text))
                             {:keys [nam
                                     city
                                     phone]} order]
                         (log/info "New order "
                                   (merge chat order))
                         (send-email ["markov.artem.p@gmail.com"
                                      "m.varlakova@realweb.ru"
                                      "e.belyanina@realweb.ru"
                                      "S-razdelkina@marya.ru"
                                      "Yul-platitsina@marya.ru"
                                      "dispatcher@marya.ru"
                                      "User3210@realweb.ru"]
                                     (->email-html username
                                                   first_name
                                                   last_name
                                                   id
                                                   nam
                                                   city
                                                   phone))
                         (push-request {:telegram-username username
                                        :telegram-firstname first_name
                                        :telegram-lastname last_name
                                        :telegram-chat-id id
                                        :received-name nam
                                        :received-city city
                                        :received-phone phone})
                         (swap! orders dissoc id)
                         (answer (str "<b>Спасибо за заявку!</b>\n\n"
                                      "Наш менеджер свяжется с вами в ближайшее время, чтобы обсудить детали вашего дизайн-проекта и помочь вам максимально выгодно приобрести кухню вашей мечты.\n\n"
                                      "<b>Вдохновения вам и скорейшего завершения ремонта!💫</b>")
                                 {:reply_markup {:inline_keyboard [[{:text (get-in commands [:main :button-text])
                                                                     :callback_data (name :main)}]]}
                                  :parse_mode "HTML"}))
                       (answer "Укажите, пожалуйста, ваш телефон в формате +7XXXXXXXXXX"))))))

(defonce members (atom #{}))

(defmethod ig/init-key ::msg->answer [_ {:keys [->dialogue ->menu]}]
  (fn [msg]
    (let [{{:keys [id]} :chat
           :keys [data]} msg
          command-id (cond
                       data (keyword data)
                       (get @members id) :default
                       :else (do (swap! members conj id)
                                 :main))]
      (when (pos? id)
        (if (get @orders id)
          (->dialogue msg)
          (->menu command-id id))))))
