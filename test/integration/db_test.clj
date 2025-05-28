(ns db-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [fixtures :refer [with-shutdown-db]]
   [test-utils :refer [ig-init-and-get-key]]))

(use-fixtures :each with-shutdown-db)

(deftest execute!-test
  (let [execute! (ig-init-and-get-key :db/execute!)]
    (is (= [#:next.jdbc{:update-count 0}]
           (execute! ["create table test_table (id serial primary key, foo varchar(32));"])))
    (is (= [#:next.jdbc{:update-count 1}]
           (execute! ["insert into test_table (foo) values ('bar');"])))
    (is (= [#:TEST_TABLE{:ID 1, :FOO "bar"}]
           (execute! ["select * from test_table;"])))))
