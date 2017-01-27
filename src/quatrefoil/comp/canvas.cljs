
(ns quatrefoil.comp.canvas
  (:require [quatrefoil.dsl.alias
             :refer
             [create-comp group box sphere point-light perspective-camera scene]]))

(def comp-demo
  (create-comp
   :demo
   nil
   (fn []
     (fn [state mutate! instant]
       (group
        {}
        (box
         {:params {:width 16, :height 2, :depth 6},
          :material {:kind :mesh-basic, :color 0x808080, :opacity 0.6}}))))))

(def comp-canvas
  (create-comp
   :canvas
   {}
   (fn [store]
     (fn [state mutate! instant]
       (scene
        {}
        (comp-demo)
        (sphere
         {:params {:radius 4, :x 40},
          :material {:kind :mesh-basic, :opacity 0.6, :color 0x9050c0}})
        (point-light
         {:params {:color 0xffaaaa, :x 60, :y 20, :z 0, :intensity 1, :distance 100}})
        (perspective-camera
         {:params {:x 0,
                   :y 0,
                   :z 80,
                   :fov 45,
                   :aspect (/ js/window.innerWidth js/window.innerHeight),
                   :near 0.1,
                   :far 1000}}))))))