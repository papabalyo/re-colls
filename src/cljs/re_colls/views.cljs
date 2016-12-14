(ns re-colls.views
  (:require [re-frame-datatable.core :as dt]
            [re-colls.subs :as subs]))



(defn main-panel []
  [:div.ui.container
   [:h3.ui.dividing.header "Basic table"]

   [dt/datatable
    :songs
    [::subs/songs-list]
    [{::dt/column-key   [:index]
      ::dt/sorting      {::dt/enabled? true}
      ::dt/th-classes   ["two" "wide"]
      ::dt/column-label "#"}
     {::dt/column-key   [:name]
      ::dt/th-classes   ["ten" "wide"]
      ::dt/column-label "Name"}
     {::dt/column-key   [:duration]
      ::dt/column-label "Duration"
      ::dt/sorting      {::dt/enabled? true}
      ::dt/th-classes   ["four" "wide"]
      :render-fn        (fn [val]
                          [:span
                           (let [m (quot val 60)
                                 s (mod val 60)]
                             (if (zero? m)
                               s
                               (str m ":" (when (< s 10) 0) s)))])}]

    {::dt/pagination    {::dt/enabled? true
                         ::dt/per-page 5}
     ::dt/table-classes ["ui" "table" "celled"]}]])
