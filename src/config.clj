(ns config
  (:gen-class)
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string]
   [integrant.core :as ig]))

(defn env-to-set
  [env-var-name]
  (let [env-var (System/getenv env-var-name)]
    (when env-var
      (->> (clojure.string/split env-var #",")
           (map #(Integer/parseInt %))
           set))))

(defmethod aero/reader 'custom/env-to-set
  [_ _ value]
  (env-to-set value))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset [_ _ value]
  (ig/refset value))

(defn load-config
  ([] (load-config (or (keyword (System/getProperty "Profile"))
                       :default)))
  ([profile]
   (-> "common_config.edn"
       io/resource
       (aero/read-config {:profile profile}))))

(defn load-namespaces
  [cfg]
  (ig/load-namespaces cfg)
  cfg)

(defn prepare
  ([] (prepare :default))
  ([profile] (-> profile
                 load-config
                 load-namespaces)))

(defn init!
  ([] (init! :default))
  ([profile]
   (-> profile
       prepare
       ig/init)))

(defn stop!
  [system]
  (ig/halt! system))
