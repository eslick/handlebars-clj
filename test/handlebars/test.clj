(ns handlebars.test
  (:use handlebars.templates
	clojure.test
	hiccup.core))

(def s1-temp [:p (% test)])
(def s1-ctx {:test "Paragraph text"})

(deftest simple-subst
  (is (= [:p "{{test}}"] (apply-template s1-temp))
      "Simple template rendering, strings in hiccup")
  (is (= "<p>{{test}}</p>" (html (apply-template s1-temp)))
      "Simple template render, as html string")
  (is (= [:p "Paragraph text"] (apply-template s1-temp s1-ctx))
      "Text should be substituted in hiccup expression")
  (is (= "<p>Paragraph text</p>" (html (apply-template s1-temp s1-ctx)))
      "Text should be substituted in html paragraph"))

(deftemplate s2-temp
  [:p {:id (% test_id)} "Empty"])

(def s2-ctx {:test_id "test123"})
  
(deftest option-subst-with-macro
  (is (= [:p {:id "{{test_id}}"} "Empty"] (s2-temp))
      "Apply template directly to get hiccup with template strings")
  (is (= "<p id=\"{{test_id}}\">Empty</p>" (html (s2-temp)))
      "Direct to hiccup HTML template")
  (is (= [:p {:id "test123"} "Empty"] (s2-temp s2-ctx))
      "Direct clojure subst into tag options")
  (is (= "<p id=\"test123\">Empty</p>" (html (s2-temp s2-ctx)))
      "Direct clojure subst into html"))
