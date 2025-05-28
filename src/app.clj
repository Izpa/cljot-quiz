(ns app
  (:gen-class)
  (:require
   [config :refer [init!]]))

(defn -main
  "Main java entrypoint into Cljot."
  [& _]
  (init!))
