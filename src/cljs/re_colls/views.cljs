(ns re-colls.views
  (:require [re-frame.core :as re-frame]
            [re-colls.datatable.core :as datatable]
            [re-colls.subs :as subs]))



(defn main-panel []
  [datatable/datatable
   :songs
   [::subs/songs-list]
   [{:key     :index
     :sorting {:enabled? true}
     :label   "#"}
    {:key   :name
     :label "Name"}
    {:key       :duration
     :label     "Duration"
     :sorting   {:enabled? true}
     :render-fn (fn [val]
                  [:span
                   (let [m (quot val 60)
                         s (mod val 60)]
                     (if (zero? m)
                       s
                       (str m ":" (when (< s 10) 0) s)))])}]])
