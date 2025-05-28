(ns db
  (:require
   [honey.sql :as sql]
   [integrant.core :as ig]
   [migratus.core :as migratus]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [taoensso.timbre :as log]))

(defmethod ig/init-key ::ds [_ {:keys [db] :as db-config}]
  (migratus/init db-config)
  (if-let [migrations (migratus/migrate db-config)]
    (log/error "Migration error" {:error migrations})
    (log/info "Migrations completed"))
  (jdbc/get-datasource db))

(defn execute-sql-map!
  ([ds sql-map] (execute-sql-map! ds sql-map false))
  ([ds sql-map one?]
   (log/debug "execute sql-query: " (sql/format sql-map))
   ((if one?
      jdbc/execute-one!
      jdbc/execute!) ds
                     (sql/format sql-map)
                     {:builder-fn rs/as-unqualified-kebab-maps
                      :return-keys true
                      :pretty true})))

(defmethod ig/init-key ::execute! [_ {:keys [ds]}]
  (partial execute-sql-map! ds))
