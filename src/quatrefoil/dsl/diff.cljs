
(ns quatrefoil.dsl.diff
  (:require [clojure.set :as set] [quatrefoil.util.core :refer [comp? shape?]]))

(declare diff-children)

(declare diff-tree)

(defn diff-params [prev-params params coord collect!]
  (let [prev-keys (into #{} (keys prev-params))
        curr-keys (into #{} (keys params))
        added-params (set/difference curr-keys prev-keys)
        removed-params (into #{} (set/difference prev-keys curr-keys))
        common-keys (set/intersection prev-keys curr-keys)
        changed-params (->> common-keys
                            (filter (fn [k] (not= (get prev-params k) (get params k))))
                            (map (fn [k] [k (get params k)]))
                            (into {}))]
    (if (not (empty? removed-params)) (collect! [coord :remove-params removed-params]))
    (if (not (empty? added-params))
      (collect! [coord :add-params (select-keys params added-params)]))
    (if (not (empty? changed-params)) (collect! [coord :update-params changed-params]))))

(defn diff-events [prev-events events coord collect!]
  (let [prev-event-names (into #{} (keys prev-events))
        event-names (into #{} (keys events))
        added-events (set/difference event-names prev-event-names)
        removed-events (set/difference prev-event-names event-names)]
    (if (not (empty? added-events)) (collect! [coord :add-events added-events]))
    (if (not (empty? removed-events)) (collect! [coord :remove-events removed-events]))))

(defn diff-material [prev-material material coord collect!]
  (let [prev-keys (into #{} (keys prev-material))
        curr-keys (into #{} (keys material))
        added-material (->> (set/difference curr-keys prev-keys)
                            (map (fn [k] [k (get material k)])))
        removed-keys (->> (set/difference prev-keys curr-keys) (into #{}))
        updated-material (->> (set/intersection prev-keys curr-keys)
                              (filter
                               (fn [k] (not= (get prev-material k) (get material k))))
                              (map (fn [k] [k (get material k)]))
                              (into {}))]
    (if (not (empty? added-material)) (collect! [coord :add-material added-material]))
    (if (not (empty? removed-keys)) (collect! [coord :remove-material removed-keys]))
    (if (not (empty? updated-material))
      (collect! [coord :update-material updated-material]))))

(defn diff-tree [prev-tree tree coord collect!]
  (.log js/console "Diffing:" coord (some? prev-tree) (some? tree))
  (cond
    (comp? prev-tree) (recur (:tree prev-tree) tree coord collect!)
    (comp? tree) (recur prev-tree (:tree tree) coord collect!)
    :else
      (if (some? prev-tree)
        (if (some? tree)
          (do
           (diff-params (:params prev-tree) (:params tree) coord collect!)
           (diff-material (:material prev-tree) (:material tree) coord collect!)
           (diff-events (:event prev-tree) (:event tree) coord collect!)
           (diff-children (:children prev-tree) (:children tree) coord collect!))
          (collect! [coord :remove]))
        (if (some? tree) (collect! [coord :create tree]) nil))))

(defn diff-children [prev-children children coord collect!]
  (let [prev-keys (into #{} (keys prev-children))
        curr-keys (into #{} (keys children))
        removed-keys (set/difference prev-keys curr-keys)
        added-children (->> (set/difference curr-keys prev-keys)
                            (map (fn [k] [k (get children k)])))
        common-keys (set/intersection prev-keys curr-keys)]
    (if (not (empty? removed-keys)) (collect! [coord :remove-children removed-keys]))
    (if (not (empty? added-children)) (collect! [coord :add-children added-children]))
    (.log js/console "Common keys to diff:" common-keys prev-children children)
    (doall
     (doseq [k common-keys]
       (.log js/console "Diffing children:" coord)
       (diff-tree (get prev-children k) (get children k) (conj coord k) collect!)))))
