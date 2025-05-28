(ns fixtures
  (:require
   [config]
   [test-utils :refer [ig-init-and-get-key]]))

(defn with-shutdown-db
  [f]
  (f)
  ((ig-init-and-get-key :test-utils/db-shutdown)))
