
(ns quatrefoil.dsl.object3d-dom
  (:require [cljsjs.three] [quatrefoil.util.core :refer [purify-tree]]))

(defonce camera-ref (atom nil))

(defn create-perspective-camera [params]
  (let [fov (:fov params)
        aspect (:aspect params)
        near (:near params)
        far (:far params)
        object3d (js/THREE.PerspectiveCamera. fov aspect near far)]
    (.set object3d.position (:x params) (:y params) (:z params))
    (reset! camera-ref object3d)
    object3d))

(defn create-material [material]
  (case (:kind material)
    :line-basic (js/THREE.LineBasicMaterial. (clj->js (dissoc material :kind)))
    :mesh-basic (js/THREE.MeshBasicMaterial. (clj->js (dissoc material :kind)))
    (do
     (.warn js/console "Unknown material:" material)
     (js/THREE.LineBasicMaterial. (clj->js (dissoc material :kind))))))

(defn create-sphere-element [params material event]
  (let [geometry (js/THREE.SphereGeometry.
                  (or (:radius params) 8)
                  (or (:width-segments params) 32)
                  (or (:height-segments params) 32))
        object3d (js/THREE.Mesh. geometry (create-material material))]
    (.set object3d.position (:x params) (:y (:y params)) (:z (:z params)))
    (.log js/console "Sphere:" object3d)
    object3d))

(defn create-box-element [params material event]
  (let [geometry (js/THREE.BoxGeometry. (:width params) (:height params) (:depth params))
        object3d (js/THREE.Mesh. geometry (create-material material))]
    (.set object3d.position (:x params) (:y (:y params)) (:z (:z params)))
    object3d))

(def global-scene (js/THREE.Scene.))

(defn create-point-light [params]
  (let [color (:color params)
        intensity (:intensity params)
        distance (:distance params)
        object3d (js/THREE.PointLight. color intensity distance)]
    (.set object3d.position (:x params) (:y params) (:z params))
    (.log js/console "Light:" object3d)
    object3d))

(defn create-element [element]
  (.log js/console "Element:" element)
  (let [params (or (:params element) {})
        material (or (:material element) {:kind :mesh-basic, :color 0xa0a0a0})
        event (:event element)]
    (case (:name element)
      :scene global-scene
      :group (js/THREE.Group.)
      :box (create-box-element params material event)
      :sphere (create-sphere-element params material event)
      :point-light (create-point-light params)
      :perspective-camera (create-perspective-camera params)
      (do (.warn js/console "Unknown element" element) (js/THREE.Object3D.)))))

(defonce virtual-tree-ref (atom {}))

(defn remove-child [] )

(defn append-child [] )

(defn set-event [] )

(defn set-param [] )

(defn build-tree [coord tree]
  (let [object3d (create-element (dissoc tree :children))
        children (->> (:children tree)
                      (map
                       (fn [entry]
                         (update
                          entry
                          1
                          (fn [child] (build-tree (conj coord (first entry)) child)))))
                      (into {}))
        virtual-element {:object3d object3d, :children children}]
    (doseq [entry children]
      (let [child (last entry)] (.log js/console "Child:" child entry) (.add object3d child)))
    (swap! virtual-tree-ref assoc-in (conj coord 'data) virtual-element)
    object3d))

(defn set-material [] )