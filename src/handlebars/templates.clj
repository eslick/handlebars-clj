(ns handlebars.templates
  "Handlebar templating library for Clojure, allows for server or client-side
   interpretation"
  (:use [hiccup.core]
	[clojure.walk])
  (:require [clojure.string :as str]))

;; =============================
;; Hiccup Shorthand and Helpers
;; =============================

(defmacro %
  "Support {{ctx-expr}} as (% ctx.child)"
  [& exprs]
  (assert (= (count exprs) 1))
  `(quote [%var ~@exprs]))

(defn var-expr? [expr]
  (and (sequential? expr)
       (= (name (first expr)) (name '%var))))


(defmacro %h
  "Support {{#helper ctx-expr}} as (%h helper expr & body) for
   use by shorthand macros" 
  [& exprs]
  (assert (>= (count exprs) 3))
  `['%block '~(first exprs) '~(second exprs) ~@(drop 2 exprs)])

(defn block-expr? [expr]
  (and (sequential? expr)
       (= (name (first expr)) (name '%block))))


(defmacro %str
  "Special form for guaranteeing whitespace between forms on rendering"
  [& exprs]
  `['%str ~@exprs])

(defn str-expr? [expr]
  (and (sequential? expr)
       (= (name (first expr)) (name '%str))))


(defmacro defhelper
  "Makes it easy to define your own block helper shorthand.  The tag
   names a %tag macro for integration with hiccup expressions,
   and a helper function %helper-tag that takes three arguments,
   the keyword symbol tag, the current context and a body function
   which you can call to evaluate the body expression of the block"
  [tag description arglist & body]
  (assert (not (= (first (str tag)) \%)))
  (let [shorthand (symbol (str "%" tag))
	longhand (symbol (str "%helper-" tag))]
    `(do (defmacro ~shorthand
	   ~description
	   [& exprs#]
	   `(%h ~'~tag ~@exprs#))
	 (defn ~longhand ~arglist
	   ~@body))))

(defn get-helper
  [helper]
  (assert (or (fn? helper) (symbol? helper) (keyword? helper) (string? helper)))
  (if (fn? helper) helper
      (resolve (symbol (str "%helper-" helper)))))

(defn call-helper
  [helper var fn]
  (assert (or (fn? fn) (symbol? fn)))
  ((get-helper helper) (keyword var) fn))

;; ============================================
;; Applying templates to contexts
;; ============================================

(def ^:dynamic *parent-context* nil)
(def ^:dynamic *context* nil)

;; Expressions

(defn hb-tag?
  "Is this the handlebar template tag?"
  [sym]
  (and (or (symbol? sym) (keyword? sym) (string? sym))
       (= (first (str sym)) \%)))

(defn hb-expr?
  "Is this hiccup expression a handlebar template expression?"
  [expr]
  (or (var-expr? expr) (block-expr? expr) (str-expr? expr)))


;; Variables

(defn parent-ref?
  "Does this variable have a parent reference?"
  [var]
  (= (subs (str var) 0 3) "../"))

(defn var-path
  "Resolve the path reference to a path, as in get-in,
   ignoring the parent context reference if it exists."
  [var]
  (if (parent-ref? var)
    (var-path (subs (str var) 3))
    (map keyword (str/split (str var) #"\."))))

(defn resolve-var
  "[%var <var>] => hiccup expression or string"
  [var]
  (let [path (var-path var)]
    (if (parent-ref? var)
      (get-in *parent-context* path)
      (get-in *context* path))))

;; Expansion handlebars.clj -> hiccup

(declare expand-template)

(defn resolve-block
  "[%block <helper> <var> & <body>] => hiccup expression or string
   Take the block expression and pass the helper a closure that
   will expand the body in a possibly modified context"
  [[tag helper var & body]]
  (call-helper helper var *context*
	       (fn [newctx]
		 (expand-template body newctx))))
  
(defn resolve-hb-expr [expr]
  (println "Walking: " expr)
  (cond
   (var-expr? expr)
   (do (println "Resolving " (second expr) " in " *context*)
       (resolve-var (second expr)))

   (block-expr? expr)
   (resolve-block expr)
   true expr))

(defn expand-template
  "Expand a template according to context"
  [template context]
  (binding [*context* context]
    (clojure.walk/prewalk resolve-hb-expr template)))

;; ======================================================
;; Render Handlebar Template as valid Hiccup Expression
;; ======================================================

;; Rendering handlebars.clj -> html+handlebars.js

(defn render-hb-expr [expr]
  (println "Walking: " expr)
  (cond
   (var-expr? expr)
   (str "{{" (second expr) "}}")

   (block-expr? expr)
   (str "{{#" (second expr) " " (nth expr 2) "}}")

   (str-expr? expr)
   
   true expr))

(defn render-template
  "Expand a template into a valid hiccup expression with
   handlebar expressions embedded inside"
  [template]
  (clojure.walk/prewalk render-hb-expr template))

;; ===================
;; Built-in Helpers
;; ===================

(defhelper with
  "{{#with person}}...{{/with}} => (%with person & body)"
  [var fn]
  (fn (resolve-var var)))
  
(defhelper each
  "{{#each person}}...{{/each}} => (%each person & body)"
  [var fn]
  (mapcat fn (resolve-var var)))

(defhelper unless
  "{{#unless person}}{{/unless}} => (%unless person & body)"
   [var fn]
   (let [val (resolve-var var)]
     (when (not val)
       (fn val))))

(defhelper else
  "Synonym for %unless"
   [var fn]
   (let [val (resolve-var var)]
     (when (not val)
       (fn val))))

(defhelper if
  "{{#if person}}...{{else}}...{{/if}} => (%if person & body) (%else person & else)"
  [var fn]
  (when-let [new-ctx (resolve-var var)]
    (fn new-ctx)))


;; ================
;; Handlebar API
;; ================

(defn render-html
  "Render a handlebar structure or compiled template as
   as HTML, using the context to expand the template"
  [template context]
  (binding [*parent-context* context]
    (expand-template template context)))

(defn render-template
  "Render a structure or compiled template as a Handlebar
   template suitable for client-side handlebar.js"
  [template]
  (render-template template))

;;
;; Syntactic sugar around template definitions
;;

(defmacro deftemplate [name & body]
  `(let [template# (html ',@body)]
     (defn name
       ([context] (render-html template# context))
       ([]       (render-template template)))))
