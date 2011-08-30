;;;; Ring middleware


(ns sthuebner.superupload.middleware
  (:use [sthuebner.superupload.monitoring]
        [ring.middleware multipart-params]
	[ring.util response])
  (:require [sthuebner.superupload.storage :as storage]
	    [clojure.java.io :as io])
  (:import [java.io File]))


;;;; content negotiation
(defn content-type-dispatch
  [handler mime-type app]
  (fn [{{accept "accept"} :headers :as req}]
    (if (and accept
	     (.startsWith accept mime-type))
      (-> (app req)
	  (content-type "application/json"))
      (handler req))))
      



;;;; Middleware for file uploads
;;;;
;;;; to workaround Ring's original wrap-multipart-param, whose store function blocks
;;;; until file is fully stored to disk
(defn- make-temp-file
  "Returns a new temporary file. The file will be deleted on VM exit."
  []
  (let [temp-file (File/createTempFile "superupload-" nil)]
    (.deleteOnExit temp-file)
    temp-file))


;; a multipart-store-function takes a map with keys :filename, :content-type, and :stream
;; it processes the stream and returns a map with keys :filename, :content-type, :tempfile, and :size
(defn- store-file-and-add-to-storage
  "Stores the file behind the given ITEM and adds an entry to our storage."
  [id expected-bytes]
  (fn [item]
    (let [temp-file (make-temp-file)
	  upload-entry (-> (select-keys item [:filename :content-type])
			   (assoc :tempfile temp-file
                                  ;; expected-bytes is probably not the actual file size, so we update afterwards
                                  :size expected-bytes
                                  :status "incomplete"))
          
          ;; monitor the inputstream and catch the number of bytes read
          read-monitor (fn [n]
                         (when (pos? n)
                           (let [old (storage/bytes-uploaded id)
                                 new (+ old n)]
                             (storage/set-bytes-uploaded id new))))
          monitoring-stream (make-inputstream-read-monitor (:stream item) read-monitor)]

      ;; create storage entry
      (storage/add-upload id upload-entry)

      ;; FIXME squeeze stream monitoring in here to update progress information
      (io/copy monitoring-stream temp-file)

      ;; update storage entry
      (storage/update-upload id (assoc (storage/get-upload id)
				  ;; take the acutal file size
				  :size (.length temp-file)
				  :status "complete")))))


;; parse multipart forms (also saves uploaded files as temp files)
(defn wrap-multipart-params-with-storage
  "Wrapper to parse multipart forms. Files are stored using async-uploader"
  [handler id]
  (fn [{:keys [content-length] :as req}]
    (let [uploader (store-file-and-add-to-storage id content-length)
	  ;; use the original wrapper to do the multipart parsing
	  ;; just inject our custom store-function
	  inner-wrapper (wrap-multipart-params handler {:store uploader})]
      (inner-wrapper req))))
