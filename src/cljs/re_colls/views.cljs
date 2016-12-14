(ns re-colls.views
  (:require [re-colls.datatable.core :as datatable]
            [re-colls.subs :as subs]))



(defn main-panel []
  [:div.ui.container


   [datatable/datatable
    :songs
    [::subs/songs-list]
    [{::datatable/column-key [:index]
      :sorting               {:enabled? true}
      :css                   {:column "two wide"}
      :label                 "#"}
     {::datatable/column-key [:name]
      :css                   {:column "ten wide"}
      :label                 "Name"}
     {::datatable/column-key [:duration]
      :label                 "Duration"
      :sorting               {:enabled? true}
      :css                   {:column "four wide"}
      :render-fn             (fn [val]
                               [:span
                                (let [m (quot val 60)
                                      s (mod val 60)]
                                  (if (zero? m)
                                    s
                                    (str m ":" (when (< s 10) 0) s)))])}]

    {:pagination {:enabled? true
                  :per-page 5}
     :css        {:table "ui table celled"}}]])
