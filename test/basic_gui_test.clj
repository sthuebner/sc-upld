;;;; basic tests for GUI elements

(ns basic-gui-test
  (:use clojure.test
	clojure.contrib.zip-filter.xml)
  (:require [clojure.xml :as xml]
	    [clojure.zip :as zip])
  (:import [java.io File]))


;;; helper functions

(defn- load-page
  "Load and parse the resource behind the given URL."
  [url]
  (xml/parse url))

(defn- page-exists?
  "Tests whether a page with a given url exists."
  [url]
  (.exists (File. url)))

(defn- page-title
  "Returns the title of a given HTML page."
  [root]
  (-> root zip/xml-zip (xml1-> :head text)))


(defn- has-element?
  "Tests whether an element exists in the given XML represented by root.

   Example: (has-element? content :head :title)

   See also: http://clojure.github.com/clojure-contrib/zip-filter-api.html#clojure.contrib.zip-filter.xml/xml->
"
  [root & preds]
  (not (empty? (apply xml-> (zip/xml-zip root) preds))))




;;;; Actual tests

;; testing the upload page
(let [url "html/upload.html"]

  ;; testing if the page is accessible and is the right one
  (deftest load-upload-page
    (is (page-exists? url)
	"The Upload page should be accessible")
    (is (= "SuperUpload" (page-title (load-page url)))
	"The Upload page should have a title"))

  ;; testing existence of necessary form elements
  (deftest upload-page-form-elements
    (let [content (load-page url)]
      (is (has-element? content :body :form)
	  "The Upload page should have a form element")
      (is (has-element? content :body :form :input (attr= :name "file") (attr= :type "file"))
	  "The Upload page should have a file selection element")
      )))


(run-tests 'basic-gui-test)
