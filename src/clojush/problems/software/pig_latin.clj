;; pig_latin.clj
;; Tom Helmuth, thelmuth@cs.umass.edu
;;
;; Problem Source: iJava (http://ijava.cs.umass.edu/)
;;
;; Given a string of length <= 50 containing lowercase words separated by single
;; spaces, print the string with each word translated to pig Latin. More
;; specifically, if a word starts with a vowel, it should have "ay" added to its
;; end; otherwise, the first letter is moved to the end of the word, followed by "ay".
;;
;; input stack has the input string

(ns clojush.problems.software.pig-latin
  (:use clojush.pushgp.pushgp
        [clojush pushstate interpreter random util globals]
        clojush.instructions.tag
        clojure.math.numeric-tower)
  (:require [clojure.string :as string]))

(defn split-at-blank
  "Takes a sentence and return a vector of strings, split at blank space."
  [sentence]
  (vec (remove empty? (string/split sentence #" "))))

;; Define test cases
(defn pig-latin-input
  "Makes a Pig Latin input of length len."
  [len]
  (split-at-blank (apply str
                         (repeatedly len
                                     (fn []
                                       (if (< (lrand) 0.2)
                                         \space
                                         (lrand-nth (map char (range 97 123)))))))))

; Atom generators
(def pig-latin-atom-generators
  (concat (list
            "ay"
            \space
            \a \e \i \o \u
            "aeiou"
            []
            ;;; end constants
            (fn [] (lrand-nth (concat [\newline \tab] (map char (range 32 127))))) ;Visible character ERC
            (fn [] (pig-latin-input (lrand-int 21))) ;String ERC
            ;;; end ERCs
            (tag-instruction-erc [:string :vector_string :char :integer :boolean :exec] 1000)
            (tagged-instruction-erc 1000)
            ;;; end tag ERCs
            'in1
            ;;; end input instructions
            )
          (registered-for-stacks [:string :vector_string :char :integer :boolean :exec])))


;; A list of data domains for the problem. Each domain is a vector containing
;; a "set" of inputs and two integers representing how many cases from the set
;; should be used as training and testing cases respectively. Each "set" of
;; inputs is either a list or a function that, when called, will create a
;; random element of the set.
(def pig-latin-data-domains
  [[(concat (map split-at-blank
                 (list "", "a", "b", "c", "d", "e", "i", "m", "o", "u", "y", "z"
                       "hello", "there", "world", "eat", "apple", "yellow", "orange", "umbrella", "ouch", "in",
                       "hello there world"
                       "out at the plate"
                       "nap time on planets"
                       "supercalifragilistic"
                       "expialidocious"))
            (map split-at-blank
                 (list (apply str (repeat 50 \u))
                       (apply str (repeat 50 \s))
                       (apply str (take 49 (cycle (list \w \space))))
                       (apply str (take 49 (cycle (list \e \space))))
                       (apply str (take 50 (cycle (list \h \a \space))))
                       (apply str (take 49 (cycle (list \x \space \y \space))))))) 33 0] ;; "Special" inputs covering some base cases
   [(fn [] (pig-latin-input (+ 3 (lrand-int 48)))) 167 1000]
   ])

;;Can make Pig Latin test data like this:
;(test-and-train-data-from-domains pig-latin-data-domains)

; Helper function for error function
(defn pig-latin-test-cases
  "Takes a sequence of inputs and gives IO test cases of the form
   [input output]."
  [inputs]
  (map (fn [in]
         (vector in
                 (vec (map #(if (some #{(first %)} "aeiou")
                              (str % "ay")
                              (str (apply str (rest %)) (first %) "ay"))
                           in))))
       inputs))

(defn make-pig-latin-error-function-from-cases
  [train-cases test-cases]
  (fn the-actual-pig-latin-error-function
    ([individual]
     (the-actual-pig-latin-error-function individual :train))
    ([individual data-cases] ;; data-cases should be :train or :test
     (the-actual-pig-latin-error-function individual data-cases false))
    ([individual data-cases print-outputs]
     (let [behavior (atom '())
           errors (doall
                    (for [[input correct-output] (case data-cases
                                                   :train train-cases
                                                   :test test-cases
                                                   data-cases)]
                      (let [final-state (run-push (:program individual)
                                                  (->> (make-push-state)
                                                       (push-item input :input)))
                            result (top-item :vector_string final-state)]
                        (when print-outputs
                          (println (format "\n| Correct output: %s\n| Program output: %s" (pr-str correct-output) (pr-str result))))
                        ; Record the behavior
                        (swap! behavior conj result)
                        ; Error is Levenshtein distance for printed string
                        (if (vector? result)
                          (levenshtein-distance (pr-str correct-output) (pr-str result))
                          1000000))))]
       (if (= data-cases :test)
         (assoc individual :test-errors errors)
         (assoc individual :behaviors @behavior :errors errors))))))

(defn get-pig-latin-train-and-test
  "Returns the train and test cases."
  [data-domains]
  (map #(sort-by (comp count first) %)
       (map pig-latin-test-cases
            (test-and-train-data-from-domains data-domains))))

; Define train and test cases
(def pig-latin-train-and-test-cases
  (get-pig-latin-train-and-test pig-latin-data-domains))

(defn pig-latin-initial-report
  [argmap]
  (println "Train and test cases:")
  (doseq [[i case] (map vector (range) (first pig-latin-train-and-test-cases))]
    (println (format "Train Case: %3d | Input/Output: %s" i (str case))))
  (doseq [[i case] (map vector (range) (second pig-latin-train-and-test-cases))]
    (println (format "Test Case: %3d | Input/Output: %s" i (str case))))
  (println ";;******************************"))

(defn pig-latin-report
  "Custom generational report."
  [best population generation error-function report-simplifications]
  (let [best-test-errors (:test-errors (error-function best :test))
        best-total-test-error (apply +' best-test-errors)]
    (println ";;******************************")
    (printf ";; -*- Pig Latin problem report - generation %s\n" generation)(flush)
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
  {:error-function (make-pig-latin-error-function-from-cases (first pig-latin-train-and-test-cases)
                                                             (second pig-latin-train-and-test-cases))
   :training-cases (first pig-latin-train-and-test-cases)
   :atom-generators pig-latin-atom-generators
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
   :problem-specific-report pig-latin-report
   :problem-specific-initial-report pig-latin-initial-report
   :report-simplifications 0
   :final-report-simplifications 5000
   :max-error 5000
   })
