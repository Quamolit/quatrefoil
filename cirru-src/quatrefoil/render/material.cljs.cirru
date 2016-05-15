
ns quatrefoil.render.material

defn render-material-dsl (dsl)
  let
    (kind $ :kind dsl)
      params $ dissoc dsl :kind
      js-params $ ->> params
        map $ fn (entry)
          []
            name $ key entry
            val entry

        into $ {}
        clj->js

    case kind
      :lambert $ THREE.MeshLambertMaterial. js-params
      :line-basic $ THREE.LineBasicMaterial. js-params
      :mesh-basic $ THREE.MeshBasicMaterial. js-params
      throw $ str "|Material not found:" kind
