(ns test-utils
  (:require
   [config :refer [load-config load-namespaces]]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]))

(def test-config
  (-> :test
      load-config
      load-namespaces))

(defn ig-init-and-get-key
  [k]
  (-> test-config
      (ig/init [k])
      k))

(defmethod ig/init-key ::db-ds [_ config]
  (jdbc/get-datasource config))

(defmethod ig/init-key ::db-shutdown [_ {:keys [execute!]}]
  #(execute! ["SHUTDOWN"]))
