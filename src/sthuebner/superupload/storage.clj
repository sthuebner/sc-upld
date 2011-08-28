;;;; Uploads Storage
;;;;
;;;; Commons FileUpload is used to temporarily store uploaded files on disk.
;;;; To avoid holding onto the upload entry forever, it is only weakly referenced.
(ns sthuebner.superupload.storage
  (:import [java.lang.ref WeakReference]))


;; the uploads storage is wrapped in an atom for save concurrent access
(defonce ^{:doc "Map of recent uploads"}
  uploads (atom {}))

(defn get-upload
  "Returns an upload for a given ID."
  [id]
  (when-let [weak-ref (get @uploads id)]
    (.get weak-ref)))

(defn add-upload
  "Thread-safely add entry to the uploads Map.
Entries are only weakly referenced for GC to be able to clean up.
"
  [id entry]
  (swap! uploads assoc id (WeakReference. entry)))

(def ^{:doc "Updates a given entry"} update-upload add-upload)

(defn exists?
  [id]
  (contains? @uploads id))

(defn remove-upload
  [id]
  (swap! uploads dissoc id))


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
  (-> id get-upload :tempfile .length))

(defn content-type
  [id]
  (-> id get-upload :content-type))

(defn status
  [id]
  (-> id get-upload :status))

(defn local-file
  [id]
  (-> id get-upload :tempfile))
