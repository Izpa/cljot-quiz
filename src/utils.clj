(ns utils
  (:require
   [clojure.pprint :as pprint]))

(defn pformat
  [& args]
  (with-out-str
    (apply pprint/pprint args)))

(defn ->num
  "The report number reads differently in different cases.
   This function is guaranteed to cast it to int"
  [n]
  (->> n
       str
       (re-find  #"\d+")
       Integer/parseInt))
