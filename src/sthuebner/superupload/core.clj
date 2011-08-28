;;;; Application Core
;;;;
;;;; Used libraries:
;;;; - Ring https://github.com/mmcgrana/ring
;;;; - Moustache https://github.com/cgrand/moustache
;;;; - Ring uses Apache Commons FileUpload

(ns sthuebner.superupload.core
  (:require [sthuebner.superupload.storage :as storage]
	    [clojure.contrib.json :as json])
  (:use clojure.test
	[ring.handler dump]
	[ring.middleware file-info lint resource]
	[ring.util codec response]
	[net.cgrand.moustache :only [app]]))


(comment
  ;;; start the app, fire up a browser
  (use 'ring.util.serve)
  (binding [clojure.java.browse/*open-url-script* "/usr/bin/chromium"]
    (serve app))
)





;;;; request handlers

(def not-found {:status 404})

(defn- upload-handler
  "Creates an upload handler for a given file ID."
  [id]
  (fn [req]
    {:status 204
     :headers {"Location" (str "/upload/" id)}}))


(defn- upload-get-handler
  [id]
  (fn [req]
    (when (storage/exists? id)
      (-> {:name (storage/filename id)
	   :size (storage/filesize id)
	   :content-type (storage/content-type id)
	   :status (storage/status id)
	   :local-file (storage/local-file id)}
	  json/json-str
	  response
	  (content-type "application/json")))))


(defn- progress-handler
  "Creates a progress handler for a given file ID.
Progress is represented as <bytes-uploaded>/<filesizes>"
  [id]
  (fn [req]
    (when (storage/exists? id)
      (response (str (storage/bytes-uploaded id) "/" (storage/filesize id))))))


(defn- download-handler
  "Creates a handler to download a given upload"
  [id]
  (fn [req]
    (when (storage/exists? id)
      (let [file (.toString (storage/local-file id))
	    type (storage/content-type id)]
	(-> file file-response
	    (content-type type))))))




;;;; application routing

(def ^{:doc "Application Endpoints"}
  endpoints

  ;; app is the central Macro provided by Moustache
  (app

   ;; provide static resources off the "public" folder
   [#".*\.(css|html|js)"] (app wrap-file-info
			       (wrap-resource "public")
			       not-found)
   
   ;; specific endpoints and their handlers:
   
   ;; POST /upload/[id] => upload files
   ;; GET /upload/[id] => get status for uploaded files
   ["upload" id] {:post (app
			 ;; parses multipart forms, uploads contained
			 ;; files, and updates the upload storage
			 (wrap-multipart-params-with-storage id)

			 ;; return 204
			 (upload-handler id))

		  :get (upload-get-handler id)}

   ;; GET /upload/[id]/progress => get progress for uploading file
   ["upload" id "progress"] {:get (progress-handler id)}

   ;; GET /upload/[id]/file => download the uploaded file
   ["upload" id "file"] {:get (download-handler id)}
   
   ;; /
   [""] (fn [req] (redirect "/upload.html"))))




;;;; Test Helpers

(defn- GET
  "Requests the given URI from the endpoints"
  [uri]
  (endpoints {:request-method :get, :uri uri, :headers {}}))

(defn- http-ok? [res]
  (= 200 (:status res)))

(defn- http-redirect? [res]
  (= 302 (:status res)))

(defn- http-not-found? [res]
  (= 404 (:status res)))


;;;; Test fixtures

(defn- dummy-upload
  [f]
  ;; populate storage
  (storage/add-upload "lolcat" {:filename "lolcat.png"
				:content-type "image/jped"
				:tempfile "test/lolcat.jpg"
				:status "complete"})
  
  ;; run the test
  (f)

  ;; reset storage
  (storage/remove-upload "lolcat"))

(use-fixtures :once dummy-upload)

;;;; Tests

(deftest test-endpoints
  (is (http-ok? (GET "/upload.html")))
  (is (http-ok? (GET "/upload/lolcat")))
  (is (http-ok? (GET "/upload/lolcat/progress")))
  (is (http-ok? (GET "/upload/lolcat/file")))
  (is (http-not-found? (GET "/upload/foo")))
  (is (http-not-found? (GET "/upload/foo/progress")))
  (is (http-not-found? (GET "/upload/foo/file")))
  (is (http-redirect? (GET "/")))
  )

;; run them automatically
(run-tests *ns*)
