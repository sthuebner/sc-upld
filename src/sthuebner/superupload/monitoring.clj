(ns sthuebner.superupload.monitoring
  (:import [java.io InputStream]))


(defn make-inputstream-read-monitor
  "Returns an InputStream wrapping the given stream. read-monitor must
be a function and is called with the number of bytes read in the last
call to InputStream#read."
  [stream read-monitor]
  (proxy [InputStream] []
    (read [buffer]
      (let [bytes-read (.read stream buffer)]
        (read-monitor bytes-read)
        bytes-read))))
