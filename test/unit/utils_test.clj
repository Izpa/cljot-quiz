(ns utils-test
  (:require
   [clojure.test :refer [deftest is]]
   [utils :as sut]))

(deftest ->num-test
  (is (= 42 (sut/->num "42")))
  (is (= 42 (sut/->num 42)))
  (is (= 42 (sut/->num "i42"))))
