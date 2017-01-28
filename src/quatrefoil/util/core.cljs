
(ns quatrefoil.util.core (:require [quatrefoil.types :refer [Component Shape]]))

(defn comp? [x] (= Component (type x)))

(defn purify-tree [tree]
  (if (comp? tree)
    (recur (:tree tree))
    (update
     tree
     :children
     (fn [children]
       (->> children
            (map (fn [entry] (update entry 1 (fn [child] (purify-tree child)))))
            (into {}))))))

(defn =seq? [xs ys]
  (let [xs-empty? (empty? xs), ys-empty? (empty? ys)]
    (if xs-empty?
      ys-empty?
      (if ys-empty?
        (if (identical? (first xs) (first ys)) (recur (rest xs) (rest ys)) false)))))

(defn find-element [tree comp-coord]
  (.log js/console "Find..." tree comp-coord)
  (if (empty? comp-coord)
    tree
    (let [cursor (first comp-coord)]
      (if (comp? tree)
        (if (= cursor (:name tree)) (recur (:tree tree) (rest comp-coord)) nil)
        (if (contains? (:children tree) cursor)
          (recur (get-in tree [:children cursor]) (rest comp-coord))
          nil)))))

(defn shape? [x] (= Shape (type x)))

(defn collect-children [element collect!]
  (.forEach
   element.children
   (fn [child]
     (comment .log js/console "Child:" child)
     (collect! child)
     (if (some? child.children) (collect-children child collect!)))))
