(ns me.arrdem.pseudotypes
  (:require [clojure.set :refer [subset?]]))

(defn- sym-to-pred
  "Given a symbol, this function computes the \"predicate\" form
of the symbol as would be used for testing type.
Ex. 'vector' -> 'vector?', 'foo' -> 'foo?' etc."
  [sym]
  (symbol (str sym "?")))

(defn- sym-to-converter
  "Given a symbol, this function computes the \"translation\" form
of the type represented by the symbol. Used for converting a map
of one pseudo-type (which may contain extra keys) into the subset
defined by the pseudotype."
  [sym]
  (symbol (str "to-" sym)))

(defmacro apply-tests
  "A macro for generating the and of several predicates of a single col."
  [colsym fns]
  `(and ~@(map (fn [f] `(~f ~colsym)) fns)))

(defmacro defpseudotype
  "Defines a pseudo-type, being a type represented only as a map with keys
which may or may not themselves have types. This structure supports _only_
type specification for first level keys, nested keys are not supported yet."
  ;; @TODO add nested key support in needs, allows and preds
  [name & {:keys [needs allows] :as preds}]
  (let [strname (str name)
        needs (set needs)
        preds (dissoc preds :needs :allows)
        preds (map (fn [[k p]] `(fn [x#] (~p (get x# ~k)))) preds)]
    `(defn ~(sym-to-pred name) [arg#]
       (apply-tests arg#
         [;(fn [x#] (prn x#) true)
          (fn [x#] (subset? ~needs (set (keys x#))))
         ~@preds]))
    `(deftypedfn ~(sym-to-converter name) ~(symbol strname) [x# ~(symbol strname)]
      (select-keys x# (concat ~needs ~allows)))))

(defmacro d1if
  "Short for \"drop one if\", generates a function which computes a predicate
of first element of its argument sequence and returns a sequence being the
argument sequence with the first element (drop)ed if the predicate was true."
  [pred]
  `(fn [x#] (if (~pred (first x#)) (drop 1 x#) x#)))

(def any? (fn [& x] true))

(defn- parse-typedfn-args
  "A parser for arguments to the deftypedfn macro which deals with discovering
the 'name', 'type', 'doc-string', 'attr-map' and 'bodies' values of the function
as defined in the clojure.core/defn syntax. 'bodies' as used here is the name
for the sequence of ([arguments*] forms+) which comprise the body of variable
arity functions. In the case of a single arity function, 'bodies' is a seq of a
single element."
  [args]
  (-> {}
      (assoc :name
        (let [s (first args)]
          (if (symbol? s) s (throw (Exception. "fn name not a symbol")))))

      (assoc :type
        (let [s (first (rest args))]
          (if (symbol? s) s (fn [_] true))))

      (assoc :doc-string
        (first (filter string? (take 3 args))))

      (assoc :attr-map
        (merge (first (filter map? (take 4 args)))
               (if (map? (last args)) (last args))))

      ((fn [data]
        (if (some vector? (take 3 (drop 2 args)))
          ; inline forms case
          (-> data
              ;; (assoc :binding-forms
              ;;   (first (filter vector? (take 3 (drop 2 args)))))

              (assoc :bodies
                [(reduce concat (drop 1 (partition-by vector? args)))]))

          ; multiple bodies case
          (let [bodies (-> args
                           ((d1if any?))
                           ((d1if symbol?))
                           ((d1if string?))
                           ((d1if map?))
                           )
                bodies (if (map? (last bodies)) (butlast bodies) bodies)]
            (-> data
                ;; (assoc :binding-forms
                ;;   (reduce concat (map first bodies)))
                (assoc :bodies bodies))))))))


(defn- predicated-body
  "Given a body, being a seq of the form ([ binding, type*] exprs+), this
function computes a new seq containing {:pre, :post} arguments representing
the type-based preconditions and postconditions of the exprs based on
pseudo-types. Note that this generator will not clobber existing pre/post
conditions specified in the body form."
  [body type]
  (let [bindings (first body)
        pre-post (if (map? (second body)) (second body) {})
        tail (-> body
                 ((d1if any?))
                 ((d1if map?)))]
    `(~(vec (map first (partition 2 bindings)))
        {:pre ~(concat [`(and ~@(map (fn [[sym type]]
                                      `(do (assert (~(sym-to-pred type) ~sym))
                                           true))
                                    (partition 2 bindings)))]
                      (:pre pre-post))
        :post ~(concat [`(~(sym-to-pred type) ~'%)]
                       (:post pre-post))}
    ~@tail)))

; (deftypedfn name type? doc-string? attr-map? [params*] body)
; (deftypedfn name type? doc-string? attr-map? ([params*] body)+ attr-map?)

(defmacro deftypedfn
  ^{:doc "Generates a defn with pre and post conditions on all body forms
to provide runtime type-checking with respect to pseudo-types. Operates just
like clojure.core/defn and macroexpands to one."
   :arglists '([name type? doc-string? attr-map? [ param, type *] body]
               [name type? doc-string? attr-map? ([param, type *] body)+ attr-map?])}
[& args]
  (let [{:keys [name type doc-string attr-map bodies]}
         (parse-typedfn-args args)]
    `(defn ~name ~doc-string ~(or attr-map {})
         ~@(map #(predicated-body %1 type) bodies))))
