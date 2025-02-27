;; digits.clj
;; Tom Helmuth, thelmuth@cs.umass.edu
;;
;; Problem Source:
;;   C. Le Goues et al., "The ManyBugs and IntroClass Benchmarks for Automated Repair of C Programs,"
;;   in IEEE Transactions on Software Engineering, vol. 41, no. 12, pp. 1236-1256, Dec. 1 2015.
;;   doi: 10.1109/TSE.2015.2454513
;;
;; Given an integer |x| < 10,000,000,000, print that integer's digits each on
;; their own line starting with the least significant digit. A negative integer
;; should have the negative sign printed before the most significant digit.
;;
;; input stack has input integer

(ns clojush.problems.software.digits
  (:use clojush.pushgp.pushgp
        [clojush pushstate interpreter random util globals]
        clojush.instructions.tag
        [clojure.math numeric-tower]
        ))

; Atom generators
(def digits-atom-generators
  (concat (list
            []
            ;;; end constants
            (fn [] (- (lrand-int 19) 9)) ; [-9, 9]
            ;;; end ERCs
            (tag-instruction-erc [:integer :boolean :vector_integer :exec] 1000)
            (tagged-instruction-erc 1000)
            ;;; end tag ERCs
            'in1
            ;;; end input instructions
            )
          (registered-for-stacks [:integer :boolean :vector_integer :exec])))

(defn my-rand-long
  "replaces rand-int when need longs"
  [end]
  (long (* (lrand) end)))

;; A list of data domains for the problem. Each domain is a vector containing
;; a "set" of inputs and two integers representing how many cases from the set
;; should be used as training and testing cases respectively. Each "set" of
;; inputs is either a list or a function that, when called, will create a
;; random element of the set.
(def digits-data-domains
  [[(list -9495969798 -20008000 -777777 -9876 -482 -97 -20 0 19 620 24068 512000 8313227 30000000 9998887776) 15 0] ;; Edge cases by hand
   [(fn []
      (let [digs (inc (lrand-int 10))
            start (expt 10 (dec digs))
            end (expt 10 digs)]
        ((if (< (lrand) 0.5) - +)
          (+ (my-rand-long (- end start)) start)))) 85 1000] ;; Random cases such that each number of digits between 1 and 10 will be represented evenly, as will negatives and positives
   ])

;;Can make Digits test data like this:
;(test-and-train-data-from-domains digits-data-domains)

; Helper function for error function
(defn digits-test-cases
  "Takes a sequence of inputs and gives IO test cases of the form
   [input output]."
  [inputs]
  (map (fn [in] (vector in (vec (reduce
                                 (fn [acc i]
                                   (let [out (* (if (and (empty? acc) (< in 0)) -1 1) (read-string (str i)))] ; if in is negative and i is the first number, multiply by -1
                                     (cons out acc)))
                                 (list)
                                 (str (abs in))))))
       inputs))

(defn make-digits-error-function-from-cases
  [train-cases test-cases]
  (fn the-actual-digits-error-function
    ([individual]
      (the-actual-digits-error-function individual :train))
    ([individual data-cases] ;; data-cases should be :train or :test
     (the-actual-digits-error-function individual data-cases false))
    ([individual data-cases print-outputs]
      (let [behavior (atom '())
            errors (doall
                     (for [[input1 correct-output] (case data-cases
                                                     :train train-cases
                                                     :test test-cases
                                                     data-cases)]
                       (let [final-state (run-push (:program individual)
                                                   (->> (make-push-state)
                                                        (push-item input1 :input)))
                             result (top-item :vector_integer final-state)]
                         (when print-outputs
                           (println (format "| Correct output: %s\n| Program output: %s\n" (pr-str correct-output) (pr-str result))))
                         ; Record the behavior
                         (swap! behavior conj result)
                         ; Error is Levenshtein distance of printed strings
                         (if (vector? result)
                           (levenshtein-distance (pr-str correct-output) (pr-str result))
                           1000000))))]
        (if (= data-cases :test)
          (assoc individual :test-errors errors)
          (assoc individual :behaviors @behavior :errors errors))))))

(defn get-digits-train-and-test
  "Returns the train and test cases."
  [data-domains]
  (map sort (map digits-test-cases
                 (test-and-train-data-from-domains data-domains))))

; Define train and test cases
(def digits-train-and-test-cases
  (get-digits-train-and-test digits-data-domains))

(defn digits-initial-report
  [argmap]
  (println "Train and test cases:")
  (doseq [[i case] (map vector (range) (first digits-train-and-test-cases))]
    (println (format "Train Case: %3d | Input/Output: %s" i (str case))))
  (doseq [[i case] (map vector (range) (second digits-train-and-test-cases))]
    (println (format "Test Case: %3d | Input/Output: %s" i (str case))))
  (println ";;******************************"))

(defn digits-report
  "Custom generational report."
  [best population generation error-function report-simplifications]
  (let [best-test-errors (:test-errors (error-function best :test))
        best-total-test-error (apply +' best-test-errors)]
    (println ";;******************************")
    (printf ";; -*- Digits problem report - generation %s\n" generation)(flush)
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
  {:error-function (make-digits-error-function-from-cases (first digits-train-and-test-cases)
                                                          (second digits-train-and-test-cases))
   :training-cases (first digits-train-and-test-cases)
   :atom-generators digits-atom-generators
   :max-points 1200
   :max-genome-size-in-initial-program 150
   :evalpush-limit 600
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
   :problem-specific-report digits-report
   :problem-specific-initial-report digits-initial-report
   :report-simplifications 0
   :final-report-simplifications 5000
   :max-error 5000
   })
