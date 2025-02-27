;; string_differences.clj
;; Tom Helmuth, thelmuth@cs.umass.edu
;;
;; Problem Source: iJava (http://ijava.cs.umass.edu/)
;;
;; Given 2 strings of length 10 or less (without whitespace) as input, find the indices at which
;; the strings have different chraracters, stopping at the end of the shorter
;; one. For each such index, print a line containing the index as well as the
;; character in each string. For example, if the strings are "dealer" and
;; "dollars", your code should report this:
;;    1 e o
;;    2 a l
;;    4 e a
;;
;; input stack has the 2 input strings
;;
;; NOTE: I could add a second error value per case. For example, comparing the
;; number of newlines to the correct number of newlines. Or that the format
;; of each line is correct (number, space, char, space, char). Or something similar.
;;
;; NOTE 2: I have added an error for the correct format. This problem is still too
;; difficult. It could possibly be made easier by guaranteeing that the two
;; strings are of the same length.

(ns clojush.problems.software.string-differences
  (:use clojush.pushgp.pushgp
        [clojush pushstate interpreter random util globals]
        clojush.instructions.tag
        clojure.math.numeric-tower)
    (:require [clojure.string :as string]))

; Atom generators
(def string-differences-atom-generators
  (concat (list
            []
            \space
            \newline
            ;;; end constants
            (fn [] (- (lrand-int 21) 10)) ;; Integer ERC [-10,10]
            ;;; end ERCs
            (tag-instruction-erc [:integer :vector_integer :boolean :string :char :vector_string :exec] 1000)
            (tagged-instruction-erc 1000)
            ;;; end tag ERCs
            'in1
            'in2
            ;;; end input instructions
            )
          (registered-for-stacks [:integer :vector_integer :boolean :string :char :vector_string :exec])))


;; Define test cases
(defn string-differences-input
  "Makes a pair of String Differences inputs."
  [min-length]
  (let [len (+ min-length (lrand-int (- 11 min-length)))
        input1 (apply str
                      (repeatedly len
                                  (fn []
                                    (lrand-nth (map char (range 33 127))))))
        change-char-sometimes-fn (fn [c]
                                   "Each char has 40% chance of being replaced"
                                   (if (< (lrand) 0.4)
                                     (lrand-nth (map char (range 33 127)))
                                     c))
        num-chars-to-drop-last (lrand-nth (list 0 0 0 1 1 (lrand-int (inc (count input1)))))
        input2 (apply str (drop-last num-chars-to-drop-last
                                     (map change-char-sometimes-fn input1)))]
    (if (< (lrand) 0.5) ;;Choose random order since len(input1) >= len(input2)
      [input1 input2]
      [input2 input1])))

;; A list of data domains for the problem. Each domain is a vector containing
;; a "set" of inputs and two integers representing how many cases from the set
;; should be used as training and testing cases respectively. Each "set" of
;; inputs is either a list or a function that, when called, will create a
;; random element of the set.
(def string-differences-data-domains
  [[(list ["" ""]
          ["" "hi"]
          ["ThereWorld" ""]
          ["A" "A"]
          ["B" "C"]
          ["&" "#"]
          ["4" "456789"]
          ["rat" "hat"]
          ["new" "net"]
          ["big" "bag"]
          ["STOP" "SIGN"]
          ["abcde" "a"]
          ["abcde" "abcde"]
          ["abcde" "edcba"]
          ["2n" "nn"]
          ["hi" "zipper"]
          ["dealer" "dollars"]
          ["nacho" "cheese"]
          ["loud" "louder"]
          ["qwertyuiop" "asdfghjkl;"]
          ["LALALALALA" "LLLLLLLLLL"]
          ["!!!!!!" ".?."]
          ["9r2334" "9223d4r"]
          ["WellWell" "wellwell"]
          ["TakeThat!" "TAKETHAT!!"]
          ["CHOCOLATE^" "CHOCOLATE^"]
          [(apply str (repeat 10 \s)) (apply str (repeat 10 \~))]
          [(apply str (take 10 (cycle (list \> \_ \= \])))) (apply str (take 10 (cycle (list \q \_))))]
          [(apply str (take 10 (cycle (list \( \))))) (apply str (take 10 (cycle (list \p \p \)))))]
          [(apply str (take 10 (cycle (list \H \a)))) (apply str (take 10 (cycle (list \H \i))))]) 30 0] ; Edge case inputs
   [#(string-differences-input 2) 170 0] ; Random inputs. Length 1 strings are covered by hand-coded cases
   [#(string-differences-input 1) 0 2000]
   ])

;;Can make test data like this:
;(test-and-train-data-from-domains string-differences-data-domains)

(defn string-differences-pair-list
  "Takes two strings, in1 and in2, and returns a list of pairs
   of the form [true idx char1 char2], where true
   is the constant true, idx is the index in the
   strings such that there are different chars 
   and char1 is the char at index idx in in1
   and char2 is the char at index idx in in2"
  [in1 in2]
  (filter first
          (map #(vector (not= %1 %2) %3 %1 %2)
               in1
               in2
               (range))))

(defn string-differences-string-list
  "Takes pair list of the form [true idx char1 char2]
   and return a list of strings of the form [\"char1char2\"]"
  [pairs]
  (vec (map #(apply str (rest (rest %))) pairs)))

(defn string-differences-index-list
  "Takes a pair list of the form [true idx char1 char2]
   and returns a list of the indexes"
  [pairs]
  (vec (map #(second %) pairs)))

; Helper function for error function
(defn string-differences-test-cases
  "Takes a sequence of inputs and gives IO test cases of the form
   [input output]."
  [inputs]
  (map (fn [[in1 in2]]
         (let [pairs (string-differences-pair-list in1 in2)]
           (vector [in1 in2]
                   [(string-differences-string-list pairs)
                    (string-differences-index-list pairs)])))
       inputs))

(defn make-string-differences-error-function-from-cases
  [train-cases test-cases]
  (fn the-actual-string-differences-error-function
    ([individual]
      (the-actual-string-differences-error-function individual :train))
    ([individual data-cases] ;; data-cases should be :train or :test
     (the-actual-string-differences-error-function individual data-cases false))
    ([individual data-cases print-outputs]
      (let [behavior (atom '())
            errors (flatten (doall
                              (for [[[input1 input2] [correct-output correct-idxs]] (case data-cases
                                                                       :train train-cases
                                                                       :test test-cases
                                                                       data-cases)]
                                (let [final-state (run-push (:program individual)
                                                            (->> (make-push-state)
                                                              (push-item input2 :input)
                                                              (push-item input1 :input)))
                                      idx-result (top-item :vector_integer final-state)
                                      result (top-item :vector_string final-state)]
                                  (when print-outputs
                                    (println (format "INPUT1: %s" (pr-str input1)))
                                    (println (format "INPUT2: %s" (pr-str input2)))
                                    (println (format "| Correct output: %s\n| Program output: %s\n" (pr-str correct-output) (pr-str result)))
                                    (println (format "| Correct index %s\n| Program index %s\n" (pr-str correct-idxs) (pr-str idx-result))))
                                  ; Record the behavior
                                  (swap! behavior conj result)
                                  ; Error is:
                                  ;   1. Levenshtein distance of printed strings
                                  ;   2. Levenshtein distance of indexes
                                  (+ (if (vector? result)
                                       (levenshtein-distance (pr-str correct-output) (pr-str result))
                                       1000000)
                                     (if (vector? idx-result)
                                       (levenshtein-distance (pr-str correct-idxs) (pr-str idx-result))
                                       1000000))))))] ;;NOTE: SEE NOTE IN INTRO
        (if (= data-cases :test)
          (assoc individual :test-errors errors)
          (assoc individual :behaviors @behavior :errors errors))))))

(defn get-string-differences-train-and-test
  "Returns the train and test cases."
  [data-domains]
  (map string-differences-test-cases
       (test-and-train-data-from-domains data-domains)))

; Define train and test cases
(def string-differences-train-and-test-cases
  (get-string-differences-train-and-test string-differences-data-domains))

(defn string-differences-initial-report
  [argmap]
  (println "Train and test cases:")
  (doseq [[i case] (map vector (range) (first string-differences-train-and-test-cases))]
    (println (format "Train Case: %3d | Input/Output: %s" i (str case))))
  (doseq [[i case] (map vector (range) (second string-differences-train-and-test-cases))]
    (println (format "Test Case: %3d | Input/Output: %s" i (str case))))
  (println ";;******************************"))

(defn string-differences-report
  "Custom generational report."
  [best population generation error-function report-simplifications]
  (let [best-test-errors (:test-errors (error-function best :test))
        best-total-test-error (apply +' best-test-errors)]
    (println ";;******************************")
    (printf ";; -*- String Differences problem report - generation %s\n" generation)(flush)
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
  {:error-function (make-string-differences-error-function-from-cases (first string-differences-train-and-test-cases)
                                                                      (second string-differences-train-and-test-cases))
   :training-cases (first string-differences-train-and-test-cases)
   :atom-generators string-differences-atom-generators
   :max-points 4000
   :max-genome-size-in-initial-program 500
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
   :problem-specific-report string-differences-report
   :problem-specific-initial-report string-differences-initial-report
   :report-simplifications 0
   :final-report-simplifications 5000
   :max-error 5000
   })
