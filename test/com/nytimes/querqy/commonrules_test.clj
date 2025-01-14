(ns com.nytimes.querqy.commonrules-test
  (:refer-clojure :exclude [filter])
  (:require
   [clojure.test :refer [deftest is testing]]
   [testit.core :refer [facts =>]]
   [clojure.datafy :refer [datafy]]
   [com.nytimes.querqy.commonrules :as r :refer [match match* synonym boost filter delete]]
   [clojure.java.io :as io]
   [com.nytimes.querqy :as querqy])
  (:import
   (querqy.rewrite.commonrules.select.booleaninput BooleanInputParser)
   (querqy.rewrite.commonrules.select.booleaninput.model BooleanInputElement BooleanInputElement$Type)))

(defn parsed
  "Small helper function to convert the list of BooleanInputElements back into sexpr format.
  Helpful for visualizing the parsed output."
  [input]
  (let [parsed-input (r/parse-boolean-input input)
        string-input (apply str (for [^BooleanInputElement token parsed-input]
                                  (cond
                                    (= BooleanInputElement$Type/AND (.-type token))
                                    " AND "

                                    (= BooleanInputElement$Type/OR (.-type token))
                                    " OR "

                                    (= BooleanInputElement$Type/NOT (.-type token))
                                    "NOT "

                                    :else
                                    (.-term token))))]
    (.validateBooleanInput (BooleanInputParser.) parsed-input string-input)
    string-input))

(deftest parse-boolean-input-test
  (is (= "(A OR B)" (parsed '(or "A" "B"))))
  (is (= "(A OR B OR C)" (parsed '(or "A" "B" "C"))))
  (is (= "(A AND B)" (parsed '(and "A" "B"))))
  (is (= "(A AND B AND C)" (parsed '(and "A" "B" "C"))))
  (is (= "(A AND (NOT B))" (parsed '(and "A" (not "B"))))))

;; ----------------------------------------------------------------------
;; Rewriter tests

;; Both of the rewriters we define here should have the same results.

(def resource-rewriter
  (r/rules-rewriter
   (io/resource "com/nytimes/querqy/common-rules.txt")))

(def dsl-rewriter
  (r/rules-rewriter
    ;; basics
   (match "A1" (synonym "B1"))
   (match "A2 B2" (synonym "C2"))
   (match "A3" (synonym "B3") (synonym "C3"))
   (match "A4 B4" (synonym "C4") (synonym "D4"))
   (match "A5" (boost 2 "B5"))
   (match "A6" (filter "B6"))
   (match "A7 B7" (delete "B7"))
   (match "A8" (synonym "B8") (boost 2 "C8"))
    ;; boolean rules
   (match (or "A9" "B9") (boost 2 "C9"))
   (match (and "A10" "B10") (boost 2 "C10"))
   (match (and "A11" (not "B11")) (boost 2 "C11"))))

(defn rewrite
  "util to do a rewrite and datafy the result for easier comparison"
  [rw string]
  (datafy (querqy/rewrite rw string)))

(deftest rewriter-test
  (facts "DSL & Resource Rewriters have the same output"
    (rewrite dsl-rewriter "A1") => (rewrite resource-rewriter "A1")
    (rewrite dsl-rewriter "A2 B2") => (rewrite resource-rewriter "A2 B2")
    (rewrite dsl-rewriter "A3") => (rewrite resource-rewriter "A3")
    (rewrite dsl-rewriter "A4 B4") => (rewrite resource-rewriter "A4 B4")
    (rewrite dsl-rewriter "A5") => (rewrite resource-rewriter "A5")
    (rewrite dsl-rewriter "A6") => (rewrite resource-rewriter "A6")
    (rewrite dsl-rewriter "A7 B7") => (rewrite resource-rewriter "A7 B7")
    (rewrite dsl-rewriter "A8") => (rewrite resource-rewriter "A8")
    (rewrite dsl-rewriter "A9") => (rewrite resource-rewriter "A9")
    (rewrite dsl-rewriter "B9") => (rewrite resource-rewriter "B9")
    (rewrite dsl-rewriter "A10 B10") => (rewrite resource-rewriter "A10 B10")
    (rewrite dsl-rewriter "A11") => (rewrite resource-rewriter "A11")
    (rewrite dsl-rewriter "A11 B11") => (rewrite resource-rewriter "A11 B11")))

(deftest match-inputs-test
  (testing "match accepts instructions or a list of instructions"
    (is (fn? (match "a"
               (synonym "b"))))
    (is (fn? (match "a"
               [(synonym "b") (synonym "c")])))
    (is (fn? (match "a"
               [(synonym "b") (synonym "c")]
               [(boost 2 "d") (boost 2 "e")])))))

(deftest rule-builder-test
  (let [mutual-synonyms (fn [terms]
                          (for [term terms]
                            (let [tail (map synonym (disj terms term))]
                              (match* term tail))))
        rewriter        (r/rules-rewriter (mutual-synonyms #{"a" "b" "c"}))]
    (facts "custom rule builders"
      (rewrite rewriter "a") =>
      {:type       querqy.model.ExpandedQuery,
       :user-query {:type  querqy.model.Query,
                    :occur :should,
                    :clauses
                    [{:type  querqy.model.DisjunctionMaxQuery,
                      :occur :should,
                      :clauses
                      [{:type querqy.model.Term, :field nil, :value "a"}
                       {:type querqy.model.Term, :field nil, :value "c"}
                       {:type querqy.model.Term, :field nil, :value "b"}]}]},
       :boost-up   [],
       :boost-down [],
       :filter     []}

      (rewrite rewriter "b") =>
      {:type       querqy.model.ExpandedQuery,
       :user-query {:type  querqy.model.Query,
                    :occur :should,
                    :clauses
                    [{:type  querqy.model.DisjunctionMaxQuery,
                      :occur :should,
                      :clauses
                      [{:type querqy.model.Term, :field nil, :value "b"}
                       {:type querqy.model.Term, :field nil, :value "c"}
                       {:type querqy.model.Term, :field nil, :value "a"}]}]},
       :boost-up   [],
       :boost-down [],
       :filter     []}

      (rewrite rewriter "c") =>
      {:type       querqy.model.ExpandedQuery,
       :user-query {:type  querqy.model.Query,
                    :occur :should,
                    :clauses
                    [{:type  querqy.model.DisjunctionMaxQuery,
                      :occur :should,
                      :clauses
                      [{:type querqy.model.Term, :field nil, :value "c"}
                       {:type querqy.model.Term, :field nil, :value "b"}
                       {:type querqy.model.Term, :field nil, :value "a"}]}]},
       :boost-up   [],
       :boost-down [],
       :filter     []})))
