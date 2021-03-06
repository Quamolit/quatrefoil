
(ns quatrefoil.dsl.object3d-dom
  (:require [quatrefoil.util.core
             :refer
             [purify-tree collect-children find-element scale-zero]]
            ["three" :as THREE]))

(defonce camera-ref (atom nil))

(defn create-material [material]
  (case (:kind material)
    :line-basic (THREE/LineBasicMaterial. (clj->js (dissoc material :kind)))
    :mesh-basic (THREE/MeshBasicMaterial. (clj->js (dissoc material :kind)))
    :mesh-lambert (THREE/MeshLambertMaterial. (clj->js (dissoc material :kind)))
    (do
     (.warn js/console "Unknown material:" material)
     (THREE/LineBasicMaterial. (clj->js (dissoc material :kind))))))

(defn create-box-element [params material event comp-coord]
  (let [geometry (THREE/BoxGeometry. (:width params) (:height params) (:depth params))
        object3d (THREE/Mesh. geometry (create-material material))]
    (.set object3d.position (:x params) (:y params) (:z params))
    (.set
     object3d.scale
     (scale-zero (:scale-x params))
     (scale-zero (:scale-y params))
     (scale-zero (:scale-y params)))
    (set! object3d.coord comp-coord)
    object3d))

(defn create-group-element [params]
  (let [object3d (THREE/Group.)]
    (.set object3d.position (:x params) (:y params) (:z params))
    (.set object3d.scale (:scale-x params) (:scale-y params) (:scale-y params))
    object3d))

(defn create-perspective-camera [params]
  (let [fov (:fov params)
        aspect (:aspect params)
        near (:near params)
        far (:far params)
        object3d (THREE/PerspectiveCamera. fov aspect near far)]
    (.set object3d.position (:x params) (:y params) (:z params))
    (reset! camera-ref object3d)
    object3d))

(defn create-point-light [params]
  (let [color (:color params)
        intensity (:intensity params)
        distance (:distance params)
        object3d (THREE/PointLight. color intensity distance)]
    (.set object3d.position (:x params) (:y params) (:z params))
    (comment .log js/console "Light:" object3d)
    object3d))

(defn create-sphere-element [params material event comp-coord]
  (let [geometry (THREE/SphereGeometry.
                  (or (:radius params) 8)
                  (or (:width-segments params) 32)
                  (or (:height-segments params) 32))
        object3d (THREE/Mesh. geometry (create-material material))]
    (.set object3d.position (:x params) (:y params) (:z params))
    (.set
     object3d.scale
     (scale-zero (:scale-x params))
     (scale-zero (:scale-y params))
     (scale-zero (:scale-y params)))
    (set! object3d.coord comp-coord)
    (comment .log js/console "Sphere:" object3d)
    object3d))

(defonce ref-dirty-call! (atom nil))

(defonce font-ref
  (do
   (let [loader (THREE/FontLoader.)]
     (.load
      loader
      "hind.json"
      (fn [response]
        (.log js/console response)
        (@ref-dirty-call!)
        (reset! font-ref response))))
   (atom (THREE/Font. nil))))

(defn create-text-element [params material]
  (let [geometry (THREE/TextGeometry.
                  (or (:text params) "Quatrefoil")
                  (clj->js (assoc params :font @font-ref)))
        object3d (THREE/Mesh. geometry (create-material material))]
    (.set object3d.position (:x params) (:y params) (:z params))
    object3d))

(def default-params {:x 0, :y 0, :z 0, :scale-x 1, :scale-y 1, :scale-z 1})

(defonce global-scene (THREE/Scene.))

(defn create-element [element]
  (comment .log js/console "Element:" element (:coord element))
  (let [params (merge default-params (:params element))
        material (or (:material element) {:kind :mesh-basic, :color 0xa0a0a0})
        event (:event element)
        coord (:coord element)]
    (case (:name element)
      :scene global-scene
      :group (create-group-element params)
      :box (create-box-element params material event coord)
      :sphere (create-sphere-element params material event coord)
      :point-light (create-point-light params)
      :perspective-camera (create-perspective-camera params)
      :text (create-text-element params material)
      (do (.warn js/console "Unknown element" element) (js/THREE.Object3D.)))))

(defonce virtual-tree-ref (atom {}))

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
      (let [child (last entry)]
        (comment .log js/console "Child:" child entry)
        (.addBy object3d (first entry) child)))
    (swap! virtual-tree-ref assoc-in (conj coord 'data) virtual-element)
    object3d))

(defn on-canvas-click [event dispatch! tree-ref]
  (let [mouse (THREE/Vector2.), raycaster (THREE/Raycaster.)]
    (set! mouse.x (dec (* 2 (/ event.clientX js/window.innerWidth))))
    (set! mouse.y (- 1 (* 2 (/ event.clientY js/window.innerHeight))))
    (.setFromCamera raycaster mouse @camera-ref)
    (let [intersects (.intersectObjects
                      raycaster
                      (let [children (clj->js []), collect! (fn [x] (.push children x))]
                        (collect-children global-scene collect!)
                        children))
          maybe-target (aget intersects 0)]
      (.log js/console intersects)
      (if (some? maybe-target)
        (let [coord maybe-target.object.coord
              target-el (find-element @tree-ref coord)
              maybe-handler (:click (:event target-el))]
          (if (some? maybe-handler)
            (maybe-handler event dispatch!)
            (println "Found no handler for" coord)))))))
