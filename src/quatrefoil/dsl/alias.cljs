
(ns quatrefoil.dsl.alias (:require [quatrefoil.types :refer [Shape Component]]))

(defn create-element [el-name props children]
  (Shape.
   el-name
   (:params props)
   (:material props)
   (:event props)
   (if (seq? children) (->> children (map-indexed vector) (into {})) children)
   nil))

(defn point-light [props & children] (create-element :point-light props children))

(def basic-hooks
  {:init-state (fn [& args] {}),
   :init-instant (fn [& args] {}),
   :on-mutate merge,
   :on-update merge,
   :on-tick (fn [] ),
   :on-unmount (fn [] ),
   :remove? (fn [] false)})

(defn perspective-camera [props & children]
  (create-element :perspective-camera props children))

(defn group [props & children] (create-element :group props children))

(defn camera [props & children] (create-element :camera props children))

(defn create-comp [comp-name hooks render]
  (fn [& args] (Component. comp-name args {} {} render nil (merge basic-hooks hooks) false)))

(defn box [props & children] (create-element :box props children))

(defn line [props & children] (create-element :line props children))

(defn sphere [props & children] (create-element :sphere props children))

(defn scene [props & children] (create-element :scene props children))
