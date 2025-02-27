;; even_squares.clj
;; Tom Helmuth, thelmuth@cs.umass.edu
;;
;; Problem Source: iJava (http://ijava.cs.umass.edu/)
;;
;; Given an integer 0 < n < 10000, print all of the positive even perfect
;; squares < n on separate lines.
;;
;; input stack has input integer n

(ns clojush.problems.software.even-squares
  (:use clojush.pushgp.pushgp
        [clojush pushstate interpreter random util globals]
        clojush.instructions.tag
        [clojure.math numeric-tower])
  (:require [clojure.string :as string]))

; Atom generators
(def even-squares-atom-generators
  (concat (list
            []
            ;;; end ERCs
            (tag-instruction-erc [:integer :boolean :vector_integer :exec] 1000)
            (tagged-instruction-erc 1000)
            ;;; end tag ERCs
            'in1
            ;;; end input instructions
            )
          (registered-for-stacks [:integer :boolean :vector_integer :exec])))


;; A list of data domains for the problem. Each domain is a vector containing
;; a "set" of inputs and two integers representing how many cases from the set
;; should be used as training and testing cases respectively. Each "set" of
;; inputs is either a list or a function that, when called, will create a
;; random element of the set.
(def even-squares-data-domains
  [[(list 1 2 3 4 5 6 15 16 17 18 36 37 64 65) 14 0] ;; Small edge cases
   [(list 9600 9700 9999) 3 0] ;; Large edge cases
   [(fn [] (+ 20 (lrand-int 9980))) 83 1000] ;; Random cases
   ])

;;Can make Even Squares test data like this:
;(test-and-train-data-from-domains even-squares-data-domains)

; Helper function for error function
(defn even-squares-test-cases
  "Takes a sequence of inputs and gives IO test cases of the form
   [input output]."
  [inputs]
  (map (fn [in]
         (let [nums (vec (rest (take-while #(< % in)
                                      (map #(* 4 % %)
                                           (range)))))]
               (vector in nums)))
       inputs))

(defn make-even-squares-error-function-from-cases
  [train-cases test-cases]
  (fn the-actual-even-squares-error-function
    ([individual]
      (the-actual-even-squares-error-function individual :train))
    ([individual data-cases] ;; data-cases should be :train or :test
     (the-actual-even-squares-error-function individual data-cases false))
    ([individual data-cases print-outputs]
      (let [behavior (atom '())
            errors (flatten
                     (doall
                       (for [[input1 correct-integers] (case data-cases
                                                                          :train train-cases
                                                                          :test test-cases
                                                                          data-cases)]
                         (let [final-state (run-push (:program individual)
                                                     (->> (make-push-state)
                                                       (push-item input1 :input)))
                               result (top-item :vector_integer final-state)]
                           (when print-outputs
                             (println (format "| Correct output: %s\n| Program output: %s\n" (pr-str correct-integers) (pr-str result))))
                           ; Record the behavior
                           (swap! behavior conj result)
                           (if (vector? result)
                             (levenshtein-distance (pr-str correct-integers) (pr-str result))
                             1000000)))))]
        (if (= data-cases :test)
          (assoc individual :test-errors errors)
          (assoc individual :behaviors @behavior :errors errors))))))

(defn get-even-squares-train-and-test
  "Returns the train and test cases."
  [data-domains]
  (map #(sort-by first %)
       (map even-squares-test-cases
            (test-and-train-data-from-domains data-domains))))

; Define train and test cases
(def even-squares-train-and-test-cases
  (get-even-squares-train-and-test even-squares-data-domains))

(defn even-squares-initial-report
  [argmap]
  (println "Train and test cases:")
  (doseq [[i case] (map vector (range) (first even-squares-train-and-test-cases))]
    (println (format "Train Case: %3d | Input/Output: %s" i (str case))))
  (doseq [[i case] (map vector (range) (second even-squares-train-and-test-cases))]
    (println (format "Test Case: %3d | Input/Output: %s" i (str case))))
  (println ";;******************************"))

(defn even-squares-report
  "Custom generational report."
  [best population generation error-function report-simplifications]
  (let [best-test-errors (:test-errors (error-function best :test))
        best-total-test-error (apply +' best-test-errors)]
    (println ";;******************************")
    (printf ";; -*- Even Squares problem report - generation %s\n" generation)(flush)
    (println "Test total error for best:" best-total-test-error)
    (println (format "Test mean error for best: %.5f" (double (/ best-total-test-error (count best-test-errors)))))
    (when (zero? (:total-error best))
      (doseq [[i error] (map vector
                             (range)
                             best-test-errors)]
        (println (format "Test Case  %3d | Error: %s" i (str error)))))
    (println ";;------------------------------")
    (println "Outputs of best individual on training cases:")
    (error-function best :train true)
    (println ";;******************************")
    )) ;; To do validation, could have this function return an altered best individual
       ;; with total-error > 0 if it had error of zero on train but not on validation
       ;; set. Would need a third category of data cases, or a defined split of training cases.


; Define the argmap
(def argmap
  {:error-function (make-even-squares-error-function-from-cases (first even-squares-train-and-test-cases)
                                                                (second even-squares-train-and-test-cases))
   :training-cases (first even-squares-train-and-test-cases)
   :atom-generators even-squares-atom-generators
   :max-points 1600
   :max-genome-size-in-initial-program 200
   :evalpush-limit 2000
   :population-size 1000
   :max-generations 300
   :parent-selection :lexicase
   :genetic-operator-probabilities {:alternation 0.2
                                    :uniform-mutation 0.2
                                    :uniform-close-mutation 0.1
                                    [:alternation :uniform-mutation] 0.5
                                    }
   :alternation-rate 0.01
   :alignment-deviation 10
   :uniform-mutation-rate 0.01
   :problem-specific-report even-squares-report
   :problem-specific-initial-report even-squares-initial-report
   :report-simplifications 0
   :final-report-simplifications 5000
   :max-error 5000
   })
