(defproject handlebars-clj "0.9.1"
  :description "Implementation of handlebars templating in Clojure; can apply templates on the server side or serialize them for client-side application"
  :author "Ian Eslick"
  :license {:name "BSD License"
            :url "http://github.com/eslick/handlebars-clj/blob/master/LICENSE.md"
            :distribution :repositories}
  :url "http://github.com/eslick/handlebars-clj"
  :scm {:name "git"
        :url "http://github.com/eslick/handlebars-clj"}
  
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hiccup "1.0.2"]])
