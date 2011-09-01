;;;; Uploads Storage
;;;;
;;;; Commons FileUpload is used to temporarily store uploaded files on disk.
(ns sthuebner.superupload.storage
  (:use clojure.test))


;; the uploads storage is wrapped in an atom for save concurrent access
(defonce ^{:doc "Map of recent uploads"}
  uploads (atom {}))

(defn add-upload
  "Thread-safely add entry to the uploads Map."
  [id entry]
  (swap! uploads assoc id entry)
  entry)

(defn get-upload
  "Returns an upload for a given ID."
  [id]
  (get @uploads id))

(defn update-upload
  "Updates a given entry"
  [id props]
  (swap! uploads assoc id (merge (get-upload id) props))
  (get-upload id))

(defn exists-upload?
  [id]
  (contains? @uploads id))

(defn remove-upload
  [id]
  (let [entry (get-upload id)]
    (swap! uploads dissoc id)
    entry))

(defn reset-uploads
  "FOR TESTING PURPOSES ONLY. Resets the storage."
  []
  (swap! uploads {}))

;;;; accessors to the properties of uploads
;;;; encapsulating the inner structure of upload entries against the outer world

(defn filename
  [id]
  (-> id get-upload :filename))

(defn filesize
  [id]
  (-> id get-upload :size))

(defn bytes-uploaded
  [id]
  (long (or (-> id get-upload :bytes-uploaded) 0)))

(defn set-bytes-uploaded
  [id n]
  (update-upload id {:bytes-uploaded n}))

(defn content-type
  [id]
  (-> id get-upload :content-type))

(defn status
  [id]
  (-> id get-upload :status))

(defn local-file
  [id]
  (-> id get-upload :tempfile))

(defn description
  [id]
  (-> id get-upload :description))



;;;; upload record

(defrecord Upload
    [filename content-type filesize status local-file bytes-uploaded description])

(defn make-upload
  ([filename content-type file-size]
     (make-upload filename content-type file-size nil nil nil nil))
  ([filename content-type filesize status local-file bytes-uploaded description]
     (Upload. filename
              content-type
              (when filesize (long filesize))
              status
              local-file
              (when bytes-uploaded (long bytes-uploaded))
              description)))

(deftest test-upload
  (testing "make-upload"
    (let [upload (make-upload "Test" "text/plain" 3)]
      (are [key value] (= value (key upload))
           :filename "Test"
           :content-type "text/plain"
           :filesize 3)
      )
    ))


;;;; storage tests

(defn- reset-storage-fixture
  [f]
  ;; run test
  (f)
  ;; reset storage
  (swap! uploads {}))

(use-fixtures :each reset-storage-fixture)

(deftest test-upload-storage
  (let [upload (make-upload "foo.txt" "text/plain" 3)
        uuid (.toString (java.util.UUID/randomUUID))]
    
    (testing "add-upload"
      (is (identical? upload (add-upload uuid upload)) "Should return the entry"))
    
    (testing "get-upload"
      (is (identical? upload (get-upload uuid)) "Should return the entry with the given ID"))

    (testing "update-upload"
      (let [updated-entry (update-upload uuid {:status "incomplete"})]
        (is (= Upload (type updated-entry)) "Should return the updated entry")
        (are [k v] (= v (k updated-entry))
             :filename "foo.txt"
             :content-type "text/plain"
             :filesize 3
             :status "incomplete")))

    (testing "remove-upload"
      (let [entry (get-upload uuid)]
        (is (= entry (remove-upload uuid)) "Should return the removed upload")
        (is (nil? (get-upload uuid)) "Entry should be removed from storage")
        ;; putting it back
        (add-upload uuid entry)))

    (testing "exists-upload?"
      (let [entry (get-upload uuid)]
        (is (exists-upload? uuid))
        (remove-upload uuid) ; removing the entry
        (is (not (exists-upload? uuid)))
        (add-upload uuid entry) ; putting it back
        ))))

(deftest resetting-storage
  (let [upload (make-upload "foo.txt" "text/plain" 3)
        uuid (.toString (java.util.UUID/randomUUID))]
    (add-upload uuid upload)
    (is (exists-upload? uuid))
    (reset-uploads)
    (is (not (exists-upload? uuid)))))

(run-tests *ns*
           ;;'sthuebner.superupload.core
           )
